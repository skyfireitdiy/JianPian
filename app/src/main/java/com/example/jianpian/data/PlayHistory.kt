package com.example.jianpian.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.LongSerializationPolicy
import com.google.gson.reflect.TypeToken

data class PlayHistory(
    val movieDetailId: String,
    val movieTitle: String,
    val movieCoverUrl: String,
    val episodeName: String,
    val episodeUrl: String,
    val playbackPosition: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
)

object PlayHistoryManager {
    private const val PREF_NAME = "play_history"
    private const val KEY_HISTORY = "history"
    const val MAX_HISTORY = 20

    private val gson = GsonBuilder()
        .setLongSerializationPolicy(LongSerializationPolicy.STRING)
        .create()

    fun saveHistory(context: Context, history: PlayHistory) {
        Log.d("PlayHistoryManager", "Saving history: movie=${history.movieTitle}, episode=${history.episodeName}")
        val histories = getHistories(context).toMutableList()
        Log.d("PlayHistoryManager", "Current histories size: ${histories.size}")
        
        // 移除相同剧集的旧记录
        val removedCount = histories.count { 
            it.movieDetailId == history.movieDetailId && it.episodeUrl == history.episodeUrl 
        }
        histories.removeAll { 
            it.movieDetailId == history.movieDetailId && it.episodeUrl == history.episodeUrl 
        }
        Log.d("PlayHistoryManager", "Removed $removedCount old records")
        
        // 添加新记录到开头
        histories.add(0, history)
        Log.d("PlayHistoryManager", "Added new record")
        
        // 如果超过最大数量，移除旧的记录
        if (histories.size > MAX_HISTORY) {
            val removeCount = histories.size - MAX_HISTORY
            Log.d("PlayHistoryManager", "Removing $removeCount old records to maintain max size")
            repeat(removeCount) {
                histories.removeLast()
            }
        }
        
        saveHistories(context, histories)
    }

    fun saveHistories(context: Context, histories: List<PlayHistory>) {
        try {
            val json = gson.toJson(histories)
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_HISTORY, json)
                .apply()
            Log.d("PlayHistoryManager", "Saved ${histories.size} histories")
        } catch (e: Exception) {
            Log.e("PlayHistoryManager", "Error saving histories", e)
        }
    }

    fun getHistories(context: Context): List<PlayHistory> {
        val json = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_HISTORY, "[]")
        Log.d("PlayHistoryManager", "Loading histories from SharedPreferences")
        Log.d("PlayHistoryManager", "Raw JSON: ${json?.take(200)}...")
        
        val type = object : TypeToken<List<PlayHistory>>() {}.type
        return try {
            val histories = gson.fromJson<List<PlayHistory>>(json, type)
            Log.d("PlayHistoryManager", "Loaded ${histories.size} histories")
            histories.forEach { history ->
                Log.d("PlayHistoryManager", "Loaded history: movieId=${history.movieDetailId}, " +
                    "title=${history.movieTitle}, episode=${history.episodeName}, " +
                    "position=${history.playbackPosition}")
            }
            histories
        } catch (e: Exception) {
            Log.e("PlayHistoryManager", "Error loading histories", e)
            emptyList()
        }
    }

    fun clearHistories(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_HISTORY)
            .apply()
        Log.d("PlayHistoryManager", "Cleared all histories")
    }
}