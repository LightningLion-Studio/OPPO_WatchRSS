package com.lightningstudio.watchrss.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun SearchInputBar(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "搜索",
    onSearch: (() -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current
    val shape = RoundedCornerShape(30.dp)
    val borderColor = Color(0xFF202124)
    val textStyle = TextStyle(
        color = Color.White,
        fontSize = 17.sp,
        fontWeight = FontWeight.Medium,
        fontFamily = MaterialTheme.typography.titleMedium.fontFamily
    )
    val placeholderColor = Color(0xB3FFFFFF)

    Row(
        modifier = modifier
            .height(60.dp)
            .border(width = 2.dp, color = borderColor, shape = shape)
            .clip(shape)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            BasicTextField(
                value = keyword,
                onValueChange = onKeywordChange,
                singleLine = true,
                textStyle = textStyle,
                cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        onSearch?.invoke()
                        focusManager.clearFocus()
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            )
            if (keyword.isEmpty()) {
                Text(text = placeholder, style = textStyle.copy(color = placeholderColor))
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "搜索",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}
