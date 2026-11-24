package com.lexinote.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lexinote.app.data.ArticleEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onProfileClick: () -> Unit,
    onCreateArticle: () -> Unit,
    articles: List<ArticleEntity>, recentlyRead: List<ArticleEntity>, onOpenArticle: (Long) -> Unit, onDeleteArticle: (Long) -> Unit
) {
    var deleteTarget by remember { mutableStateOf<ArticleEntity?>(null) }
    Column(
        modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("LexiNote", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("阅读 · 标注 · 积累", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FilledTonalButton(onClick = onProfileClick, shape = CircleShape, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(48.dp)) {
                Text("我", style = MaterialTheme.typography.titleMedium)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("已保存文章", articles.size.toString(), Modifier.weight(1f))
            StatCard("已收录单词", "0", Modifier.weight(1f))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text("最近阅读", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (recentlyRead.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                ) {
                    Box(Modifier.fillMaxWidth().height(150.dp).padding(20.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("还没有阅读记录", style = MaterialTheme.typography.titleMedium)
                            Text("保存或打开文章后，它会出现在这里。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                val latest = recentlyRead.first()
                Card(
                    onClick = { onOpenArticle(latest.id) },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                ) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(latest.title.ifBlank { "未命名文章" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("最近阅读：${SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(latest.lastReadAt!!))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(latest.content.replace(Regex("\\s+"), " ").take(140), style = MaterialTheme.typography.bodyMedium, maxLines = 3, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Text("历史项目", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 20.dp))
            if (articles.isEmpty()) Text("还没有保存的文章", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            articles.forEach { article ->
                Card(onClick = { onOpenArticle(article.id) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(article.title.ifBlank { "未命名文章" }, style = MaterialTheme.typography.titleMedium)
                            Text(SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(article.updatedAt)), style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { deleteTarget = article }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
        Button(
            onClick = onCreateArticle,
            modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)
        ) { Text("＋  新建文章") }
    }
    deleteTarget?.let { target -> AlertDialog(onDismissRequest = { deleteTarget = null }, title = { Text("删除文章") }, text = { Text("确定删除“${target.title.ifBlank { "未命名文章" }}”吗？") }, confirmButton = { TextButton(onClick = { onDeleteArticle(target.id); deleteTarget = null }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("删除") } }, dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } }) }
}

@Composable
private fun StatCard(label: String, count: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(count, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
