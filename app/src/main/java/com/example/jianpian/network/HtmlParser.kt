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
        
        return elements.map { element ->
            val link = element.select("a.stui-vodlist__thumb").first()
            val id = link?.attr("href")?.substringAfter("/jpvod/")?.substringBefore(".html") ?: ""
            val title = link?.attr("title") ?: ""
            val coverUrl = link?.attr("data-original") ?: ""
            
            Log.d("HtmlParser", "Parsed movie: id=$id, title=$title, coverUrl=$coverUrl")
            
            Movie(
                id = id,
                title = title,
                coverUrl = coverUrl,
                description = "",
                playUrl = ""
            )
        }
    }

    fun parseMovieDetail(html: String): MovieDetail {
        val doc: Document = Jsoup.parse(html)
        
        // 获取基本信息
        val detail = doc.select(".stui-content__detail").first()
        val title = detail?.select("h1.title")?.text() ?: ""
        val coverUrl = doc.select(".stui-content__thumb img").attr("data-original")
        
        // 获取详细信息
        val info = detail?.select(".data")
        var director = ""
        var actors = ""
        var genre = ""
        var area = ""
        var year = ""
        
        info?.forEach { element ->
            val text = element.text()
            when {
                text.startsWith("导演：") -> director = text.substringAfter("导演：")
                text.startsWith("主演：") -> actors = text.substringAfter("主演：")
                text.startsWith("类型：") -> genre = text.substringAfter("类型：")
                text.startsWith("地区：") -> area = text.substringAfter("地区：")
                text.startsWith("年份：") -> year = text.substringAfter("年份：")
            }
        }
        
        // 获取简介
        val description = doc.select(".desc").text()
        
        // 获取播放列表
        val episodes = doc.select(".stui-content__playlist li a").map { element ->
            Episode(
                name = element.text(),
                url = element.attr("href")
            )
        }
        
        Log.d("HtmlParser", "Parsed movie detail: $title")
        Log.d("HtmlParser", "Episodes: ${episodes.size}")
        
        return MovieDetail(
            id = "",  // 从URL中提取
            title = title,
            coverUrl = coverUrl,
            description = description,
            director = director,
            actors = actors,
            genre = genre,
            area = area,
            year = year,
            episodes = episodes
        )
    }
} 