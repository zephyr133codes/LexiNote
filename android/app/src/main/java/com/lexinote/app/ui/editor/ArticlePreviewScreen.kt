package com.lexinote.app.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle


@Composable
fun ArticlePreviewScreen(id: Long, vm: ArticleViewModel, onBack: () -> Unit, onEdit: () -> Unit) {
    val article by vm.article.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val lookup by vm.lookup.collectAsStateWithLifecycle()
    val annotationState by vm.annotations.collectAsStateWithLifecycle()
    val missingPhonetic by vm.missingPhonetic.collectAsStateWithLifecycle()
    val annotationError by vm.annotationError.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showPhonetics by remember { mutableStateOf(context.getSharedPreferences("reader_preferences", 0).getBoolean("show_phonetics", true)) }
    var showTranslations by remember { mutableStateOf(context.getSharedPreferences("reader_preferences", 0).getBoolean("show_translations", false)) }
    LaunchedEffect(id) { vm.openPreview(id) }
    DisposableEffect(id) {
        vm.observeAnnotations(id)
        onDispose { vm.stopObservingAnnotations(id) }
    }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("‹ 返回") }
            TextButton(onClick = onEdit, enabled = article?.id == id) { Text("重新编辑") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { Text("音标"); Switch(checked = showPhonetics, onCheckedChange = { showPhonetics = it; context.getSharedPreferences("reader_preferences", 0).edit().putBoolean("show_phonetics", it).apply() }) }
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { Text("翻译"); Switch(checked = showTranslations, onCheckedChange = { showTranslations = it; context.getSharedPreferences("reader_preferences", 0).edit().putBoolean("show_translations", it).apply() }) }
        }
        annotationState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        when {
            loading -> CircularProgressIndicator()
            error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
            article?.id != id -> Text("找不到这篇文章")
            else -> {
                Text(article!!.title.ifBlank { "未命名文章" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                key(id, article!!.content) {
                    ReaderBody(id, article!!.content, vm, showPhonetics, showTranslations)
                }

            }
        }
    }
    lookup?.let { state ->
        AlertDialog(
            onDismissRequest = vm::clearLookup,
            title = {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(state.word, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        val entry = (state as? LookupState.Result)?.entry
                        Text(formatDictionaryPhonetic(entry?.phonetic), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = vm::clearLookup) { Text("×", style = MaterialTheme.typography.titleLarge) }
                }
            },
            text = {
                Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    when (state) {
                        is LookupState.Loading -> CircularProgressIndicator()
                        is LookupState.Failure -> Text(state.message, color = MaterialTheme.colorScheme.error)
                        is LookupState.Result -> {
                            val entry = state.entry
                            if (entry == null) Text("本地词典暂未收录")
                            else if (entry.senses.isEmpty()) Text("暂无释义")
                            else entry.senses.groupBy { it.pos }.entries.toList().forEachIndexed { index, (pos, senses) ->
                                if (index > 0) HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                                Text(formatDictionaryPartOfSpeech(pos), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = if (index == 0) 8.dp else 0.dp))
                                senses.sortedBy { it.order }.forEach { sense ->
                                    Text(sense.meaning, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { vm.saveCurrentPhonetic(id) }, enabled = state is LookupState.Result) { Text("保留音标") }
                    TextButton(onClick = {}, enabled = false) { Text("保留意思") }
                    TextButton(onClick = {}, enabled = false) { Text("保留 All") }
                }
            }
        )
    }
    if (missingPhonetic) AlertDialog(onDismissRequest = vm::dismissMissingPhonetic, title = { Text("暂无注释") }, text = { Text("当前离线词典暂未收录该单词的音标。后续版本将支持联网查询，获取更多单词信息。") }, confirmButton = { TextButton(onClick = vm::dismissMissingPhonetic) { Text("关闭") } })
    if (annotationError != null) AlertDialog(onDismissRequest = vm::dismissAnnotationError, title = { Text("保存失败") }, text = { Text(annotationError!!) }, confirmButton = { TextButton(onClick = vm::dismissAnnotationError) { Text("关闭") } })
}
