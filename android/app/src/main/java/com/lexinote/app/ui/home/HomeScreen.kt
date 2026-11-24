package com.lexinote.app.ui.home

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onProfileClick: () -> Unit,
    onCreateArticle: () -> Unit
) {
    val context = LocalContext.current
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
            StatCard("已保存文章", "0", Modifier.weight(1f))
            StatCard("已收录单词", "0", Modifier.weight(1f))
        }
        Column(Modifier.weight(1f)) {
            Text("最近阅读", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            ) {
                Box(Modifier.fillMaxWidth().height(190.dp).padding(20.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("还没有阅读记录", style = MaterialTheme.typography.titleMedium)
                        Text("保存一篇文章后，它会出现在这里。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Button(
            onClick = { Toast.makeText(context, "新建文章功能开发中", Toast.LENGTH_SHORT).show(); onCreateArticle() },
            modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)
        ) { Text("＋  新建文章") }
    }
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
