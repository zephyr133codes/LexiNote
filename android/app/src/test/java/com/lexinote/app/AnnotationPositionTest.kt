package com.lexinote.app

import com.lexinote.app.ui.editor.annotationMatchesToken
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnnotationPositionTest {
    @Test fun matchesOnlySelectedOccurrence() {
        val text = "book then book"
        assertTrue(annotationMatchesToken(text, 10, 14, "book", 10, 14))
        assertFalse(annotationMatchesToken(text, 10, 14, "book", 0, 4))
    }

    @Test fun rejectsStaleOrMismatchedRange() {
        val text = "a book, books"
        assertFalse(annotationMatchesToken(text, 2, 6, "book", 2, 7))
        assertFalse(annotationMatchesToken(text, 2, 6, "books", 2, 6))
        assertFalse(annotationMatchesToken(text, -1, 3, "book", -1, 3))
    }
}
