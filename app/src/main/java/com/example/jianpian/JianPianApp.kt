package com.example.jianpian

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager
import java.security.SecureRandom
import java.security.cert.X509Certificate

class JianPianApp : Application(), ImageLoaderFactory {
    companion object {
        lateinit var instance: JianPianApp
            private set
        
        private const val MEMORY_CACHE_PERCENT = 0.15  // 使用15%的可用内存作为内存缓存
        private const val MAX_DISK_CACHE_SIZE = 100L * 1024 * 1024  // 100MB
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
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

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .crossfade(true)
            .okHttpClient {
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
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(MEMORY_CACHE_PERCENT)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("movie_covers"))
                    .maxSizeBytes(MAX_DISK_CACHE_SIZE)
                    .build()
            }
            .logger(DebugLogger())
            .respectCacheHeaders(false)
            .allowRgb565(true)
            .build()
    }
} 