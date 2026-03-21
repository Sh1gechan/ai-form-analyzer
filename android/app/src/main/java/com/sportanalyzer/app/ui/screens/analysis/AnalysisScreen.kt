package com.sportanalyzer.app.ui.screens.analysis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.sportanalyzer.app.ui.AnalysisState
import com.sportanalyzer.app.ui.MainViewModel
import com.sportanalyzer.app.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun AnalysisScreen(
    navController: NavController,
    videoUri: String?,
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // 分析開始：処理中でなければ常にリセットして再開
    // （同じURIの再分析・ホームから戻って再選択の両方に対応）
    LaunchedEffect(videoUri) {
        if (videoUri != null) {
            val active = uiState.analysisState in listOf(
                AnalysisState.LOADING, AnalysisState.PROCESSING, AnalysisState.API_ANALYSIS
            )
            if (!active) {
                viewModel.resetAnalysis()
                viewModel.startAnalysis(videoUri)
            }
        }
    }

    // 完了 → サマリー画面へ自動遷移
    LaunchedEffect(uiState.analysisState) {
        if (uiState.analysisState == AnalysisState.COMPLETED) {
            delay(400)
            uiState.analysisId?.let { id ->
                navController.navigate("summary/$id") {
                    popUpTo("analysis/{videoUri}") { inclusive = true }
                }
            }
        }
    }

    val isProcessing = uiState.analysisState in listOf(
        AnalysisState.LOADING,
        AnalysisState.PROCESSING,
        AnalysisState.API_ANALYSIS
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SystemBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.analysisState == AnalysisState.ERROR) {
                ErrorView(
                    message = uiState.errorMessage ?: "不明なエラーが発生しました",
                    onRetry = {
                        viewModel.resetAnalysis()
                        if (videoUri != null) viewModel.startAnalysis(videoUri)
                    },
                    onBack = {
                        viewModel.resetAnalysis()
                        navController.popBackStack()
                    }
                )
            } else {
                ProcessingView(
                    progress = uiState.progress,
                    stepText = uiState.currentStep.ifEmpty { "分析を準備しています…" },
                    isApiPhase = uiState.analysisState == AnalysisState.API_ANALYSIS
                )
            }
        }

        if (isProcessing) {
            TextButton(
                onClick = {
                    viewModel.resetAnalysis()
                    navController.popBackStack()
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            ) {
                Text("キャンセル", fontSize = 16.sp, color = iOSRed)
            }
        }
    }
}

@Composable
private fun ProcessingView(progress: Float, stepText: String, isApiPhase: Boolean) {
    CircularProgressIndicator(
        modifier = Modifier.size(64.dp),
        color = iOSBlue,
        strokeWidth = 3.dp,
        trackColor = SystemFill
    )

    Spacer(modifier = Modifier.height(28.dp))

    Text(
        text = if (isApiPhase) "AIが分析中" else "動画を処理中",
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        color = PrimaryLabel
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = stepText,
        fontSize = 14.sp,
        color = SecondaryLabel,
        textAlign = TextAlign.Center,
        lineHeight = 20.sp
    )

    if (progress > 0f) {
        Spacer(modifier = Modifier.height(24.dp))
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth(0.65f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = iOSBlue,
            trackColor = SystemFill
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${(progress * 100).toInt()}%",
            fontSize = 13.sp,
            color = SecondaryLabel
        )
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    Icon(
        Icons.Default.ErrorOutline,
        contentDescription = null,
        tint = iOSRed,
        modifier = Modifier.size(56.dp)
    )
    Spacer(modifier = Modifier.height(20.dp))
    Text("エラーが発生しました", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = PrimaryLabel)
    Spacer(modifier = Modifier.height(10.dp))
    Text(message, fontSize = 14.sp, color = SecondaryLabel, textAlign = TextAlign.Center, lineHeight = 20.sp)
    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = onRetry,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = iOSBlue)
    ) { Text("再試行", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }

    Spacer(modifier = Modifier.height(10.dp))

    OutlinedButton(
        onClick = onBack,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = SecondaryLabel)
    ) { Text("ホームに戻る", fontSize = 16.sp) }
}
