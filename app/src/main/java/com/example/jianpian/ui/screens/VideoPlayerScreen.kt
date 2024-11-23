package com.example.jianpian.ui.screens

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import androidx.tv.material3.*
import com.example.jianpian.data.Episode
import com.example.jianpian.data.MovieDetail
import com.example.jianpian.viewmodel.HomeViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import android.util.Log
import okhttp3.OkHttpClient
import androidx.media3.datasource.DefaultHttpDataSource
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    movieDetail: MovieDetail,
    currentEpisode: Episode,
    onPreviousEpisode: () -> Unit,
    onNextEpisode: () -> Unit,
    onBackClick: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    var showControls by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val playUrl by viewModel.currentPlayUrl.collectAsState()
    
    val okHttpClient = remember {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    val exoPlayer = remember {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(30000)
            .setReadTimeoutMs(30000)
            .setAllowCrossProtocolRedirects(true)
        
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpDataSourceFactory))
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_OFF
                playWhenReady = true
            }
    }

    LaunchedEffect(currentEpisode) {
        viewModel.getPlayUrl(currentEpisode.url)
    }

    LaunchedEffect(playUrl) {
        if (playUrl.isNotEmpty()) {
            Log.d("VideoPlayer", "Setting media item: $playUrl")
            exoPlayer.setMediaItem(MediaItem.fromUri(playUrl))
            exoPlayer.prepare()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video Player
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    player = exoPlayer
                    layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Controls overlay
        if (showControls) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = onBackClick) {
                        Text("返回")
                    }
                    Button(
                        onClick = onPreviousEpisode,
                        enabled = movieDetail.episodes.indexOf(currentEpisode) > 0
                    ) {
                        Text("上一集")
                    }
                    Button(
                        onClick = onNextEpisode,
                        enabled = movieDetail.episodes.indexOf(currentEpisode) < movieDetail.episodes.size - 1
                    ) {
                        Text("下一集")
                    }
                }

                Text(
                    text = "${movieDetail.title} - ${currentEpisode.name}",
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp),
                    color = Color.White
                )
            }
        }
    }
} 