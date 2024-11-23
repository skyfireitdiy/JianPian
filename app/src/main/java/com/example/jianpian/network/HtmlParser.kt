package com.example.jianpian.network

import android.util.Log
import com.example.jianpian.data.Movie
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
} 