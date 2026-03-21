package com.sportanalyzer.app.ui.screens.trim

import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.sportanalyzer.app.ui.MainViewModel
import com.sportanalyzer.app.ui.components.SimpleNavBar
import com.sportanalyzer.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoTrimScreen(
    navController: NavController,
    videoUri: String?,
    viewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val durationMs = remember(videoUri) {
        if (videoUri == null) 0L
        else try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, Uri.parse(videoUri))
            val d = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
            retriever.release()
            d
        } catch (e: Exception) { 0L }
    }

    var rangeStart by remember { mutableFloatStateOf(0f) }
    var rangeEnd by remember { mutableFloatStateOf(durationMs.toFloat().coerceAtLeast(1f)) }

    // クロップ状態（正規化座標 0〜1）
    var cropLeft by remember { mutableFloatStateOf(0f) }
    var cropTop by remember { mutableFloatStateOf(0f) }
    var cropRight by remember { mutableFloatStateOf(1f) }
    var cropBottom by remember { mutableFloatStateOf(1f) }
    var isCropMode by remember { mutableStateOf(false) }

    LaunchedEffect(durationMs) {
        if (durationMs > 0) {
            rangeStart = 0f
            rangeEnd = durationMs.toFloat()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SystemBlack)
    ) {
        SimpleNavBar(
            title = "動画を編集",
            onBack = { navController.popBackStack() }
        )

        if (videoUri == null || durationMs <= 0L) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("動画を読み込めませんでした", color = SecondaryLabel, fontSize = 16.sp)
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── 動画プレビュー + クロップオーバーレイ ──────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SystemDark)
            ) {
                TrimVideoPlayer(
                    videoUri = videoUri,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                )

                // クロップモード時にオーバーレイを表示
                if (isCropMode) {
                    CropOverlay(
                        cropLeft = cropLeft,
                        cropTop = cropTop,
                        cropRight = cropRight,
                        cropBottom = cropBottom,
                        onCropChanged = { l, t, r, b ->
                            cropLeft = l
                            cropTop = t
                            cropRight = r
                            cropBottom = b
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                    )
                }
            }

            // ── クロップモード切替 ──────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ModeChip(
                    label = "⏱ 時間",
                    selected = !isCropMode,
                    onClick = { isCropMode = false },
                    modifier = Modifier.weight(1f)
                )
                ModeChip(
                    label = "✂️ 切り取り",
                    selected = isCropMode,
                    onClick = { isCropMode = true },
                    modifier = Modifier.weight(1f)
                )
            }

            if (!isCropMode) {
                // ── 時間範囲選択 ──────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SystemDark)
                        .padding(16.dp)
                ) {
                    Text("分析時間の範囲", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = PrimaryLabel)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("不要な部分をカットして分析時間を短縮", fontSize = 13.sp, color = SecondaryLabel)
                    Spacer(modifier = Modifier.height(12.dp))

                    RangeSlider(
                        value = rangeStart..rangeEnd,
                        onValueChange = { range ->
                            rangeStart = range.start
                            rangeEnd = range.endInclusive
                        },
                        valueRange = 0f..durationMs.toFloat(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = iOSBlue,
                            activeTrackColor = iOSBlue,
                            inactiveTrackColor = SystemFill
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatTime(rangeStart.toLong()), fontSize = 13.sp, color = iOSBlue, fontWeight = FontWeight.Medium)
                        Text("選択: ${formatTime((rangeEnd - rangeStart).toLong())}", fontSize = 13.sp, color = SecondaryLabel)
                        Text(formatTime(rangeEnd.toLong()), fontSize = 13.sp, color = iOSBlue, fontWeight = FontWeight.Medium)
                    }
                }
            } else {
                // ── クロップ設定 ──────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SystemDark)
                        .padding(16.dp)
                ) {
                    Text("フレーム切り取り", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = PrimaryLabel)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("動画の上で矩形をドラッグして分析範囲を指定", fontSize = 13.sp, color = SecondaryLabel)
                    Spacer(modifier = Modifier.height(12.dp))

                    val cropW = ((cropRight - cropLeft) * 100).toInt()
                    val cropH = ((cropBottom - cropTop) * 100).toInt()

                    Text(
                        "選択範囲: ${cropW}% × ${cropH}%",
                        fontSize = 13.sp,
                        color = iOSBlue,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            cropLeft = 0f; cropTop = 0f; cropRight = 1f; cropBottom = 1f
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SecondaryLabel)
                    ) {
                        Text("リセット", fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── アクションボタン ────────────────────────────────────
            Button(
                onClick = {
                    viewModel.setTrimRange(rangeStart.toLong(), rangeEnd.toLong())
                    viewModel.setCropRect(cropLeft, cropTop, cropRight, cropBottom)
                    val encodedUri = Uri.encode(videoUri)
                    navController.navigate("analysis/$encodedUri") {
                        popUpTo("video_trim/{videoUri}") { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = iOSBlue)
            ) {
                val cropLabel = if (cropLeft > 0.01f || cropTop > 0.01f || cropRight < 0.99f || cropBottom < 0.99f) " + 切り取り" else ""
                Text("この設定で分析する$cropLabel", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = {
                    viewModel.setTrimRange(0L, 0L)
                    viewModel.clearCrop()
                    val encodedUri = Uri.encode(videoUri)
                    navController.navigate("analysis/$encodedUri") {
                        popUpTo("video_trim/{videoUri}") { inclusive = true }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SecondaryLabel)
            ) {
                Text("編集せずに全体を分析", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ── クロップオーバーレイ ─────────────────────────────────────────

@Composable
private fun CropOverlay(
    cropLeft: Float,
    cropTop: Float,
    cropRight: Float,
    cropBottom: Float,
    onCropChanged: (Float, Float, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var viewSize by remember { mutableStateOf(IntSize.Zero) }

    // ドラッグの種類を追跡
    var dragType by remember { mutableStateOf(DragType.NONE) }

    Box(
        modifier = modifier
            .onSizeChanged { viewSize = it }
            .pointerInput(viewSize) {
                if (viewSize.width == 0 || viewSize.height == 0) return@pointerInput
                val w = viewSize.width.toFloat()
                val h = viewSize.height.toFloat()
                val handleRadius = 40f

                detectDragGestures(
                    onDragStart = { offset ->
                        // どのハンドルに近いか判定
                        val cx = offset.x / w
                        val cy = offset.y / h
                        val threshold = handleRadius / w.coerceAtLeast(1f)

                        dragType = when {
                            // 四隅
                            kotlin.math.abs(cx - cropLeft) < threshold && kotlin.math.abs(cy - cropTop) < threshold -> DragType.TOP_LEFT
                            kotlin.math.abs(cx - cropRight) < threshold && kotlin.math.abs(cy - cropTop) < threshold -> DragType.TOP_RIGHT
                            kotlin.math.abs(cx - cropLeft) < threshold && kotlin.math.abs(cy - cropBottom) < threshold -> DragType.BOTTOM_LEFT
                            kotlin.math.abs(cx - cropRight) < threshold && kotlin.math.abs(cy - cropBottom) < threshold -> DragType.BOTTOM_RIGHT
                            // 辺
                            kotlin.math.abs(cy - cropTop) < threshold && cx in cropLeft..cropRight -> DragType.TOP
                            kotlin.math.abs(cy - cropBottom) < threshold && cx in cropLeft..cropRight -> DragType.BOTTOM
                            kotlin.math.abs(cx - cropLeft) < threshold && cy in cropTop..cropBottom -> DragType.LEFT
                            kotlin.math.abs(cx - cropRight) < threshold && cy in cropTop..cropBottom -> DragType.RIGHT
                            // 内部→移動
                            cx in cropLeft..cropRight && cy in cropTop..cropBottom -> DragType.MOVE
                            else -> DragType.NONE
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val dx = dragAmount.x / w
                        val dy = dragAmount.y / h
                        val minSize = 0.1f

                        when (dragType) {
                            DragType.TOP_LEFT -> {
                                val nl = (cropLeft + dx).coerceIn(0f, cropRight - minSize)
                                val nt = (cropTop + dy).coerceIn(0f, cropBottom - minSize)
                                onCropChanged(nl, nt, cropRight, cropBottom)
                            }
                            DragType.TOP_RIGHT -> {
                                val nr = (cropRight + dx).coerceIn(cropLeft + minSize, 1f)
                                val nt = (cropTop + dy).coerceIn(0f, cropBottom - minSize)
                                onCropChanged(cropLeft, nt, nr, cropBottom)
                            }
                            DragType.BOTTOM_LEFT -> {
                                val nl = (cropLeft + dx).coerceIn(0f, cropRight - minSize)
                                val nb = (cropBottom + dy).coerceIn(cropTop + minSize, 1f)
                                onCropChanged(nl, cropTop, cropRight, nb)
                            }
                            DragType.BOTTOM_RIGHT -> {
                                val nr = (cropRight + dx).coerceIn(cropLeft + minSize, 1f)
                                val nb = (cropBottom + dy).coerceIn(cropTop + minSize, 1f)
                                onCropChanged(cropLeft, cropTop, nr, nb)
                            }
                            DragType.TOP -> {
                                val nt = (cropTop + dy).coerceIn(0f, cropBottom - minSize)
                                onCropChanged(cropLeft, nt, cropRight, cropBottom)
                            }
                            DragType.BOTTOM -> {
                                val nb = (cropBottom + dy).coerceIn(cropTop + minSize, 1f)
                                onCropChanged(cropLeft, cropTop, cropRight, nb)
                            }
                            DragType.LEFT -> {
                                val nl = (cropLeft + dx).coerceIn(0f, cropRight - minSize)
                                onCropChanged(nl, cropTop, cropRight, cropBottom)
                            }
                            DragType.RIGHT -> {
                                val nr = (cropRight + dx).coerceIn(cropLeft + minSize, 1f)
                                onCropChanged(cropLeft, cropTop, nr, cropBottom)
                            }
                            DragType.MOVE -> {
                                val cw = cropRight - cropLeft
                                val ch = cropBottom - cropTop
                                var nl = cropLeft + dx
                                var nt = cropTop + dy
                                nl = nl.coerceIn(0f, 1f - cw)
                                nt = nt.coerceIn(0f, 1f - ch)
                                onCropChanged(nl, nt, nl + cw, nt + ch)
                            }
                            DragType.NONE -> {}
                        }
                    },
                    onDragEnd = { dragType = DragType.NONE }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 半透明の外側
            val outerPath = Path().apply { addRect(Rect(0f, 0f, w, h)) }
            val innerPath = Path().apply {
                addRect(Rect(cropLeft * w, cropTop * h, cropRight * w, cropBottom * h))
            }
            val dimPath = Path()
            dimPath.op(outerPath, innerPath, PathOperation.Difference)
            drawPath(dimPath, Color.Black.copy(alpha = 0.55f))

            // 枠線
            drawRect(
                color = Color.White,
                topLeft = Offset(cropLeft * w, cropTop * h),
                size = Size((cropRight - cropLeft) * w, (cropBottom - cropTop) * h),
                style = Stroke(width = 2.dp.toPx())
            )

            // グリッド線（3分割）
            val cl = cropLeft * w; val ct = cropTop * h
            val cw = (cropRight - cropLeft) * w; val ch = (cropBottom - cropTop) * h
            for (i in 1..2) {
                val x = cl + cw * i / 3
                drawLine(Color.White.copy(alpha = 0.3f), Offset(x, ct), Offset(x, ct + ch), strokeWidth = 1.dp.toPx())
            }
            for (i in 1..2) {
                val y = ct + ch * i / 3
                drawLine(Color.White.copy(alpha = 0.3f), Offset(cl, y), Offset(cl + cw, y), strokeWidth = 1.dp.toPx())
            }

            // 四隅ハンドル
            val handleLen = 20.dp.toPx()
            val handleStroke = 3.dp.toPx()
            val corners = listOf(
                Offset(cl, ct), Offset(cl + cw, ct),
                Offset(cl, ct + ch), Offset(cl + cw, ct + ch)
            )
            corners.forEach { corner ->
                val sx = if (corner.x <= cl + 1) 1f else -1f
                val sy = if (corner.y <= ct + 1) 1f else -1f
                drawLine(iOSBlue, corner, Offset(corner.x + handleLen * sx, corner.y), strokeWidth = handleStroke)
                drawLine(iOSBlue, corner, Offset(corner.x, corner.y + handleLen * sy), strokeWidth = handleStroke)
            }
        }
    }
}

private enum class DragType {
    NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, TOP, BOTTOM, LEFT, RIGHT, MOVE
}

// ── モード切替チップ ────────────────────────────────────────────

@Composable
private fun ModeChip(
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
            contentColor = if (selected) Color.White else SecondaryLabel
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
    ) {
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

// ── ヘルパー ────────────────────────────────────────────────────

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val millis = (ms % 1000) / 100
    return if (minutes > 0) "%d:%02d.%d".format(minutes, seconds, millis)
    else "%d.%d秒".format(seconds, millis)
}

@Composable
private fun TrimVideoPlayer(
    videoUri: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val player = remember(videoUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(videoUri)))
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = false
            prepare()
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = true
                setBackgroundColor(android.graphics.Color.BLACK)
            }
        },
        modifier = modifier
    )
}
