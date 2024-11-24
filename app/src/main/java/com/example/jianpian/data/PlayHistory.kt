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
        
        // 修改判断条件：只要是同一部电影就移除旧记录
        val removedCount = histories.count { 
            it.movieDetailId == history.movieDetailId  // 移除相同电影ID的所有记录
        }
        histories.removeAll { 
            it.movieDetailId == history.movieDetailId  // 移除相同电影ID的所有记录
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
        
        val type = object : TypeToken<List<PlayHistory>>() {}.type
        return try {
            val rawHistories = gson.fromJson<List<PlayHistory>>(json, type)
            
            // 对历史记录进行去重处理，只保留每部电影最新的一条记录
            val uniqueHistories = rawHistories
                .groupBy { it.movieDetailId } // 按电影ID分组
                .map { (_, histories) -> 
                    // 每组取时间戳最新的一条
                    histories.maxByOrNull { it.timestamp }!! 
                }
                .sortedByDescending { it.timestamp } // 按时间戳降序排序
                .take(MAX_HISTORY) // 确保不超过最大数量
            
            Log.d("PlayHistoryManager", "Loaded and deduplicated: original=${rawHistories.size}, " +
                "unique=${uniqueHistories.size}")
            
            // 如果去重后的结果与原始数据不同，保存更新后的数据
            if (uniqueHistories.size != rawHistories.size) {
                Log.d("PlayHistoryManager", "Saving deduplicated histories")
                saveHistories(context, uniqueHistories)
            }
            
            uniqueHistories
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

    fun deleteHistory(context: Context, movieDetailId: String) {
        val histories = getHistories(context).toMutableList()
        val removedCount = histories.removeAll { it.movieDetailId == movieDetailId }
        Log.d("PlayHistoryManager", "Deleted $removedCount history records for movie: $movieDetailId")
        saveHistories(context, histories)
    }
}