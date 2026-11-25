package com.lexinote.app

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.lexinote.app.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

class AnnotationPersistenceTest {
    @Test fun concurrentSavesPreserveUniquePositionsAndFlowPublishesCommittedRows() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, LexiNoteDatabase::class.java).build()
        try {
            val dao = db.articleDao()
            val id = dao.insert(ArticleEntity(content = "book book", createdAt = 1, updatedAt = 1))
            val update = async { withTimeout(5000) { dao.observeAnnotations(id).first { it.size == 2 } } }
            coroutineScope {
                repeat(10) { launch(Dispatchers.IO) { dao.savePhonetic(id, 0, 4, "book", "buk") } }
                launch(Dispatchers.IO) { dao.savePhonetic(id, 5, 9, "book", "buk") }
            }
            assertEquals(listOf(0, 5), update.await().map { it.startOffset })
            val before = dao.findAnnotation(id, 0, 4)!!
            dao.updateAnnotation(before.copy(meaning = "已有释义"))
            dao.savePhonetic(id, 0, 4, "book", "bʊk")
            val after = dao.findAnnotation(id, 0, 4)!!
            assertEquals(before.id, after.id)
            assertEquals(before.createdAt, after.createdAt)
            assertEquals("已有释义", after.meaning)
            assertEquals(2, dao.getAnnotations(id).size)
        } finally { db.close() }
    }
}
