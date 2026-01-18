package com.lightningstudio.watchrss.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lightningstudio.watchrss.ui.theme.WatchRSSTheme

@Composable
fun CollaboratorsScreen(
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val bgOuter1 = Color(0xFF0B1211)
        val bgOuter2 = Color(0xFF070B0B)

        val circleBg1 = Color(0xFF050708)
        val circleBg2 = Color(0xFF000000)

        val circle = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(circleBg1, circleBg2)
                )
            )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(bgOuter1, bgOuter2)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = circle) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.931.dp)
                        .padding(top = 13.106.dp, bottom = 16.019.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(13.106.dp))

                    Spacer(modifier = Modifier.height(5.825.dp))

                    Text(
                        text = "协作者名单",
                        color = Color.White,
                        fontSize = 17.475.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(13.106.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.194.dp)
                    ) {
                        CollaboratorPill(
                            name = "闪电狮",
                            role = "前端开发",
                            leadingText = "S"
                        )
                        CollaboratorPill(
                            name = "林海阔",
                            role = "前端开发",
                            leadingText = "L"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CollaboratorPill(
    name: String,
    role: String,
    leadingText: String,
    modifier: Modifier = Modifier
) {
    val pillColor = Color(0xFF23272A)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(52.425.dp),
        color = pillColor,
        shape = RoundedCornerShape(29.125.dp),
        shadowElevation = 7.281.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaddingValues(horizontal = 14.562.dp, vertical = 10.194.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(26.213.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.12f),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = leadingText,
                        color = Color.White.copy(alpha = 0.95f),
                        fontSize = 11.65.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.size(10.194.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = name,
                    color = Color.White,
                    fontSize = 13.106.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(1.456.dp))
                Text(
                    text = role,
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 9.466.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

@Preview(widthDp = 466, heightDp = 466, showBackground = true)
@Composable
private fun CollaboratorsScreenPreview() {
    WatchRSSTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            CollaboratorsScreen()
        }
    }
}
