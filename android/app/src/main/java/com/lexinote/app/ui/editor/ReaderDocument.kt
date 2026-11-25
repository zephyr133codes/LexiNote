package com.lexinote.app.ui.editor

import androidx.compose.runtime.Immutable

@Immutable
data class ReaderToken(val text: String, val start: Int, val end: Int)
@Immutable
data class ReaderBlock(val start: Int, val end: Int, val tokens: List<ReaderToken>, val breakText: String)
class ReaderDocumentCache {
    private var articleId: Long? = null
    private var source: String? = null
    private var blocks = emptyList<ReaderBlock>()
    var builds = 0
        private set
    @Synchronized fun get(id: Long, text: String): List<ReaderBlock> {
        if (articleId == id && source == text) return blocks
        val result = mutableListOf<ReaderBlock>()
        var start = 0
        var tokens = mutableListOf<ReaderToken>()
        val pattern = Regex("\\r\\n|\\r|\\n|[A-Za-z]+(?:['’\\-][A-Za-z]+)*|[^\\S\\r\\n]+|[^A-Za-z\\s]+")
        for (match in pattern.findAll(text)) {
            val token = ReaderToken(match.value, match.range.first, match.range.last + 1)
            if (token.text == "\n" || token.text == "\r" || token.text == "\r\n") {
                // A physical newline immediately after a technical split terminates that block;
                // it must not manufacture an additional empty visual paragraph.
                val previous = result.lastOrNull()
                if (tokens.isEmpty() && previous != null && previous.end == start && previous.breakText.isEmpty()) {
                    result[result.lastIndex] = previous.copy(end = token.end, breakText = token.text)
                } else result.add(ReaderBlock(start, token.end, tokens.toList(), token.text))
                tokens = mutableListOf(); start = token.end
            } else {
                tokens.add(token)
                // Split only at whitespace boundaries; never cut a word or its following punctuation.
                if (tokens.size >= 160 && token.text.all { it.isWhitespace() }) {
                    result.add(ReaderBlock(start, token.end, tokens.toList(), ""))
                    tokens = mutableListOf(); start = token.end
                }
            }
        }
        if (tokens.isNotEmpty() || result.isEmpty() || result.last().breakText.isNotEmpty())
            result.add(ReaderBlock(start, text.length, tokens.toList(), ""))
        articleId = id; source = text; blocks = result.toList(); builds++
        return blocks
    }
}
