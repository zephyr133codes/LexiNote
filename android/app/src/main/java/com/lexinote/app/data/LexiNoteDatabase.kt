package com.lexinote.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ArticleEntity::class, AnnotationEntity::class], version = 2, exportSchema = false)
abstract class LexiNoteDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
    companion object {
        @Volatile private var instance: LexiNoteDatabase? = null
        fun get(context: Context): LexiNoteDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, LexiNoteDatabase::class.java, "lexinote.db").addMigrations(MIGRATION_1_2).build().also { instance = it }
        }
        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS annotations (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, articleId INTEGER NOT NULL, startOffset INTEGER NOT NULL, endOffset INTEGER NOT NULL, word TEXT NOT NULL, phonetic TEXT NOT NULL, meaning TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, FOREIGN KEY(articleId) REFERENCES articles(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_annotations_articleId_startOffset_endOffset ON annotations(articleId, startOffset, endOffset)")
            }
        }
    }
}
