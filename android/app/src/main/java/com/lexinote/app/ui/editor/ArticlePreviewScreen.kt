package com.lexinote.app.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ArticlePreviewScreen(id: Long, vm: ArticleViewModel, onBack: () -> Unit, onEdit: () -> Unit) {
    val article by vm.article.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    LaunchedEffect(id) { vm.openPreview(id) }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("‹ 返回") }
            TextButton(onClick = onEdit, enabled = article?.id == id) { Text("重新编辑") }
        }
        when {
            loading -> CircularProgressIndicator()
            error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
            article?.id != id -> Text("找不到这篇文章")
            else -> {
                Text(article!!.title.ifBlank { "未命名文章" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(article!!.content, modifier = Modifier.fillMaxSize().padding(top = 16.dp).verticalScroll(rememberScrollState()), style = MaterialTheme.typography.bodyLarge, lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.5)
            }
        }
    }
}
