package com.lightningstudio.watchrss

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.lightningstudio.watchrss.ui.theme.WatchRSSTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WatchRSSTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DemoLauncher(modifier = Modifier.padding(innerPadding))
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

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WatchRSSTheme {
        DemoLauncher()
    }
}
