package com.sportanalyzer.app.ui.screens.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.sportanalyzer.app.ui.MainViewModel
import com.sportanalyzer.app.ui.navigation.Screen
import com.sportanalyzer.app.ui.screens.home.SectionLabel
import com.sportanalyzer.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: MainViewModel = hiltViewModel()
) {
    val historyList by viewModel.historyList.collectAsState()
    var isEditMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    // ── ストレージ管理ステート ─────────────────────────────────
    var sessions by remember { mutableStateOf(listOf<MainViewModel.AnalysisSession>()) }
    var selectedPaths by remember { mutableStateOf(setOf<String>()) }
    var showStorageDeleteDialog by remember { mutableStateOf(false) }
    var storageDeleteAll by remember { mutableStateOf(false) }
    var deletedMessage by remember { mutableStateOf("") }

    fun refreshSessions() {
        sessions = viewModel.getAnalysisSessionList()
        selectedPaths = emptySet()
    }
    LaunchedEffect(Unit) { refreshSessions() }

    // 削除完了メッセージ消去
    LaunchedEffect(deletedMessage) {
        if (deletedMessage.isNotEmpty()) {
            kotlinx.coroutines.delay(3000)
            deletedMessage = ""
        }
    }

    // ── 解析履歴 削除確認ダイアログ ──────────────────────────────
    if (showDeleteDialog && selectedIds.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = SystemDark,
            titleContentColor = PrimaryLabel,
            textContentColor = SecondaryLabel,
            title = { Text("${selectedIds.size}件を削除", fontWeight = FontWeight.SemiBold) },
            text  = { Text("選択した解析結果を削除します。この操作は取り消せません。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteHistoryRecords(selectedIds)
                    selectedIds = emptySet()
                    isEditMode  = false
                    showDeleteDialog = false
                }) { Text("削除", color = iOSRed, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("キャンセル", color = iOSBlue) }
            }
        )
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            containerColor = SystemDark,
            titleContentColor = PrimaryLabel,
            textContentColor = SecondaryLabel,
            title = { Text("すべて削除", fontWeight = FontWeight.SemiBold) },
            text  = { Text("すべての記録を削除します。この操作は取り消せません。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearHistory()
                    isEditMode = false
                    selectedIds = emptySet()
                    showClearAllDialog = false
                }) { Text("削除", color = iOSRed, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) { Text("キャンセル", color = iOSBlue) }
            }
        )
    }

    // ── 動画ファイル削除確認ダイアログ ───────────────────────────
    if (showStorageDeleteDialog) {
        val targetPaths = if (storageDeleteAll) sessions.map { it.dirPath } else selectedPaths.toList()
        AlertDialog(
            onDismissRequest = { showStorageDeleteDialog = false },
            containerColor = SystemDark,
            titleContentColor = PrimaryLabel,
            textContentColor = SecondaryLabel,
            title = { Text(if (storageDeleteAll) "動画をすべて削除" else "選択した動画を削除", fontWeight = FontWeight.SemiBold) },
            text  = { Text("骨格推定動画ファイルを削除します。この操作は取り消せません。") },
            confirmButton = {
                TextButton(onClick = {
                    showStorageDeleteDialog = false
                    viewModel.deleteAnalysisSessions(targetPaths) { deleted ->
                        val mb = "%.1f".format(deleted / 1_048_576.0)
                        deletedMessage = "${mb} MB を削除しました"
                        refreshSessions()
                    }
                }) { Text("削除", color = iOSRed, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showStorageDeleteDialog = false }) { Text("キャンセル", color = iOSBlue) }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SystemBlack)
    ) {
        // ── ナビゲーションバー ───────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SystemDark)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isEditMode) {
                TextButton(onClick = { isEditMode = false; selectedIds = emptySet() }) {
                    Text("完了", color = iOSBlue, fontSize = 17.sp)
                }
            } else {
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = "記録",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryLabel,
                modifier = Modifier.weight(1f).wrapContentWidth(Alignment.CenterHorizontally)
            )

            if (historyList.isNotEmpty()) {
                if (isEditMode) {
                    TextButton(onClick = { showClearAllDialog = true }) {
                        Text("すべて削除", color = iOSRed, fontSize = 15.sp)
                    }
                } else {
                    TextButton(onClick = { isEditMode = true }) {
                        Text("編集", color = iOSBlue, fontSize = 17.sp)
                    }
                }
            } else {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {

            // ── 解析履歴 空状態 ──────────────────────────────────
            if (historyList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, tint = SystemFill2, modifier = Modifier.size(56.dp))
                            Text("まだ記録がありません", fontSize = 17.sp, color = SecondaryLabel)
                            Text("分析すると、ここに記録されます", fontSize = 14.sp, color = SystemFill2)
                        }
                    }
                }
            } else {
                // 編集モード：全選択ボタン
                if (isEditMode) {
                    item {
                        val allSelected = selectedIds.size == historyList.size
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = {
                                selectedIds = if (allSelected) emptySet() else historyList.map { it.id }.toSet()
                            }) {
                                Text(if (allSelected) "全選択解除" else "全選択", color = iOSBlue, fontSize = 15.sp)
                            }
                            if (selectedIds.isNotEmpty()) {
                                Text("${selectedIds.size}件選択中", fontSize = 13.sp, color = SecondaryLabel)
                            }
                        }
                    }
                }

                // 解析履歴リスト
                items(historyList, key = { it.id }) { record ->
                    val isSelected = record.id in selectedIds
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)) {
                        HistoryCard(
                            record     = record,
                            isEditMode = isEditMode,
                            isSelected = isSelected,
                            onTap = {
                                if (isEditMode) {
                                    selectedIds = if (isSelected) selectedIds - record.id else selectedIds + record.id
                                } else {
                                    navController.navigate(Screen.HistoryDetail.createRoute(record.id))
                                }
                            },
                            onLongPress = {
                                if (!isEditMode) { isEditMode = true; selectedIds = setOf(record.id) }
                            }
                        )
                    }
                }

                // 選択アクションボタン
                if (isEditMode && selectedIds.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 2件選択時：比較ボタン
                            if (selectedIds.size == 2) {
                                val ids = selectedIds.toList()
                                Button(
                                    onClick = {
                                        navController.navigate(Screen.Compare.createRoute(ids[0], ids[1]))
                                    },
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = iOSBlue)
                                ) {
                                    Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("2件を比較する", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            // 削除ボタン
                            Button(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = iOSRed)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("${selectedIds.size}件を削除", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // ── ストレージ管理セクション ─────────────────────────────
            item {
                Spacer(modifier = Modifier.height(28.dp))
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionLabel("骨格推定動画ファイル")
                        Spacer(modifier = Modifier.weight(1f))
                        if (sessions.isNotEmpty()) {
                            TextButton(
                                onClick = { storageDeleteAll = true; showStorageDeleteDialog = true },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Text("すべて削除", fontSize = 13.sp, color = iOSRed)
                            }
                        }
                    }

                    if (sessions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SystemDark)
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("保存された動画ファイルはありません", fontSize = 14.sp, color = SecondaryLabel)
                        }
                    } else {
                        // 合計サイズ表示
                        val totalBytes = sessions.sumOf { it.sizeBytes }
                        Text(
                            "合計 ${"%.1f".format(totalBytes / 1_048_576.0)} MB（${sessions.size}件）",
                            fontSize = 12.sp,
                            color = SecondaryLabel,
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(SystemDark)
                        ) {
                            sessions.forEachIndexed { index, session ->
                                val isChecked = session.dirPath in selectedPaths
                                val isLast = index == sessions.lastIndex
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedPaths = if (isChecked)
                                                    selectedPaths - session.dirPath
                                                else
                                                    selectedPaths + session.dirPath
                                            }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = {
                                                selectedPaths = if (it) selectedPaths + session.dirPath else selectedPaths - session.dirPath
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = iOSBlue,
                                                uncheckedColor = SecondaryLabel
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(session.name, fontSize = 14.sp, color = PrimaryLabel)
                                            Text(
                                                "${"%.1f".format(session.sizeBytes / 1_048_576.0)} MB",
                                                fontSize = 12.sp,
                                                color = SecondaryLabel
                                            )
                                        }
                                    }
                                    if (!isLast) {
                                        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).padding(start = 56.dp).background(Separator))
                                    }
                                }
                            }
                        }

                        // 選択削除ボタン
                        if (selectedPaths.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            val selectedBytes = sessions.filter { it.dirPath in selectedPaths }.sumOf { it.sizeBytes }
                            Button(
                                onClick = { storageDeleteAll = false; showStorageDeleteDialog = true },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = iOSRed)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "${selectedPaths.size}件を削除（${"%.1f".format(selectedBytes / 1_048_576.0)} MB）",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // 削除完了メッセージ
                    if (deletedMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(iOSRed.copy(alpha = 0.10f))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = iOSRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(deletedMessage, fontSize = 13.sp, color = iOSRed)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryCard(
    record: MainViewModel.AnalysisRecord,
    isEditMode: Boolean,
    isSelected: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val preview = record.analysisText.lines()
        .firstOrNull { it.isNotBlank() }
        ?.take(60)
        ?.let { if (record.analysisText.length > 60) "$it…" else it }
        ?: ""

    val scoreColor = when {
        record.score >= 80 -> iOSBlue
        record.score >= 60 -> iOSOrange
        record.score > 0   -> iOSRed
        else               -> SecondaryLabel
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SystemDark)
            .combinedClickable(onClick = onTap, onLongClick = onLongPress)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isEditMode) {
            Icon(
                if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) iOSBlue else SecondaryLabel,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = record.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = PrimaryLabel)
                val modeLabel = if (record.analysisMode == "pose") "骨格推定" else "スタンダード"
                val modeBg    = if (record.analysisMode == "pose") iOSIndigo else iOSBlue
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(modeBg.copy(alpha = 0.18f))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(modeLabel, fontSize = 11.sp, color = modeBg, fontWeight = FontWeight.SemiBold)
                }
            }
            if (preview.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(preview, fontSize = 13.sp, color = SecondaryLabel, maxLines = 2, lineHeight = 18.sp)
            }
        }

        Spacer(modifier = Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.End) {
            if (record.score > 0) {
                Text(text = "${record.score}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = scoreColor)
                Text("点", fontSize = 11.sp, color = SecondaryLabel)
            }
        }

        if (!isEditMode) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SystemFill2, modifier = Modifier.size(18.dp))
        }
    }
}
