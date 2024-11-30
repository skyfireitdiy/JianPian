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
import com.example.jianpian.data.*
import com.example.jianpian.viewmodel.HomeViewModel
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import android.util.Log
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.activity.compose.BackHandler
import androidx.compose.ui.window.PopupProperties
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.input.key.*
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.delay

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
fun CategorySection(
    title: String,
    items: List<FilterItem>,
    onItemClick: (FilterItem) -> Unit
) {
    if (items.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        TvLazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            items(items) { item ->
                Button(
                    onClick = { onItemClick(item) },
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(item.name, fontSize = 14.sp)
                }
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
    var isSearchFocused by remember { mutableStateOf(false) }
    val movies = viewModel.movies.collectAsState().value
    val histories = viewModel.histories.collectAsState().value
    val favorites = viewModel.favorites.collectAsState().value
    val isLoading = viewModel.isLoading.collectAsState().value
    val currentMovie = viewModel.currentMovie.collectAsState().value
    val hotMovies = viewModel.hotMovies.collectAsState().value
    val showSubCategories = viewModel.showSubCategories.collectAsState().value
    val categoryFilters = viewModel.categoryFilters.collectAsState().value
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
            showSubCategories -> {  // 添加这个条件
                viewModel.resetCategoryPage()  // 使用 ViewModel 中的方法来重置状态
            }
            else -> {
                onBackPressed()
            }
        }
    }
    
    when {
        // 将视频播放器和详情页移到最外层的条件渲染中
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
                    showDetail = false
                }
            )
        }
        else -> {
            // 主页面内容
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 顶部固定部分
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // 分类按钮行
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            listOf("电影", "电视剧", "综艺", "动漫").forEachIndexed { index, category ->
                                Button(
                                    onClick = {
                                        viewModel.searchByCategory(index + 1)
                                    },
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Text(category)
                                }
                            }
                        }

                        // 搜索栏
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
                    }

                    // 可滚动的内容部分
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)  // 添加这行，让内容区域占据剩余空间
                    ) {
                        if (showSubCategories) {
                            // 子分类布局
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
                                // 分类部分
                                item(span = { TvGridItemSpan(6) }) {
                                    Column {
                                        CategorySection(
                                            title = "按类型",
                                            items = categoryFilters.types,
                                            onItemClick = { filterItem ->
                                                viewModel.handleFilterClick(filterItem)
                                            }
                                        )
                                        
                                        CategorySection(
                                            title = "按地区",
                                            items = categoryFilters.regions,
                                            onItemClick = { filterItem ->
                                                viewModel.handleFilterClick(filterItem)
                                            }
                                        )
                                        
                                        CategorySection(
                                            title = "按年份",
                                            items = categoryFilters.years,
                                            onItemClick = { filterItem ->
                                                viewModel.handleFilterClick(filterItem)
                                            }
                                        )
                                        
                                        CategorySection(
                                            title = "按语言",
                                            items = categoryFilters.languages,
                                            onItemClick = { filterItem ->
                                                viewModel.handleFilterClick(filterItem)
                                            }
                                        )
                                        
                                        CategorySection(
                                            title = "按字母",
                                            items = categoryFilters.letters,
                                            onItemClick = { filterItem ->
                                                viewModel.handleFilterClick(filterItem)
                                            }
                                        )
                                    }
                                }

                                // 影片列表标题
                                if (movies.isNotEmpty()) {
                                    item(span = { TvGridItemSpan(6) }) {
                                        Text(
                                            text = "影片列表",
                                            fontSize = 20.sp,
                                            color = Color.White,
                                            modifier = Modifier.padding(vertical = 16.dp)
                                        )
                                    }

                                    // 影片列表内容
                                    items(movies) { movie ->
                                        MovieCard(
                                            movie = movie,
                                            onClick = {
                                                Log.d("HomeScreen", "Movie clicked: ${movie.title}")
                                                viewModel.getMovieDetail(movie.id)
                                                showDetail = true
                                            },
                                            onLongClick = { },
                                            modifier = Modifier
                                        )
                                    }
                                }

                                // 在影片列表的最后添加加载更多
                                if (showSubCategories && !isLoading) {
                                    item(span = { TvGridItemSpan(6) }) {
                                        LaunchedEffect(Unit) {
                                            viewModel.loadCategoryNextPage()
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
                        } else {
                            // 原有的历史记录、收藏和热门内容
                            if (searchQuery.isEmpty()) {
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
                                    // 播放历史
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
                                                        viewModel.getMovieDetail(history.movieDetailId)
                                                        showDetail = true
                                                    },
                                                    onLongClick = {
                                                        selectedHistoryId = history.movieDetailId
                                                        showMenu = true
                                                    },
                                                    modifier = if (histories.indexOf(history) == 0) {
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

                                    // 收藏列表
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
                                }
                            } else {
                                // 搜索结果
                                TvLazyVerticalGrid(
                                    columns = TvGridCells.Fixed(6),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    item(span = { TvGridItemSpan(6) }) {
                                        Text(
                                            text = "搜索结果",
                                            fontSize = 20.sp,
                                            color = Color.White,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }

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

                                    // 加载更多
                                    if (searchQuery.isNotEmpty() && !isLoading) {
                                        item(span = { TvGridItemSpan(6) }) {
                                            LaunchedEffect(Unit) {
                                                viewModel.loadNextPage()
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
        }
    }
} 