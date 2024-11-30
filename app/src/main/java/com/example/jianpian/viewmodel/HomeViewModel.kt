package com.example.jianpian.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jianpian.data.*
import com.example.jianpian.network.ApiService
import com.example.jianpian.network.HtmlParser
import com.example.jianpian.data.PlayHistory
import com.example.jianpian.data.Movie
import com.example.jianpian.data.MovieDetail
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
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager
import java.security.SecureRandom
import java.security.cert.X509Certificate
import com.example.jianpian.data.CategoryFilters

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
    
    private val _cacheProgress = MutableStateFlow(0)
    val cacheProgressFlow: StateFlow<Int> = _cacheProgress
    
    private val _hotMovies = MutableStateFlow<List<Movie>>(emptyList())
    val hotMovies: StateFlow<List<Movie>> = _hotMovies
    
    private val _showSubCategories = MutableStateFlow(false)
    val showSubCategories: StateFlow<Boolean> = _showSubCategories
    
    private val _categoryFilters = MutableStateFlow(CategoryFilters())
    val categoryFilters: StateFlow<CategoryFilters> = _categoryFilters
    
    val movieSubCategories = listOf(
        "动作片", "喜剧片", "爱情片", "科幻片", 
        "恐怖片", "剧情片", "战争片", "纪录片", 
        "动画片", "4K电影"
    )
    
    val regions = listOf(
        "大陆", "香港", "台湾", "日本", "韩国", 
        "泰国", "美国", "法国", "英国", "德国", 
        "印度", "其他"
    )
    
    val years = listOf(
        "2024", "2023", "2022", "2021", "2020",
        "2019", "2018", "2017", "2016", "2015",
        "2014", "2013", "2012"
    )
    
    val languages = listOf(
        "国语", "粤语", "英语", "日语", "韩语",
        "法语", "德语", "西班牙语", "俄语", "泰语", "其他"
    )
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .hostnameVerifier { _, _ -> true }
        .sslSocketFactory(
            TrustAllCerts.createSSLSocketFactory(),
            TrustAllCerts.trustAllManager
        )
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
    
    private var currentCategory = 0
    private var categoryPage = 1
    private var isCategoryLastPage = false
    private var isLoadingCategoryNextPage = false

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
                
                // 如果缓存中没有，则开始缓存
                _cacheProgress.value = 0
                val urls = mutableMapOf<String, String>()
                movieDetail.episodes.forEachIndexed { index, episode ->
                    val playUrl = getPlayUrl(episode.url)
                    if (playUrl.isNotEmpty()) {
                        urls[episode.url] = playUrl
                    }
                    _cacheProgress.value = index + 1
                }
                
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
            try {
                Log.d("HomeViewModel", "Loading play histories")
                val histories = PlayHistoryManager.getHistories(context)
                Log.d("HomeViewModel", "Loaded ${histories.size} histories")
                
                // 过滤掉无效的历史记录
                val validHistories = histories.filter { history -> 
                    try {
                        // 检查所有必需字段是否有效
                        !history.movieDetailId.isNullOrEmpty() &&
                        !history.movieTitle.isNullOrEmpty() &&
                        !history.movieCoverUrl.isNullOrEmpty() &&
                        !history.episodeName.isNullOrEmpty() &&
                        !history.episodeUrl.isNullOrEmpty()
                    } catch (e: Exception) {
                        Log.e("HomeViewModel", "Error checking history validity", e)
                        false
                    }
                }
                Log.d("HomeViewModel", "Valid histories: ${validHistories.size}")
                
                if (validHistories.size != histories.size) {
                    Log.d("HomeViewModel", "Filtered out ${histories.size - validHistories.size} invalid histories")
                    // 清除无效的历史记录
                    PlayHistoryManager.saveHistories(context, validHistories)
                }
                
                validHistories.forEach { history ->
                    Log.d("HomeViewModel", "Valid history: movie=${history.movieTitle}, id=${history.movieDetailId}, episode=${history.episodeName}")
                }
                
                _histories.value = validHistories
                Log.d("HomeViewModel", "Updated histories state")
                
                // 如果没有搜索结果，显示历史记录
                if (_movies.value.isEmpty()) {
                    Log.d("HomeViewModel", "No search results, showing histories as movies")
                    _movies.value = validHistories.map { history ->
                        Movie(
                            id = history.movieDetailId,
                            title = history.movieTitle,
                            coverUrl = history.movieCoverUrl,
                            description = ""
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading play histories", e)
            }
        }
    }

    fun savePlayHistory(context: Context, movieDetailId: String, episodeName: String, episodeUrl: String, playbackPosition: Long = 0) {
        viewModelScope.launch {
            if (movieDetailId.isNotEmpty()) {
                val movieDetail = _currentMovie.value
                if (movieDetail != null) {
                    Log.d("HomeViewModel", "Saving play history for movie: id=$movieDetailId, title=${movieDetail.title}")
                    val history = PlayHistory(
                        movieDetailId = movieDetailId,
                        movieTitle = movieDetail.title,
                        movieCoverUrl = movieDetail.coverUrl,
                        episodeName = episodeName,
                        episodeUrl = episodeUrl,
                        playbackPosition = playbackPosition
                    )
                    PlayHistoryManager.saveHistory(context, history)
                    
                    // 更新内存中的历史记录列表，使用与 PlayHistoryManager 相同的去重逻辑
                    _histories.value = PlayHistoryManager.getHistories(context)
                    
                    Log.d("HomeViewModel", "Updated histories in memory")
                } else {
                    Log.e("HomeViewModel", "Cannot save history: movie detail not available")
                }
            } else {
                Log.e("HomeViewModel", "Cannot save history: invalid movie id")
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

    fun refreshPlayUrls(context: Context, movieDetail: MovieDetail) {
        viewModelScope.launch {
            _cacheProgress.value = 0
            val urls = mutableMapOf<String, String>()
            
            movieDetail.episodes.forEachIndexed { index, episode ->
                val playUrl = getPlayUrl(episode.url)
                if (playUrl.isNotEmpty()) {
                    urls[episode.url] = playUrl
                }
                _cacheProgress.value = index + 1
            }
            
            PlayUrlCache.saveUrls(context, movieDetail.id, urls)
            _playUrls.value = urls
        }
    }

    fun loadHotMovies() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = apiService.getHomePage()
                val movies = HtmlParser.parseHotMovies(response)
                _hotMovies.value = movies
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading hot movies", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deletePlayHistory(context: Context, movieDetailId: String) {
        viewModelScope.launch {
            PlayHistoryManager.deleteHistory(context, movieDetailId)
            // 重新加载历史记录以更新UI
            loadPlayHistories(context)
        }
    }

    fun clearSearchResults() {
        _movies.value = emptyList()
    }

    fun searchByCategory(categoryId: Int) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _showSubCategories.value = true
                currentCategory = categoryId
                categoryPage = 1
                isCategoryLastPage = false
                
                val response = apiService.getCategoryPage(categoryId)
                // 解析过滤选项
                _categoryFilters.value = HtmlParser.parseCategoryFilters(response)
                // 解析电影列表
                val movies = HtmlParser.parseMovieList(response)
                _movies.value = movies
                
                if (movies.isEmpty()) {
                    isCategoryLastPage = true
                }
                
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading category $categoryId", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadCategoryNextPage() {
        if (isCategoryLastPage || isLoadingCategoryNextPage || currentCategory == 0) {
            return
        }

        viewModelScope.launch {
            try {
                isLoadingCategoryNextPage = true
                categoryPage++
                
                Log.d("HomeViewModel", "Loading category $currentCategory page $categoryPage")
                val response = apiService.getCategoryNextPage(currentCategory, categoryPage)
                val newMovies = HtmlParser.parseMovieList(response)
                Log.d("HomeViewModel", "Loaded ${newMovies.size} new movies")
                
                if (newMovies.isEmpty()) {
                    isCategoryLastPage = true
                    Log.d("HomeViewModel", "Reached last page")
                    return@launch
                }
                
                _movies.value = _movies.value + newMovies
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading next page", e)
                categoryPage--  // 恢复页码，以便重试
            } finally {
                isLoadingCategoryNextPage = false
            }
        }
    }

    fun handleFilterClick(filterItem: FilterItem) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // 从 URL 中提取参数并请求新的页面
                val response = apiService.getFilterPage(filterItem.url)
                val movies = HtmlParser.parseMovieList(response)
                _movies.value = movies
                
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error handling filter click", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}

private object TrustAllCerts {
    val trustAllManager = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }

    fun createSSLSocketFactory(): SSLSocketFactory {
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(trustAllManager), SecureRandom())
        return sslContext.socketFactory
    }
} 