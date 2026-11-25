package com.lexinote.app.ui.vocabulary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexinote.app.data.WordRecord
import com.lexinote.app.data.WordRecordRepository
import com.lexinote.app.dictionary.DictionaryEntry
import com.lexinote.app.dictionary.DictionaryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WordLibraryViewModel(private val records: WordRecordRepository, private val dictionary: DictionaryRepository) : ViewModel() {
    val starred = records.starred().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val newWords = records.newWords().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val history = records.history().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _selectedWord = MutableStateFlow<String?>(null)
    val selectedWord = _selectedWord.asStateFlow()
    private val _entry = MutableStateFlow<DictionaryEntry?>(null)
    val entry = _entry.asStateFlow()
    private val _entries = MutableStateFlow<Map<String, DictionaryEntry?>>(emptyMap())
    val entries = _entries.asStateFlow()
    private val _lookupErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val lookupErrors = _lookupErrors.asStateFlow()
    private val _loadingWords = MutableStateFlow<Set<String>>(emptySet())
    val loadingWords = _loadingWords.asStateFlow()
    private val cache = object : LinkedHashMap<String, DictionaryEntry?>(128, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, DictionaryEntry?>?): Boolean = size > 128
    }
    private val loading = mutableSetOf<String>()
    fun recordQuery(word: String) { viewModelScope.launch { records.recordQuery(word) } }
    fun isStarred(word: String): Boolean = starred.value.any { it.normalizedWord == normalize(word) }
    fun toggleStar(word: String) { viewModelScope.launch { records.toggleStar(word) } }
    fun setMastered(word: String, value: Boolean, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val saved = try { records.setMastered(word, value) }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) { false }
            onComplete(saved)
        }
    }
    fun openWord(word: String) { _selectedWord.value = word; _entry.value = null; loadEntry(word) }
    fun closeWord() { _selectedWord.value = null; _entry.value = null }
    fun loadEntry(word: String) {
        val key = normalize(word)
        if (cache.containsKey(key)) {
            val cached = cache[key]
            _entries.value = cache.entries.associate { it.key to it.value }
            if (_selectedWord.value?.let(::normalize) == key) _entry.value = cached
            return
        }
        if (!loading.add(key)) return
        _loadingWords.value = loading.toSet()
        _lookupErrors.value = _lookupErrors.value - key
        viewModelScope.launch {
            try {
                val found = dictionary.lookup(word)
                cache[key] = found
                _entries.value = cache.entries.associate { it.key to it.value }
                _lookupErrors.value = _lookupErrors.value - key
                if (_selectedWord.value?.let(::normalize) == key) _entry.value = found
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                _entries.value = _entries.value - key
                _lookupErrors.value = _lookupErrors.value + (key to "本地词典读取失败，请稍后重试")
                if (_selectedWord.value?.let(::normalize) == key) _entry.value = null
            } finally {
                loading.remove(key)
                _loadingWords.value = loading.toSet()
            }
        }
    }
    private fun normalize(word: String) = word.trim().lowercase(java.util.Locale.ROOT)
}
