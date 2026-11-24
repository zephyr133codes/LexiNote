package com.lexinote.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.lexinote.app.ui.home.HomeScreen
import com.lexinote.app.ui.profile.ProfileScreen
import com.lexinote.app.ui.vocabulary.VocabularyScreen

private enum class Page { Home, Vocabulary, Profile }

@Composable
fun LexiNoteApp() {
    var page by remember { mutableStateOf(Page.Home) }
    BackHandler(enabled = page == Page.Profile) { page = Page.Home }

    Scaffold(
        bottomBar = {
            if (page != Page.Profile) {
                NavigationBar {
                    NavigationBarItem(
                        selected = page == Page.Home,
                        onClick = { page = Page.Home },
                        icon = { Text("⌂") },
                        label = { Text("首页") }
                    )
                    NavigationBarItem(
                        selected = page == Page.Vocabulary,
                        onClick = { page = Page.Vocabulary },
                        icon = { Text("Aa") },
                        label = { Text("单词库") }
                    )
                }
            }
        }
    ) { padding ->
        when (page) {
            Page.Home -> HomeScreen(
                modifier = Modifier.padding(padding),
                onProfileClick = { page = Page.Profile },
                onCreateArticle = { }
            )
            Page.Vocabulary -> VocabularyScreen(Modifier.padding(padding))
            Page.Profile -> ProfileScreen(
                modifier = Modifier.padding(padding),
                onBack = { page = Page.Home }
            )
        }
    }
}
