package com.lexinote.app.ui.editor

/** Matches a persisted source range to exactly one tokenizer token without changing source offsets. */
internal fun annotationMatchesToken(
    content: String,
    annotationStart: Int,
    annotationEnd: Int,
    annotationWord: String,
    tokenStart: Int,
    tokenEnd: Int
): Boolean = annotationStart == tokenStart && annotationEnd == tokenEnd &&
    annotationStart >= 0 && annotationStart < annotationEnd && annotationEnd <= content.length &&
    content.substring(annotationStart, annotationEnd).equals(annotationWord, ignoreCase = true)
