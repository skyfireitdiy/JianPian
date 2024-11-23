package com.example.jianpian.data

data class Movie(
    val id: String,
    val title: String,
    val coverUrl: String,
    val description: String = "",
    val playUrl: String = ""
) 