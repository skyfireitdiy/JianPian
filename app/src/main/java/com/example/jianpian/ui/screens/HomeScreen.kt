package com.example.jianpian.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items
import com.example.jianpian.data.Movie
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.jianpian.viewmodel.HomeViewModel
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import android.util.Log
import com.example.jianpian.data.Episode
import com.example.jianpian.data.MovieDetail
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.example.jianpian.data.PlayHistory

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
    var showDetail by remember { mutableStateOf(false) }
    var currentEpisode by remember { mutableStateOf<Episode?>(null) }
    
    BackHandler {
        when {
            currentEpisode != null -> {
                currentEpisode = null
            }
            showDetail -> {
                showDetail = false
            }
            else -> {
                onBackPressed()
            }
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.loadPlayHistories(context)
        viewModel.loadFavorites(context)
    }
    
    LaunchedEffect(currentEpisode) {
        if (currentEpisode != null && currentMovie != null) {
            viewModel.savePlayHistory(
                context,
                Movie(
                    id = currentMovie!!.id,
                    title = currentMovie!!.title,
                    coverUrl = currentMovie!!.coverUrl,
                    description = currentMovie!!.description
                ),
                currentEpisode!!.name,
                currentEpisode!!.url
            )
        }
    }
    
    LaunchedEffect(currentMovie) {
        if (searchQuery.isEmpty() && currentMovie != null) {
            histories.find { it.movie.id == currentMovie!!.id }?.let { history ->
                currentMovie!!.episodes.find { it.url == history.episodeUrl }?.let { episode ->
                    currentEpisode = episode
                }
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
                        currentEpisode = null
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Search bar
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.DarkGray.copy(alpha = 0.3f))
                            .padding(16.dp)
                            .focusable(),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "搜索电影...",
                                        color = Color.Gray
                                    )
                                }
                                innerTextField()
                            }
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color.White
                        ),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                viewModel.searchMovies(searchQuery)
                            }
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("加载中...", color = Color.White)
                        }
                    } else {
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
                                // 收藏列表标题
                                if (favorites.isNotEmpty()) {
                                    item(span = { androidx.tv.foundation.lazy.grid.TvGridItemSpan(6) }) {
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
                                            }
                                        )
                                    }
                                }

                                // 播放历史标题
                                if (histories.isNotEmpty()) {
                                    item(span = { androidx.tv.foundation.lazy.grid.TvGridItemSpan(6) }) {
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
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        histories.firstOrNull()?.let { history ->
                                                            viewModel.getMovieDetail(history.movie.id)
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
                                        MovieCard(
                                            movie = history.movie,
                                            subtitle = history.episodeName,
                                            onClick = {
                                                viewModel.getMovieDetail(history.movie.id)
                                                showDetail = true
                                            }
                                        )
                                    }
                                }
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
                                items(movies) { movie ->
                                    MovieCard(
                                        movie = movie,
                                        onClick = {
                                            viewModel.getMovieDetail(movie.id)
                                            showDetail = true
                                        }
                                    )
                                }
                                
                                // 添加一个监听器项
                                item(span = { androidx.tv.foundation.lazy.grid.TvGridItemSpan(6) }) {
                                    if (!isLoading) {
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
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MovieCard(
    movie: Movie,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    var isImageLoading by remember { mutableStateOf(true) }
    var isImageError by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .width(160.dp)
            .height(240.dp),
        onClick = onClick
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