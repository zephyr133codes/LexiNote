package com.lexinote.app.ui.editor

/** Pixel measurements supplied by Compose, never estimates based on character count. */
internal data class ReaderUnitSize(
    val width: Int, val originalHeight: Int, val baseline: Int,
    val phoneticHeight: Int = 0, val translationHeight: Int = 0
)
internal data class ReaderUnitPlacement(
    val index: Int, val x: Int, val top: Int, val width: Int, val height: Int,
    val phoneticY: Int, val originalY: Int, val translationY: Int
)
internal data class ReaderLinePlan(val units: List<ReaderUnitPlacement>, val height: Int) {
    fun hit(x: Float, y: Float): Int? = units.firstOrNull {
        x >= it.x && x < it.x + it.width && y >= it.top && y < it.top + it.height
    }?.index
}

internal fun planReaderLines(sizes: List<ReaderUnitSize>, maxWidth: Int, gap: Int): ReaderLinePlan {
    val result = mutableListOf<ReaderUnitPlacement>()
    var top = 0
    var start = 0
    while (start < sizes.size) {
        var end = start
        var width = 0
        while (end < sizes.size && (end == start || width + sizes[end].width <= maxWidth)) {
            width += sizes[end].width
            end++
        }
        val row = sizes.subList(start, end)
        val upper = row.maxOf { it.phoneticHeight }
        val ascent = row.maxOf { it.baseline }
        val descent = row.maxOf { it.originalHeight - it.baseline }
        val lower = row.maxOf { it.translationHeight }
        val upperGap = if (upper > 0) gap else 0
        val lowerGap = if (lower > 0) gap else 0
        val originalTop = top + upper + upperGap
        val lowerTop = originalTop + ascent + descent + lowerGap
        val height = lowerTop + lower - top
        var x = 0
        for (index in start until end) {
            val size = sizes[index]
            result += ReaderUnitPlacement(index, x, top, size.width, height,
                top + upper - size.phoneticHeight,
                originalTop + ascent - size.baseline, lowerTop)
            x += size.width
        }
        top += height + if (end < sizes.size) gap else 0
        start = end
    }
    return ReaderLinePlan(result, top)
}
