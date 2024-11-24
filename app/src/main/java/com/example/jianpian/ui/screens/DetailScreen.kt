package com.example.jianpian.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import coil.compose.AsyncImage
import com.example.jianpian.data.MovieDetail
import com.example.jianpian.data.Episode
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.items
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.example.jianpian.data.Movie
import com.example.jianpian.viewmodel.HomeViewModel
import com.example.jianpian.data.PlayUrlCache

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DetailScreen(
    movieDetail: MovieDetail,
    onPlayClick: (Episode) -> Unit,
    onBackClick: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    var isFavorite by remember { mutableStateOf(false) }
    var isCaching by remember { mutableStateOf(false) }
    var cacheProgress by remember { mutableStateOf(0f) }
    var isCached by remember { mutableStateOf(false) }

    LaunchedEffect(movieDetail.id) {
        isFavorite = viewModel.isFavorite(context, movieDetail.id)
        val cachedUrls = PlayUrlCache.getUrls(context, movieDetail.id)
        if (cachedUrls == null) {
            isCaching = true
            cacheProgress = 0f
            viewModel.refreshPlayUrls(context, movieDetail)
        } else {
            isCached = true
        }
    }

    LaunchedEffect(isCaching) {
        if (isCaching) {
            val totalEpisodes = movieDetail.episodes.size
            viewModel.cacheProgressFlow.collect { progress ->
                cacheProgress = progress.toFloat() / totalEpisodes
                if (progress == totalEpisodes) {
                    isCaching = false
                    isCached = true
                }
            }
        }
    }

    Log.d("DetailScreen", "Displaying movie detail: ${movieDetail.title}")
    Log.d("DetailScreen", "Movie info: id=${movieDetail.id}, " +
        "director=${movieDetail.director}, " +
        "actors=${movieDetail.actors}, " +
        "genre=${movieDetail.genre}, " +
        "area=${movieDetail.area}, " +
        "year=${movieDetail.year}")
    Log.d("DetailScreen", "Episodes: ${movieDetail.episodes.size}")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                // 电影海报
                AsyncImage(
                    model = movieDetail.coverUrl,
                    contentDescription = movieDetail.title,
                    modifier = Modifier
                        .width(200.dp)
                        .fillMaxHeight(),
                    contentScale = ContentScale.Crop
                )

                // 电影信息
                Column(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .fillMaxHeight()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = movieDetail.title,
                            fontSize = 28.sp,
                            color = Color.White,
                            style = androidx.compose.ui.text.TextStyle(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                        
                        Button(
                            onClick = {
                                viewModel.toggleFavorite(
                                    context,
                                    Movie(
                                        id = movieDetail.id,
                                        title = movieDetail.title,
                                        coverUrl = movieDetail.coverUrl,
                                        description = movieDetail.description
                                    )
                                )
                                isFavorite = !isFavorite
                            }
                        ) {
                            Text(if (isFavorite) "取消收藏" else "收藏")
                        }
                    }

                    // 其他信息
                    if (movieDetail.director.isNotEmpty()) {
                        Text(
                            text = "导演：${movieDetail.director}",
                            color = Color.LightGray,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    if (movieDetail.actors.isNotEmpty()) {
                        Text(
                            text = "主演：${movieDetail.actors}",
                            color = Color.LightGray,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    if (movieDetail.genre.isNotEmpty()) {
                        Text(
                            text = "类型：${movieDetail.genre}",
                            color = Color.LightGray,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    if (movieDetail.area.isNotEmpty()) {
                        Text(
                            text = "地区：${movieDetail.area}",
                            color = Color.LightGray,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    if (movieDetail.year.isNotEmpty()) {
                        Text(
                            text = "年份：${movieDetail.year}",
                            color = Color.LightGray,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = movieDetail.description,
                        color = Color.Gray,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            // 在播放列表标题旁添加缓存状���和操作
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "播放列表",
                    fontSize = 20.sp,
                    color = Color.White
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isCaching) {
                        // 显示缓存进度
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "首次播放，缓存播放链接...",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                            Box(
                                modifier = Modifier
                                    .width(200.dp)
                                    .height(4.dp)
                                    .background(Color.DarkGray)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(200.dp * cacheProgress)
                                        .background(Color.Green)
                                )
                            }
                            Text(
                                text = "${(cacheProgress * 100).toInt()}%",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    } else if (isCached) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "播放链接已缓存",
                                color = Color.Green,
                                fontSize = 14.sp
                            )
                            Button(
                                onClick = {
                                    isCaching = true
                                    cacheProgress = 0f
                                    viewModel.refreshPlayUrls(context, movieDetail)
                                }
                            ) {
                                Text("重新缓存")
                            }
                        }
                    }
                }
            }

            // 使用 TvLazyVerticalGrid 替换 TvLazyRow
            TvLazyVerticalGrid(
                columns = TvGridCells.Fixed(6),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)  // 固定高度，可以根据需要调整
            ) {
                items(movieDetail.episodes) { episode ->
                    Button(
                        onClick = { onPlayClick(episode) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = episode.name,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
} 