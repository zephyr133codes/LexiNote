package com.lexinote.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.lexinote.app.data.AnnotationEntity
import com.lexinote.app.ui.editor.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ReaderLayoutTest {
    @get:Rule val rule = createComposeRule()

    @Test fun narrowScreenConstrainsLongOriginalAndAuxiliaryText() {
        val word = "supercalifragilisticexpialidocious"
        val block = ReaderDocumentCache().get(1, "$word room").single()
        val annotation = AnnotationEntity(articleId = 1, startOffset = 0, endOffset = word.length,
            word = word, phonetic = "verylongphonetic".repeat(10), meaning = "很长的中文释义".repeat(10), createdAt = 1)
        rule.setContent {
            MaterialTheme {
                Box(Modifier.width(120.dp).testTag("paragraph")) {
                    ReaderParagraphLayout(block, mapOf(0 to annotation), true, true) { }
                }
            }
        }
        val parent = rule.onNodeWithTag("paragraph").fetchSemanticsNode().boundsInRoot
        val original = rule.onNodeWithText(word).fetchSemanticsNode().boundsInRoot
        val meaning = rule.onNodeWithText(annotation.meaning!!).fetchSemanticsNode().boundsInRoot
        val next = rule.onNodeWithText("room").fetchSemanticsNode().boundsInRoot
        assertTrue(original.width <= parent.width + 1f)
        assertTrue(meaning.width <= parent.width + 1f)
        assertTrue(meaning.top >= original.bottom)
        assertTrue(next.top >= meaning.bottom)
    }

    @Test fun realTextMeasurementAlignsBaselineAndPreservesClicksAcrossFourModes() {
        val phonetics = mutableStateOf(false)
        val translations = mutableStateOf(false)
        val selected = mutableListOf<ReaderToken>()
        val block = ReaderDocumentCache().get(1, "book room").single()
        val annotation = AnnotationEntity(articleId = 1, startOffset = 0, endOffset = 4,
            word = "book", phonetic = "bʊk", meaning = "书籍及账簿", createdAt = 1)
        rule.setContent {
            MaterialTheme {
                Box(Modifier.width(320.dp).testTag("paragraph")) {
                    ReaderParagraphLayout(block, mapOf(0 to annotation), phonetics.value, translations.value) { selected.add(it) }
                }
            }
        }
        fun bounds(text: String) = rule.onNodeWithText(text).fetchSemanticsNode().boundsInRoot
        fun baseline(text: String): Float {
            val layouts = mutableListOf<TextLayoutResult>()
            rule.onNodeWithText(text).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            return bounds(text).top + layouts.single().firstBaseline
        }
        fun paragraphHeight() = rule.onNodeWithTag("paragraph").fetchSemanticsNode().boundsInRoot.height
        val plainHeight = paragraphHeight()
        rule.onNodeWithText("book").performTouchInput { click() }
        assertEquals(0, selected.last().start)
        rule.runOnIdle { phonetics.value = true }
        rule.onNodeWithText("/bʊk/").assertExists()
        assertEquals(baseline("book"), baseline("room"), 1f)
        assertTrue(paragraphHeight() > plainHeight)
        rule.onNodeWithText("/bʊk/").performTouchInput { click() }
        assertEquals(0, selected.last().start)
        rule.runOnIdle { phonetics.value = false; translations.value = true }
        rule.onNodeWithText("/bʊk/").assertDoesNotExist()
        assertEquals(baseline("book"), baseline("room"), 1f)
        assertTrue(bounds("书籍及账簿").top >= bounds("book").bottom)
        val translatedHeight = paragraphHeight()
        rule.runOnIdle { phonetics.value = true }
        assertTrue(paragraphHeight() > translatedHeight)
        assertEquals(bounds("书籍及账簿").center.x, bounds("book").center.x, 1f)
        rule.onNodeWithText("room").performTouchInput { click() }
        assertEquals(5, selected.last().start)
        rule.runOnIdle { phonetics.value = false; translations.value = false }
        assertEquals(plainHeight, paragraphHeight(), 1f)
    }

    @Test fun longPressUsesWordCallbackWithoutTapAndScrollDoesNotDispatchTap() {
        val tapped = mutableListOf<ReaderToken>()
        val longPressed = mutableListOf<ReaderToken>()
        val scroll = ScrollState(0)
        val block = ReaderDocumentCache().get(1, "book room ".repeat(35)).single()
        rule.setContent {
            MaterialTheme {
                Column(Modifier.width(300.dp).height(120.dp).verticalScroll(scroll)) {
                    ReaderParagraphLayout(
                        block = block,
                        annotations = emptyMap(),
                        phonetics = false,
                        translations = false,
                        onWord = { tapped += it },
                        onLongPress = { longPressed += it }
                    )
                }
            }
        }

        rule.onAllNodesWithText("book").onFirst().performTouchInput { longClick() }
        assertTrue(tapped.isEmpty())
        assertEquals(0, longPressed.single().start)

        rule.onAllNodesWithText("room").onFirst().performTouchInput { click() }
        assertEquals(5, tapped.single().start)
        rule.onAllNodesWithText("book").onFirst().performTouchInput { swipeUp() }
        assertEquals(1, tapped.size)
        assertTrue(scroll.value > 0)
    }
}
