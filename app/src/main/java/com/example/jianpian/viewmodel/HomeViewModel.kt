package com.example.jianpian.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jianpian.data.Movie
import com.example.jianpian.data.MovieDetail
import com.example.jianpian.network.ApiService
import com.example.jianpian.network.HtmlParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

class HomeViewModel : ViewModel() {
    private val _movies = MutableStateFlow<List<Movie>>(emptyList())
    val movies: StateFlow<List<Movie>> = _movies
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _currentMovie = MutableStateFlow<MovieDetail?>(null)
    val currentMovie: StateFlow<MovieDetail?> = _currentMovie
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    
    private val apiService = Retrofit.Builder()
        .baseUrl("https://vodjp.com/")
        .client(okHttpClient)
        .addConverterFactory(ScalarsConverterFactory.create())
        .build()
        .create(ApiService::class.java)
    
    fun searchMovies(keyword: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d("HomeViewModel", "Searching for: $keyword")
                val response = apiService.searchMovies(keyword)
                Log.d("HomeViewModel", "Response: $response")
                val movies = HtmlParser.parseMovieList(response)
                Log.d("HomeViewModel", "Parsed movies: ${movies.size}")
                movies.forEach { movie ->
                    Log.d("HomeViewModel", "Movie: ${movie.title}, Cover: ${movie.coverUrl}")
                }
                _movies.value = movies
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error searching movies", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getMovieDetail(id: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = apiService.getMovieDetail(id)
                val movieDetail = HtmlParser.parseMovieDetail(response)
                _currentMovie.value = movieDetail
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error getting movie detail", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
} 