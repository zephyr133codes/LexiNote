package com.lexinote.app.ui.editor

import com.lexinote.app.data.AnnotationEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class AnnotationUiState(
    val articleId: Long? = null,
    val rows: List<AnnotationEntity> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

internal class AnnotationSubscription(
    private val scope: CoroutineScope,
    private val observe: (Long) -> Flow<List<AnnotationEntity>>
) {
    private val mutableState = MutableStateFlow(AnnotationUiState())
    val state: StateFlow<AnnotationUiState> = mutableState.asStateFlow()
    private var job: Job? = null
    private var generation = 0L

    fun select(id: Long) {
        if (state.value.articleId == id && job?.isActive == true) return
        job?.cancel()
        val ticket = ++generation
        mutableState.value = AnnotationUiState(articleId = id, loading = true)
        job = scope.launch {
            try {
                observe(id).collect { rows ->
                    if (ticket == generation) mutableState.value = AnnotationUiState(id, rows.filter { it.articleId == id })
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (ticket == generation) mutableState.value = AnnotationUiState(id, error = e.message ?: "标注读取失败")
            }
        }
    }

    fun stop(id: Long) {
        if (state.value.articleId != id) return
        ++generation
        job?.cancel()
        job = null
        mutableState.value = AnnotationUiState()
    }
}
