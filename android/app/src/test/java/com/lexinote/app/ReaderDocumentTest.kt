package com.lexinote.app

import com.lexinote.app.ui.editor.ReaderDocumentCache
import org.junit.Assert.*
import org.junit.Test

class ReaderDocumentTest {
    @Test fun newlineAfterTechnicalSplitDoesNotAddAnEmptyParagraph() {
        val text = "book ".repeat(80) + "\r\nroom"
        val blocks = ReaderDocumentCache().get(1, text)
        assertEquals(2, blocks.size)
        assertEquals("\r\n", blocks.first().breakText)
        assertEquals(text, blocks.joinToString("") { b -> b.tokens.joinToString("") { it.text } + b.breakText })
        assertEquals(text.indexOf("room"), blocks.last().tokens.first().start)
    }
    @Test fun preservesAllCharactersAndOffsets() {
        val text = "book, book\r\n\r\n\ncan't re-read.  book\r"
        val blocks = ReaderDocumentCache().get(1, text)
        assertEquals(text, blocks.joinToString("") { b -> b.tokens.joinToString("") { it.text } + b.breakText })
        blocks.forEach { b -> b.tokens.forEach { t -> assertEquals(t.text, text.substring(t.start, t.end)) } }
        assertTrue(blocks.count { it.tokens.isEmpty() } >= 3)
        assertEquals(3, blocks.flatMap { it.tokens }.count { it.text == "book" })
    }
    @Test fun cachesOnlySameArticleAndContent() {
        val cache = ReaderDocumentCache()
        val first = cache.get(1, "book")
        repeat(10) { assertSame(first, cache.get(1, "book")) }
        assertEquals(1, cache.builds)
        cache.get(2, "book"); assertEquals(2, cache.builds)
        cache.get(2, "books"); assertEquals(3, cache.builds)
    }
    @Test fun splitsLongParagraphAtTokenBoundaries() {
        val text = "book, next-word  ".repeat(10000)
        val blocks = ReaderDocumentCache().get(1, text)
        assertTrue(blocks.size > 100)
        assertEquals(text, blocks.joinToString("") { b -> b.tokens.joinToString("") { it.text } + b.breakText })
        assertEquals(10000, blocks.flatMap { it.tokens }.count { it.text == "next-word" })
        assertEquals(text.length, blocks.last().end)
    }
}
