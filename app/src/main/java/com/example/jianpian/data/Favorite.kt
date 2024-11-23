package com.example.jianpian.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class Favorite(
    val movie: Movie,
    val timestamp: Long = System.currentTimeMillis()
)

object FavoriteManager {
    private const val PREF_NAME = "favorites"
    private const val KEY_FAVORITES = "favorites_list"

    fun saveFavorite(context: Context, favorite: Favorite) {
        Log.d("FavoriteManager", "Saving favorite: movie=${favorite.movie.title}")
        val favorites = getFavorites(context).toMutableList()
        
        // 检查是否已经收藏
        if (favorites.none { it.movie.id == favorite.movie.id }) {
            favorites.add(0, favorite)
            Log.d("FavoriteManager", "Added new favorite")
            
            val json = Gson().toJson(favorites)
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_FAVORITES, json)
                .apply()
        }
    }

    fun removeFavorite(context: Context, movieId: String) {
        Log.d("FavoriteManager", "Removing favorite: movieId=$movieId")
        val favorites = getFavorites(context).toMutableList()
        favorites.removeAll { it.movie.id == movieId }
        
        val json = Gson().toJson(favorites)
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FAVORITES, json)
            .apply()
    }

    fun getFavorites(context: Context): List<Favorite> {
        val json = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_FAVORITES, "[]")
        
        val type = object : TypeToken<List<Favorite>>() {}.type
        return try {
            Gson().fromJson<List<Favorite>>(json, type)
        } catch (e: Exception) {
            Log.e("FavoriteManager", "Error loading favorites", e)
            emptyList()
        }
    }

    fun isFavorite(context: Context, movieId: String): Boolean {
        return getFavorites(context).any { it.movie.id == movieId }
    }

    fun clearFavorites(context: Context) {
        Log.d("FavoriteManager", "Clearing all favorites")
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FAVORITES, "[]")
            .apply()
    }
} 