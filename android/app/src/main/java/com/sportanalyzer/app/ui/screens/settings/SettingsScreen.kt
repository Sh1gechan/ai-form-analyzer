package com.sportanalyzer.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.sportanalyzer.app.ui.MainViewModel
import com.sportanalyzer.app.ui.screens.home.SectionLabel
import com.sportanalyzer.app.ui.theme.*

private data class GeminiModel(
    val id: String,
    val displayName: String,
    val description: String
)

private val geminiModels = listOf(
    GeminiModel("gemini-2.5-pro",        "Gemini 2.5 Pro",        "最高品質・最新（処理時間長め）"),
    GeminiModel("gemini-2.5-flash",      "Gemini 2.5 Flash",      "推奨・高速・高品質"),
    GeminiModel("gemini-2.5-flash-lite", "Gemini 2.5 Flash Lite", "軽量・最速")
)

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var apiKey by remember(uiState.apiKey) { mutableStateOf(uiState.apiKey) }
    var customPrompt by remember(uiState.customPrompt) { mutableStateOf(uiState.customPrompt) }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var showSavedMessage by remember { mutableStateOf(false) }

    LaunchedEffect(showSavedMessage) {
        if (showSavedMessage) {
            kotlinx.coroutines.delay(2500)
            showSavedMessage = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SystemBlack)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Text("設定", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = PrimaryLabel)

        Spacer(modifier = Modifier.height(28.dp))

        // ── モデル選択 ────────────────────────────────────────────
        SectionLabel("Gemini モデル")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SystemDark)
        ) {
            geminiModels.forEachIndexed { index, model ->
                val isSelected = uiState.selectedModel == model.id
                val isLast = index == geminiModels.lastIndex
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.updateModel(model.id)
                                viewModel.saveModel()
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = model.displayName,
                                fontSize = 16.sp,
                                color = PrimaryLabel,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                            Text(text = model.description, fontSize = 13.sp, color = SecondaryLabel)
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = iOSBlue, modifier = Modifier.size(20.dp))
                        }
                    }
                    if (!isLast) {
                        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).padding(start = 14.dp).background(Separator))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("選択したモデルはAPI版分析で使用されます", fontSize = 12.sp, color = SecondaryLabel, modifier = Modifier.padding(start = 4.dp))

        Spacer(modifier = Modifier.height(28.dp))

        // ── API キー ──────────────────────────────────────────────
        SectionLabel("Gemini API")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SystemDark)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                    Box(
                        modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(iOSBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("APIキー", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = PrimaryLabel)
                    Spacer(modifier = Modifier.weight(1f))
                    if (apiKey.isNotEmpty()) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = iOSGreen, modifier = Modifier.size(18.dp))
                    }
                }
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    placeholder = { Text("AIza...", color = SecondaryLabel) },
                    visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                            Icon(
                                if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = SecondaryLabel,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = iOSBlue,
                        unfocusedBorderColor = Separator,
                        focusedTextColor = PrimaryLabel,
                        unfocusedTextColor = PrimaryLabel,
                        cursorColor = iOSBlue
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Google AI Studio でAPIキーを取得してください", fontSize = 12.sp, color = SecondaryLabel, modifier = Modifier.padding(start = 4.dp))

        Spacer(modifier = Modifier.height(28.dp))

        // ── カスタムプロンプト ─────────────────────────────────────
        SectionLabel("プロンプト設定")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SystemDark)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                    Box(
                        modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(iOSIndigo),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.EditNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("カスタムプロンプト", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = PrimaryLabel)
                }
                OutlinedTextField(
                    value = customPrompt,
                    onValueChange = { customPrompt = it },
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    maxLines = 6,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = iOSBlue,
                        unfocusedBorderColor = Separator,
                        focusedTextColor = PrimaryLabel,
                        unfocusedTextColor = PrimaryLabel,
                        cursorColor = iOSBlue
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(onClick = { customPrompt = viewModel.getDefaultPrompt() }, contentPadding = PaddingValues(0.dp)) {
                    Text("デフォルトに戻す", fontSize = 14.sp, color = iOSBlue)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("分析で使用するプロンプトをカスタマイズできます", fontSize = 12.sp, color = SecondaryLabel, modifier = Modifier.padding(start = 4.dp))

        Spacer(modifier = Modifier.height(28.dp))

        // ── 保存ボタン ────────────────────────────────────────────
        Button(
            onClick = {
                viewModel.updateApiKey(apiKey)
                viewModel.saveApiKey()
                viewModel.updateCustomPrompt(customPrompt)
                viewModel.saveCustomPrompt()
                showSavedMessage = true
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = iOSBlue)
        ) {
            Text("設定を保存", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        if (showSavedMessage) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(iOSGreen.copy(alpha = 0.12f))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = iOSGreen, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("設定を保存しました", fontSize = 14.sp, color = iOSGreen, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
