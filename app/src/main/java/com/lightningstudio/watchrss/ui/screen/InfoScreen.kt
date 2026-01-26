package com.lightningstudio.watchrss.ui.screen

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.lightningstudio.watchrss.ProjectInfoActivity
import com.lightningstudio.watchrss.R
import com.lightningstudio.watchrss.ui.components.WatchSurface

@Composable
fun InfoScreen(title: String, content: String) {
    val safePadding = dimensionResource(R.dimen.watch_safe_padding)
    val context = LocalContext.current

    WatchSurface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(safePadding)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Check if this is the licenses page and contains the GPL text
            if (title == "开源许可与清单" && content.contains("本项目已依据 GPL-3.0 开源")) {
                val parts = content.split("本项目已依据 GPL-3.0 开源")

                // First part (before the clickable text)
                Text(
                    text = parts[0],
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Clickable text
                val annotatedString = buildAnnotatedString {
                    pushStringAnnotation(tag = "GPL", annotation = "gpl_link")
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append("本项目已依据 GPL-3.0 开源")
                    }
                    pop()
                }

                ClickableText(
                    text = annotatedString,
                    style = MaterialTheme.typography.bodySmall,
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(tag = "GPL", start = offset, end = offset)
                            .firstOrNull()?.let {
                                val intent = Intent(context, ProjectInfoActivity::class.java)
                                context.startActivity(intent)
                            }
                    }
                )

                // Second part (after the clickable text)
                if (parts.size > 1) {
                    Text(
                        text = parts[1],
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
