package com.example.jianpian.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jianpian.data.Movie
import com.example.jianpian.data.MovieDetail
import com.example.jianpian.data.PlayHistory
import com.example.jianpian.network.ApiService
import com.example.jianpian.network.HtmlParser
import com.example.jianpian.data.PlayHistoryManager
import com.example.jianpian.data.Favorite
import com.example.jianpian.data.FavoriteManager
import com.example.jianpian.data.PlayUrlCache
import com.example.jianpian.JianPianApp
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
    
    private val _histories = MutableStateFlow<List<PlayHistory>>(emptyList())
    val histories: StateFlow<List<PlayHistory>> = _histories
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _currentMovie = MutableStateFlow<MovieDetail?>(null)
    val currentMovie: StateFlow<MovieDetail?> = _currentMovie
    
    private val _currentPlayUrl = MutableStateFlow<String>("")
    val currentPlayUrl: StateFlow<String> = _currentPlayUrl
    
    private val _playUrls = MutableStateFlow<Map<String, String>>(emptyMap())
    val playUrls: StateFlow<Map<String, String>> = _playUrls
    
    private val _favorites = MutableStateFlow<List<Favorite>>(emptyList())
    val favorites: StateFlow<List<Favorite>> = _favorites
    
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
    
    private var currentPage = 1
    private var currentKeyword = ""
    private var isLastPage = false
    private var isLoadingNextPage = false

    fun searchMovies(keyword: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                currentKeyword = keyword
                currentPage = 1
                isLastPage = false
                
                Log.d("HomeViewModel", "Searching for: $keyword")
                val response = apiService.searchMovies(keyword)
                Log.d("HomeViewModel", "Response: $response")
                val movies = HtmlParser.parseMovieList(response)
                Log.d("HomeViewModel", "Parsed movies: ${movies.size}")
                
                if (movies.isEmpty()) {
                    isLastPage = true
                }
                
                _movies.value = movies
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error searching movies", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadNextPage() {
        if (isLastPage || isLoadingNextPage || currentKeyword.isEmpty()) {
            return
        }

        viewModelScope.launch {
            try {
                isLoadingNextPage = true
                currentPage++
                
                Log.d("HomeViewModel", "Loading page $currentPage for keyword: $currentKeyword")
                val response = apiService.searchMoviesNextPage(currentKeyword, currentPage)
                val newMovies = HtmlParser.parseMovieList(response)
                Log.d("HomeViewModel", "Loaded ${newMovies.size} new movies")
                
                if (newMovies.isEmpty()) {
                    isLastPage = true
                    Log.d("HomeViewModel", "Reached last page")
                    return@launch
                }
                
                _movies.value = _movies.value + newMovies
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading next page", e)
                currentPage--  // 恢复页码，以便重试
            } finally {
                isLoadingNextPage = false
            }
        }
    }

    fun getMovieDetail(id: String) {
        viewModelScope.launch {
            try {
                if (id.isEmpty()) {
                    Log.e("HomeViewModel", "Cannot get movie detail: ID is empty")
                    return@launch
                }
                
                _isLoading.value = true
                Log.d("HomeViewModel", "Getting movie detail for id: $id")
                val response = apiService.getMovieDetail(id)
                val movieDetail = HtmlParser.parseMovieDetail(response)
                _currentMovie.value = movieDetail
                
                // 先尝试从缓存获取播放链接
                val context = JianPianApp.instance.applicationContext
                val cachedUrls = PlayUrlCache.getUrls(context, id)
                if (cachedUrls != null) {
                    Log.d("HomeViewModel", "Using cached play urls")
                    _playUrls.value = cachedUrls
                    return@launch
                }
                
                // 如果缓存中没有，则获取新的播放链接
                Log.d("HomeViewModel", "Getting new play urls")
                val urls = mutableMapOf<String, String>()
                movieDetail.episodes.forEach { episode ->
                    Log.d("HomeViewModel", "Getting play url for episode: ${episode.name}, url: ${episode.url}")
                    val playUrl = getPlayUrl(episode.url)
                    if (playUrl.isNotEmpty()) {
                        Log.d("HomeViewModel", "Got play url: $playUrl")
                        urls[episode.url] = playUrl
                    } else {
                        Log.e("HomeViewModel", "Failed to get play url for episode: ${episode.name}")
                    }
                }
                
                // 保存到缓存
                PlayUrlCache.saveUrls(context, id, urls)
                _playUrls.value = urls
                
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error getting movie detail for id: $id", e)
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun getPlayUrl(episodeUrl: String): String {
        return try {
            Log.d("HomeViewModel", "Parsing episode ids from url: $episodeUrl")
            val (id, sid, nid) = HtmlParser.parseEpisodeIds(episodeUrl)
            Log.d("HomeViewModel", "Parsed ids: id=$id, sid=$sid, nid=$nid")
            
            if (id.isNotEmpty() && sid.isNotEmpty() && nid.isNotEmpty()) {
                Log.d("HomeViewModel", "Getting play url for id=$id, sid=$sid, nid=$nid")
                val response = apiService.getPlayUrl(id, sid, nid)
                Log.d("HomeViewModel", "Got response: ${response.take(200)}...")
                val playUrl = HtmlParser.parsePlayUrl(response)
                Log.d("HomeViewModel", "Parsed play url: $playUrl")
                _currentPlayUrl.value = playUrl
                playUrl
            } else {
                Log.e("HomeViewModel", "Failed to parse episode ids from url: $episodeUrl")
                ""
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error getting play url for episode: $episodeUrl", e)
            e.printStackTrace()
            ""
        }
    }

    fun loadPlayHistories(context: Context) {
        viewModelScope.launch {
            Log.d("HomeViewModel", "Loading play histories")
            val histories = PlayHistoryManager.getHistories(context)
            Log.d("HomeViewModel", "Loaded ${histories.size} histories")
            
            // 过滤掉无效的历史记录
            val validHistories = histories.filter { 
                it.movie.id.isNotEmpty() && it.movie.title.isNotEmpty()  // 同时检查ID和标题
            }
            Log.d("HomeViewModel", "Valid histories: ${validHistories.size}")
            
            if (validHistories.size != histories.size) {
                Log.d("HomeViewModel", "Filtered out ${histories.size - validHistories.size} invalid histories")
            }
            
            validHistories.forEach { history ->
                Log.d("HomeViewModel", "Valid history: movie=${history.movie.title}, id=${history.movie.id}, episode=${history.episodeName}")
            }
            
            _histories.value = validHistories
            Log.d("HomeViewModel", "Updated histories state")
            
            // 如果没有搜索结果，显示历史记录
            if (_movies.value.isEmpty()) {
                Log.d("HomeViewModel", "No search results, showing histories as movies")
                _movies.value = validHistories.map { it.movie }
            }
        }
    }

    fun savePlayHistory(context: Context, movie: Movie, episodeName: String, episodeUrl: String) {
        viewModelScope.launch {
            if (movie.id.isNotEmpty() && movie.title.isNotEmpty()) {  // 添加标题检查
                Log.d("HomeViewModel", "Saving play history for movie: id=${movie.id}, title=${movie.title}")
                val history = PlayHistory(
                    movie = Movie(
                        id = movie.id,
                        title = movie.title,  // 确保标题被保存
                        coverUrl = movie.coverUrl,
                        description = movie.description
                    ),
                    episodeName = episodeName,
                    episodeUrl = episodeUrl
                )
                PlayHistoryManager.saveHistory(context, history)
                Log.d("HomeViewModel", "Saved history, reloading histories")
                loadPlayHistories(context)
            } else {
                Log.e("HomeViewModel", "Cannot save history: invalid movie data - id=${movie.id}, title=${movie.title}")
            }
        }
    }

    fun clearPlayHistories(context: Context) {
        viewModelScope.launch {
            Log.d("HomeViewModel", "Clearing play histories")
            PlayHistoryManager.clearHistories(context)
            loadPlayHistories(context)  // 重新加载以更新UI
        }
    }

    fun loadFavorites(context: Context) {
        viewModelScope.launch {
            _favorites.value = FavoriteManager.getFavorites(context)
        }
    }

    fun toggleFavorite(context: Context, movie: Movie) {
        viewModelScope.launch {
            if (FavoriteManager.isFavorite(context, movie.id)) {
                FavoriteManager.removeFavorite(context, movie.id)
            } else {
                FavoriteManager.saveFavorite(context, Favorite(movie))
            }
            loadFavorites(context)
        }
    }

    fun isFavorite(context: Context, movieId: String): Boolean {
        return FavoriteManager.isFavorite(context, movieId)
    }

    fun clearFavorites(context: Context) {
        viewModelScope.launch {
            FavoriteManager.clearFavorites(context)
            loadFavorites(context)
        }
    }
} 