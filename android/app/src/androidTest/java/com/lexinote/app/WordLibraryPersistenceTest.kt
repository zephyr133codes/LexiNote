package com.lexinote.app

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.lexinote.app.data.ArticleEntity
import com.lexinote.app.data.LexiNoteDatabase
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordLibraryPersistenceTest {
    @Test fun repeatedCaseInsensitiveQueriesShareRecordAndStarIsIndependent() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, LexiNoteDatabase::class.java).build()
        try {
            val dao = db.wordRecordDao()
            dao.toggleStar("Book")
            assertEquals(1, dao.observeStarred().first().size)
            assertTrue(dao.observeHistory().first().isEmpty())

            dao.recordQuery("BOOK")
            dao.recordQuery("book")
            coroutineScope {
                repeat(20) { index -> launch(Dispatchers.IO) { dao.recordQuery(if (index % 2 == 0) "Book" else "BOOK") } }
            }
            val row = dao.get("book")
            assertNotNull(row)
            assertEquals(22L, row!!.viewCount)
            assertTrue(row.isStarred)
            assertEquals(1, dao.observeHistory().first().size)
            assertEquals(listOf("book"), dao.observeNewWords().first().map { it.normalizedWord })

            dao.setMastered("book", true, 200L)
            assertEquals(1, dao.observeStarred().first().size)
            assertTrue(dao.observeNewWords().first().isEmpty())
            assertEquals(1, dao.observeHistory().first().size)
            dao.setMastered("book", false, null)
            assertEquals(1, dao.observeNewWords().first().size)

            dao.toggleStar("bOoK")
            assertTrue(dao.observeStarred().first().isEmpty())
            assertEquals(22L, dao.get("book")!!.viewCount)
            assertTrue(dao.observeNewWords().first().isEmpty())
            assertEquals(1, dao.observeHistory().first().size)

            dao.recordQuery("unstarred")
            dao.setMastered("unstarred", true, 300L)
            dao.setMastered("unstarred", false, null)
            assertTrue(dao.observeStarred().first().isEmpty())
            assertEquals(setOf("book", "unstarred"), dao.observeHistory().first().map { it.normalizedWord }.toSet())
        } finally { db.close() }
    }

    @Test fun masteredStateSurvivesDatabaseReopenAndCanBeUndone() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "word-library-persistence-test.db"
        context.deleteDatabase(name)
        try {
            val first = Room.databaseBuilder(context, LexiNoteDatabase::class.java, name).build()
            try {
                first.wordRecordDao().recordQuery("practice")
                assertEquals(1, first.wordRecordDao().setMastered("practice", true, 123L))
            } finally { first.close() }
            val reopened = Room.databaseBuilder(context, LexiNoteDatabase::class.java, name).build()
            try {
                assertTrue(reopened.wordRecordDao().get("practice")!!.isMastered)
                assertEquals(123L, reopened.wordRecordDao().get("practice")!!.masteredAt)
                reopened.wordRecordDao().setMastered("practice", false, null)
                assertFalse(reopened.wordRecordDao().get("practice")!!.isMastered)
                assertEquals(1L, reopened.wordRecordDao().get("practice")!!.viewCount)
            } finally { reopened.close() }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun versionTwoMigrationPreservesArticlesAndAnnotations() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "word-library-migration-test.db"
        context.deleteDatabase(name)
        val factory = FrameworkSQLiteOpenHelperFactory()
        val v2 = factory.create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE articles (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, content TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, lastReadAt INTEGER)")
                        db.execSQL("CREATE TABLE annotations (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, articleId INTEGER NOT NULL, startOffset INTEGER NOT NULL, endOffset INTEGER NOT NULL, word TEXT NOT NULL, phonetic TEXT NOT NULL, meaning TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, FOREIGN KEY(articleId) REFERENCES articles(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                        db.execSQL("CREATE UNIQUE INDEX index_annotations_articleId_startOffset_endOffset ON annotations(articleId, startOffset, endOffset)")
                        db.execSQL("INSERT INTO articles VALUES (41, 'Saved', 'book', 10, 20, 30)")
                        db.execSQL("INSERT INTO annotations VALUES (51, 41, 0, 4, 'book', '/bʊk/', NULL, 11, 12)")
                    }
                    override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build()
        )
        try {
            v2.close()
            val migrated = Room.databaseBuilder(context, LexiNoteDatabase::class.java, name)
                .addMigrations(LexiNoteDatabase.MIGRATION_2_3).build()
            try {
                runBlocking {
                    val article = migrated.articleDao().getById(41)
                    assertNotNull(article)
                    assertEquals(10L, article!!.createdAt)
                    assertEquals(20L, article.updatedAt)
                    assertEquals(30L, article.lastReadAt)
                    assertEquals("/bʊk/", migrated.articleDao().getAnnotations(41).single().phonetic)
                    assertNull(migrated.wordRecordDao().get("book"))
                }
            } finally { migrated.close() }
        } finally { v2.close(); context.deleteDatabase(name) }
    }
}
