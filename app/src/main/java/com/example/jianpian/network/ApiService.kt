package com.example.jianpian.network

import retrofit2.http.POST
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Url

interface ApiService {
    @FormUrlEncoded
    @POST("jpsearch/-------------.html")
    suspend fun searchMovies(@Field("wd") keyword: String): String
    
    @GET("jpsearch/{keyword}----------{page}---.html")
    suspend fun searchMoviesNextPage(
        @Path("keyword") keyword: String,
        @Path("page") page: Int
    ): String
    
    @GET("jpvod/{id}.html")
    suspend fun getMovieDetail(@Path("id") id: String): String

    @GET("jpplay/{id}-{sid}-{nid}.html")
    suspend fun getPlayUrl(
        @Path("id") id: String,
        @Path("sid") sid: String,
        @Path("nid") nid: String
    ): String
    
    @GET("/")
    suspend fun getHomePage(): String

    @GET("jplb/{category}--------{page}---.html")
    suspend fun getCategoryMovies(
        @Path("category") category: Int,
        @Path("page") page: Int
    ): String

    @GET("jplist/{category}.html")
    suspend fun getCategoryPage(@Path("category") category: Int): String

    @GET
    suspend fun getFilterPage(@Url url: String): String

    @GET("jplist/{category}-{page}.html")
    suspend fun getCategoryNextPage(
        @Path("category") category: Int,
        @Path("page") page: Int
    ): String
} 