package com.sportanalyzer.app.ui.screens.results

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.sportanalyzer.app.ui.MainViewModel
import com.sportanalyzer.app.ui.components.MarkdownContent
import com.sportanalyzer.app.ui.components.SimpleNavBar
import com.sportanalyzer.app.ui.navigation.Screen
import com.sportanalyzer.app.ui.theme.*

@Composable
fun ResultsScreen(
    navController: NavController,
    @Suppress("UNUSED_PARAMETER") analysisId: String?,
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val result = uiState.analysisResult

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SystemBlack)
    ) {
        SimpleNavBar(title = "分析結果", onBack = { navController.popBackStack() })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // ── 動画プレビュー ────────────────────────────────────────
            val originalUri = uiState.videoPath
            val poseUri     = uiState.savedPoseVideoPath

            if (originalUri != null) {
                VideoSection(
                    originalUri  = originalUri,
                    poseVideoPath = poseUri
                )
            }

            // ── スコアバナー ──────────────────────────────────────────
            if (result != null) {
                val score = (result.overallStats.averageFormScore * 100).toInt()
                val scoreColor = when {
                    score >= 80 -> iOSGreen
                    score >= 60 -> iOSOrange
                    else        -> iOSRed
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SystemDark)
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(iOSBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Psychology,
                                contentDescription = null,
                                tint = iOSBlue,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "AI 分析完了", fontSize = 13.sp, color = SecondaryLabel)
                            Text(
                                text = "Gemini による動画分析",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryLabel
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$score",
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold,
                                color = scoreColor
                            )
                            Text(text = "/ 100", fontSize = 12.sp, color = SecondaryLabel)
                        }
                    }
                }

                // ── Gemini 分析テキスト ──────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SystemDark)
                        .padding(16.dp)
                ) {
                    MarkdownContent(
                        text = result.analysisText,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

            } else if (poseUri != null) {
                // 骨格推定のみ完了（Gemini 未実行）
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SystemDark)
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = "骨格推定が完了しています",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryLabel
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "AI 分析も実行するには API キーを設定してください。",
                            fontSize = 14.sp,
                            color = SecondaryLabel,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    viewModel.resetAnalysis()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SecondaryLabel)
            ) {
                Text("ホームに戻る", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ── 動画セクション（元動画 ↔ 骨格推定動画 タブ切替） ────────────────

@Composable
private fun VideoSection(
    originalUri: String,
    poseVideoPath: String?
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val hasPoseVideo = poseVideoPath != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SystemDark)
    ) {
        // タブ（骨格推定動画がある場合のみ表示）
        if (hasPoseVideo) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VideoTabChip(
                    label = "元動画",
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.weight(1f)
                )
                VideoTabChip(
                    label = "骨格推定",
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Text(
                text = "元動画",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = SecondaryLabel,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }

        val currentUri = when {
            selectedTab == 1 && poseVideoPath != null ->
                // ローカルファイルパスを file:// に変換
                if (poseVideoPath.startsWith("/")) "file://$poseVideoPath" else poseVideoPath
            else -> originalUri
        }

        key(currentUri) {
            VideoPlayer(
                videoUri = currentUri,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            )
        }
    }
}

// ── タブチップ ──────────────────────────────────────────────────

@Composable
private fun VideoTabChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(34.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) iOSBlue else SystemFill,
            contentColor   = if (selected) Color.White else SecondaryLabel
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
    ) {
        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

// ── ExoPlayer ビュー ────────────────────────────────────────────

@Composable
private fun VideoPlayer(
    videoUri: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val player = remember(videoUri) {
        ExoPlayer.Builder(context).build().apply {
            val uri = Uri.parse(videoUri)
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = false
            prepare()
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = true
                setBackgroundColor(android.graphics.Color.BLACK)
            }
        },
        modifier = modifier
    )
}

