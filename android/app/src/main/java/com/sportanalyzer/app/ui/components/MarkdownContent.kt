package com.sportanalyzer.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sportanalyzer.app.ui.theme.*

/** 番号付きリスト判定用の Regex（recomposition ごとの再生成を防ぐ） */
private val ORDERED_LIST_REGEX = Regex("^\\d+\\.\\s.*")

/**
 * Gemini の Markdown テキストをレンダリングする共有コンポーザブル。
 * ResultsScreen と HistoryDetailScreen の両方で使用される。
 */
@Composable
fun MarkdownContent(text: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        text.lines().forEach { line ->
            when {
                line.startsWith("# ") -> {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = line.removePrefix("# ").trim(),
                        fontSize = 20.sp, fontWeight = FontWeight.Bold,
                        color = iOSBlue, lineHeight = 26.sp
                    )
                }
                line.startsWith("## ") -> {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = line.removePrefix("## ").trim(),
                        fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                        color = PrimaryLabel, lineHeight = 22.sp
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 3.dp, bottom = 4.dp)
                            .height(0.5.dp)
                            .background(Separator)
                    )
                }
                line.startsWith("### ") -> {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = line.removePrefix("### ").trim(),
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        color = iOSOrange, lineHeight = 20.sp
                    )
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    val content = line.removePrefix("- ").removePrefix("* ").trim()
                    Row(
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            fontSize = 14.sp, color = iOSBlue,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 6.dp, top = 1.dp)
                        )
                        InlineMarkdownText(text = content, fontSize = 14.sp, baseColor = SecondaryLabel)
                    }
                }
                line.matches(ORDERED_LIST_REGEX) -> {
                    val num = line.substringBefore(".").trim()
                    val content = line.substringAfter(". ").trim()
                    Row(
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "$num.", fontSize = 14.sp, color = iOSBlue,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.width(24.dp).padding(top = 1.dp)
                        )
                        InlineMarkdownText(text = content, fontSize = 14.sp, baseColor = SecondaryLabel)
                    }
                }
                line == "---" || line == "***" || line == "___" -> {
                    Spacer(Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Separator))
                    Spacer(Modifier.height(4.dp))
                }
                line.isBlank() -> Spacer(Modifier.height(4.dp))
                else -> InlineMarkdownText(text = line, fontSize = 14.sp, baseColor = SecondaryLabel)
            }
        }
    }
}

@Composable
fun InlineMarkdownText(
    text: String,
    fontSize: TextUnit,
    baseColor: Color,
    modifier: Modifier = Modifier
) {
    val annotated = buildAnnotatedString {
        var remaining = text
        while (remaining.isNotEmpty()) {
            when {
                remaining.startsWith("**") && remaining.indexOf("**", 2) >= 0 -> {
                    val end = remaining.indexOf("**", 2)
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = PrimaryLabel)) {
                        append(remaining.substring(2, end))
                    }
                    remaining = remaining.substring(end + 2)
                }
                remaining.startsWith("*") && remaining.indexOf("*", 1) > 0 -> {
                    val end = remaining.indexOf("*", 1)
                    // m1 修正: 閉じ * が存在し、かつ空でないことを保証
                    if (end > 1) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = baseColor)) {
                            append(remaining.substring(1, end))
                        }
                        remaining = remaining.substring(end + 1)
                    } else {
                        withStyle(SpanStyle(color = baseColor)) { append("*") }
                        remaining = remaining.substring(1)
                    }
                }
                remaining.startsWith("`") && remaining.indexOf("`", 1) >= 0 -> {
                    val end = remaining.indexOf("`", 1)
                    withStyle(SpanStyle(color = iOSOrange, fontWeight = FontWeight.Medium)) {
                        append(remaining.substring(1, end))
                    }
                    remaining = remaining.substring(end + 1)
                }
                else -> {
                    val nextMark = listOf(
                        remaining.indexOf("**").takeIf { it > 0 } ?: Int.MAX_VALUE,
                        remaining.indexOf("*").takeIf { it > 0 }  ?: Int.MAX_VALUE,
                        remaining.indexOf("`").takeIf { it > 0 }  ?: Int.MAX_VALUE
                    ).min()
                    if (nextMark == Int.MAX_VALUE) {
                        withStyle(SpanStyle(color = baseColor)) { append(remaining) }
                        remaining = ""
                    } else {
                        withStyle(SpanStyle(color = baseColor)) { append(remaining.substring(0, nextMark)) }
                        remaining = remaining.substring(nextMark)
                    }
                }
            }
        }
    }
    Text(
        text = annotated,
        fontSize = fontSize,
        lineHeight = (fontSize.value * 1.55f).sp,
        modifier = modifier
    )
}
