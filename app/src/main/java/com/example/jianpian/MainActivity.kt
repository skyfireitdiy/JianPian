package com.example.jianpian

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.addCallback
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.example.jianpian.ui.theme.JianPianTheme
import com.example.jianpian.ui.screens.HomeScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.jianpian.viewmodel.HomeViewModel

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 处理返回键事件
        onBackPressedDispatcher.addCallback(this) {
            // 不做任何事情，让 Compose 处理返回事件
            // 这样可以防止 Activity 被直接关闭
            isEnabled = false
            onBackPressed()
        }

        setContent {
            val viewModel: HomeViewModel = viewModel()
            var currentScreen by remember { mutableStateOf(Screen.Home) }

            JianPianTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    HomeScreen(
                        onBackPressed = {
                            when (currentScreen) {
                                Screen.Home -> {
                                    // 如果在主页，则退出应用
                                    finish()
                                }
                                else -> {
                                    // 否则返回主页
                                    currentScreen = Screen.Home
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

enum class Screen {
    Home,
    Detail,
    Player
}