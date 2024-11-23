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
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.DefaultLoadControl
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
import androidx.compose.ui.graphics.toArgb
import android.graphics.Color as AndroidColor
import kotlinx.coroutines.delay
import android.view.WindowManager
import androidx.compose.ui.platform.LocalView
import android.content.Context

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
    val view = LocalView.current
    val window = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val playUrls by viewModel.playUrls.collectAsState()
    
    var playerView by remember { mutableStateOf<PlayerView?>(null) }
    var isControllerShowing by remember { mutableStateOf(false) }
    
    val exoPlayer = remember {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
        
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpDataSourceFactory))
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        DefaultLoadControl.DEFAULT_MIN_BUFFER_MS / 2,
                        DefaultLoadControl.DEFAULT_MAX_BUFFER_MS / 2,
                        DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS / 2,
                        DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS / 2
                    )
                    .setPrioritizeTimeOverSizeThresholds(true)
                    .build()
            )
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_OFF
                playWhenReady = true
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        when (state) {
                            Player.STATE_BUFFERING -> {
                                Log.d("VideoPlayer", "Buffering...")
                            }
                            Player.STATE_READY -> {
                                Log.d("VideoPlayer", "Ready to play")
                            }
                            Player.STATE_ENDED -> {
                                val currentIndex = movieDetail.episodes.indexOf(currentEpisode)
                                if (currentIndex < movieDetail.episodes.size - 1) {
                                    onNextEpisode()
                                }
                            }
                            Player.STATE_IDLE -> {
                                Log.d("VideoPlayer", "Player idle")
                            }
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.e("VideoPlayer", "Player error", error)
                        // 可以在这里添加重试逻辑
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
        val originalFlags = view.keepScreenOn
        view.keepScreenOn = true
        
        onDispose {
            view.keepScreenOn = originalFlags
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
                    
                    controllerAutoShow = true
                    setKeepContentOnPlayerReset(true)
                    setShutterBackgroundColor(AndroidColor.BLACK)
                    
                    setControllerVisibilityListener(
                        PlayerView.ControllerVisibilityListener { visibility ->
                            isControllerShowing = visibility == android.view.View.VISIBLE
                        }
                    )
                    
                    player?.addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            when (playbackState) {
                                Player.STATE_READY -> {
                                    showController()
                                    postDelayed({ hideController() }, 1000)
                                }
                            }
                        }
                    })
                    
                    playerView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    LaunchedEffect(playUrls) {
        if (playUrls.isNotEmpty()) {
            playerView?.showController()
            delay(1000)
            playerView?.hideController()
        }
    }
} 