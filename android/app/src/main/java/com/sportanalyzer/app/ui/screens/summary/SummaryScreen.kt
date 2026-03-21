package com.sportanalyzer.app.ui.screens.summary

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.sportanalyzer.app.ui.MainViewModel
import com.sportanalyzer.app.ui.components.SimpleNavBar
import com.sportanalyzer.app.ui.navigation.Screen
import com.sportanalyzer.app.ui.theme.*

@Composable
fun SummaryScreen(
    navController: NavController,
    analysisId: String?,
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val result = uiState.analysisResult

    val score = result?.overallStats?.averageFormScore?.times(100)?.toInt() ?: 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SystemBlack)
    ) {
        SimpleNavBar(title = "分析サマリー", onBack = { navController.popBackStack() })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── スコアリング ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SystemDark)
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "TOTAL SCORE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = SecondaryLabel
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    ScoreRing(score = score)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = when {
                            score >= 90 -> "Excellent"
                            score >= 80 -> "Good"
                            score >= 70 -> "Fair"
                            score > 0   -> "Keep Going"
                            else        -> "分析完了"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryLabel
                    )
                    if (score == 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "スコアは詳細結果に記載されています",
                            fontSize = 12.sp,
                            color = SecondaryLabel
                        )
                    }
                }
            }

            // ── Gemini 分析プレビュー ─────────────────────────────────
            if (result != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SystemDark)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "AI 分析プレビュー",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SecondaryLabel,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    // 最初の 200 文字だけ表示（詳細は ResultsScreen で）
                    val preview = result.analysisText.take(200).let {
                        if (result.analysisText.length > 200) "$it…" else it
                    }
                    Text(
                        text = preview,
                        fontSize = 14.sp,
                        color = PrimaryLabel,
                        lineHeight = 21.sp
                    )
                }
            }

            // ── アクションボタン ──────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        navController.navigate("results/$analysisId") {
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = iOSBlue)
                ) {
                    Icon(
                        Icons.Default.TrendingUp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "詳細結果を見る",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }

                OutlinedButton(
                    onClick = {
                        viewModel.resetAnalysis()
                        // popUpTo で安全にホームに戻る
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
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ── Score Ring ─────────────────────────────────────────────────────

@Composable
private fun ScoreRing(score: Int) {
    val ringColor = when {
        score >= 80 -> iOSBlue
        score >= 60 -> iOSOrange
        score > 0   -> iOSRed
        else        -> SecondaryLabel
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(148.dp)
    ) {
        Canvas(modifier = Modifier.size(148.dp)) {
            val strokeWidth = 12.dp.toPx()
            val inset = strokeWidth / 2f
            val arcTopLeft = Offset(inset, inset)
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)

            drawArc(
                color = SystemFill,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = arcTopLeft,
                size = arcSize
            )
            if (score > 0) {
                drawArc(
                    color = ringColor,
                    startAngle = 135f,
                    sweepAngle = 270f * (score / 100f),
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    topLeft = arcTopLeft,
                    size = arcSize
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (score > 0) "$score" else "—",
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryLabel
            )
            Text(text = "/ 100", fontSize = 13.sp, color = SecondaryLabel)
        }
    }
}
