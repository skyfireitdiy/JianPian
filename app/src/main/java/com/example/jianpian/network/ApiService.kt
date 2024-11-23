package com.example.jianpian.network

import retrofit2.http.POST
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {
    @FormUrlEncoded
    @POST("jpsearch/-------------.html")
    suspend fun searchMovies(@Field("wd") keyword: String): String
    
    @GET("jpvod/{id}.html")
    suspend fun getMovieDetail(@Path("id") id: String): String
} 