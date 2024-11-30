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
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.ui.PlayerView
import androidx.tv.material3.*
import com.example.jianpian.data.Episode
import com.example.jianpian.data.Movie
import com.example.jianpian.data.MovieDetail
import com.example.jianpian.data.PlayHistory
import com.example.jianpian.data.PlayHistoryManager
import com.example.jianpian.viewmodel.HomeViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import android.util.Log
import okhttp3.OkHttpClient
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
import kotlinx.coroutines.*
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager
import androidx.media3.datasource.okhttp.OkHttpDataSource

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
    val playUrls by viewModel.playUrls.collectAsState()
    
    var playerView by remember { mutableStateOf<PlayerView?>(null) }
    var isControllerShowing by remember { mutableStateOf(false) }
    
    // 创建信任所有证书的 OkHttpClient
    val okHttpClient = remember {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .hostnameVerifier { _, _ -> true }
            .sslSocketFactory(
                TrustAllCerts.createSSLSocketFactory(),
                TrustAllCerts.trustAllManager
            )
            .build()
    }

    // 创建 ExoPlayer 并配置 OkHttpDataSource
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(context)
                    .setDataSourceFactory(
                        OkHttpDataSource.Factory(okHttpClient)
                    )
            )
            .build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
                playWhenReady = true
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        Log.d("VideoPlayerScreen", "Playback state changed: $playbackState")
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

    DisposableEffect(context, exoPlayer) {
        // 设置播放器监听器
        var isFirstReady = true
        var job: Job? = null
        var positionRestored = false
        var pendingSeekPosition: Long = -1
        var initialHistoryLoaded = false
        var hasHistory = false

        // 预先加载历史记录
        val histories = PlayHistoryManager.getHistories(context)
        Log.d("VideoPlayerScreen", "Looking for history: movieId=${movieDetail.id}, episodeUrl=${currentEpisode.url}")
        val lastHistory = histories.find { it.movieDetailId == movieDetail.id && it.episodeUrl == currentEpisode.url }
        Log.d("VideoPlayerScreen", "Found history: $lastHistory")
        
        // 如果有历史播放位置，先保存起来
        lastHistory?.let { history ->
            Log.d("VideoPlayerScreen", "History playback position: ${history.playbackPosition}")
            if (history.playbackPosition > 0) {
                pendingSeekPosition = history.playbackPosition
                hasHistory = true
            }
        }

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.d("VideoPlayerScreen", "Playback state changed: $playbackState, isFirstReady: $isFirstReady")
                
                when (playbackState) {
                    Player.STATE_READY -> {
                        if (isFirstReady) {
                            isFirstReady = false
                            initialHistoryLoaded = true

                            // 如果有待恢复的位置，现在恢复
                            if (hasHistory && !positionRestored && pendingSeekPosition > 0) {
                                Log.d("VideoPlayerScreen", "Seeking to position: $pendingSeekPosition")
                                exoPlayer.seekTo(pendingSeekPosition)
                                positionRestored = true
                            }

                            // 开始定期保存播放位置
                            if (job == null) {
                                Log.d("VideoPlayerScreen", "Starting position saving job")
                                job = CoroutineScope(Dispatchers.Main).launch {
                                    while (isActive) {
                                        delay(5000) // 每5秒保存一次
                                        val currentPosition = exoPlayer.currentPosition
                                        val duration = exoPlayer.duration
                                        Log.d("VideoPlayerScreen", "Current position: $currentPosition, Duration: $duration")
                                        
                                        if (currentPosition > 0 && duration > 0) {
                                            viewModel.savePlayHistory(
                                                context = context,
                                                movieDetailId = movieDetail.id,
                                                episodeName = currentEpisode.name,
                                                episodeUrl = currentEpisode.url,
                                                playbackPosition = currentPosition
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("VideoPlayerScreen", "Player error", error)
            }
        })

        onDispose {
            // 取消定期保存任务
            job?.cancel()
            
            // 保存最终播放位置
            val currentPosition = exoPlayer.currentPosition
            val duration = exoPlayer.duration
            Log.d("VideoPlayerScreen", "Final position: $currentPosition, Duration: $duration")
            
            if (currentPosition > 0 && duration > 0) {
                viewModel.savePlayHistory(
                    context = context,
                    movieDetailId = movieDetail.id,
                    episodeName = currentEpisode.name,
                    episodeUrl = currentEpisode.url,
                    playbackPosition = currentPosition
                )
            }
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
                    
                    controllerAutoShow = true
                    setKeepContentOnPlayerReset(true)
                    setShutterBackgroundColor(AndroidColor.BLACK)
                    
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

    LaunchedEffect(playUrls) {
        if (playUrls.isNotEmpty()) {
            playerView?.showController()
            delay(1000)
            playerView?.hideController()
        }
    }
}

private object TrustAllCerts {
    val trustAllManager = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }

    fun createSSLSocketFactory(): SSLSocketFactory {
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(trustAllManager), SecureRandom())
        return sslContext.socketFactory
    }
}