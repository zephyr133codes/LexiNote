package com.lexinote.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lexinote.app.data.*
import com.lexinote.app.dictionary.DictionaryRepository
import com.lexinote.app.ui.editor.*
import com.lexinote.app.ui.home.HomeScreen
import com.lexinote.app.ui.profile.ProfileScreen
import com.lexinote.app.ui.vocabulary.VocabularyScreen

private enum class Page { Home, Vocabulary, Profile, Editor, Preview }

@Composable
fun LexiNoteApp() {
    val context = LocalContext.current
    val vm: ArticleViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = ArticleViewModel(ArticleRepository(LexiNoteDatabase.get(context).articleDao()), DictionaryRepository.get(context)) as T
    })
    val articles by vm.articles.collectAsStateWithLifecycle(initialValue = emptyList())
    val recent by vm.recentlyRead.collectAsStateWithLifecycle(initialValue = emptyList())
    var page by remember { mutableStateOf(Page.Home) }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    BackHandler(enabled = page == Page.Profile || page == Page.Preview) { page = Page.Home }
    Scaffold(bottomBar = {
        if (page != Page.Profile && page != Page.Editor && page != Page.Preview) NavigationBar {
            NavigationBarItem(selected = page == Page.Home, onClick = { page = Page.Home }, icon = { Text("⌂") }, label = { Text("首页") })
            NavigationBarItem(selected = page == Page.Vocabulary, onClick = { page = Page.Vocabulary }, icon = { Text("Aa") }, label = { Text("单词库") })
        }
    }) { padding ->
        when (page) {
            Page.Home -> HomeScreen(Modifier.padding(padding), { page = Page.Profile }, { selectedId = null; page = Page.Editor }, articles, recent, { selectedId = it; page = Page.Preview }, vm::delete)
            Page.Vocabulary -> VocabularyScreen(Modifier.padding(padding))
            Page.Profile -> ProfileScreen(Modifier.padding(padding), { page = Page.Home })
            Page.Editor -> ArticleEditorScreen(selectedId, vm, { page = if (selectedId == null) Page.Home else Page.Preview }, { selectedId = it; page = Page.Preview })
            Page.Preview -> selectedId?.let { id -> ArticlePreviewScreen(id, vm, { page = Page.Home }, { page = Page.Editor }) } ?: run { page = Page.Home }
        }
    }
}
