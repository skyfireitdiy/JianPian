package com.example.jianpian.ui.screens

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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Back button
        Button(
            onClick = onBackClick,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text("返回")
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            // Movie poster
            AsyncImage(
                model = movieDetail.coverUrl,
                contentDescription = movieDetail.title,
                modifier = Modifier
                    .width(200.dp)
                    .fillMaxHeight(),
                contentScale = ContentScale.Crop
            )

            // Movie info
            Column(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .fillMaxHeight()
            ) {
                Text(
                    text = movieDetail.title,
                    fontSize = 24.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("导演：${movieDetail.director}")
                Text("主演：${movieDetail.actors}")
                Text("类型：${movieDetail.genre}")
                Text("地区：${movieDetail.area}")
                Text("年份：${movieDetail.year}")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = movieDetail.description,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Episodes
        Text(
            text = "播放列表",
            fontSize = 20.sp,
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
} 