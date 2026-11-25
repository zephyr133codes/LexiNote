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
import com.lexinote.app.ui.home.ProjectListScreen
import com.lexinote.app.ui.profile.ProfileScreen
import com.lexinote.app.ui.vocabulary.VocabularyScreen
import com.lexinote.app.ui.vocabulary.WordLibraryViewModel
import com.lexinote.app.ui.vocabulary.WordListScreen
import com.lexinote.app.ui.vocabulary.ReviewScreen
import com.lexinote.app.ui.vocabulary.WordListSource

private enum class Page { Home, Projects, Vocabulary, WordList, Review, Profile, Editor, Preview }

@Composable
fun LexiNoteApp() {
    val context = LocalContext.current
    val wordVm: WordLibraryViewModel = viewModel(key = "word-library", factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = WordLibraryViewModel(WordRecordRepository(LexiNoteDatabase.get(context).wordRecordDao()), DictionaryRepository.get(context)) as T
    })
    val vm: ArticleViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = ArticleViewModel(ArticleRepository(LexiNoteDatabase.get(context).articleDao()), DictionaryRepository.get(context), wordVm) as T
    })
    val articles by vm.articles.collectAsStateWithLifecycle(initialValue = emptyList())
    val recent by vm.recentlyRead.collectAsStateWithLifecycle(initialValue = emptyList())
    val favorites by wordVm.starred.collectAsStateWithLifecycle()
    var page by remember { mutableStateOf(Page.Home) }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var previewReturnPage by remember { mutableStateOf(Page.Home) }
    var wordSource by remember { mutableStateOf(WordListSource.Favorites) }
    BackHandler(enabled = page == Page.Profile || page == Page.Preview || page == Page.WordList || page == Page.Vocabulary || page == Page.Projects) {
        page = when (page) {
            Page.WordList -> Page.Vocabulary
            Page.Vocabulary, Page.Profile, Page.Projects -> Page.Home
            Page.Preview -> previewReturnPage
            else -> Page.Home
        }
    }
    BackHandler(enabled = page == Page.Review) { page = Page.WordList }
    Scaffold(bottomBar = {
        if (page != Page.Profile && page != Page.Editor && page != Page.Preview && page != Page.WordList && page != Page.Review && page != Page.Projects) NavigationBar {
            NavigationBarItem(selected = page == Page.Home, onClick = { page = Page.Home }, icon = { Text("⌂") }, label = { Text("首页") })
            NavigationBarItem(selected = page == Page.Vocabulary, onClick = { page = Page.Vocabulary }, icon = { Text("Aa") }, label = { Text("单词库") })
        }
    }) { padding ->
        when (page) {
            Page.Home -> HomeScreen(
                modifier = Modifier.padding(padding),
                onProfileClick = { page = Page.Profile },
                onCreateArticle = { selectedId = null; page = Page.Editor },
                articles = articles,
                recentlyRead = recent,
                favoriteCount = favorites.size,
                onOpenArticle = { selectedId = it; previewReturnPage = page; page = Page.Preview },
                onOpenProjects = { page = Page.Projects },
                onOpenFavorites = { wordSource = WordListSource.Favorites; page = Page.WordList },
                onDeleteArticle = vm::delete
            )
            Page.Projects -> ProjectListScreen(
                articles = articles,
                onBack = { page = Page.Home },
                onOpenArticle = { selectedId = it; previewReturnPage = Page.Projects; page = Page.Preview },
                onDeleteArticle = vm::delete
            )
            Page.Vocabulary -> VocabularyScreen(Modifier.padding(padding), wordVm) { wordSource = it; page = Page.WordList }
            Page.WordList -> WordListScreen(wordSource, wordVm, { page = Page.Vocabulary }, { page = Page.Review })
            Page.Review -> ReviewScreen(wordSource, wordVm) { page = Page.WordList }
            Page.Profile -> ProfileScreen(Modifier.padding(padding), { page = Page.Home })
            Page.Editor -> ArticleEditorScreen(selectedId, vm, { page = if (selectedId == null) Page.Home else Page.Preview }, { selectedId = it; page = Page.Preview })
            Page.Preview -> selectedId?.let { id -> ArticlePreviewScreen(id, vm, wordVm, { page = previewReturnPage }, { page = Page.Editor }) } ?: run { page = Page.Home }
        }
    }
}
