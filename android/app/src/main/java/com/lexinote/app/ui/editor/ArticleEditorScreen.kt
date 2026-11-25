package com.lexinote.app.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ArticleEditorScreen(id: Long?, vm: ArticleViewModel, onExit: () -> Unit, onSaved: (Long) -> Unit) {
    var title by rememberSaveable(id) { mutableStateOf("") }
    var body by rememberSaveable(id) { mutableStateOf("") }
    var initialized by rememberSaveable(id) { mutableStateOf(id == null) }
    var showExitDialog by rememberSaveable { mutableStateOf(false) }
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val original by vm.article.collectAsStateWithLifecycle()
    val annotationState by vm.annotations.collectAsStateWithLifecycle()
    val existingAnnotations = annotationState.rows.takeIf { annotationState.articleId == id } ?: emptyList()
    var showAnnotationWarning by rememberSaveable { mutableStateOf(false) }
    DisposableEffect(id) {
        if (id != null) vm.observeAnnotations(id)
        onDispose { if (id != null) vm.stopObservingAnnotations(id) }
    }
    LaunchedEffect(id) { if (id != null) vm.load(id) }
    LaunchedEffect(original, id) { if (id != null && original?.id == id && !initialized) { title = original!!.title; body = original!!.content; initialized = true } }
    val changed = if (id == null) title.isNotBlank() || body.isNotBlank() else initialized && (title != original?.title || body != original?.content)
    fun requestExit() { if (changed) showExitDialog = true else onExit() }
    BackHandler { requestExit() }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Bottom)).imePadding()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            TextButton(onClick = ::requestExit) { Text("‹  返回") }
            Text(if (id == null) "新建文章" else "编辑文章", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(onClick = {
                    if (id != null && body != original?.content && existingAnnotations.isNotEmpty()) showAnnotationWarning = true
                    else vm.save(id, title, body) { onSaved(it!!) }
                }, enabled = body.isNotBlank() && initialized && !loading && (id == null || (annotationState.articleId == id && !annotationState.loading && annotationState.error == null))) { Text(if (loading) "保存中…" else "保存并继续") }
            }
        }
        OutlinedTextField(value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), placeholder = { Text("标题（可选）") }, singleLine = true)
        OutlinedTextField(value = body, onValueChange = { body = it }, modifier = Modifier.fillMaxWidth().weight(1f).padding(20.dp).verticalScroll(rememberScrollState()), placeholder = { Text("在此输入英文正文…") }, minLines = 8, enabled = initialized)
        if (annotationState.error != null) Text(annotationState.error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
    }
    if (showExitDialog) AlertDialog(onDismissRequest = { showExitDialog = false }, title = { Text("您的项目还未保存") }, text = { Text("退出后，未保存的内容将会丢失。确定要退出吗？") }, confirmButton = { TextButton(onClick = onExit, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("退出") } }, dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text("取消") } })
    if (showAnnotationWarning) AlertDialog(
        onDismissRequest = { showAnnotationWarning = false },
        title = { Text("正文已修改") },
        text = { Text("正文修改可能使已有音标标注无法对应原文。继续保存将清除这篇文章的旧标注。") },
        confirmButton = { TextButton(onClick = { showAnnotationWarning = false; vm.save(id, title, body, clearAnnotations = true) { onSaved(it!!) } }) { Text("确认保存") } },
        dismissButton = { TextButton(onClick = { showAnnotationWarning = false }) { Text("取消") } }
    )
}
