package com.sportanalyzer.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sportanalyzer.app.ui.theme.*

/**
 * 全画面で共有されるシンプルなナビゲーションバー。
 *
 * @param title 中央に表示するタイトル文字列
 * @param onBack 戻るボタン押下時のコールバック
 * @param subtitle タイトル下に表示するサブテキスト（省略可）
 * @param trailingContent 右端に配置する任意のコンテンツ（省略可）
 */
@Composable
fun SimpleNavBar(
    title: String,
    onBack: () -> Unit,
    subtitle: String? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SystemDark)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "戻る", tint = iOSBlue)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryLabel
            )
            if (subtitle != null) {
                Text(text = subtitle, fontSize = 12.sp, color = SecondaryLabel)
            }
        }

        if (trailingContent != null) {
            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                trailingContent()
            }
        }
    }
}
