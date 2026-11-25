package com.lexinote.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ArticleEntity::class, AnnotationEntity::class, WordRecord::class, ArticleTitleSequence::class], version = 4, exportSchema = false)
abstract class LexiNoteDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
    abstract fun wordRecordDao(): WordRecordDao
    companion object {
        @Volatile private var instance: LexiNoteDatabase? = null
        fun get(context: Context): LexiNoteDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, LexiNoteDatabase::class.java, "lexinote.db").addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { instance = it }
        }
        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS annotations (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, articleId INTEGER NOT NULL, startOffset INTEGER NOT NULL, endOffset INTEGER NOT NULL, word TEXT NOT NULL, phonetic TEXT NOT NULL, meaning TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, FOREIGN KEY(articleId) REFERENCES articles(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_annotations_articleId_startOffset_endOffset ON annotations(articleId, startOffset, endOffset)")
            }
        }
        internal val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS word_records (normalizedWord TEXT NOT NULL PRIMARY KEY, displayWord TEXT NOT NULL, lastViewedAt INTEGER NOT NULL, viewCount INTEGER NOT NULL, isStarred INTEGER NOT NULL, starredAt INTEGER, isMastered INTEGER NOT NULL, masteredAt INTEGER)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_word_records_isStarred_starredAt ON word_records(isStarred, starredAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_word_records_lastViewedAt ON word_records(lastViewedAt)")
            }
        }
        internal val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE annotations ADD COLUMN phoneticSourceType TEXT NOT NULL DEFAULT 'word'")
                db.execSQL("ALTER TABLE annotations ADD COLUMN phoneticSourceLemma TEXT")
                db.execSQL("CREATE TABLE IF NOT EXISTS article_title_sequence (id INTEGER NOT NULL PRIMARY KEY, nextNumber INTEGER NOT NULL)")
                db.execSQL("INSERT OR IGNORE INTO article_title_sequence (id, nextNumber) VALUES (1, 1)")
            }
        }
    }
}
