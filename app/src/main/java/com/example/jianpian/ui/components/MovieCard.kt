package com.example.jianpian.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.example.jianpian.data.Movie
import androidx.compose.ui.input.key.*

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MovieCard(
    movie: Movie,
    subtitle: String = "",
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }
    var pressStartTime by remember { mutableStateOf(0L) }
    var isLongPress by remember { mutableStateOf(false) }
    var skipNextClick by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(240.dp)
            .onKeyEvent { event ->
                when {
                    event.key == Key.DirectionCenter && event.type == KeyEventType.KeyDown -> {
                        Log.d("MovieCard", "Key Down - ${movie.title}")
                        pressStartTime = event.nativeKeyEvent.eventTime
                        false
                    }
                    event.key == Key.DirectionCenter && event.type == KeyEventType.KeyUp -> {
                        val pressDuration = event.nativeKeyEvent.eventTime - pressStartTime
                        Log.d("MovieCard", "Key Up - ${movie.title}, duration: $pressDuration, isLongPress: $isLongPress, skipNextClick: $skipNextClick")
                        
                        if (pressDuration > 500) {
                            Log.d("MovieCard", "Triggering long press - ${movie.title}")
                            isLongPress = true
                            skipNextClick = true
                            onLongClick()
                            true
                        } else if (!isLongPress) {
                            Log.d("MovieCard", "Triggering click - ${movie.title}")
                            onClick()
                            true
                        } else if (skipNextClick) {
                            Log.d("MovieCard", "Skipping click after long press - ${movie.title}")
                            skipNextClick = false
                            isLongPress = false
                            true
                        } else {
                            Log.d("MovieCard", "Normal click after long press - ${movie.title}")
                            onClick()
                            isLongPress = false
                            true
                        }
                    }
                    else -> false
                }
            },
        onClick = { 
            Log.d("MovieCard", "Card onClick called - ${movie.title}")
            /* 不在这里处理点击，而是通过 onKeyEvent */ 
        },
        scale = CardDefaults.scale(focusedScale = 1.1f)
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
                    onLoading = { isLoading = true },
                    onSuccess = { isLoading = false },
                    onError = { 
                        isLoading = false
                        isError = true
                    }
                )
                
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("加载中...", color = Color.White)
                    }
                }
                
                if (isError) {
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
            
            if (subtitle.isNotEmpty()) {
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