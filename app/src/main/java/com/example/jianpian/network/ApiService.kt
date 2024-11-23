package com.example.jianpian.network

import retrofit2.http.POST
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded

interface ApiService {
    @FormUrlEncoded
    @POST("jpsearch/-------------.html")
    suspend fun searchMovies(@Field("wd") keyword: String): String
} 