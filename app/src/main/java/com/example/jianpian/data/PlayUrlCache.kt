package com.example.jianpian.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class PlayUrlCacheEntry(
    val movieId: String,
    val urls: Map<String, String>,  // episodeUrl -> playUrl
    val timestamp: Long = System.currentTimeMillis()
)

object PlayUrlCache {
    private const val PREF_NAME = "play_url_cache"
    private const val KEY_CACHE = "url_cache"
    private const val CACHE_EXPIRE_TIME = 7 * 24 * 60 * 60 * 1000L  // 7天的缓存时间

    fun saveUrls(context: Context, movieId: String, urls: Map<String, String>) {
        Log.d("PlayUrlCache", "Saving play urls for movie: $movieId")
        val cache = getCache(context).toMutableList()
        
        // 移除过期缓存
        val now = System.currentTimeMillis()
        cache.removeAll { (now - it.timestamp) > CACHE_EXPIRE_TIME }
        
        // 移除同一电影的旧缓存
        cache.removeAll { it.movieId == movieId }
        
        // 添加新缓存
        cache.add(0, PlayUrlCacheEntry(movieId, urls))
        
        val json = Gson().toJson(cache)
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CACHE, json)
            .apply()
    }

    fun getUrls(context: Context, movieId: String): Map<String, String>? {
        Log.d("PlayUrlCache", "Getting play urls for movie: $movieId")
        val cache = getCache(context)
        
        // 移除过期缓存
        val now = System.currentTimeMillis()
        if (cache.any { (now - it.timestamp) > CACHE_EXPIRE_TIME }) {
            val updatedCache = cache.filter { (now - it.timestamp) <= CACHE_EXPIRE_TIME }
            val json = Gson().toJson(updatedCache)
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CACHE, json)
                .apply()
        }
        
        return cache.find { it.movieId == movieId && (now - it.timestamp) <= CACHE_EXPIRE_TIME }?.urls
    }

    private fun getCache(context: Context): List<PlayUrlCacheEntry> {
        val json = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CACHE, "[]")
        val type = object : TypeToken<List<PlayUrlCacheEntry>>() {}.type
        return try {
            Gson().fromJson(json, type)
        } catch (e: Exception) {
            Log.e("PlayUrlCache", "Error loading cache", e)
            emptyList()
        }
    }

    fun clearCache(context: Context) {
        Log.d("PlayUrlCache", "Clearing cache")
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CACHE, "[]")
            .apply()
    }
} 