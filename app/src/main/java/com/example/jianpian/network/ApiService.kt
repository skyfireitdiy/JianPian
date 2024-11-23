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
} 