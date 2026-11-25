package com.lexinote.app

import com.lexinote.app.ui.editor.AnnotationSubscription
import com.lexinote.app.data.AnnotationEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Test

class AnnotationSubscriptionTest {
    @Test fun switchesCancelOldFlowAndRepeatedSelectionDoesNotSubscribeAgain() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val feeds = mapOf(1L to MutableStateFlow<List<AnnotationEntity>>(emptyList()), 2L to MutableStateFlow<List<AnnotationEntity>>(emptyList()))
        val starts = mutableListOf<Long>()
        var active = 0
        val subscription = AnnotationSubscription(scope) { id -> flow {
            starts.add(id); active++
            try { emitAll(feeds.getValue(id)) } finally { active-- }
        } }
        try {
            subscription.select(1); subscription.select(1)
            assertEquals(listOf(1L), starts)
            feeds.getValue(1).value = listOf(AnnotationEntity(articleId = 1, startOffset = 0, endOffset = 4, word = "book", phonetic = "buk", createdAt = 1))
            assertEquals(1, subscription.state.value.rows.size)
            subscription.select(2)
            assertEquals(1, active)
            feeds.getValue(1).value = emptyList()
            assertEquals(2L, subscription.state.value.articleId)
            subscription.stop(1) // Old screen disposal must not stop current article.
            assertEquals(1, active)
            subscription.stop(2)
            assertEquals(0, active)
            subscription.select(2)
            assertEquals(1, active)
        } finally { scope.cancel() }
    }
}
