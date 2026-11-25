package com.lexinote.app.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexinote.app.data.ArticleEntity
import com.lexinote.app.data.ArticleRepository
import com.lexinote.app.data.AnnotationEntity
import com.lexinote.app.dictionary.DictionaryLookupResult
import com.lexinote.app.dictionary.DictionaryRepository
import com.lexinote.app.ui.vocabulary.WordLibraryViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import android.util.Log

class ArticleViewModel(private val repository: ArticleRepository, private val dictionary: DictionaryRepository, private val wordLibrary: WordLibraryViewModel) : ViewModel() {
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
    private val _phoneticFallback = MutableStateFlow<PhoneticFallbackRequest?>(null)
    val phoneticFallback: StateFlow<PhoneticFallbackRequest?> = _phoneticFallback
    private val _savingPhonetic = MutableStateFlow(false)
    val savingPhonetic: StateFlow<Boolean> = _savingPhonetic
    private val _phoneticSaveEvents = MutableSharedFlow<PhoneticSaveEvent>(extraBufferCapacity = 4)
    val phoneticSaveEvents = _phoneticSaveEvents.asSharedFlow()
    private val quickActionMutex = Mutex()
    private var quickModeActive = false
    private var quickModeGeneration = 0L
    private var lookupGeneration = 0L
    fun observeAnnotations(articleId: Long) = annotationSubscription.select(articleId)
    fun stopObservingAnnotations(articleId: Long) = annotationSubscription.stop(articleId)
    fun setQuickModeActive(active: Boolean) {
        if (quickModeActive == active) return
        quickModeActive = active
        quickModeGeneration++
        clearLookup()
        _missingPhonetic.value = false
        _annotationError.value = null
    }

