package com.example.jianpian.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class PlayHistory(
    val movie: Movie,
    val episodeName: String,
    val episodeUrl: String,
    val timestamp: Long = System.currentTimeMillis()
)

object PlayHistoryManager {
    private const val PREF_NAME = "play_history"
    private const val KEY_HISTORY = "history"
    private const val MAX_HISTORY = 20

    fun saveHistory(context: Context, history: PlayHistory) {
        Log.d("PlayHistoryManager", "Saving history: movie=${history.movie.title}, episode=${history.episodeName}")
        val histories = getHistories(context).toMutableList()
        Log.d("PlayHistoryManager", "Current histories size: ${histories.size}")
        
        // 移除相同电影的旧记录
        val removedCount = histories.count { it.movie.id == history.movie.id }
        histories.removeAll { it.movie.id == history.movie.id }
        Log.d("PlayHistoryManager", "Removed $removedCount old records")
        
        // 添加新记录到开头
        histories.add(0, history)
        Log.d("PlayHistoryManager", "Added new record")
        
        // 保持最大数量限制
        while (histories.size > MAX_HISTORY) {
            histories.removeLast()
            Log.d("PlayHistoryManager", "Removed oldest record, size now: ${histories.size}")
        }
        
        val json = Gson().toJson(histories)
        Log.d("PlayHistoryManager", "Saving JSON: ${json.take(200)}...")
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_HISTORY, json)
            .apply()
    }

    fun getHistories(context: Context): List<PlayHistory> {
        val json = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_HISTORY, "[]")
        Log.d("PlayHistoryManager", "Loading JSON: ${json?.take(200)}...")
        
        val type = object : TypeToken<List<PlayHistory>>() {}.type
        return try {
            val histories = Gson().fromJson<List<PlayHistory>>(json, type)
            Log.d("PlayHistoryManager", "Loaded ${histories.size} histories")
            histories.forEach { history ->
                Log.d("PlayHistoryManager", "History: movie=${history.movie.title}, id=${history.movie.id}, episode=${history.episodeName}")
            }
            histories
        } catch (e: Exception) {
            Log.e("PlayHistoryManager", "Error loading histories", e)
            emptyList()
        }
    }

    fun clearHistories(context: Context) {
        Log.d("PlayHistoryManager", "Clearing all histories")
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_HISTORY, "[]")
            .apply()
    }
} 