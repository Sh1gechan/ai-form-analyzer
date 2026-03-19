package com.sportanalyzer.app.ui.screens.compare

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Remove
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
import com.sportanalyzer.app.ui.MainViewModel
import com.sportanalyzer.app.ui.components.MarkdownContent
import com.sportanalyzer.app.ui.components.SimpleNavBar
import com.sportanalyzer.app.ui.theme.*

@Composable
fun CompareScreen(
    navController: NavController,
    recordId1: String,
    recordId2: String,
    viewModel: MainViewModel = hiltViewModel()
) {
    val record1 = remember(recordId1) { viewModel.getHistoryRecord(recordId1) }
    val record2 = remember(recordId2) { viewModel.getHistoryRecord(recordId2) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SystemBlack)
    ) {
        SimpleNavBar(
            title = "比較分析",
            onBack = { navController.popBackStack() }
        )

        if (record1 == null || record2 == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("比較データが見つかりません", color = SecondaryLabel, fontSize = 16.sp)
            }
            return@Column
        }

        // 古い方を左（Before）、新しい方を右（After）に配置
        val (before, after) = if (record1.timestamp <= record2.timestamp)
            record1 to record2 else record2 to record1

        val scoreDiff = after.score - before.score

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── スコア比較ヘッダー ───────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SystemDark)
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SCORE COMPARISON",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = SecondaryLabel
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Before
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("BEFORE", fontSize = 10.sp, color = SecondaryLabel, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${before.score}",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = scoreColor(before.score)
                            )
                            Text(text = "/ 100", fontSize = 12.sp, color = SecondaryLabel)
                        }

                        // 差分表示
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val diffColor = when {
                                scoreDiff > 0 -> iOSGreen
                                scoreDiff < 0 -> iOSRed
                                else -> SecondaryLabel
                            }
                            val diffIcon = when {
                                scoreDiff > 0 -> Icons.Default.ArrowUpward
                                scoreDiff < 0 -> Icons.Default.ArrowDownward
                                else -> Icons.Default.Remove
                            }
                            Icon(diffIcon, contentDescription = null, tint = diffColor, modifier = Modifier.size(24.dp))
                            Text(
                                text = if (scoreDiff >= 0) "+$scoreDiff" else "$scoreDiff",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = diffColor
                            )
                        }

                        // After
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("AFTER", fontSize = 10.sp, color = SecondaryLabel, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${after.score}",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = scoreColor(after.score)
                            )
                            Text(text = "/ 100", fontSize = 12.sp, color = SecondaryLabel)
                        }
                    }
                }
            }

            // ── 詳細情報 ─────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InfoChip(
                    label = "Before",
                    value = before.title,
                    modifier = Modifier.weight(1f)
                )
                InfoChip(
                    label = "After",
                    value = after.title,
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Before 分析結果 ──────────────────────────────────
            var selectedTab by remember { mutableIntStateOf(0) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompareTabChip(
                    label = "Before",
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.weight(1f)
                )
                CompareTabChip(
                    label = "After",
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.weight(1f)
                )
            }

            val displayRecord = if (selectedTab == 0) before else after

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SystemDark)
                    .padding(16.dp)
            ) {
                MarkdownContent(
                    text = displayRecord.analysisText,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SystemDark)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, fontSize = 11.sp, color = SecondaryLabel, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, fontSize = 13.sp, color = PrimaryLabel, textAlign = TextAlign.Center)
    }
}

@Composable
private fun CompareTabChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(38.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) iOSBlue else SystemFill,
            contentColor = if (selected) androidx.compose.ui.graphics.Color.White else SecondaryLabel
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
    ) {
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

private fun scoreColor(score: Int) = when {
    score >= 80 -> iOSBlue
    score >= 60 -> iOSOrange
    score > 0 -> iOSRed
    else -> SecondaryLabel
}
