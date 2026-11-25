package com.lexinote.app

import com.lexinote.app.data.WordRecord
import com.lexinote.app.ui.vocabulary.moveReviewIndex
import com.lexinote.app.ui.vocabulary.normalizeReviewIndex
import com.lexinote.app.ui.vocabulary.pendingReviewWords
import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewQueueTest {
    @Test fun onlyUnmasteredItemsRemainInTheReviewQueue() {
        val rows = listOf(record("book"), record("run", mastered = true), record("work"))
        assertEquals(listOf("book", "work"), pendingReviewWords(rows).map { it.normalizedWord })
    }

    @Test fun navigationClampsAtBothEndsAndHandlesEmptyAndSingleItemQueues() {
        assertEquals(0, moveReviewIndex(0, -1, 3))
        assertEquals(2, moveReviewIndex(1, 1, 3))
        assertEquals(0, moveReviewIndex(0, 1, 1))
        assertEquals(0, moveReviewIndex(4, 1, 0))
        assertEquals(1, normalizeReviewIndex(2, 2))
        assertEquals(2, normalizeReviewIndex(2, 0))
    }

    @Test fun markingCurrentWordMovesItsSuccessorIntoSameIndex() {
        val rows = listOf(record("book"), record("run"), record("work"))
        val queueAfterMarkingRun = pendingReviewWords(rows.map { if (it.normalizedWord == "run") it.copy(isMastered = true) else it })
        val currentIndex = normalizeReviewIndex(1, queueAfterMarkingRun.size)
        assertEquals("work", queueAfterMarkingRun[currentIndex].normalizedWord)
    }

    @Test fun finishedQueueIsEmptyWithoutChangingMasteredRecords() {
        val rows = listOf(record("book", mastered = true), record("run", mastered = true))
        assertEquals(emptyList<String>(), pendingReviewWords(rows).map { it.normalizedWord })
        assertEquals(listOf(true, true), rows.map { it.isMastered })
    }

    private fun record(word: String, mastered: Boolean = false) = WordRecord(
        normalizedWord = word,
        displayWord = word,
        lastViewedAt = 1,
        viewCount = 1,
        isMastered = mastered
    )
}
