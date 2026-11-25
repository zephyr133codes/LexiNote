package com.lexinote.app.ui.vocabulary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lexinote.app.data.WordRecord
import com.lexinote.app.ui.editor.formatDictionaryPartOfSpeech
import com.lexinote.app.ui.editor.formatDictionaryPhonetic

enum class WordListSource { Favorites, NewWords, History }

@Composable
fun VocabularyScreen(modifier: Modifier = Modifier, vm: WordLibraryViewModel, onOpenList: (WordListSource) -> Unit) {
    val favorites by vm.starred.collectAsStateWithLifecycle()
    val newWords by vm.newWords.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(vertical = 20.dp)) {
        item { Text("单词库", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                VocabularySection("我的收藏", favorites, WordListSource.Favorites, onOpenList)
                HorizontalDivider(Modifier.padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                VocabularySection("我的生词", newWords, WordListSource.NewWords, onOpenList)
                HorizontalDivider(Modifier.padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
            }
        }
        item { VocabularySection("浏览记录", history, WordListSource.History, onOpenList) }
    }
}

@Composable
private fun VocabularySection(title: String, words: List<WordRecord>, source: WordListSource, onOpenList: (WordListSource) -> Unit) {
    Card(onClick = { onOpenList(source) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("${words.size} 个", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (words.isEmpty()) Text("暂无单词", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else {
                Text(words.take(3).joinToString("  ·  ") { it.displayWord }, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (words.size > 3) Text("还有 ${words.size - 3} 个单词", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Text("查看全部  ›", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun WordListScreen(source: WordListSource, vm: WordLibraryViewModel, onBack: () -> Unit, onReview: () -> Unit) {
    val favorites by vm.starred.collectAsStateWithLifecycle()
    val newWords by vm.newWords.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val dictionaryEntries by vm.entries.collectAsStateWithLifecycle()
    val lookupErrors by vm.lookupErrors.collectAsStateWithLifecycle()
    val loadingWords by vm.loadingWords.collectAsStateWithLifecycle()
    val selected by vm.selectedWord.collectAsStateWithLifecycle()
    val rows = when (source) {
        WordListSource.Favorites -> favorites
        WordListSource.NewWords -> newWords
        WordListSource.History -> history
    }
    val title = when (source) {
        WordListSource.Favorites -> "我的收藏"
        WordListSource.NewWords -> "我的生词"
        WordListSource.History -> "浏览记录"
    }
    DisposableEffect(vm) { onDispose { vm.closeWord() } }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 16.dp)) {
        Box(Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
            Text(title, Modifier.fillMaxWidth().padding(horizontal = 92.dp), style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) { Text("← 返回") }
            TextButton(onClick = onReview, modifier = Modifier.align(Alignment.CenterEnd)) { Text("背单词") }
        }
        if (rows.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(when (source) {
                WordListSource.Favorites -> "还没有收藏的单词"
                WordListSource.NewWords -> "暂无待学习生词"
                WordListSource.History -> "还没有浏览记录"
            }, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        } else LazyColumn(Modifier.fillMaxSize()) {
            items(rows, key = { it.normalizedWord }) { row ->
                LaunchedEffect(row.normalizedWord) { vm.loadEntry(row.displayWord) }
                val matching = dictionaryEntries[row.normalizedWord]
                val pos = lookupErrors[row.normalizedWord]
                    ?: if (row.normalizedWord in loadingWords) "查询中"
                    else matching?.senses?.map { formatDictionaryPartOfSpeech(it.pos) }?.distinct()?.joinToString(" / ").orEmpty()
                TextButton(onClick = { vm.openWord(row.displayWord) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(row.displayWord, color = MaterialTheme.colorScheme.onSurface)
                        Text(pos.ifEmpty { "词性暂无" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                if (selected == row.displayWord) WordDetailDialog(row, vm)
            }
        }
    }
}

@Composable
fun ReviewScreen(source: WordListSource, vm: WordLibraryViewModel, onBack: () -> Unit) {
    val favorites by vm.starred.collectAsStateWithLifecycle()
    val newWords by vm.newWords.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val lookupErrors by vm.lookupErrors.collectAsStateWithLifecycle()
    val loadingWords by vm.loadingWords.collectAsStateWithLifecycle()
    val dictionaryEntries by vm.entries.collectAsStateWithLifecycle()
    val sourceRows = when (source) {
        WordListSource.Favorites -> favorites
        WordListSource.NewWords -> newWords
        WordListSource.History -> history
    }
    val queue = remember(sourceRows) { pendingReviewWords(sourceRows) }
    val queueKeys = remember(queue) { queue.map { it.normalizedWord } }
    var selectedWord by remember(source) { mutableStateOf<String?>(null) }
    var fallbackIndex by remember(source) { mutableIntStateOf(0) }
    var savingMastered by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf(false) }
    var finishRequested by remember(source) { mutableStateOf(false) }
    val selectedIndex = selectedWord?.let { word -> queue.indexOfFirst { it.normalizedWord == word } } ?: -1
    val index = if (selectedIndex >= 0) selectedIndex else normalizeReviewIndex(fallbackIndex, queue.size)
    val current = queue.getOrNull(index)
    LaunchedEffect(queueKeys, selectedWord) {
        if (queue.isEmpty()) {
            selectedWord = null
            fallbackIndex = 0
        } else if (selectedWord == null || selectedIndex < 0) {
            val next = normalizeReviewIndex(fallbackIndex, queue.size)
            selectedWord = queue[next].normalizedWord
            fallbackIndex = next
        } else fallbackIndex = selectedIndex
    }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← 返回") }
            Text("背单词", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            if (current == null) {
                TextButton(onClick = {
                    if (!finishRequested) {
                        finishRequested = true
                        onBack()
                    }
                }, enabled = !finishRequested) { Text("完成") }
            } else Text("${index + 1}/${queue.size}")
        }
        if (current == null) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(if (sourceRows.isEmpty()) "当前没有单词" else "本轮完成", style = MaterialTheme.typography.headlineSmall)
        } else {
            LaunchedEffect(current.normalizedWord) { vm.loadEntry(current.displayWord) }
            val shown = dictionaryEntries[current.normalizedWord]?.takeIf { it.word.equals(current.displayWord, true) }
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp)) {
                Card(
                    Modifier.align(Alignment.Center).fillMaxWidth().heightIn(max = maxHeight * 0.78f).wrapContentHeight(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(current.displayWord, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        if (current.normalizedWord !in loadingWords) {
                            Text(formatDictionaryPhonetic(shown?.phonetic), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        }
                        when {
                            current.normalizedWord in loadingWords -> CircularProgressIndicator()
                            shown == null -> Text(lookupErrors[current.normalizedWord] ?: "本地词典暂未收录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            shown.senses.isEmpty() -> Text("暂无词性和释义", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            else -> shown.senses.groupBy { it.pos }.entries.forEachIndexed { index, (pos, senses) ->
                                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Text(formatDictionaryPartOfSpeech(pos), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                                senses.sortedBy { it.order }.forEach { sense ->
                                    Text(sense.meaning, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { val next = moveReviewIndex(index, -1, queue.size); fallbackIndex = next; selectedWord = queue[next].normalizedWord }, enabled = index > 0) { Text("上一个") }
                Button(onClick = {
                    if (!savingMastered) {
                        savingMastered = true
                        vm.setMastered(current.displayWord, true) { success ->
                            savingMastered = false
                            saveError = !success
                        }
                    }
                }, enabled = !savingMastered) { Text(if (savingMastered) "保存中" else "掌握记住") }
                TextButton(onClick = { val next = moveReviewIndex(index, 1, queue.size); fallbackIndex = next; selectedWord = queue[next].normalizedWord }, enabled = index < queue.lastIndex) { Text("下一个") }
            }
            if (saveError) AlertDialog(onDismissRequest = { saveError = false }, title = { Text("保存失败") }, text = { Text("暂时无法更新掌握状态，请重试。") }, confirmButton = { TextButton(onClick = { saveError = false }) { Text("关闭") } })
        }
    }
}

@Composable
private fun WordDetailDialog(record: WordRecord, vm: WordLibraryViewModel) {
    val entry by vm.entry.collectAsStateWithLifecycle()
    val starred by vm.starred.collectAsStateWithLifecycle()
    val lookupErrors by vm.lookupErrors.collectAsStateWithLifecycle()
    val word = record.displayWord
    val shown = entry?.takeIf { it.word.equals(word, true) }
    val isStarred = starred.any { it.normalizedWord == record.normalizedWord }
    AlertDialog(onDismissRequest = vm::closeWord, title = {
        DictionaryDialogTitle(word, isStarred, { vm.toggleStar(word) }, vm::closeWord)
    }, text = {
        Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
            Text(formatDictionaryPhonetic(shown?.phonetic))
            if (shown == null) Text(lookupErrors[record.normalizedWord] ?: "本地词典暂未收录")
            else if (shown.senses.isEmpty()) Text("暂无词性和释义")
            else shown.senses.groupBy { it.pos }.entries.forEachIndexed { index, (pos, senses) ->
                if (index > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Text(formatDictionaryPartOfSpeech(pos), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                senses.sortedBy { it.order }.forEach { Text(it.meaning, modifier = Modifier.padding(top = 4.dp)) }
            }
        }
    }, confirmButton = {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = { vm.setMastered(word, !record.isMastered) }) { Text(if (record.isMastered) "撤销掌握" else "掌握记住") }
            TextButton(onClick = vm::closeWord) { Text("关闭") }
        }
    })
}
