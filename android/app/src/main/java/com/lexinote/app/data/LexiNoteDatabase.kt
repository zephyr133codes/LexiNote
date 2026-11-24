package com.lexinote.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ArticleEntity::class], version = 1, exportSchema = false)
abstract class LexiNoteDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
    companion object {
        @Volatile private var instance: LexiNoteDatabase? = null
        fun get(context: Context): LexiNoteDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, LexiNoteDatabase::class.java, "lexinote.db").build().also { instance = it }
        }
    }
}
