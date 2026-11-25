package com.lexinote.app.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexinote.app.data.ArticleEntity
import com.lexinote.app.data.ArticleRepository
import com.lexinote.app.data.AnnotationEntity
import com.lexinote.app.dictionary.DictionaryEntry
import com.lexinote.app.dictionary.DictionaryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.util.Log

class ArticleViewModel(private val repository: ArticleRepository, private val dictionary: DictionaryRepository) : ViewModel() {
    val readerCache = ReaderDocumentCache()
    val articles = repository.observeAll()
    val recentlyRead = repository.observeRecentlyRead()
    private val _article = MutableStateFlow<ArticleEntity?>(null)
    val article: StateFlow<ArticleEntity?> = _article
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    private val _savedId = MutableStateFlow<Long?>(null)
    val savedId: StateFlow<Long?> = _savedId
    private val _lookup = MutableStateFlow<LookupState?>(null)
    val lookup: StateFlow<LookupState?> = _lookup
    private val annotationSubscription = AnnotationSubscription(viewModelScope, repository::observeAnnotations)
    val annotations = annotationSubscription.state
    private val _annotationError = MutableStateFlow<String?>(null)
    val annotationError: StateFlow<String?> = _annotationError
    private val _missingPhonetic = MutableStateFlow(false)
    val missingPhonetic: StateFlow<Boolean> = _missingPhonetic
    fun observeAnnotations(articleId: Long) = annotationSubscription.select(articleId)
    fun stopObservingAnnotations(articleId: Long) = annotationSubscription.stop(articleId)
    fun saveCurrentPhonetic(articleId: Long) { val s = _lookup.value as? LookupState.Result ?: run { Log.w("LexiNoteAnnotation", "save ignored: lookup is not a result"); return }; val phonetic = s.entry?.phonetic?.takeIf { it.isNotBlank() }; if (phonetic == null) { _missingPhonetic.value = true; return }; viewModelScope.launch { try { repository.savePhonetic(articleId, s.start, s.end, s.word, phonetic); if (annotations.value.articleId == articleId && _lookup.value == s) clearLookup() } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { Log.e("LexiNoteAnnotation", "save failed articleId=$articleId range=[${s.start},${s.end})", e); _annotationError.value = e.message ?: "音标保存失败" } } }
    fun dismissMissingPhonetic() { _missingPhonetic.value = false }
    fun dismissAnnotationError() { _annotationError.value = null }

    fun lookupWord(word: String, start: Int, end: Int) { viewModelScope.launch {
        _lookup.value = LookupState.Loading(word, start, end)
        try { _lookup.value = LookupState.Result(word, start, end, dictionary.lookup(word)) }
        catch (e: Exception) { _lookup.value = LookupState.Failure(word, start, end, e.message ?: "词典读取失败") }
    } }
    fun clearLookup() { _lookup.value = null }

    fun load(id: Long) { viewModelScope.launch { _loading.value = true; try { _article.value = repository.get(id); if (_article.value == null) _error.value = "找不到这篇文章" } catch (e: Exception) { _error.value = e.message ?: "读取失败" } finally { _loading.value = false } } }
    fun openPreview(id: Long) { viewModelScope.launch { _loading.value = true; _error.value = null; try { repository.markRead(id); _article.value = repository.get(id); if (_article.value == null) _error.value = "找不到这篇文章" } catch (e: Exception) { _error.value = e.message ?: "读取失败" } finally { _loading.value = false } } }
    fun save(id: Long?, title: String, content: String, clearAnnotations: Boolean = false, done: (Long?) -> Unit) { viewModelScope.launch { _loading.value = true; _error.value = null; try { val saved = repository.save(id, title, content, clearAnnotations); _savedId.value = saved; done(saved) } catch (e: Exception) { _error.value = e.message ?: "保存失败，请重试" } finally { _loading.value = false } } }
    fun delete(id: Long) { viewModelScope.launch { try { repository.delete(id) } catch (e: Exception) { _error.value = e.message ?: "删除失败" } } }
    fun clearError() { _error.value = null }
}

sealed interface LookupState {
    val word: String
    val start: Int
    val end: Int
    data class Loading(override val word: String, override val start: Int, override val end: Int) : LookupState
    data class Result(override val word: String, override val start: Int, override val end: Int, val entry: DictionaryEntry?) : LookupState
    data class Failure(override val word: String, override val start: Int, override val end: Int, val message: String) : LookupState
}
