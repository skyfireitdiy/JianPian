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
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.KeyEvent
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester

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
    val context = LocalContext.current
    val playUrls by viewModel.playUrls.collectAsState()
    
    var playerView by remember { mutableStateOf<PlayerView?>(null) }
    var isControllerShowing by remember { mutableStateOf(false) }
    
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
                
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_ENDED) {
                            val currentIndex = movieDetail.episodes.indexOf(currentEpisode)
                            if (currentIndex < movieDetail.episodes.size - 1) {
                                onNextEpisode()
                            }
                        }
                    }
                })
            }
    }

    LaunchedEffect(playUrls) {
        if (playUrls.isNotEmpty()) {
            exoPlayer.clearMediaItems()
            movieDetail.episodes.forEach { episode ->
                playUrls[episode.url]?.let { url ->
                    exoPlayer.addMediaItem(MediaItem.fromUri(url))
                }
            }
            val currentIndex = movieDetail.episodes.indexOf(currentEpisode)
            if (currentIndex >= 0) {
                exoPlayer.seekTo(currentIndex, 0)
                exoPlayer.prepare()
            }
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
            .focusable(true)
            .onKeyEvent { event ->
                when (event.nativeKeyEvent.keyCode) {
                    android.view.KeyEvent.KEYCODE_BACK -> {
                        onBackClick()
                        true
                    }
                    android.view.KeyEvent.KEYCODE_DPAD_CENTER,
                    android.view.KeyEvent.KEYCODE_ENTER -> {
                        playerView?.apply {
                            if (isControllerShowing) {
                                hideController()
                                isControllerShowing = false
                            } else {
                                showController()
                                isControllerShowing = true
                            }
                        }
                        true
                    }
                    else -> false
                }
            }
    ) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    player = exoPlayer
                    layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    setShowNextButton(true)
                    setShowPreviousButton(true)
                    controllerShowTimeoutMs = 3000
                    controllerHideOnTouch = true
                    useController = true
                    
                    setControllerVisibilityListener(
                        PlayerView.ControllerVisibilityListener { visibility ->
                            isControllerShowing = visibility == android.view.View.VISIBLE
                        }
                    )
                    
                    playerView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
} 