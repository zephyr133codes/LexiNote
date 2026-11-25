package com.lexinote.app.ui.vocabulary

import com.lexinote.app.data.WordRecord

internal fun pendingReviewWords(records: List<WordRecord>): List<WordRecord> = records.filterNot { it.isMastered }

internal fun normalizeReviewIndex(index: Int, queueSize: Int): Int =
    if (queueSize <= 0) index.coerceAtLeast(0) else index.coerceIn(0, queueSize - 1)

internal fun moveReviewIndex(index: Int, delta: Int, queueSize: Int): Int =
    if (queueSize <= 0) 0 else (index + delta).coerceIn(0, queueSize - 1)
