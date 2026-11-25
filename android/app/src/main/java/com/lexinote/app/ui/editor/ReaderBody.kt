package com.lexinote.app.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun ReaderBody(id: Long, content: String, vm: ArticleViewModel, phonetics: Boolean, translations: Boolean) {
    val document by produceState<List<ReaderBlock>?>(null, id, content) {
        value = null
        value = withContext(Dispatchers.Default) { vm.readerCache.get(id, content) }
    }
    val annotations = vm.annotations.collectAsStateWithLifecycle()
    val indexed by remember(id, annotations) { derivedStateOf {
        annotations.value.rows.takeIf { annotations.value.articleId == id }?.associateBy { it.startOffset } ?: emptyMap()
    } }
    val currentIndex = rememberUpdatedState(indexed)
    val onWord = remember(vm) { { token: ReaderToken -> vm.lookupWord(token.text, token.start, token.end) } }
    if (document == null) CircularProgressIndicator()
    else LazyColumn(Modifier.fillMaxSize().padding(top = 16.dp)) {
        items(document!!, key = { "$id:${it.start}:${it.end}" }) { block ->
            val local by remember(block, currentIndex) { derivedStateOf {
                block.tokens.mapNotNull { token -> currentIndex.value[token.start]?.takeIf { it.endOffset == token.end && it.word.equals(token.text, true) } }.associateBy { it.startOffset }
            } }
            ReaderParagraphLayout(block, local, phonetics, translations, onWord)
        }
    }
}
