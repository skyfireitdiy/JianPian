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
    
    private val _currentPlayUrl = MutableStateFlow<String>("")
    val currentPlayUrl: StateFlow<String> = _currentPlayUrl
    
    private val _playUrls = MutableStateFlow<Map<String, String>>(emptyMap())
    val playUrls: StateFlow<Map<String, String>> = _playUrls
    
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
                
                // 预加载所有播放链接
                val urls = mutableMapOf<String, String>()
                movieDetail.episodes.forEach { episode ->
                    val playUrl = getPlayUrl(episode.url)
                    if (playUrl.isNotEmpty()) {
                        urls[episode.url] = playUrl
                    }
                }
                _playUrls.value = urls
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error getting movie detail", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun getPlayUrl(episodeUrl: String): String {
        return try {
            val (id, sid, nid) = HtmlParser.parseEpisodeIds(episodeUrl)
            if (id.isNotEmpty() && sid.isNotEmpty() && nid.isNotEmpty()) {
                val response = apiService.getPlayUrl(id, sid, nid)
                val playUrl = HtmlParser.parsePlayUrl(response)
                _currentPlayUrl.value = playUrl
                playUrl
            } else {
                ""
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error getting play url", e)
            ""
        }
    }
} 