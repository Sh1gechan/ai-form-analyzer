package com.sportanalyzer.app.ui.screens.results

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Share
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
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.sportanalyzer.app.domain.model.AnalysisResult
import com.sportanalyzer.app.ui.MainViewModel
import com.sportanalyzer.app.ui.MainUiState
import com.sportanalyzer.app.ui.components.MarkdownContent
import com.sportanalyzer.app.ui.components.SimpleNavBar
import com.sportanalyzer.app.ui.navigation.Screen
import com.sportanalyzer.app.ui.theme.*

@Composable
fun ResultsScreen(
    navController: NavController,
    analysisId: String?,
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SystemBlack)
    ) {
        SimpleNavBar(
            title = "分析結果",
            onBack = { navController.popBackStack() },
            trailingContent = { ShareButton(uiState = uiState) }
        )

        ResultsBody(
            uiState = uiState,
            analysisId = analysisId,
            viewModel = viewModel,
            navController = navController
        )
    }
}

// ── 共有ボタン（AnalysisResult を直接パラメータで受け取り smart cast 回避）─

@Composable
private fun ShareButton(uiState: MainUiState) {
    val context = LocalContext.current
    val analysisResult: AnalysisResult? = uiState.analysisResult
    if (analysisResult != null) {
        IconButton(onClick = {
            val score = (analysisResult.overallStats.averageFormScore * 100).toInt()
            val mode = if (uiState.usePoseEstimation) "骨格推定" else "スタンダード"
            val shareText = buildString {
                appendLine("📊 Forma - フォーム分析結果")
                appendLine("スコア: $score/100 [$mode]")
                appendLine("──────────")
                appendLine(analysisResult.analysisText)
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
    }
}

// ── メインボディ（smart cast 問題を避けるため分離）──────────────────

@Composable
private fun ResultsBody(
    uiState: MainUiState,
    analysisId: String?,
    viewModel: MainViewModel,
    navController: NavController
) {
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
        val poseUri = uiState.savedPoseVideoPath

        if (originalUri != null) {
            VideoSection(
                originalUri = originalUri,
                poseVideoPath = poseUri
            )
        }

        // ── 分析結果セクション ────────────────────────────────────
        val analysisResult: AnalysisResult? = uiState.analysisResult

        if (analysisResult != null) {
            ScoreBanner(analysisResult = analysisResult)
            AnalysisTextSection(analysisText = analysisResult.analysisText)
        } else if (poseUri != null) {
            PoseOnlyMessage()
        }

        // ── AIに質問ボタン ──────────────────────────────────
        if (analysisResult != null) {
            Button(
                onClick = {
                    viewModel.initChat(analysisResult.analysisText)
                    navController.navigate(Screen.Chat.createRoute(analysisId ?: "current"))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = iOSIndigo)
            ) {
                Text("💬 AIにもっと質問する", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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

// ── スコアバナー ────────────────────────────────────────────────

@Composable
private fun ScoreBanner(analysisResult: AnalysisResult) {
    val score = (analysisResult.overallStats.averageFormScore * 100).toInt()
    val scoreColor = when {
        score >= 80 -> iOSGreen
        score >= 60 -> iOSOrange
        else -> iOSRed
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
}

// ── 分析テキストセクション ──────────────────────────────────────

@Composable
private fun AnalysisTextSection(analysisText: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SystemDark)
            .padding(16.dp)
    ) {
        MarkdownContent(
            text = analysisText,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ── 骨格推定のみ完了メッセージ ──────────────────────────────────

@Composable
private fun PoseOnlyMessage() {
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
        if (hasPoseVideo) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VideoTabChip("元動画", selectedTab == 0, { selectedTab = 0 }, Modifier.weight(1f))
                VideoTabChip("骨格推定", selectedTab == 1, { selectedTab = 1 }, Modifier.weight(1f))
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
                if (poseVideoPath.startsWith("/")) "file://$poseVideoPath" else poseVideoPath
            else -> originalUri
        }

        var playbackSpeed by remember { mutableFloatStateOf(1.0f) }

        key(currentUri) {
            VideoPlayer(
                videoUri = currentUri,
                speed = playbackSpeed,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("速度", fontSize = 12.sp, color = SecondaryLabel, modifier = Modifier.padding(end = 4.dp))
            listOf(0.25f, 0.5f, 1.0f, 1.5f, 2.0f).forEach { speed ->
                val label = if (speed == 1.0f) "1x" else "${speed}x"
                VideoTabChip(label, playbackSpeed == speed, { playbackSpeed = speed }, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun VideoTabChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.height(34.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) iOSBlue else SystemFill,
            contentColor = if (selected) Color.White else SecondaryLabel
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
    ) {
        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

// ── ExoPlayer ビュー ────────────────────────────────────────────

@Composable
private fun VideoPlayer(videoUri: String, speed: Float = 1.0f, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var playerError by remember { mutableStateOf<String?>(null) }

    val player = remember(videoUri) {
        try {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(Uri.parse(videoUri)))
                repeatMode = Player.REPEAT_MODE_ONE
                playWhenReady = false
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        playerError = when (error.errorCode) {
                            androidx.media3.common.PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                                "動画ファイルが見つかりません"
                            androidx.media3.common.PlaybackException.ERROR_CODE_IO_NO_PERMISSION ->
                                "動画へのアクセス権限がありません"
                            else -> "動画を再生できません (${error.errorCode})"
                        }
                    }
                })
                prepare()
            }
        } catch (e: Exception) {
            playerError = "動画プレーヤーの初期化に失敗しました"
            null
        }
    }

    LaunchedEffect(speed, player) {
        if (player != null && player.playbackState != Player.STATE_IDLE) {
            try { player.playbackParameters = PlaybackParameters(speed) }
            catch (_: Exception) { /* player already released */ }
        }
    }

    DisposableEffect(player) {
        onDispose { player?.release() }
    }

    if (playerError != null) {
        Box(modifier = modifier.background(SystemDark), contentAlignment = Alignment.Center) {
            Text(playerError ?: "", fontSize = 13.sp, color = SecondaryLabel, modifier = Modifier.padding(16.dp))
        }
    } else if (player != null) {
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
}
