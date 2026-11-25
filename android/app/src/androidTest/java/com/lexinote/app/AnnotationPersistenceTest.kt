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

    @Test fun phoneticCanBeToggledPerPositionWithoutDeletingMeaningOrLemmaProvenance() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, LexiNoteDatabase::class.java).build()
        try {
            val dao = db.articleDao()
            val id = dao.insert(ArticleEntity(content = "book book", createdAt = 1, updatedAt = 1))
            dao.savePhonetic(id, 0, 4, "book", "bʊk", "lemma", "book-root")
            val first = dao.findAnnotation(id, 0, 4)!!
            dao.updateAnnotation(first.copy(meaning = "书"))
            dao.savePhonetic(id, 5, 9, "book", "bʊk")

            val clearedFlow = async(start = CoroutineStart.UNDISPATCHED) {
                dao.observeAnnotations(id).first { rows -> rows.any { it.startOffset == 0 && it.phonetic.isBlank() } }
            }
            assertTrue(dao.clearPhonetic(id, 0, 4, "book"))
            assertEquals("", clearedFlow.await().first { it.startOffset == 0 }.phonetic)
            val retainedMeaning = dao.findAnnotation(id, 0, 4)!!
            assertEquals("", retainedMeaning.phonetic)
            assertEquals("书", retainedMeaning.meaning)
            assertEquals("word", retainedMeaning.phoneticSourceType)
            assertNull(retainedMeaning.phoneticSourceLemma)
            assertEquals("bʊk", dao.findAnnotation(id, 5, 9)?.phonetic)
            assertFalse(dao.clearPhonetic(id, 0, 4, "book"))

            assertTrue(dao.clearPhonetic(id, 5, 9, "book"))
            assertNull(dao.findAnnotation(id, 5, 9))
            dao.savePhonetic(id, 5, 9, "book", "bʊk")
            assertEquals(2, dao.getAnnotations(id).size)
            assertEquals("bʊk", dao.findAnnotation(id, 5, 9)?.phonetic)

            dao.savePhonetic(id, 0, 4, "book", "bʊk")
            assertTrue(dao.clearMeaning(id, 0, 4, "book"))
            assertNull(dao.findAnnotation(id, 0, 4)?.meaning)
            assertEquals("bʊk", dao.findAnnotation(id, 0, 4)?.phonetic)
            assertTrue(dao.clearAnnotation(id, 0, 4, "book"))
            assertNull(dao.findAnnotation(id, 0, 4))
            assertEquals(1, dao.getAnnotations(id).size)
        } finally { db.close() }
    }
}
