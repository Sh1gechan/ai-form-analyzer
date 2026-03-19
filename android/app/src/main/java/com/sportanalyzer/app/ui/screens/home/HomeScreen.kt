package com.sportanalyzer.app.ui.screens.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.sportanalyzer.app.ui.MainViewModel
import com.sportanalyzer.app.ui.theme.*

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: MainViewModel = hiltViewModel()
) {
    // 元動画分析用ピッカー（骨格推定 OFF）→ トリム画面へ
    val rawVideoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.setPoseEstimation(false)
            navController.navigate("video_trim/${Uri.encode(it.toString())}")
        }
    }

    // 骨格推定分析用ピッカー（骨格推定 ON）→ トリム画面へ
    val poseVideoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.setPoseEstimation(true)
            navController.navigate("video_trim/${Uri.encode(it.toString())}")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SystemBlack)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        // ── Large Title ──────────────────────────────────────────
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Forma",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryLabel
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "AIがあなたのフォームを分析し、次のレベルへ導きます",
            fontSize = 15.sp,
            color = SecondaryLabel
        )

        Spacer(modifier = Modifier.height(36.dp))

        // ── 分析モード選択 ────────────────────────────────────────
        SectionLabel("分析")
        GroupedList {
            AppListRow(
                icon    = Icons.Default.VideoLibrary,
                iconBg  = iOSBlue,
                title   = "スタンダード",
                detail  = "動画をそのまま AI に送信。すぐに結果が届く",
                badgeText  = "速い",
                badgeColor = iOSGreen,
                isLast  = false,
                onClick = { rawVideoPickerLauncher.launch("video/*") }
            )
            AppListRow(
                icon    = Icons.Default.AccessibilityNew,
                iconBg  = iOSIndigo,
                title   = "骨格推定",
                detail  = "骨格を読み取り、より精密なフィードバックを提供",
                badgeText  = "精密",
                badgeColor = iOSOrange,
                isLast  = true,
                onClick = { poseVideoPickerLauncher.launch("video/*") }
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── カメラ ────────────────────────────────────────────────
        SectionLabel("撮影")
        GroupedList {
            AppListRow(
                icon   = Icons.Default.Videocam,
                iconBg = iOSGreen,
                title  = "カメラで撮影",
                detail = "今すぐ動画を撮影する",
                isLast = true,
                onClick = { navController.navigate("camera") }
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── 設定リンク ────────────────────────────────────────────
        SectionLabel("設定")
        GroupedList {
            AppListRow(
                icon   = Icons.Default.Settings,
                iconBg = SystemFill2,
                title  = "APIキー・モデル設定",
                detail = "Gemini APIキーとモデルを設定する",
                isLast = true,
                onClick = { navController.navigate("settings") }
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// ── Shared Design Components ────────────────────────────────────

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = SecondaryLabel,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
fun GroupedList(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SystemDark),
        content = content
    )
}

@Composable
fun AppListRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    detail: String? = null,
    badgeText: String? = null,
    badgeColor: Color = iOSBlue,
    isLast: Boolean = false,
    trailingText: String? = null,
    onClick: (() -> Unit)? = null
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = title, fontSize = 16.sp, color = PrimaryLabel)
                    if (badgeText != null) {
                        Spacer(modifier = Modifier.width(7.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(badgeColor.copy(alpha = 0.18f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(badgeText, fontSize = 11.sp, color = badgeColor, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                if (detail != null) Text(detail, fontSize = 13.sp, color = SecondaryLabel)
            }
            if (trailingText != null) {
                Text(trailingText, fontSize = 15.sp, color = SecondaryLabel)
                Spacer(modifier = Modifier.width(4.dp))
            }
            if (onClick != null) {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SystemFill2, modifier = Modifier.size(20.dp))
            }
        }
        if (!isLast) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .padding(start = 58.dp)
                    .background(Separator)
            )
        }
    }
}

