package com.example.jianpian.data

data class MovieDetail(
    val id: String,
    val title: String,
    val coverUrl: String,
    val description: String,
    val director: String = "",
    val actors: String = "",
    val genre: String = "",
    val area: String = "",
    val year: String = "",
    val episodes: List<Episode> = emptyList()
)

data class Episode(
    val name: String,
    val url: String
) 