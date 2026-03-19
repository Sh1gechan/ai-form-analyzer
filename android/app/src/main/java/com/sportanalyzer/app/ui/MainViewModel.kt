package com.sportanalyzer.app.ui

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.sportanalyzer.app.data.repository.GeminiRepository
import com.sportanalyzer.app.data.repository.VideoAnalysisRepository
import com.sportanalyzer.app.domain.model.AnalysisResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

enum class AnalysisState {
    IDLE, LOADING, PROCESSING, API_ANALYSIS, COMPLETED, ERROR
}

data class MainUiState(
    val analysisState: AnalysisState = AnalysisState.IDLE,
    val progress: Float = 0f,
    val currentStep: String = "",
    val analysisId: String? = null,
    val errorMessage: String? = null,
    val apiKey: String = "",
    val isApiKeyValid: Boolean = false,
    val customPrompt: String = "",
    val analysisResult: AnalysisResult? = null,
    val videoPath: String? = null,
    val savedPoseVideoPath: String? = null,
    val usePoseEstimation: Boolean = true,
    val selectedModel: String = "gemini-2.5-flash"
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val geminiRepository: GeminiRepository,
    private val videoAnalysisRepository: VideoAnalysisRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // ── Analysis History ──────────────────────────────────────────

    data class AnalysisRecord(
        val id: String,
        val timestamp: Long,
        val title: String,        // 例: "03/20 12:34 · 骨格推定"
        val analysisText: String,
        val score: Int,
        val analysisMode: String  // "raw" | "pose"
    )

    private val _historyList = MutableStateFlow<List<AnalysisRecord>>(emptyList())
    val historyList: StateFlow<List<AnalysisRecord>> = _historyList.asStateFlow()

    private val HISTORY_PREFS = "analysis_history_prefs"
    private val HISTORY_KEY   = "records_json"

    companion object {
        private const val PREFS_NAME = "sport_analyzer_prefs"
    }

    init {
        loadInitialData()
        loadHistory()
    }

    // ── Initializers ──────────────────────────────────────────────

    private fun loadInitialData() {
        viewModelScope.launch {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val apiKey     = prefs.getString("gemini_api_key", "") ?: ""
            val model      = prefs.getString("gemini_model", "gemini-2.5-flash") ?: "gemini-2.5-flash"
            val usePoseEst = prefs.getBoolean("use_pose_estimation", true)

            val PROMPT_VERSION = 4
            val storedVersion  = prefs.getInt("prompt_version", 0)
            val prompt = if (storedVersion < PROMPT_VERSION) {
                val newDefault = getDefaultPrompt()
                prefs.edit()
                    .putString("custom_prompt", newDefault)
                    .putInt("prompt_version", PROMPT_VERSION)
                    .apply()
                newDefault
            } else {
                prefs.getString("custom_prompt", getDefaultPrompt()) ?: getDefaultPrompt()
            }

            _uiState.update {
                it.copy(
                    apiKey            = apiKey,
                    isApiKeyValid     = apiKey.isNotEmpty(),
                    customPrompt      = prompt,
                    selectedModel     = model,
                    usePoseEstimation = usePoseEst
                )
            }
        }
    }

    private fun loadHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            val prefs = context.getSharedPreferences(HISTORY_PREFS, Context.MODE_PRIVATE)
            val json  = prefs.getString(HISTORY_KEY, "[]") ?: "[]"
            val list  = parseHistoryJson(json)
            _historyList.value = list
        }
    }

    // ── Settings helpers ──────────────────────────────────────────

    fun getDefaultPrompt(): String = """
あなたは陸上競技の専門コーチです。この動画を分析し、以下の観点で日本語で詳しくフィードバックしてください。

## フォーム分析
- 腕振り（肘の角度・リズム・左右のバランス）
- 体幹の姿勢（前傾角度・上体のブレ）
- 脚の動き（ストライド・接地位置・膝の引き上げ）
- 足首・接地（フラット・母指球接地・蹴り出し）

## 良い点
具体的に2〜3点挙げてください。

## 改善点・アドバイス
優先度が高い順に2〜3点、具体的な改善方法を含めて説明してください。

## 総合評価
フォームスコアを0〜100で評価し、その根拠を簡潔に述べてください。
""".trimIndent()

    fun updateApiKey(key: String) {
        _uiState.update { it.copy(apiKey = key, isApiKeyValid = key.isNotEmpty()) }
    }

    fun saveApiKey() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString("gemini_api_key", _uiState.value.apiKey).apply()
    }

    fun updateCustomPrompt(prompt: String) {
        _uiState.update { it.copy(customPrompt = prompt) }
    }

    fun saveCustomPrompt() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString("custom_prompt", _uiState.value.customPrompt).apply()
    }

    fun updateModel(model: String) {
        _uiState.update { it.copy(selectedModel = model) }
    }

    fun saveModel() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString("gemini_model", _uiState.value.selectedModel).apply()
    }

    fun togglePoseEstimation() {
        setPoseEstimation(!_uiState.value.usePoseEstimation)
    }

    /** ホーム画面の2モードから直接設定する */
    fun setPoseEstimation(enabled: Boolean) {
        _uiState.update { it.copy(usePoseEstimation = enabled) }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean("use_pose_estimation", enabled).apply()
    }

    // ── Analysis ──────────────────────────────────────────────────

    fun startAnalysis(videoUri: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    analysisState     = AnalysisState.LOADING,
                    currentStep       = "分析を開始しています…",
                    progress          = 0f,
                    errorMessage      = null,
                    videoPath         = videoUri,
                    savedPoseVideoPath = null,
                    analysisResult    = null
                )
            }

            var videoForApi = videoUri

            if (_uiState.value.usePoseEstimation) {
                try {
                    videoAnalysisRepository.analyzeVideo(videoUri = videoUri).collect { p ->
                        if (p.outputVideoPath != null) {
                            videoForApi = p.outputVideoPath
                            Log.d("MainViewModel", "骨格推定動画パス取得: $videoForApi")
                        }
                        _uiState.update {
                            it.copy(
                                currentStep       = p.message,
                                progress          = p.progress * 0.5f,
                                analysisState     = AnalysisState.PROCESSING,
                                savedPoseVideoPath = p.outputVideoPath ?: it.savedPoseVideoPath
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "骨格推定エラー", e)
                    _uiState.update {
                        it.copy(
                            analysisState = AnalysisState.ERROR,
                            errorMessage  = buildPoseErrorMessage(e)
                        )
                    }
                    return@launch
                }
            }

            performApiAnalysis(videoForApi)
        }
    }

    private suspend fun performApiAnalysis(videoUri: String) {
        val state = _uiState.value

        if (state.apiKey.isEmpty()) {
            _uiState.update {
                it.copy(
                    analysisState = AnalysisState.ERROR,
                    errorMessage  = "APIキーが設定されていません。設定画面でAPIキーを入力してください。"
                )
            }
            return
        }

        val baseProgress = if (state.usePoseEstimation) 0.5f else 0.0f

        try {
            _uiState.update {
                it.copy(
                    analysisState = AnalysisState.API_ANALYSIS,
                    currentStep   = "Gemini AIで動画を分析中…",
                    progress      = baseProgress + 0.1f
                )
            }

            val result = geminiRepository.analyzeVideo(
                apiKey   = state.apiKey,
                videoUri = videoUri,
                prompt   = state.customPrompt,
                model    = state.selectedModel
            )

            result.fold(
                onSuccess = { analysisResult ->
                    val analysisId = "analysis_${System.currentTimeMillis()}"
                    _uiState.update {
                        it.copy(
                            analysisState = AnalysisState.COMPLETED,
                            analysisId    = analysisId,
                            analysisResult = analysisResult,
                            progress      = 1f,
                            currentStep   = "分析完了！"
                        )
                    }
                    // 履歴に自動保存
                    saveToHistory(analysisResult, if (state.usePoseEstimation) "pose" else "raw")
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            analysisState = AnalysisState.ERROR,
                            errorMessage  = buildApiErrorMessage(error)
                        )
                    }
                }
            )
        } catch (e: Exception) {
            Log.e("MainViewModel", "API分析エラー", e)
            _uiState.update {
                it.copy(
                    analysisState = AnalysisState.ERROR,
                    errorMessage  = "予期しないエラーが発生しました: ${e.message}"
                )
            }
        }
    }

    fun resetAnalysis() {
        _uiState.update {
            it.copy(
                analysisState     = AnalysisState.IDLE,
                progress          = 0f,
                currentStep       = "",
                analysisId        = null,
                errorMessage      = null,
                analysisResult    = null,
                videoPath         = null,
                savedPoseVideoPath = null
            )
        }
    }

    // ── History persistence ───────────────────────────────────────

    /** 分析結果を履歴に保存（新しい順） */
    fun saveToHistory(result: AnalysisResult, mode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val sdf   = SimpleDateFormat("MM/dd HH:mm", Locale.JAPAN)
            val label = if (mode == "pose") "骨格推定" else "スタンダード"
            val title = "${sdf.format(Date())} · $label"

            val record = AnalysisRecord(
                id           = "rec_${System.currentTimeMillis()}",
                timestamp    = System.currentTimeMillis(),
                title        = title,
                analysisText = result.analysisText,
                score        = (result.overallStats.averageFormScore * 100).toInt(),
                analysisMode = mode
            )

            val newList = listOf(record) + _historyList.value
            _historyList.value = newList
            persistHistory(newList)
        }
    }

    /** 指定 ID の履歴レコードを取得 */
    fun getHistoryRecord(id: String): AnalysisRecord? =
        _historyList.value.find { it.id == id }

    /** 指定 ID の履歴を削除 */
    fun deleteHistoryRecords(ids: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val newList = _historyList.value.filter { it.id !in ids }
            _historyList.value = newList
            persistHistory(newList)
        }
    }

    /** 履歴を全件削除 */
    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            _historyList.value = emptyList()
            persistHistory(emptyList())
        }
    }

    // ── JSON serialization ───────────────────────────────────────

    private fun persistHistory(list: List<AnalysisRecord>) {
        val arr = JSONArray()
        list.forEach { rec ->
            arr.put(JSONObject().apply {
                put("id",           rec.id)
                put("timestamp",    rec.timestamp)
                put("title",        rec.title)
                put("analysisText", rec.analysisText)
                put("score",        rec.score)
                put("analysisMode", rec.analysisMode)
            })
        }
        context.getSharedPreferences(HISTORY_PREFS, Context.MODE_PRIVATE)
            .edit().putString(HISTORY_KEY, arr.toString()).apply()
    }

    private fun parseHistoryJson(json: String): List<AnalysisRecord> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                AnalysisRecord(
                    id           = obj.getString("id"),
                    timestamp    = obj.getLong("timestamp"),
                    title        = obj.getString("title"),
                    analysisText = obj.getString("analysisText"),
                    score        = obj.getInt("score"),
                    analysisMode = obj.optString("analysisMode", "raw")
                )
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "履歴パースエラー", e)
            emptyList()
        }
    }

    // ── Storage management ────────────────────────────────────────

    data class AnalysisSession(
        val name: String,
        val dirPath: String,
        val sizeBytes: Long
    )

    fun getAnalysisSessionList(): List<AnalysisSession> {
        val root = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "SportAnalyzer")
        if (!root.exists()) return emptyList()
        return root.listFiles()
            ?.filter { it.isDirectory }
            ?.sortedByDescending { it.lastModified() }
            ?.map { dir ->
                val size = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                val displayName = dir.name.removePrefix("pose_analysis_").let { raw ->
                    runCatching {
                        val d = raw.substring(0, 8)
                        val t = raw.substring(9)
                        "${d.substring(0,4)}/${d.substring(4,6)}/${d.substring(6,8)} " +
                        "${t.substring(0,2)}:${t.substring(2,4)}:${t.substring(4,6)}"
                    }.getOrDefault(dir.name)
                }
                AnalysisSession(displayName, dir.absolutePath, size)
            } ?: emptyList()
    }

    fun deleteAnalysisSessions(paths: List<String>, onComplete: (deletedBytes: Long) -> Unit) {
        viewModelScope.launch {
            val deleted = withContext(Dispatchers.IO) {
                paths.sumOf { path ->
                    val dir = File(path)
                    val size = if (dir.exists()) dir.walkTopDown().filter { it.isFile }.sumOf { it.length() } else 0L
                    dir.deleteRecursively()
                    size
                }
            }
            if (paths.any { _uiState.value.savedPoseVideoPath?.startsWith(it) == true }) {
                _uiState.update { it.copy(savedPoseVideoPath = null) }
            }
            onComplete(deleted)
        }
    }

    fun clearAnalysisData(onComplete: (deletedBytes: Long) -> Unit) {
        viewModelScope.launch {
            val deleted = withContext(Dispatchers.IO) {
                val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "SportAnalyzer")
                val size = if (dir.exists()) dir.walkTopDown().filter { it.isFile }.sumOf { it.length() } else 0L
                dir.deleteRecursively()
                size
            }
            _uiState.update { it.copy(savedPoseVideoPath = null) }
            onComplete(deleted)
        }
    }

    // ── Error message helpers ─────────────────────────────────────

    private fun buildPoseErrorMessage(e: Exception): String = when {
        e.message?.contains("フレームを抽出できませんでした") == true ->
            "動画の形式がサポートされていません。別の動画をお試しください。"
        e.message?.contains("骨格を検出できませんでした") == true ->
            "動画から人物を検出できませんでした。人物が明確に映っている動画をお試しください。"
        else -> "骨格推定中にエラーが発生しました: ${e.message ?: "不明なエラー"}"
    }

    private fun buildApiErrorMessage(error: Throwable): String = when {
        error is java.net.UnknownHostException ||
        error.message?.contains("Unable to resolve host") == true ->
            "インターネットに接続されていません。Wi-Fi またはモバイルデータをオンにして再試行してください。"
        error is java.net.SocketTimeoutException ||
        error.message?.contains("timeout") == true ->
            "通信がタイムアウトしました。接続が安定している場所で再試行してください。"
        error.message?.contains("503") == true ||
        error.message?.contains("UNAVAILABLE") == true ||
        error.message?.contains("high demand") == true ->
            "Gemini APIが現在混雑しています。しばらく時間をおいて再試行してください。\n（gemini-2.5-flash に切り替えると成功しやすくなります）"
        error.message?.contains("Rate limit exceeded") == true ||
        error.message?.contains("429") == true ->
            "Gemini APIのレート制限に達しました。しばらく時間をおいて再度お試しください。"
        error.message?.contains("404") == true ->
            "選択したモデルはこのAPIキーでは使用できません。設定でモデルを変更してください。"
        error.message?.contains("401") == true ->
            "APIキーが無効です。設定画面で正しいAPIキーを入力してください。"
        else -> "Gemini API分析中にエラーが発生しました: ${error.message ?: "不明なエラー"}"
    }
}