    fun toggleQuickPhonetic(articleId: Long, token: ReaderToken) {
        if (!quickModeActive) return
        val generation = quickModeGeneration
        viewModelScope.launch {
            quickActionMutex.withLock {
                if (!quickModeActive || generation != quickModeGeneration) return@withLock
                if (_phoneticFallback.value != null || _missingPhonetic.value || _annotationError.value != null) return@withLock
                _savingPhonetic.value = true
                try {
                    val existing = repository.getAnnotationAt(articleId, token.start, token.end)
                    if (!quickModeActive || generation != quickModeGeneration) return@withLock
                    if (existing?.phonetic?.isNotBlank() == true) {
                        val removed = repository.clearPhonetic(articleId, token.start, token.end, token.text)
                        Log.i("LexiNoteAnnotation", "quick clear articleId=$articleId range=[${token.start},${token.end}) changed=$removed")
                        if (!removed) {
                            _annotationError.value = "当前位置没有可取消的音标"
                        }
                        return@withLock
                    }

                    val details = dictionary.lookupDetailed(token.text)
                    if (!quickModeActive || generation != quickModeGeneration) return@withLock
                    val ownPhonetic = details.currentEntry?.phonetic?.takeIf { it.isNotBlank() }
                    if (ownPhonetic != null) {
                        savePhoneticNow(articleId, token.start, token.end, token.text, ownPhonetic, "word", null)
                        return@withLock
                    }

                    val choices = fallbackChoices(details)
                    if (choices.isEmpty()) {
                        _missingPhonetic.value = true
                    } else {
                        _lookup.value = LookupState.Result(token.text, token.start, token.end, details)
                        _phoneticFallback.value = PhoneticFallbackRequest(articleId, token.text, token.start, token.end, choices, quickMode = true)
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e("LexiNoteAnnotation", "quick phonetic action failed articleId=$articleId range=[${token.start},${token.end})", e)
                    _annotationError.value = e.message ?: "快速音标操作失败"
                } finally {
                    _savingPhonetic.value = false
                }
            }
        }
    }

    fun requestSaveCurrentPhonetic(articleId: Long) {
        val result = _lookup.value as? LookupState.Result ?: return
        val ownPhonetic = result.entry?.phonetic?.takeIf { it.isNotBlank() }
        if (ownPhonetic != null) {
            savePhonetic(articleId, result.start, result.end, result.word, ownPhonetic, "word", null)
            return
        }
        val choices = fallbackChoices(result.details)
        if (choices.isEmpty()) {
            _missingPhonetic.value = true
            clearLookup()
            return
        }
        _phoneticFallback.value = PhoneticFallbackRequest(articleId, result.word, result.start, result.end, choices)
    }

    private fun fallbackChoices(details: DictionaryLookupResult): List<PhoneticFallbackChoice> = details.lemmaCards.mapNotNull { card ->
        val entry = card.entry ?: return@mapNotNull null
        val phonetic = entry.phonetic?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        PhoneticFallbackChoice(entry.word, phonetic)
    }.distinctBy { it.lemma.lowercase(java.util.Locale.ROOT) to it.phonetic }

    fun confirmFallbackPhonetic(lemma: String) {
        val request = _phoneticFallback.value ?: return
        val choice = request.choices.firstOrNull { it.lemma.equals(lemma, ignoreCase = true) } ?: return
        savePhonetic(request.articleId, request.start, request.end, request.word, choice.phonetic, "lemma", choice.lemma)
    }

    fun clearCurrentPhonetic(articleId: Long) {
        val result = _lookup.value as? LookupState.Result ?: return
        if (_savingPhonetic.value) return
        _savingPhonetic.value = true
        viewModelScope.launch {
            try {
                quickActionMutex.withLock {
                    val changed = repository.clearPhonetic(articleId, result.start, result.end, result.word)
                    Log.i("LexiNoteAnnotation", "clear phonetic complete articleId=$articleId range=[${result.start},${result.end}) changed=$changed")
                    if (!changed) _annotationError.value = "当前位置没有可取消的音标"
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("LexiNoteAnnotation", "clear phonetic failed articleId=$articleId range=[${result.start},${result.end})", e)
                _annotationError.value = e.message ?: "取消音标失败"
            } finally {
                _savingPhonetic.value = false
            }
        }
    }

    fun dismissPhoneticFallback() {
        val request = _phoneticFallback.value
        _phoneticFallback.value = null
        if (request?.quickMode == true) clearLookup()
    }
    fun clearPhoneticPrompts() {
        _phoneticFallback.value = null
        _missingPhonetic.value = false
        _annotationError.value = null
    }

    private fun savePhonetic(articleId: Long, start: Int, end: Int, word: String, phonetic: String, sourceType: String, sourceLemma: String?) {
        if (_savingPhonetic.value) return
        _savingPhonetic.value = true
        viewModelScope.launch {
            try {
                quickActionMutex.withLock { savePhoneticNow(articleId, start, end, word, phonetic, sourceType, sourceLemma) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("LexiNoteAnnotation", "save failed articleId=$articleId range=[$start,$end)", e)
                val quickFallback = _phoneticFallback.value?.quickMode == true
                _phoneticFallback.value = null
                if (quickFallback) clearLookup()
                _annotationError.value = e.message ?: "音标保存失败"
            } finally {
                _savingPhonetic.value = false
            }
        }
    }
    private suspend fun savePhoneticNow(articleId: Long, start: Int, end: Int, word: String, phonetic: String, sourceType: String, sourceLemma: String?) {
        val changed = repository.savePhonetic(articleId, start, end, word, phonetic, sourceType, sourceLemma)
        Log.i("LexiNoteAnnotation", "save complete articleId=$articleId range=[$start,$end) source=$sourceType lemma=$sourceLemma changed=$changed")
        val current = _lookup.value as? LookupState.Result
        if (current != null && current.start == start && current.end == end && current.word == word) clearLookup()
        _phoneticSaveEvents.emit(PhoneticSaveEvent.Saved(articleId, changed))
    }
    fun dismissMissingPhonetic() { _missingPhonetic.value = false }
    fun dismissAnnotationError() { _annotationError.value = null }

    fun lookupWord(word: String, start: Int, end: Int) {
        wordLibrary.recordQuery(word)
        val requestGeneration = ++lookupGeneration
        viewModelScope.launch {
            if (requestGeneration != lookupGeneration) return@launch
            _lookup.value = LookupState.Loading(word, start, end)
            try {
                val details = dictionary.lookupDetailed(word)
                if (requestGeneration == lookupGeneration) {
                    _lookup.value = LookupState.Result(word, start, end, details)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                if (requestGeneration == lookupGeneration) {
                    _lookup.value = LookupState.Failure(word, start, end, e.message ?: "词典读取失败")
                }
            }
        }
    }
    fun clearLookup() {
        lookupGeneration++
        _lookup.value = null
        _phoneticFallback.value = null
    }

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
    data class Result(override val word: String, override val start: Int, override val end: Int, val details: DictionaryLookupResult) : LookupState {
        val entry get() = details.currentEntry
    }
    data class Failure(override val word: String, override val start: Int, override val end: Int, val message: String) : LookupState
}

data class PhoneticFallbackChoice(val lemma: String, val phonetic: String)
data class PhoneticFallbackRequest(val articleId: Long, val word: String, val start: Int, val end: Int, val choices: List<PhoneticFallbackChoice>, val quickMode: Boolean = false)
sealed interface PhoneticSaveEvent {
    data class Saved(val articleId: Long, val changed: Boolean) : PhoneticSaveEvent
}
