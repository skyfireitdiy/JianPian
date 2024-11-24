package com.example.jianpian.network

import android.util.Log
import com.example.jianpian.data.Movie
import com.example.jianpian.data.MovieDetail
import com.example.jianpian.data.Episode
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object HtmlParser {
    fun parseMovieList(html: String): List<Movie> {
        Log.d("HtmlParser", "Parsing HTML: ${html.take(200)}...")
        val doc: Document = Jsoup.parse(html)
        val elements = doc.select("li.stui-vodlist__item")
        Log.d("HtmlParser", "Found ${elements.size} movie elements")
        
        return elements.mapNotNull { element ->
            try {
                val link = element.select("a.stui-vodlist__thumb").first()
                val titleElement = element.select("h4.stui-vodlist__title a").first()
                
                val id = link?.attr("href")?.substringAfter("/jpvod/")?.substringBefore(".html") ?: ""
                val title = titleElement?.text() ?: link?.attr("title") ?: ""
                val coverUrl = link?.attr("data-original") ?: ""
                
                Log.d("HtmlParser", "Parsing movie element: ${element.html()}")
                Log.d("HtmlParser", "Parsed movie: id=$id, title=$title, coverUrl=$coverUrl")
                
                if (id.isNotEmpty() && title.isNotEmpty()) {
                    Movie(
                        id = id,
                        title = title,
                        coverUrl = coverUrl,
                        description = "",
                        playUrl = ""
                    )
                } else {
                    Log.e("HtmlParser", "Invalid movie data: id=$id, title=$title")
                    null
                }
            } catch (e: Exception) {
                Log.e("HtmlParser", "Error parsing movie element", e)
                null
            }
        }
    }

    fun parseMovieDetail(html: String): MovieDetail {
        Log.d("HtmlParser", "Parsing movie detail HTML: ${html.take(200)}...")
        val doc = Jsoup.parse(html)
        
        // 获取基本信息
        val detail = doc.select(".stui-content__detail").first()
        Log.d("HtmlParser", "Found detail element: ${detail != null}")
        
        // 尝试多种方式获取标题
        var title = ""
        try {
            // 方法1：从h3.title直接获取，去除评分
            title = detail?.select("h3.title")?.first()?.let { element ->
                element.ownText().trim().ifEmpty {
                    element.text().substringBefore("<span").trim()
                }
            } ?: ""
            Log.d("HtmlParser", "Method 1 - Title from h3.title: $title")
            
            // 如果标题为空，尝试方法2
            if (title.isEmpty()) {
                // 方法2：从页面标题获取
                title = doc.title()
                    .substringBefore("详情介绍")
                    .substringBefore("在线观看")
                    .trim()
                Log.d("HtmlParser", "Method 2 - Title from page title: $title")
            }
            
            // 如果还是空，尝试方法3
            if (title.isEmpty()) {
                // 方法3：从简介中获取
                title = doc.select(".stui-content__desc").first()?.ownText()
                    ?.substringBefore("剧情：")
                    ?.substringBefore("是由")
                    ?.trim() ?: ""
                Log.d("HtmlParser", "Method 3 - Title from description: $title")
            }
            
            Log.d("HtmlParser", "Final title: $title")
        } catch (e: Exception) {
            Log.e("HtmlParser", "Error parsing title", e)
        }
        
        // 获取封面图片
        val coverUrl = try {
            doc.select(".stui-content__thumb img.img-responsive").attr("data-original")
                .ifEmpty { doc.select(".stui-content__thumb img").attr("src") }
                .also { Log.d("HtmlParser", "Found cover url: $it") }
        } catch (e: Exception) {
            Log.e("HtmlParser", "Error parsing cover url", e)
            ""
        }
        
        // 获取详细信息
        val info = detail?.select(".data")
        var director = ""
        var actors = ""
        var genre = ""
        var area = ""
        var year = ""
        
        try {
            info?.forEach { element ->
                val text = element.text()
                Log.d("HtmlParser", "Processing info text: $text")
                when {
                    text.contains("导演：") -> director = text.substringAfter("导演：")
                    text.contains("主演：") -> actors = text.substringAfter("主演：")
                    text.contains("类型：") -> genre = text.substringAfter("类型：")
                    text.contains("地区：") -> area = text.substringAfter("地区：")
                    text.contains("年份：") -> year = text.substringAfter("年份：")
                }
            }
        } catch (e: Exception) {
            Log.e("HtmlParser", "Error parsing info", e)
        }
        
        // 获取简介
        val description = try {
            doc.select(".desc").text()
                .ifEmpty { doc.select(".stui-content__desc").text() }
                .also { Log.d("HtmlParser", "Found description: $it") }
        } catch (e: Exception) {
            Log.e("HtmlParser", "Error parsing description", e)
            ""
        }
        
        // 获取播放列表
        val episodes = try {
            val playlistElements = doc.select(".stui-content__playlist li a")
            Log.d("HtmlParser", "Found ${playlistElements.size} episodes")
            
            playlistElements.map { element ->
                val name = element.text()
                val url = element.attr("href")
                Log.d("HtmlParser", "Episode: name=$name, url=$url")
                Episode(name = name, url = url)
            }
        } catch (e: Exception) {
            Log.e("HtmlParser", "Error parsing episodes", e)
            emptyList()
        }
        
        // 从播放列表链接中提取ID
        val id = episodes.firstOrNull()?.url
            ?.substringAfter("/jpplay/")
            ?.substringBefore("-")
            ?: ""
        Log.d("HtmlParser", "Extracted ID: $id")
        
        return MovieDetail(
            id = id,
            title = title,
            coverUrl = coverUrl,
            description = description,
            director = director,
            actors = actors,
            genre = genre,
            area = area,
            year = year,
            episodes = episodes
        ).also {
            Log.d("HtmlParser", "Created MovieDetail: $it")
            Log.d("HtmlParser", "Title in final MovieDetail: ${it.title}")
        }
    }

    fun parsePlayUrl(html: String): String {
        val doc = Jsoup.parse(html)
        // 查找包含播放配置的脚本
        val script = doc.select("script").find { it.data().contains("player_aaaa") }
        val scriptData = script?.data() ?: ""
        
        // 使用正则表达式提取 url
        val urlPattern = "\"url\":\"(.*?)\"".toRegex()
        val match = urlPattern.find(scriptData)
        val url = match?.groupValues?.get(1)?.replace("\\/", "/") ?: ""
        
        Log.d("HtmlParser", "Parsed play url: $url")
        return url
    }

    fun parseEpisodeIds(url: String): Triple<String, String, String> {
        Log.d("HtmlParser", "Parsing episode ids from url: $url")
        val pattern = "/jpplay/(\\d+)-(\\d+)-(\\d+)\\.html".toRegex()
        val match = pattern.find(url)
        return if (match != null) {
            val (id, sid, nid) = match.destructured
            Log.d("HtmlParser", "Parsed ids: id=$id, sid=$sid, nid=$nid")
            Triple(id, sid, nid)
        } else {
            Log.e("HtmlParser", "Failed to parse episode ids from url: $url")
            Triple("", "", "")
        }
    }

    fun parseHotMovies(html: String): List<Movie> {
        Log.d("HtmlParser", "Parsing hot movies from HTML")
        val doc = Jsoup.parse(html)
        val elements = doc.select("#home0 li.stui-vodlist__item")
        Log.d("HtmlParser", "Found ${elements.size} hot movie elements")
        
        return elements.mapNotNull { element ->
            try {
                val link = element.select("a.stui-vodlist__thumb").first()
                val titleElement = element.select("h4.stui-vodlist__title a").first()
                
                val id = link?.attr("href")?.substringAfter("/jpvod/")?.substringBefore(".html") ?: ""
                val title = titleElement?.text() ?: link?.attr("title") ?: ""
                val coverUrl = link?.attr("data-original") ?: ""
                
                Log.d("HtmlParser", "Parsed hot movie: id=$id, title=$title")
                
                if (id.isNotEmpty() && title.isNotEmpty()) {
                    Movie(
                        id = id,
                        title = title,
                        coverUrl = coverUrl
                    )
                } else {
                    Log.e("HtmlParser", "Invalid hot movie data: id=$id, title=$title")
                    null
                }
            } catch (e: Exception) {
                Log.e("HtmlParser", "Error parsing hot movie element", e)
                null
            }
        }
    }
} 