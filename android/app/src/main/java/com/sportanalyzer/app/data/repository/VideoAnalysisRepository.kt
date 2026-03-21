package com.sportanalyzer.app.data.repository

import android.content.Context
import android.graphics.*
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.media.MediaFormat
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.net.Uri
import android.util.Log
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class AnalysisProgress(
    val stage: AnalysisStage,
    val progress: Float,
    val message: String,
    val outputVideoPath: String? = null
)

enum class AnalysisStage {
    VIDEO_PROCESSING,
    POSE_DETECTION,
    RESULT_PROCESSING
}

data class PoseFrame(
    val timestampMs: Long,
    val landmarks: Map<String, Pair<Float, Float>>,
    val confidence: Float
)

@Singleton
class VideoAnalysisRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val poseDetector by lazy {
        val options = AccuratePoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.SINGLE_IMAGE_MODE)
            .build()
        PoseDetection.getClient(options)
    }

    fun analyzeVideo(
        videoUri: String,
        trimStartMs: Long = 0L,
        trimEndMs: Long = 0L
    ): Flow<AnalysisProgress> = flow {

        try {
            Log.d("VideoAnalysisRepository", "分析開始: $videoUri")

            emit(AnalysisProgress(AnalysisStage.VIDEO_PROCESSING, 0.0f, "動画を読み込み中..."))
            emit(AnalysisProgress(AnalysisStage.VIDEO_PROCESSING, 0.1f, "動画からフレームを抽出中..."))

            val frames = extractFrames(videoUri, trimStartMs, trimEndMs)
            Log.d("VideoAnalysisRepository", "フレーム抽出完了: ${frames.size}フレーム")
            if (frames.isEmpty()) throw Exception("動画からフレームを抽出できませんでした。")

            emit(AnalysisProgress(AnalysisStage.VIDEO_PROCESSING, 0.2f, "フレーム抽出完了: ${frames.size}フレーム"))
            emit(AnalysisProgress(AnalysisStage.POSE_DETECTION, 0.2f, "骨格を検出中 (0%)..."))

            val poseFrames = mutableListOf<PoseFrame>()
            frames.forEachIndexed { index, bitmap ->
                try {
                    val inputImage = InputImage.fromBitmap(bitmap, 0)
                    val pose = suspendCancellableCoroutine { continuation ->
                        poseDetector.process(inputImage)
                            .addOnSuccessListener { continuation.resume(it) }
                            .addOnFailureListener { continuation.resumeWithException(it) }
                    }

                    val landmarks = mutableMapOf<String, Pair<Float, Float>>()
                    var totalConfidence = 0f
                    var landmarkCount = 0

                    for (landmark in pose.allPoseLandmarks) {
                        val name = when (landmark.landmarkType) {
                            PoseLandmark.NOSE -> "nose"
                            PoseLandmark.LEFT_SHOULDER -> "left_shoulder"
                            PoseLandmark.RIGHT_SHOULDER -> "right_shoulder"
                            PoseLandmark.LEFT_ELBOW -> "left_elbow"
                            PoseLandmark.RIGHT_ELBOW -> "right_elbow"
                            PoseLandmark.LEFT_WRIST -> "left_wrist"
                            PoseLandmark.RIGHT_WRIST -> "right_wrist"
                            PoseLandmark.LEFT_HIP -> "left_hip"
                            PoseLandmark.RIGHT_HIP -> "right_hip"
                            PoseLandmark.LEFT_KNEE -> "left_knee"
                            PoseLandmark.RIGHT_KNEE -> "right_knee"
                            PoseLandmark.LEFT_ANKLE -> "left_ankle"
                            PoseLandmark.RIGHT_ANKLE -> "right_ankle"
                            else -> null
                        }
                        name?.let {
                            landmarks[it] = Pair(landmark.position.x, landmark.position.y)
                            totalConfidence += landmark.inFrameLikelihood
                            landmarkCount++
                        }
                    }

                    poseFrames.add(
                        PoseFrame(
                            timestampMs = (index * 1000).toLong(),
                            landmarks = landmarks,
                            confidence = if (landmarkCount > 0) totalConfidence / landmarkCount else 0f
                        )
                    )
                } catch (e: Exception) {
                    Log.e("VideoAnalysisRepository", "フレーム ${index + 1} の骨格推定エラー", e)
                }

                val progress = (index + 1).toFloat() / frames.size
                emit(AnalysisProgress(
                    stage = AnalysisStage.POSE_DETECTION,
                    progress = 0.2f + progress * 0.6f,
                    message = "骨格推定中: ${index + 1}/${frames.size} フレーム (${(progress * 100).toInt()}%)"
                ))

                if (index < frames.size - 1) delay(100)
            }

            if (poseFrames.isEmpty()) throw Exception("動画から骨格を検出できませんでした。")

            emit(AnalysisProgress(AnalysisStage.RESULT_PROCESSING, 0.85f, "骨格推定動画を作成中..."))

            val outputVideoPath = createPoseOverlayVideo(frames, poseFrames)
            Log.d("VideoAnalysisRepository", "骨格推定動画作成完了: $outputVideoPath")

            emit(AnalysisProgress(
                stage = AnalysisStage.RESULT_PROCESSING,
                progress = 1.0f,
                message = "骨格推定完了！動画が保存されました。",
                outputVideoPath = outputVideoPath
            ))

        } catch (e: Exception) {
            Log.e("VideoAnalysisRepository", "Analysis failed", e)
            emit(AnalysisProgress(AnalysisStage.RESULT_PROCESSING, 0f, "エラー: ${e.message}"))
            throw e
        }
    }.flowOn(Dispatchers.Default)

    /**
     * 動画からフレームを抽出する。
     * - トリム範囲が指定されている場合はその範囲のみから抽出
     * - OOM 対策: 最大60フレーム、長辺720pxにダウンスケール
     */
    private suspend fun extractFrames(
        videoUri: String,
        trimStartMs: Long = 0L,
        trimEndMs: Long = 0L
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        val frames = mutableListOf<Bitmap>()
        try {
            retriever.setDataSource(context, Uri.parse(videoUri))
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0

            // トリム範囲の決定
            val startMs = if (trimEndMs > trimStartMs) trimStartMs else 0L
            val endMs = if (trimEndMs > trimStartMs) trimEndMs else durationMs

            // 5fps で抽出（0.2秒間隔）→ 最大60フレームで12秒をカバー（OOM 対策）
            val frameIntervalUs = 200_000L
            val maxFrames = 60
            val maxLongSide = 720  // ダウンスケール上限

            for (i in 0 until maxFrames) {
                val timeUs = startMs * 1000L + i * frameIntervalUs
                if (timeUs / 1000 > endMs && endMs != 0L) break
                if (timeUs / 1000 > durationMs && durationMs != 0L) break

                retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)?.let { raw ->
                    // OOM 対策: 長辺 720px にダウンスケール
                    val longSide = maxOf(raw.width, raw.height)
                    val scaled = if (longSide > maxLongSide) {
                        val scale = maxLongSide.toFloat() / longSide
                        val newW = (raw.width * scale).toInt()
                        val newH = (raw.height * scale).toInt()
                        val s = Bitmap.createScaledBitmap(raw, newW, newH, true)
                        raw.recycle()
                        s
                    } else {
                        raw
                    }
                    frames.add(scaled)
                }
            }
        } catch (e: Exception) {
            Log.e("VideoAnalysisRepository", "Error extracting frames", e)
            throw Exception("動画からフレームを抽出できませんでした: ${e.message}")
        } finally {
            retriever.release()
        }
        frames
    }

    private fun createPoseOverlayVideo(originalFrames: List<Bitmap>, poseFrames: List<PoseFrame>): String {
        require(originalFrames.isNotEmpty()) { "フレームリストが空です" }

        val downloadsDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "SportAnalyzer")
        if (!downloadsDir.exists()) downloadsDir.mkdirs()

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val outputDir = File(downloadsDir, "pose_analysis_$timestamp")
        outputDir.mkdirs()

        val skeledFrames = originalFrames.mapIndexed { index, originalBitmap ->
            val mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(mutableBitmap)

            if (index < poseFrames.size) {
                val landmarks = poseFrames[index].landmarks
                if (landmarks.isNotEmpty()) {
                    val linePaint = Paint().apply {
                        color = Color.GREEN
                        strokeWidth = 8f
                        style = Paint.Style.STROKE
                        isAntiAlias = true
                    }
                    val dotPaint = Paint().apply {
                        color = Color.RED
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                    drawPoseConnections(canvas, landmarks, linePaint)
                    landmarks.forEach { (_, pos) -> canvas.drawCircle(pos.first, pos.second, 12f, dotPaint) }
                }
            }
            mutableBitmap
        }

        val mp4File = File(outputDir, "pose_analysis.mp4")
        // 5fps（200ms間隔）で抽出しているので PTS も 200_000µs/フレームで書く
        createMP4Video(skeledFrames, mp4File.absolutePath, frameRate = 5, sourceFrameIntervalUs = 200_000L)

        // M3 修正: Bitmap メモリリーク防止 — エンコード後に全フレームを recycle
        originalFrames.forEach { it.recycle() }
        skeledFrames.forEach { it.recycle() }

        return mp4File.absolutePath
    }

    /**
     * ByteBuffer 入力方式でエンコード。
     * PTS をフレームインデックス × sourceFrameIntervalUs で明示的に指定するため、
     * Surface 入力の壁時計レート制御によるフレームドロップが発生しない。
     */
    private fun createMP4Video(
        bitmaps: List<Bitmap>,
        outputPath: String,
        frameRate: Int = 5,
        sourceFrameIntervalUs: Long = 200_000L
    ) {
        Log.d("VideoAnalysisRepository", "MP4作成開始: ${bitmaps.size}フレーム -> $outputPath " +
            "(${frameRate}fps, PTS間隔: ${sourceFrameIntervalUs}µs)")

        // 幅・高さを偶数に揃える（YUV420 の要件）
        val srcW = bitmaps[0].width
        val srcH = bitmaps[0].height
        val width  = if (srcW % 2 == 0) srcW else srcW - 1
        val height = if (srcH % 2 == 0) srcH else srcH - 1

        // デバイスが対応するカラーフォーマットを選択
        val colorFormat = selectColorFormat()

        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat)
            setInteger(MediaFormat.KEY_BIT_RATE, 4_000_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()

        val muxer   = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val bufInfo = MediaCodec.BufferInfo()
        var trackIndex   = -1
        var muxerStarted = false

        try {
            bitmaps.forEachIndexed { frameIdx, bitmap ->
                val ptsUs = frameIdx.toLong() * sourceFrameIntervalUs

                // ── 入力フレームを YUV(NV12) 変換してキュー ────────
                val inputBufIdx = encoder.dequeueInputBuffer(10_000L)
                if (inputBufIdx >= 0) {
                    val inputBuf = encoder.getInputBuffer(inputBufIdx)!!
                    inputBuf.clear()
                    val nv12 = bitmapToNV12(bitmap, width, height)
                    inputBuf.put(nv12)
                    encoder.queueInputBuffer(inputBufIdx, 0, nv12.size, ptsUs, 0)
                }

                // ── 利用可能な出力をドレイン（ノンブロッキング）──────
                drainEncoder(encoder, muxer, bufInfo, { trackIndex = it; muxerStarted = true },
                    { trackIndex }, { muxerStarted }, false)
            }

            // ── EOS を通知 ─────────────────────────────────────────
            val eofBufIdx = encoder.dequeueInputBuffer(10_000L)
            if (eofBufIdx >= 0) {
                encoder.queueInputBuffer(eofBufIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            }

            // ── 残り出力を全ドレイン ───────────────────────────────
            drainEncoder(encoder, muxer, bufInfo, { trackIndex = it; muxerStarted = true },
                { trackIndex }, { muxerStarted }, true)

        } finally {
            try { encoder.stop() }    catch (_: Exception) {}
            try { encoder.release() } catch (_: Exception) {}
            try { if (muxerStarted) muxer.stop() } catch (_: Exception) {}
            try { muxer.release() }   catch (_: Exception) {}
        }

        Log.d("VideoAnalysisRepository", "MP4作成完了: $outputPath")
    }

    /** エンコーダーの出力をドレインして Muxer に書き込む */
    private fun drainEncoder(
        encoder: MediaCodec,
        muxer: MediaMuxer,
        bufInfo: MediaCodec.BufferInfo,
        onTrackAdded: (Int) -> Unit,
        trackIndex: () -> Int,
        muxerStarted: () -> Boolean,
        waitForEOS: Boolean
    ) {
        val timeoutUs = if (waitForEOS) 10_000L else 0L
        var eosReached = false
        var retries = 0
        val maxRetries = if (waitForEOS) 500 else 10

        while (!eosReached && retries < maxRetries) {
            when (val idx = encoder.dequeueOutputBuffer(bufInfo, timeoutUs)) {
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    if (!muxerStarted()) {
                        onTrackAdded(muxer.addTrack(encoder.outputFormat))
                        muxer.start()
                    }
                }
                MediaCodec.INFO_TRY_AGAIN_LATER -> { retries++ }
                else -> {
                    if (idx >= 0) {
                        retries = 0
                        val isEOS = (bufInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                        val isConfig = (bufInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0
                        val buf = encoder.getOutputBuffer(idx)
                        if (!isConfig && bufInfo.size > 0 && muxerStarted() && buf != null) {
                            muxer.writeSampleData(trackIndex(), buf, bufInfo)
                        }
                        encoder.releaseOutputBuffer(idx, false)
                        if (isEOS) eosReached = true
                    }
                }
            }
        }
    }

    /** デバイスが対応する H.264 エンコーダーのカラーフォーマットを選択 */
    private fun selectColorFormat(): Int {
        try {
            val codecInfo = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).let { codec ->
                val info = codec.codecInfo
                codec.release()
                info
            }
            val caps = codecInfo.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC)
            val supported = caps.colorFormats.toSet()

            // 優先順位: NV12 > YUV420Planar > YUV420Flexible
            return when {
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar in supported ->
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar in supported ->
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible in supported ->
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
                else -> MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar // fallback
            }
        } catch (e: Exception) {
            Log.w("VideoAnalysisRepository", "カラーフォーマット検出失敗、デフォルト使用", e)
            return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
        }
    }

    /** Bitmap (ARGB_8888) を NV12 (YUV420SemiPlanar) に変換 */
    private fun bitmapToNV12(bitmap: Bitmap, width: Int, height: Int): ByteArray {
        val argb = IntArray(width * height)
        // Bitmap が width/height と異なるサイズの場合はスケーリング
        val src = if (bitmap.width != width || bitmap.height != height) {
            Bitmap.createScaledBitmap(bitmap, width, height, true)
        } else {
            bitmap
        }
        src.getPixels(argb, 0, width, 0, 0, width, height)
        if (src !== bitmap) src.recycle()

        val nv12 = ByteArray(width * height * 3 / 2)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = argb[y * width + x]
                val r = (pixel shr 16) and 0xff
                val g = (pixel shr 8)  and 0xff
                val b =  pixel         and 0xff
                // BT.601 full range
                val yVal = (( 66 * r + 129 * g +  25 * b + 128) shr 8) + 16
                nv12[y * width + x] = yVal.coerceIn(0, 255).toByte()

                if (y % 2 == 0 && x % 2 == 0) {
                    val u = ((-38 * r -  74 * g + 112 * b + 128) shr 8) + 128
                    val v = ((112 * r -  94 * g -  18 * b + 128) shr 8) + 128
                    val uvBase = width * height + (y / 2) * width + x
                    nv12[uvBase]     = u.coerceIn(0, 255).toByte()
                    nv12[uvBase + 1] = v.coerceIn(0, 255).toByte()
                }
            }
        }
        return nv12
    }

    private fun drawPoseConnections(canvas: Canvas, landmarks: Map<String, Pair<Float, Float>>, paint: Paint) {
        val connections = listOf(
            "left_shoulder" to "right_shoulder",
            "left_shoulder" to "left_hip",
            "right_shoulder" to "right_hip",
            "left_hip" to "right_hip",
            "left_shoulder" to "left_elbow",
            "left_elbow" to "left_wrist",
            "right_shoulder" to "right_elbow",
            "right_elbow" to "right_wrist",
            "left_hip" to "left_knee",
            "left_knee" to "left_ankle",
            "right_hip" to "right_knee",
            "right_knee" to "right_ankle"
        )

        connections.forEach { (start, end) ->
            val s = landmarks[start]
            val e = landmarks[end]
            if (s != null && e != null) {
                canvas.drawLine(s.first, s.second, e.first, e.second, paint)
            }
        }
    }
}
