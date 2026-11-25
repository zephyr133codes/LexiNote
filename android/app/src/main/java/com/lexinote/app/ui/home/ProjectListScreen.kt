package com.lexinote.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lexinote.app.data.ArticleEntity

@Composable
fun ProjectListScreen(
    articles: List<ArticleEntity>,
    onBack: () -> Unit,
    onOpenArticle: (Long) -> Unit,
    onDeleteArticle: (Long) -> Unit
) {
    var deleteTarget by remember { mutableStateOf<ArticleEntity?>(null) }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 18.dp)) {
        Box(Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
            Text("项目", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) { Text("← 返回") }
        }
        if (articles.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("还没有保存的项目", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(articles, key = { it.id }) { article ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f).clickable { onOpenArticle(article.id) }.padding(vertical = 6.dp)) {
                                Text(article.title.ifBlank { "未命名文章" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                                Text("最后修改：${formatDate(article.updatedAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                            }
                            TextButton(onClick = { deleteTarget = article }) {
                                Text("删除", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
    deleteTarget?.let { article ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除项目") },
            text = { Text("确定删除“${article.title.ifBlank { "未命名文章" }}”吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(onClick = { onDeleteArticle(article.id); deleteTarget = null }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } }
        )
    }
}
