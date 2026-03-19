package com.sportanalyzer.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.sportanalyzer.app.data.api.*
import com.sportanalyzer.app.domain.model.AnalysisResult
import com.sportanalyzer.app.domain.model.OverallStats
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiRepository @Inject constructor(
    private val apiService: GeminiApiService,
    @ApplicationContext private val context: Context
) {

    /**
     * 動画を分析する
     *
     * 動画からキーフレーム（最大8枚）を抽出し、JPEG画像として
     * Gemini API にインラインデータで送信します。
     * File API を使わないためタイムアウトが発生しにくく、
     * かつ骨格推定済み動画・元動画 どちらにも対応します。
     */
    suspend fun analyzeVideo(
        apiKey: String,
        videoUri: String,
        prompt: String,
        model: String = "gemini-2.5-flash"
    ): Result<AnalysisResult> = withContext(Dispatchers.IO) {
        try {
            // Step 1: キーフレーム抽出
            Log.d("GeminiRepository", "キーフレーム抽出開始: $videoUri")
            // 4 フレームに制限：リクエストサイズを削減しタイムアウトを防ぐ
            val frames = extractKeyFrames(videoUri, maxFrames = 4)
            if (frames.isEmpty()) {
                return@withContext Result.failure(
                    Exception("動画からフレームを抽出できませんでした。ファイル形式を確認してください。")
                )
            }
            Log.d("GeminiRepository", "フレーム抽出完了: ${frames.size} 枚")

            // Step 2: リクエスト組み立て（テキスト + 画像フレーム）
            val parts = mutableListOf<Part>()
            parts.add(
                Part(
                    text = """
                        $prompt
                        
                        以下は動画の連続キーフレーム（${frames.size}枚）です。
                        これらの画像から動作を分析してください。
                    """.trimIndent()
                )
            )
            frames.forEachIndexed { index, bitmap ->
                val jpegBytes = bitmapToJpeg(bitmap, quality = 75)
                val base64Data = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
                parts.add(Part(text = "フレーム ${index + 1}/${frames.size}:"))
                parts.add(
                    Part(
                        inlineData = InlineData(
                            mimeType = "image/jpeg",
                            data = base64Data
                        )
                    )
                )
                Log.d("GeminiRepository", "フレーム${index + 1}: ${jpegBytes.size / 1024}KB")
            }

            val request = GeminiRequest(
                contents = listOf(ContentPart(parts = parts))
            )

            // Step 3: API 呼び出し
            Log.d("GeminiRepository", "Gemini API 呼び出し: model=$model")
            val response = apiService.analyzeVideo(apiKey, model, request)

            if (response.isSuccessful) {
                val body = response.body()
                val analysisText = body?.candidates?.firstOrNull()
                    ?.content?.parts?.firstOrNull()?.text

                if (analysisText != null) {
                    Log.d("GeminiRepository", "分析成功 (${analysisText.length}文字)")
                    Result.success(parseAnalysisResult(analysisText))
                } else {
                    val errMsg = body?.error?.message ?: "Gemini API から空の応答が返りました"
                    Log.e("GeminiRepository", "空応答: $errMsg")
                    Result.failure(Exception(errMsg))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("GeminiRepository", "API Error: ${response.code()} - $errorBody")
                when (response.code()) {
                    401  -> Result.failure(Exception("APIキーが無効です"))
                    429  -> Result.failure(Exception("Rate limit exceeded"))
                    404  -> Result.failure(Exception("このAPIキーでは選択したモデルを使用できません"))
                    413  -> Result.failure(Exception("リクエストが大きすぎます。フレーム数を減らしてください"))
                    else -> Result.failure(Exception("API Error: ${response.code()} - $errorBody"))
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiRepository", "Analysis failed", e)
            Result.failure(e)
        }
    }

    // ────────────────────────────────────────────────────────────
    // キーフレーム抽出
    // ────────────────────────────────────────────────────────────

    private fun extractKeyFrames(videoUri: String, maxFrames: Int = 8): List<Bitmap> {
        val retriever = MediaMetadataRetriever()
        val frames = mutableListOf<Bitmap>()
        try {
            // content:// と ローカルパス を区別して setDataSource
            when {
                videoUri.startsWith("content://") ->
                    retriever.setDataSource(context, Uri.parse(videoUri))
                videoUri.startsWith("/") ->
                    retriever.setDataSource(videoUri)
                videoUri.startsWith("file://") ->
                    retriever.setDataSource(Uri.parse(videoUri).path)
                else ->
                    retriever.setDataSource(context, Uri.parse(videoUri))
            }

            val durationMs = retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLong() ?: 0L

            val step = if (durationMs > 0) durationMs / maxFrames else 1000L

            for (i in 0 until maxFrames) {
                val timeUs = i * step * 1000L   // マイクロ秒
                val bitmap = retriever.getFrameAtTime(
                    timeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                ) ?: continue

                // 640x360 にリサイズしてメモリ・転送量を削減
                val scaled = Bitmap.createScaledBitmap(bitmap, 640, 360, true)
                if (scaled !== bitmap) bitmap.recycle()
                frames.add(scaled)
            }
        } catch (e: Exception) {
            Log.e("GeminiRepository", "フレーム抽出エラー: ${e.message}", e)
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
        return frames
    }

    // ────────────────────────────────────────────────────────────
    // Bitmap → JPEG バイト列
    // ────────────────────────────────────────────────────────────

    private fun bitmapToJpeg(bitmap: Bitmap, quality: Int = 75): ByteArray {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        return out.toByteArray()
    }

    // ────────────────────────────────────────────────────────────
    // 分析結果パース
    // ────────────────────────────────────────────────────────────

    /**
     * Gemini のテキストをそのまま AnalysisResult に格納する。
     * recommendations はハードコードしない — Gemini の出力をそのまま analysisText で表示する。
     */
    private fun parseAnalysisResult(geminiText: String): AnalysisResult {
        // Gemini が "スコア: XX" や "総合評価: XX点" のような表記をしていれば抽出を試みる
        val scoreRegex = Regex("""(?:スコア|総合評価|評価)[:：]\s*(\d{1,3})""")
        val scoreMatch = scoreRegex.find(geminiText)
        val score = scoreMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 75f

        return AnalysisResult(
            id              = "gemini_analysis_${System.currentTimeMillis()}",
            videoUri        = "",
            timestamp       = System.currentTimeMillis(),
            analysisText    = geminiText,   // ← Gemini の実際の出力をそのまま渡す
            overallStats    = OverallStats(
                averageFormScore = score / 100f,
                improvementAreas = emptyList()
            ),
            recommendations = emptyList()   // ← ハードコードしない（UIで analysisText を表示する）
        )
    }
}
