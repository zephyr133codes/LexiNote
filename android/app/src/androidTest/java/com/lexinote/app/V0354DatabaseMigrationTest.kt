package com.lexinote.app

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lexinote.app.data.ArticleEntity
import com.lexinote.app.data.LexiNoteDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V0354DatabaseMigrationTest {
    @Test
    fun v3MigrationPreservesArticlesAnnotationsAndWordsAndAllocatesUniqueNames() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbName = "migration-v3-4-test.db"
        context.deleteDatabase(dbName)
        val path = context.getDatabasePath(dbName)
        path.parentFile?.mkdirs()
        val old = SQLiteDatabase.openOrCreateDatabase(path.absolutePath, null)
        old.execSQL("CREATE TABLE articles (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, content TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, lastReadAt INTEGER)")
        old.execSQL("CREATE TABLE annotations (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, articleId INTEGER NOT NULL, startOffset INTEGER NOT NULL, endOffset INTEGER NOT NULL, word TEXT NOT NULL, phonetic TEXT NOT NULL, meaning TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, FOREIGN KEY(articleId) REFERENCES articles(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        old.execSQL("CREATE UNIQUE INDEX index_annotations_articleId_startOffset_endOffset ON annotations(articleId, startOffset, endOffset)")
        old.execSQL("CREATE TABLE word_records (normalizedWord TEXT NOT NULL PRIMARY KEY, displayWord TEXT NOT NULL, lastViewedAt INTEGER NOT NULL, viewCount INTEGER NOT NULL, isStarred INTEGER NOT NULL, starredAt INTEGER, isMastered INTEGER NOT NULL, masteredAt INTEGER)")
        old.execSQL("CREATE INDEX index_word_records_isStarred_starredAt ON word_records(isStarred, starredAt)")
        old.execSQL("CREATE INDEX index_word_records_lastViewedAt ON word_records(lastViewedAt)")
        old.execSQL("INSERT INTO articles(id,title,content,createdAt,updatedAt,lastReadAt) VALUES(1,'未命名1','book',100,200,150)")
        old.execSQL("INSERT INTO annotations(id,articleId,startOffset,endOffset,word,phonetic,meaning,createdAt,updatedAt) VALUES(1,1,0,4,'book','bʊk','书',110,120)")
        old.execSQL("INSERT INTO word_records(normalizedWord,displayWord,lastViewedAt,viewCount,isStarred,starredAt,isMastered,masteredAt) VALUES('book','book',130,2,1,130,0,NULL)")
        old.version = 3
        old.close()

        val room = Room.databaseBuilder(context, LexiNoteDatabase::class.java, dbName)
            .addMigrations(LexiNoteDatabase.MIGRATION_3_4)
            .allowMainThreadQueries()
            .build()
        try {
            val article = room.articleDao().getById(1)!!
            assertEquals("未命名1", article.title)
            assertEquals(100L, article.createdAt)
            assertEquals(150L, article.lastReadAt)
            val annotation = room.articleDao().getAnnotations(1).single()
            assertEquals("书", annotation.meaning)
            assertEquals("word", annotation.phoneticSourceType)
            assertEquals(null, annotation.phoneticSourceLemma)
            assertNotNull(room.wordRecordDao().get("book"))

            val generated2 = room.articleDao().insertNewArticle(ArticleEntity(content = "first", createdAt = 300, updatedAt = 300))
            assertEquals("未命名2", room.articleDao().getById(generated2)?.title)
            room.articleDao().delete(generated2)
            val generated3 = room.articleDao().insertNewArticle(ArticleEntity(content = "second", createdAt = 400, updatedAt = 400))
            assertEquals("未命名3", room.articleDao().getById(generated3)?.title)
            room.articleDao().insert(ArticleEntity(title = "未命名4", content = "manual", createdAt = 500, updatedAt = 500))
            val generated5 = room.articleDao().insertNewArticle(ArticleEntity(content = "third", createdAt = 600, updatedAt = 600))
            assertEquals("未命名5", room.articleDao().getById(generated5)?.title)
        } finally {
            room.close()
            context.deleteDatabase(dbName)
        }
    }
}
