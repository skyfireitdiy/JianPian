package com.example.jianpian

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class JianPianApp : Application(), ImageLoaderFactory {
    companion object {
        lateinit var instance: JianPianApp
            private set
        
        // 100MB 的最大缓存大小
        private const val MAX_DISK_CACHE_SIZE = 100 * 1024 * 1024L // 100MB
        // 内存缓存使用可用内存的 15%
        private const val MEMORY_CACHE_PERCENT = 0.15
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .crossfade(true)
            .okHttpClient {
                OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .build()
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(MEMORY_CACHE_PERCENT)  // 使用15%的可用内存作为内存缓存
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("movie_covers"))  // 指定缓存目录
                    .maxSizeBytes(MAX_DISK_CACHE_SIZE)  // 设置最大缓存大小为100MB
                    .build()
            }
            .logger(DebugLogger())
            .respectCacheHeaders(false)  // 忽略服务器的缓存控制头
            .allowRgb565(true)  // 允许使用 RGB565 格式，可以减少内存占用
            .build()
    }
} 