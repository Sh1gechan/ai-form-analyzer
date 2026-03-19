package com.sportanalyzer.app.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.sportanalyzer.app.ui.ChatMessage
import com.sportanalyzer.app.ui.MainViewModel
import com.sportanalyzer.app.ui.components.MarkdownContent
import com.sportanalyzer.app.ui.components.SimpleNavBar
import com.sportanalyzer.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    navController: NavController,
    analysisId: String?,
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // 分析結果からチャットを初期化（まだ初期化されていない場合）
    LaunchedEffect(analysisId) {
        if (uiState.chatHistory.isEmpty()) {
            // 現在の分析結果から初期化を試みる
            val result = uiState.analysisResult
            if (result != null) {
                viewModel.initChat(result.analysisText)
            } else if (analysisId != null) {
                // 履歴から初期化
                viewModel.initChatFromHistory(analysisId)
            }
        }
    }

    // 新しいメッセージが追加されたら自動スクロール
    LaunchedEffect(uiState.chatHistory.size) {
        if (uiState.chatHistory.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(uiState.chatHistory.size - 1)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SystemBlack)
    ) {
        SimpleNavBar(
            title = "AIに質問",
            onBack = { navController.popBackStack() }
        )

        // ── チャット履歴 ─────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(uiState.chatHistory) { message ->
                ChatBubble(message = message)
            }

            // ローディング表示
            if (uiState.isChatLoading) {
                item {
                    Row(
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = iOSBlue,
                            strokeWidth = 2.dp
                        )
                        Text("AIが回答中…", fontSize = 13.sp, color = SecondaryLabel)
                    }
                }
            }
        }

        // ── 入力欄 ───────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SystemDark)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .imePadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("質問を入力…", color = SecondaryLabel, fontSize = 15.sp) },
                modifier = Modifier.weight(1f),
                singleLine = false,
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = iOSBlue,
                    unfocusedBorderColor = Separator,
                    focusedTextColor = PrimaryLabel,
                    unfocusedTextColor = PrimaryLabel,
                    cursorColor = iOSBlue
                ),
                shape = RoundedCornerShape(20.dp)
            )

            IconButton(
                onClick = {
                    if (inputText.isNotBlank() && !uiState.isChatLoading) {
                        viewModel.sendFollowUp(inputText.trim())
                        inputText = ""
                    }
                },
                enabled = inputText.isNotBlank() && !uiState.isChatLoading
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "送信",
                    tint = if (inputText.isNotBlank() && !uiState.isChatLoading) iOSBlue else SystemFill2
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == "user"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Text(
            text = if (isUser) "あなた" else "AI",
            fontSize = 11.sp,
            color = SecondaryLabel,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )

        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 14.dp,
                        topEnd = 14.dp,
                        bottomStart = if (isUser) 14.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 14.dp
                    )
                )
                .background(if (isUser) iOSBlue else SystemDark)
                .padding(12.dp)
        ) {
            if (isUser) {
                Text(
                    text = message.text,
                    fontSize = 15.sp,
                    color = Color.White,
                    lineHeight = 21.sp
                )
            } else {
                MarkdownContent(
                    text = message.text,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
