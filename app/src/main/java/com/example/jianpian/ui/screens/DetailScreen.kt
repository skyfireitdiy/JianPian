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
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DetailScreen(
    movieDetail: MovieDetail,
    onPlayClick: (Episode) -> Unit,
    onBackClick: () -> Unit
) {
    Log.d("DetailScreen", "Displaying movie detail: ${movieDetail.title}")
    Log.d("DetailScreen", "Movie info: id=${movieDetail.id}, " +
        "director=${movieDetail.director}, " +
        "actors=${movieDetail.actors}, " +
        "genre=${movieDetail.genre}, " +
        "area=${movieDetail.area}, " +
        "year=${movieDetail.year}")
    Log.d("DetailScreen", "Episodes: ${movieDetail.episodes.size}")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color.Black)
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
                // 电影标题
                Text(
                    text = movieDetail.title,
                    fontSize = 28.sp,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp),
                    style = androidx.compose.ui.text.TextStyle(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                )

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

        // 播放列表
        Text(
            text = "播放列表",
            fontSize = 20.sp,
            color = Color.White,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        TvLazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            items(movieDetail.episodes) { episode ->
                Button(
                    onClick = { onPlayClick(episode) },
                    modifier = Modifier
                        .width(120.dp)
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