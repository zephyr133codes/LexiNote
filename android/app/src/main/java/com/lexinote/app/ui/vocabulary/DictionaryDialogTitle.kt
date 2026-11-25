package com.lexinote.app.ui.vocabulary

import androidx.compose.foundation.layout.*
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun DictionaryDialogTitle(
    word: String,
    isStarred: Boolean,
    onToggleStar: () -> Unit,
    onClose: () -> Unit
) {
    Box(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(end = 52.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                word,
                modifier = Modifier.weight(1f, fill = false).padding(top = 10.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.size(6.dp))
            IconButton(
                onClick = onToggleStar,
                modifier = Modifier.size(48.dp).semantics {
                    contentDescription = if (isStarred) "取消收藏 $word" else "收藏 $word"
                }
            ) {
                Text(
                    if (isStarred) "★" else "☆",
                    color = if (isStarred) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }
        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopEnd).size(48.dp).semantics {
                contentDescription = "关闭词典"
            }
        ) {
            Text("×", style = MaterialTheme.typography.titleLarge)
        }
    }
}
