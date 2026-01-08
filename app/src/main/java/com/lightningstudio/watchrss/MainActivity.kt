package com.lightningstudio.watchrss

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.lightningstudio.watchrss.ui.screen.CollaboratorsScreen
import com.lightningstudio.watchrss.ui.screen.WebViewLoginScreen
import com.lightningstudio.watchrss.ui.theme.WatchRSSTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WatchRSSTheme {
                MainNavigation()
            }
        }
    }
}

@Composable
fun MainNavigation() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Collaborators) }
    var loginCookies by remember { mutableStateOf("") }

    when (currentScreen) {
        is Screen.Collaborators -> {
            CollaboratorsScreen(
                modifier = Modifier.fillMaxSize()
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
                modifier = Modifier.fillMaxSize()
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
fun MainNavigationPreview() {
	WatchRSSTheme {
		MainNavigation()
	}
}
