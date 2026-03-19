package com.sportanalyzer.app.ui.screens.history

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.sportanalyzer.app.ui.MainViewModel
import com.sportanalyzer.app.ui.components.MarkdownContent
import com.sportanalyzer.app.ui.components.SimpleNavBar
import com.sportanalyzer.app.ui.navigation.Screen
import com.sportanalyzer.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryDetailScreen(
    navController: NavController,
    recordId: String,
    viewModel: MainViewModel = hiltViewModel()
) {
    val record = remember(recordId) { viewModel.getHistoryRecord(recordId) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = SystemDark,
            titleContentColor = PrimaryLabel,
            textContentColor = SecondaryLabel,
            title = { Text("この解析を削除", fontWeight = FontWeight.SemiBold) },
            text  = { Text("この解析結果を削除します。この操作は取り消せません。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteHistoryRecords(setOf(recordId))
                    showDeleteDialog = false
                    navController.popBackStack()
                }) {
                    Text("削除", color = iOSRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("キャンセル", color = iOSBlue)
                }
            }
        )
    }

    if (record == null) {
        Box(Modifier.fillMaxSize().background(SystemBlack), contentAlignment = Alignment.Center) {
            Text("解析結果が見つかりません", color = SecondaryLabel)
        }
        return
    }

    val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.JAPAN)
    val dateStr = sdf.format(Date(record.timestamp))
    val modeLabel = if (record.analysisMode == "pose") "骨格推定" else "元動画"
    val modeBg    = if (record.analysisMode == "pose") iOSIndigo else iOSGreen

    val scoreColor = when {
        record.score >= 80 -> iOSBlue
        record.score >= 60 -> iOSOrange
        record.score > 0   -> iOSRed
        else               -> SecondaryLabel
    }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SystemBlack)
    ) {
        SimpleNavBar(
            title    = record.title,
            onBack   = { navController.popBackStack() },
            subtitle = dateStr,
            trailingContent = {
                Row {
                    IconButton(onClick = {
                        val shareText = buildString {
                            appendLine("📊 Forma - フォーム分析結果")
                            if (record.score > 0) appendLine("スコア: ${record.score}/100 [$modeLabel]")
                            appendLine("──────────")
                            appendLine(record.analysisText)
                            appendLine("──────────")
                            append("Forma - AI フォーム分析")
                        }
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, "分析結果を共有"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "共有", tint = iOSBlue)
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "削除", tint = iOSRed)
                    }
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ── ヘッダー情報 ─────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // モードバッジ
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(modeBg.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(modeLabel, fontSize = 13.sp, color = modeBg, fontWeight = FontWeight.SemiBold)
                }

                // スコアバッジ
                if (record.score > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(scoreColor.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "スコア ${record.score}",
                            fontSize = 13.sp,
                            color = scoreColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 解析テキスト ─────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SystemDark)
                    .padding(16.dp)
            ) {
                MarkdownContent(
                    text = record.analysisText,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── AIに質問ボタン ───────────────────────────────────
            Button(
                onClick = {
                    viewModel.initChatFromHistory(recordId)
                    navController.navigate(Screen.Chat.createRoute(recordId))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = iOSIndigo)
            ) {
                Text("💬 AIにもっと質問する", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
