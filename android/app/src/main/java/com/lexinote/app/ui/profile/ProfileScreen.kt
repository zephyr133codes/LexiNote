package com.lexinote.app.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ProfileScreen(modifier: Modifier = Modifier, onBack: () -> Unit) {
    Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) { Text("‹  返回") }
        Text("个人中心", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("个人资料功能开发中", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
