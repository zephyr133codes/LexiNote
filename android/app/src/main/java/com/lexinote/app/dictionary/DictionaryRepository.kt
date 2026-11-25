package com.lexinote.app.dictionary

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class DictionarySense(val pos: String, val meaning: String, val order: Int)
data class DictionaryEntry(val word: String, val phonetic: String?, val senses: List<DictionarySense>)

class DictionaryRepository private constructor(context: Context) {
    private val db: SQLiteDatabase
    init {
        val target = File(context.filesDir, "dictionary.db")
        if (!target.exists() || target.length() == 0L) {
            context.assets.open("dictionary.db").use { input -> target.outputStream().use { input.copyTo(it) } }
        }
        db = SQLiteDatabase.openDatabase(target.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
    }

    suspend fun lookup(word: String): DictionaryEntry? = withContext(Dispatchers.IO) {
        val key = word.lowercase()
        val entry = db.rawQuery("SELECT id, word, phonetic FROM dictionary_entries WHERE word = ? COLLATE NOCASE LIMIT 1", arrayOf(key)).use { c ->
            if (!c.moveToFirst()) return@withContext null
            Triple(c.getLong(0), c.getString(1), if (c.isNull(2)) null else c.getString(2))
        }
        val senses = mutableListOf<DictionarySense>()
        db.rawQuery("SELECT pos, meaning, sort_order FROM dictionary_senses WHERE entry_id = ? ORDER BY sort_order", arrayOf(entry.first.toString())).use { c ->
            while (c.moveToNext()) senses += DictionarySense(c.getString(0), c.getString(1), c.getInt(2))
        }
        DictionaryEntry(entry.second, entry.third, senses)
    }

    companion object {
        @Volatile private var instance: DictionaryRepository? = null
        fun get(context: Context): DictionaryRepository = instance ?: synchronized(this) {
            instance ?: DictionaryRepository(context.applicationContext).also { instance = it }
        }
    }
}
