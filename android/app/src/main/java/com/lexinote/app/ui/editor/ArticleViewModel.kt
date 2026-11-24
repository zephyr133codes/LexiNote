package com.lexinote.app.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lexinote.app.data.ArticleEntity
import com.lexinote.app.data.ArticleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ArticleViewModel(private val repository: ArticleRepository) : ViewModel() {
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

    fun load(id: Long) { viewModelScope.launch { _loading.value = true; try { _article.value = repository.get(id); if (_article.value == null) _error.value = "找不到这篇文章" } catch (e: Exception) { _error.value = e.message ?: "读取失败" } finally { _loading.value = false } } }
    fun openPreview(id: Long) { viewModelScope.launch { _loading.value = true; _error.value = null; try { repository.markRead(id); _article.value = repository.get(id); if (_article.value == null) _error.value = "找不到这篇文章" } catch (e: Exception) { _error.value = e.message ?: "读取失败" } finally { _loading.value = false } } }
    fun save(id: Long?, title: String, content: String, done: (Long?) -> Unit) { viewModelScope.launch { _loading.value = true; _error.value = null; try { val saved = repository.save(id, title, content); _savedId.value = saved; done(saved) } catch (e: Exception) { _error.value = e.message ?: "保存失败，请重试" } finally { _loading.value = false } } }
    fun delete(id: Long) { viewModelScope.launch { try { repository.delete(id) } catch (e: Exception) { _error.value = e.message ?: "删除失败" } } }
    fun clearError() { _error.value = null }
}
