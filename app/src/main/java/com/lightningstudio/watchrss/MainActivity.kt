package com.lightningstudio.watchrss

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.lightningstudio.watchrss.ui.screen.CollaboratorsScreen
import com.lightningstudio.watchrss.ui.screen.WebViewLoginScreen
import com.lightningstudio.watchrss.ui.theme.WatchRSSTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContent {
            WatchRSSTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) {
                    MainNavigation(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
fun DemoLauncher(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "WatchRSS")
        Button(onClick = {
            context.startActivity(Intent(context, HeytapWidgetDemoActivity::class.java))
        }) {
            Text(text = "Open Heytap Widget Demo")
        }
    }
}

@Composable
fun MainNavigation(modifier: Modifier = Modifier) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Collaborators) }
    var loginCookies by remember { mutableStateOf("") }

    when (currentScreen) {
        is Screen.Collaborators -> {
            CollaboratorsScreen(
                modifier = modifier.fillMaxSize()
            )
        }
        is Screen.WebViewLogin -> {
            WebViewLoginScreen(
                onLoginComplete = { cookies ->
                    loginCookies = cookies
                    // Handle the cookies here (e.g., save them or pass to ViewModel)
                    println("Cookies received: $cookies")
                },
                onBack = {
                    currentScreen = Screen.Collaborators
                },
                modifier = modifier.fillMaxSize()
            )
        }
    }
}

// Define navigation screens
sealed class Screen {
    object Collaborators : Screen()
    object WebViewLogin : Screen()
}

@Preview(showBackground = true)
@Composable
private fun DemoLauncherPreview() {
    WatchRSSTheme {
        DemoLauncher()
    }
}

@Preview(showBackground = true)
@Composable
private fun MainNavigationPreview() {
    WatchRSSTheme {
        MainNavigation()
    }
}
