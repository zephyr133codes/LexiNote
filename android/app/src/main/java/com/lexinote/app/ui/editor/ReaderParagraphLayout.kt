package com.lexinote.app.ui.editor

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lexinote.app.data.AnnotationEntity

private val readerWord = Regex("[A-Za-z]+(?:['’\\-][A-Za-z]+)*")
private class ReaderHitGeometry { var plan: ReaderLinePlan? = null }
private data class ReaderLayers(val token: ReaderToken, val phonetic: String?, val meaning: String?)

@Composable
internal fun ReaderParagraphLayout(
    block: ReaderBlock, annotations: Map<Int, AnnotationEntity>,
    phonetics: Boolean, translations: Boolean, onWord: (ReaderToken) -> Unit
) {
    val layers = remember(block, annotations, phonetics, translations) {
        block.tokens.map { token ->
            val annotation = annotations[token.start]?.takeIf {
                it.endOffset == token.end && it.word.equals(token.text, ignoreCase = true)
            }
            ReaderLayers(token,
                annotation?.phonetic?.trim()?.trim('/')?.takeIf { phonetics && it.isNotBlank() }?.let { "/$it/" },
                annotation?.meaning?.takeIf { translations && it.isNotBlank() })
        }
    }
    // The holder belongs to this exact layer set and is updated by measurement, without a state-write loop.
    val geometry = remember(layers) { ReaderHitGeometry() }
    val callback = rememberUpdatedState(onWord)
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    val originalStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = 18.sp, lineHeight = 26.sp, fontFamily = FontFamily.SansSerif,
        lineBreak = LineBreak.Simple, hyphens = Hyphens.None, color = colors.onSurface
    )
    val auxiliaryStyle = MaterialTheme.typography.bodySmall.copy(
        fontSize = 12.sp, lineHeight = 16.sp, fontFamily = FontFamily.SansSerif,
        hyphens = Hyphens.None
    )
    Layout(
        modifier = Modifier.fillMaxWidth().pointerInput(geometry, layers) {
            detectTapGestures { position ->
                val index = geometry.plan?.hit(position.x, position.y) ?: return@detectTapGestures
                val token = layers[index].token
                if (readerWord.matches(token.text)) callback.value(token)
            }
        },
        content = {
            layers.forEach { layer ->
                layer.phonetic?.let { Text(it, style = auxiliaryStyle, color = if (dark) Color(0xFFA7C3DE) else Color(0xFF526F89), maxLines = 2, overflow = TextOverflow.Ellipsis) }
                Text(layer.token.text, style = originalStyle)
                layer.meaning?.let { Text(it, style = auxiliaryStyle, color = if (dark) Color(0xFFA1D1B3) else Color(0xFF386B51), maxLines = 2, overflow = TextOverflow.Ellipsis) }
            }
        }
    ) { measurables, constraints ->
        val maxWidth = constraints.maxWidth.coerceAtLeast(1)
        val childConstraints = Constraints(maxWidth = maxWidth)
        var cursor = 0
        val measured = layers.map { layer ->
            val upper = if (layer.phonetic != null) measurables[cursor++].measure(childConstraints) else null
            val original = measurables[cursor++].measure(childConstraints)
            val lower = if (layer.meaning != null) measurables[cursor++].measure(childConstraints) else null
            Triple(upper, original, lower)
        }
        val sizes = measured.map { (upper, original, lower) ->
            val baseline = original[FirstBaseline].takeUnless { it == AlignmentLine.Unspecified } ?: original.height
            ReaderUnitSize(maxOf(upper?.width ?: 0, original.width, lower?.width ?: 0), original.height,
                baseline, upper?.height ?: 0, lower?.height ?: 0)
        }
        val plan = planReaderLines(sizes, maxWidth, 2.dp.roundToPx())
        geometry.plan = plan
        layout(constraints.constrainWidth(maxWidth), constraints.constrainHeight(plan.height.coerceAtLeast(if (layers.isEmpty()) 26.sp.roundToPx() else 0))) {
            plan.units.forEach { placement ->
                val (upper, original, lower) = measured[placement.index]
                upper?.place(placement.x + (placement.width - upper.width) / 2, placement.phoneticY)
                original.place(placement.x + (placement.width - original.width) / 2, placement.originalY)
                lower?.place(placement.x + (placement.width - lower.width) / 2, placement.translationY)
            }
        }
    }
}
