package com.lexinote.app

import com.lexinote.app.ui.editor.*
import org.junit.Assert.*
import org.junit.Test

class ReaderLinePlanTest {
    @Test fun slotsAppearOnlyWhenVisible() {
        fun height(p: Int, t: Int) = planReaderLines(listOf(ReaderUnitSize(30, 26, 19, p, t)), 200, 2).height
        assertEquals(26, height(0, 0))
        assertEquals(44, height(16, 0))
        assertEquals(44, height(0, 16))
        assertEquals(62, height(16, 16))
    }
    @Test fun mixedUnitsShareEnglishBaselineAndSlotsNeverOverlap() {
        val sizes = listOf(ReaderUnitSize(45, 26, 19), ReaderUnitSize(80, 30, 21, 16, 32), ReaderUnitSize(30, 28, 20))
        val plan = planReaderLines(sizes, 200, 2)
        assertEquals(1, plan.units.mapIndexed { i, p -> p.originalY + sizes[i].baseline }.distinct().size)
        val annotated = plan.units[1]
        assertTrue(annotated.phoneticY + 16 <= annotated.originalY)
        assertTrue(annotated.originalY + 30 <= annotated.translationY)
    }
    @Test fun atomicWrapHitRectsAndHiddenSlots() {
        val shown = planReaderLines(listOf(ReaderUnitSize(70, 26, 19, 16), ReaderUnitSize(50, 26, 19)), 100, 2)
        assertTrue(shown.units[1].top > shown.units[0].top)
        assertEquals(0, shown.hit(1f, 1f))
        assertEquals(1, shown.hit(1f, shown.units[1].originalY.toFloat()))
        assertNull(shown.hit(90f, shown.units[1].originalY.toFloat()))
        val hidden = planReaderLines(listOf(ReaderUnitSize(40, 26, 19), ReaderUnitSize(50, 26, 19)), 100, 2)
        assertEquals(0, hidden.units[1].top)
        assertEquals(1, hidden.hit(41f, 1f))
        assertNull(hidden.hit(-1f, 1f))
    }
    @Test fun wrappedLongWordReservesFullHeight() {
        val plan = planReaderLines(listOf(ReaderUnitSize(100, 78, 19, 32, 32), ReaderUnitSize(30, 26, 19)), 100, 2)
        assertTrue(plan.units[1].top >= plan.units[0].top + plan.units[0].height)
        assertEquals(174, plan.height) // 32 + 2 + 78 + 2 + 32, then 2 gap + 26 original-only row.
    }
}
