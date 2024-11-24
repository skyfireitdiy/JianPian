package com.example.jianpian.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.*
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items
import androidx.tv.foundation.lazy.grid.TvGridItemSpan
import androidx.compose.ui.platform.LocalContext
import com.example.jianpian.data.Movie
import com.example.jianpian.data.Episode
import com.example.jianpian.data.MovieDetail
import com.example.jianpian.viewmodel.HomeViewModel
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import android.util.Log
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.activity.compose.BackHandler
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.window.PopupProperties
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.coroutineScope

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MovieCard(
    movie: Movie,
    subtitle: String? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier
) {
    var isImageLoading by remember { mutableStateOf(true) }
    var isImageError by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .width(160.dp)
            .height(240.dp),
        onClick = onClick,
        onLongClick = onLongClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AsyncImage(
                    model = movie.coverUrl,
                    contentDescription = movie.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onLoading = { isImageLoading = true },
                    onSuccess = { isImageLoading = false },
                    onError = {
                        isImageLoading = false
                        isImageError = true
                        Log.e("MovieCard", "Error loading image: ${movie.coverUrl}", it.result.throwable)
                    }
                )
                
                if (isImageLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("加载中...", color = Color.White)
                    }
                }
                
                if (isImageError) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("加载失败", color = Color.Red)
                    }
                }
            }
            
            Text(
                text = movie.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(8.dp),
                textAlign = TextAlign.Center,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(4.dp),
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val movies by viewModel.movies.collectAsState()
    val histories by viewModel.histories.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentMovie by viewModel.currentMovie.collectAsState()
    val hotMovies by viewModel.hotMovies.collectAsState()
    var showDetail by remember { mutableStateOf(false) }
    var currentEpisode by remember { mutableStateOf<Episode?>(null) }
    
    // 添加菜单相关的状态
    var showMenu by remember { mutableStateOf(false) }
    var selectedHistoryId by remember { mutableStateOf("") }
    
    // 添加焦点请求器和状态
    val searchFocusRequester = remember { FocusRequester() }
    val historyFocusRequester = remember { FocusRequester() }
    val favoriteFocusRequester = remember { FocusRequester() }
    var initialFocusSet by remember { mutableStateOf(false) }
    
    // 添加数据加载状态追踪
    var historiesLoaded by remember { mutableStateOf(false) }
    var favoritesLoaded by remember { mutableStateOf(false) }
    var hotMoviesLoaded by remember { mutableStateOf(false) }
    
    // 加载数据
    LaunchedEffect(Unit) {
        viewModel.loadPlayHistories(context)
        viewModel.loadFavorites(context)
        viewModel.loadHotMovies()
    }
    
    // 监听数据加载状态
    LaunchedEffect(histories) {
        if (histories.isNotEmpty()) {
            historiesLoaded = true
        }
    }
    
    LaunchedEffect(favorites) {
        if (favorites.isNotEmpty()) {
            favoritesLoaded = true
        }
    }
    
    LaunchedEffect(hotMovies) {
        if (hotMovies.isNotEmpty()) {
            hotMoviesLoaded = true
        }
    }
    
    // 处理初始焦点和从详情页返回时的焦点
    LaunchedEffect(historiesLoaded, favoritesLoaded, hotMoviesLoaded, showDetail) {
        if (!showDetail && !initialFocusSet) {
            delay(500) // 给UI一些时间来初始化
            try {
                if (historiesLoaded) {
                    historyFocusRequester.requestFocus()
                    Log.d("HomeScreen", "Set focus to history")
                } else if (favoritesLoaded) {
                    favoriteFocusRequester.requestFocus()
                    Log.d("HomeScreen", "Set focus to favorite")
                } else {
                    searchFocusRequester.requestFocus()
                    Log.d("HomeScreen", "Set focus to search")
                }
                initialFocusSet = true
            } catch (e: Exception) {
                Log.e("HomeScreen", "Error setting focus", e)
            }
        }
    }
    
    LaunchedEffect(currentEpisode) {
        if (currentEpisode != null && currentMovie != null) {
            viewModel.savePlayHistory(
                context,
                movieDetailId = currentMovie!!.id,
                episodeName = currentEpisode!!.name,
                episodeUrl = currentEpisode!!.url
            )
        }
    }
    
    LaunchedEffect(currentMovie) {
        if (searchQuery.isEmpty() && currentMovie != null) {
            histories.find { it.movieDetailId == currentMovie!!.id }?.let { history ->
                currentMovie!!.episodes.find { it.url == history.episodeUrl }?.let { episode ->
                    currentEpisode = episode
                }
            }
        }
    }
    
    BackHandler {
        when {
            showMenu -> {
                showMenu = false
            }
            currentEpisode != null -> {
                currentEpisode = null
                showDetail = true
            }
            showDetail -> {
                showDetail = false
                initialFocusSet = false  // 重置焦点状态
            }
            searchQuery.isNotEmpty() -> {
                searchQuery = ""
                viewModel.clearSearchResults()
            }
            else -> {
                onBackPressed()
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            currentEpisode != null && currentMovie != null -> {
                VideoPlayerScreen(
                    movieDetail = currentMovie!!,
                    currentEpisode = currentEpisode!!,
                    onPreviousEpisode = {
                        val index = currentMovie!!.episodes.indexOf(currentEpisode)
                        if (index > 0) {
                            currentEpisode = currentMovie!!.episodes[index - 1]
                        }
                    },
                    onNextEpisode = {
                        val index = currentMovie!!.episodes.indexOf(currentEpisode)
                        if (index < currentMovie!!.episodes.size - 1) {
                            currentEpisode = currentMovie!!.episodes[index + 1]
                        }
                    },
                    onBackClick = {
                        // 返回到详情页而不是直接退出
                        currentEpisode = null
                        showDetail = true
                    }
                )
            }
            showDetail && currentMovie != null -> {
                DetailScreen(
                    movieDetail = currentMovie!!,
                    onPlayClick = { episode ->
                        currentEpisode = episode
                    },
                    onBackClick = {
                        // 返回到列表页
                        showDetail = false
                    }
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Search bar
                    var isSearchFocused by remember { mutableStateOf(false) }
    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (isSearchFocused) Color.DarkGray else Color.DarkGray.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = if (isSearchFocused) 2.dp else 0.dp,
                                color = if (isSearchFocused) Color.White else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .onFocusChanged { 
                                isSearchFocused = it.isFocused
                            }
                    ) {
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .focusRequester(searchFocusRequester)
                                .focusable(),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "搜索电影...",
                                            color = if (isSearchFocused) Color.LightGray else Color.Gray
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = Color.White,
                                fontSize = 16.sp
                            ),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    viewModel.searchMovies(searchQuery)
                                }
                            ),
                            cursorBrush = SolidColor(Color.White)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("加载中...", color = Color.White)
                        }
                    } else {
                        TvLazyVerticalGrid(
                            columns = TvGridCells.Fixed(6),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 16.dp
                            ),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // 根 searchQuery 显示不的内容
                            if (searchQuery.isEmpty()) {
                                // 播放历史标题
                                if (histories.isNotEmpty()) {
                                    item(span = { TvGridItemSpan(6) }) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "播放历史",
                                                fontSize = 20.sp,
                                                color = Color.White
                                            )
                                            
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Button(
                                                    onClick = {
                                                        histories.firstOrNull()?.let { history ->
                                                            viewModel.getMovieDetail(history.movieDetailId)
                                                            showDetail = true
                                                        }
                                                    }
                                                ) {
                                                    Text("继续观看")
                                                }
                                                
                                                Button(
                                                    onClick = {
                                                        viewModel.clearPlayHistories(context)
                                                    }
                                                ) {
                                                    Text("清除历史")
                                                }
                                            }
                                        }
                                    }
                
                                    // 播放历史内容
                                    items(histories) { history ->
                                        Box {
                                            MovieCard(
                                                movie = Movie(
                                                    id = history.movieDetailId,
                                                    title = history.movieTitle,
                                                    coverUrl = history.movieCoverUrl,
                                                    description = ""
                                                ),
                                                subtitle = history.episodeName,
                                                onClick = {
                                                    Log.d("HomeScreen", "MovieCard onClick - ${history.movieTitle}")
                                                    if (showMenu) {
                                                        Log.d("HomeScreen", "Closing menu - ${history.movieTitle}")
                                                        showMenu = false
                                                    } else {
                                                        Log.d("HomeScreen", "Opening detail - ${history.movieTitle}")
                                                        viewModel.getMovieDetail(history.movieDetailId)
                                                        showDetail = true
                                                    }
                                                },
                                                onLongClick = {
                                                    Log.d("HomeScreen", "MovieCard onLongClick - ${history.movieTitle}")
                                                    selectedHistoryId = history.movieDetailId
                                                    showMenu = true
                                                },
                                                modifier = if (histories.indexOf(history) == 0) {
                                                    Log.d("HomeScreen", "Adding focus requester to first history item")
                                                    Modifier.focusRequester(historyFocusRequester)
                                                } else {
                                                    Modifier
                                                }
                                            )

                                            if (showMenu && selectedHistoryId == history.movieDetailId) {
                                                var menuFocused by remember { mutableStateOf(false) }
                                                var isFirstClick by remember { mutableStateOf(true) }
                                                
                                                DropdownMenu(
                                                    expanded = true,
                                                    onDismissRequest = { showMenu = false },
                                                    properties = PopupProperties(
                                                        focusable = true,
                                                        dismissOnBackPress = true,
                                                        dismissOnClickOutside = true
                                                    )
                                                ) {
                                                    DropdownMenuItem(
                                                        modifier = Modifier
                                                            .background(
                                                                color = if (menuFocused) Color.Red.copy(alpha = 0.2f) else Color.Transparent,
                                                                shape = RoundedCornerShape(4.dp)
                                                            )
                                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                                            .onFocusChanged { 
                                                                menuFocused = it.isFocused
                                                                Log.d("HomeScreen", "Menu focus changed: ${it.isFocused}")
                                                            },
                                                        text = { 
                                                            Text(
                                                                "删除",
                                                                color = if (menuFocused) Color.White else Color.Gray
                                                            ) 
                                                        },
                                                        onClick = {
                                                            Log.d("HomeScreen", "Menu item clicked - Delete ${history.movieTitle}, isFirstClick: $isFirstClick")
                                                            if (isFirstClick) {
                                                                isFirstClick = false
                                                                Log.d("HomeScreen", "Skipping first click")
                                                            } else if (menuFocused) {
                                                                viewModel.deletePlayHistory(context, history.movieDetailId)
                                                                showMenu = false
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                
                                // 收藏列表标题
                                if (favorites.isNotEmpty()) {
                                    item(span = { TvGridItemSpan(6) }) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "我的收藏",
                                                fontSize = 20.sp,
                                                color = Color.White
                                            )
                                            
                                            Button(
                                                onClick = {
                                                    viewModel.clearFavorites(context)
                                                }
                                            ) {
                                                Text("清除收藏")
                                            }
                                        }
                                    }
                
                                    // 收藏列表内容
                                    items(favorites) { favorite ->
                                        MovieCard(
                                            movie = favorite.movie,
                                            onClick = {
                                                viewModel.getMovieDetail(favorite.movie.id)
                                                showDetail = true
                                            },
                                            onLongClick = {
                                                selectedHistoryId = favorite.movie.id
                                                showMenu = true
                                            },
                                            modifier = if (favorites.indexOf(favorite) == 0) {
                                                Modifier.focusRequester(favoriteFocusRequester)
                                            } else {
                                                Modifier
                                            }
                                        )
                                    }
                                }
                
                                // 最近热播
                                if (hotMovies.isNotEmpty()) {
                                    item(span = { TvGridItemSpan(6) }) {
                                        Text(
                                            text = "最近热播",
                                            fontSize = 20.sp,
                                            color = Color.White,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                
                                    items(hotMovies) { movie ->
                                        MovieCard(
                                            movie = movie,
                                            onClick = {
                                                viewModel.getMovieDetail(movie.id)
                                                showDetail = true
                                            },
                                            onLongClick = {
                                                selectedHistoryId = movie.id
                                                showMenu = true
                                            },
                                            modifier = Modifier
                                        )
                                    }
                                }
                            } else {
                                // 搜索结果标题（可选）
                                item(span = { TvGridItemSpan(6) }) {
                                    Text(
                                        text = "搜索结果",
                                        fontSize = 20.sp,
                                        color = Color.White,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                
                                // 搜索结果内容
                                items(movies) { movie ->
                                    MovieCard(
                                        movie = movie,
                                        onClick = {
                                            viewModel.getMovieDetail(movie.id)
                                            showDetail = true
                                        },
                                        onLongClick = {
                                            selectedHistoryId = movie.id
                                            showMenu = true
                                        },
                                        modifier = Modifier.then(
                                            if (movies.indexOf(movie) == 0) 
                                                Modifier.focusRequester(favoriteFocusRequester)
                                            else 
                                                Modifier
                                        )
                                    )
                                }
                            }

                            // 加载更多
                            item(span = { TvGridItemSpan(6) }) {
                                if (searchQuery.isNotEmpty() && !isLoading) {
                                    LaunchedEffect(Unit) {
                                        viewModel.loadNextPage()
                                    }
                                }
                                if (isLoading) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("加载中...", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
} 