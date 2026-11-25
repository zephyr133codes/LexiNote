package com.lexinote.app.dictionary

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale

data class DictionarySense(val pos: String, val meaning: String, val order: Int)
data class DictionaryEntry(val word: String, val phonetic: String?, val senses: List<DictionarySense>)
data class DictionaryWordForm(val lemma: String, val form: String, val pos: String, val formType: String, val source: String)
data class DictionaryLemmaCard(val entry: DictionaryEntry?, val relationTypes: List<String>, val forms: List<DictionaryWordForm>)
data class DictionaryLookupResult(
    val currentEntry: DictionaryEntry?,
    val currentRelations: List<DictionaryWordForm>,
    val lemmaCards: List<DictionaryLemmaCard>
)

class DictionaryRepository private constructor(private val context: Context) {
    private val dbLock = Any()
    @Volatile private var db: SQLiteDatabase? = null
    private var failedBundleMarker: String? = null

    suspend fun lookup(word: String): DictionaryEntry? = withContext(Dispatchers.IO) {
        val key = normalizeDictionaryQuery(word)
        if (key.isEmpty()) null else queryEntry(databaseOnIo(), key)
    }

    suspend fun lookupDetailed(word: String): DictionaryLookupResult = withContext(Dispatchers.IO) {
        val key = normalizeDictionaryQuery(word)
        if (key.isEmpty()) return@withContext DictionaryLookupResult(null, emptyList(), emptyList())
        val database = databaseOnIo()
        val entry = queryEntry(database, key)
        if (!tableExists(database, "dictionary_word_forms")) {
            return@withContext DictionaryLookupResult(entry, emptyList(), emptyList())
        }
        val relations = queryForms(database, "lemma = ? COLLATE NOCASE OR form = ? COLLATE NOCASE", arrayOf(key, key))
        val parentLemmas = relations.asSequence()
            .filter { it.form.equals(key, ignoreCase = true) && !it.lemma.equals(key, ignoreCase = true) }
            .map { it.lemma }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .toList()
        val cards = parentLemmas.map { lemma ->
            val lemmaEntry = queryEntry(database, lemma)
            val forms = queryForms(database, "lemma = ? COLLATE NOCASE", arrayOf(lemma))
            val types = relations.asSequence()
                .filter { it.lemma.equals(lemma, ignoreCase = true) && it.form.equals(key, ignoreCase = true) }
                .map { it.formType }
                .distinct()
                .toList()
            DictionaryLemmaCard(lemmaEntry, types, forms)
        }
        DictionaryLookupResult(entry, relations, cards)
    }

    fun close() = synchronized(dbLock) { db?.close(); db = null }

    companion object {
        private const val TAG = "LexiNoteDictionary"
        private const val DATABASE_NAME = "dictionary.db"
        private const val VERSION_MARKER = "dictionary.version"
        private const val TEMP_NAME = "dictionary.db.installing"
        private const val BACKUP_NAME = "dictionary.db.previous"
        private const val VERSION_ASSET = "dictionary.version"

        @Volatile private var instance: DictionaryRepository? = null

        fun get(context: Context): DictionaryRepository = synchronized(this) {
            val appContext = context.applicationContext
            instance ?: DictionaryRepository(appContext).also { instance = it }
        }

        private data class BundledMetadata(val version: String, val sha256: String) {
            val markerValue: String get() = "$version\n$sha256"
        }

        private fun readBundledMetadata(context: Context): BundledMetadata {
            val text = runCatching { context.assets.open(VERSION_ASSET).bufferedReader().use { it.readText() } }.getOrNull()
            val values = text.orEmpty().lineSequence().mapNotNull { line ->
                val split = line.split("=", limit = 2)
                if (split.size == 2) split[0].trim() to split[1].trim() else null
            }.toMap()
            val version = values["version"] ?: "legacy"
            val declaredHash = values["sha256"].orEmpty()
            val hash = declaredHash.ifBlank {
                context.assets.open(DATABASE_NAME).use { input ->
                    val digest = MessageDigest.getInstance("SHA-256")
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        digest.update(buffer, 0, read)
                    }
                    digest.digest().toHex()
                }
            }
            return BundledMetadata(version, hash)
        }

        private fun ensureBundledDatabase(context: Context) {
            val target = File(context.filesDir, DATABASE_NAME)
            val temp = File(context.filesDir, TEMP_NAME)
            val backup = File(context.filesDir, BACKUP_NAME)
            val marker = File(context.filesDir, VERSION_MARKER)
            val metadata = readBundledMetadata(context)

            if (!target.exists() && backup.exists() && isValidDatabase(backup)) backup.renameTo(target)
            if (marker.isFile && marker.readText().trim() == metadata.markerValue && isValidDatabase(target)) return

            temp.delete()
            try {
                val digest = MessageDigest.getInstance("SHA-256")
                context.assets.open(DATABASE_NAME).use { input ->
                    FileOutputStream(temp).use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            digest.update(buffer, 0, read)
                            output.write(buffer, 0, read)
                        }
                        output.fd.sync()
                    }
                }
                val copiedHash = digest.digest().toHex()
                require(copiedHash.equals(metadata.sha256, ignoreCase = true)) { "预置词典校验值不匹配" }
                require(isValidDatabase(temp, requireWordForms = true)) { "预置词典完整性检查失败" }

                if (backup.exists()) backup.delete()
                if (target.exists() && !target.renameTo(backup)) throw IllegalStateException("无法安全备份旧词典")
                if (!temp.renameTo(target)) {
                    if (backup.exists()) backup.renameTo(target)
                    throw IllegalStateException("无法启用新版词典")
                }

                val markerTemp = File(context.filesDir, "$VERSION_MARKER.tmp")
                markerTemp.writeText(metadata.markerValue)
                if (!markerTemp.renameTo(marker)) {
                    marker.delete()
                    if (!markerTemp.renameTo(marker)) throw IllegalStateException("无法写入词典版本标记")
                }
                backup.delete()
                Log.i(TAG, "Bundled dictionary ${metadata.version} installed (${copiedHash.take(12)})")
            } catch (error: Exception) {
                temp.delete()
                Log.e(TAG, "Dictionary update failed; retaining previous database when valid", error)
                if (!target.exists() && backup.exists() && isValidDatabase(backup)) backup.renameTo(target)
                if (!isValidDatabase(target)) throw IllegalStateException("离线词典初始化失败", error)
            }
        }

        private fun isValidDatabase(file: File, requireWordForms: Boolean = false): Boolean {
            if (!file.isFile || file.length() <= 0L) return false
            return runCatching {
                SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { database ->
                    val integrity = database.rawQuery("PRAGMA integrity_check", null).use { cursor -> cursor.moveToFirst() && cursor.getString(0) == "ok" }
                    val required = listOf("dictionary_entries", "dictionary_senses")
                    integrity && required.all { tableExists(database, it) } &&
                        (!requireWordForms || tableExists(database, "dictionary_word_forms"))
                }
            }.getOrDefault(false)
        }

        private fun tableExists(db: SQLiteDatabase, table: String): Boolean =
            db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=? LIMIT 1", arrayOf(table)).use { it.moveToFirst() }

        private fun queryEntry(db: SQLiteDatabase, key: String): DictionaryEntry? {
            val entry = db.rawQuery("SELECT id, word, phonetic FROM dictionary_entries WHERE word = ? COLLATE NOCASE LIMIT 1", arrayOf(key)).use { cursor ->
                if (!cursor.moveToFirst()) return null
                Triple(cursor.getLong(0), cursor.getString(1), if (cursor.isNull(2)) null else cursor.getString(2))
            }
            val senses = mutableListOf<DictionarySense>()
            db.rawQuery("SELECT pos, meaning, sort_order FROM dictionary_senses WHERE entry_id = ? ORDER BY sort_order", arrayOf(entry.first.toString())).use { cursor ->
                while (cursor.moveToNext()) senses += DictionarySense(cursor.getString(0), cursor.getString(1), cursor.getInt(2))
            }
            return DictionaryEntry(entry.second, entry.third, senses)
        }

        private fun queryForms(db: SQLiteDatabase, where: String, args: Array<String>): List<DictionaryWordForm> {
            val result = mutableListOf<DictionaryWordForm>()
            db.rawQuery("SELECT lemma,form,pos,form_type,source FROM dictionary_word_forms WHERE $where ORDER BY source_priority, lemma COLLATE NOCASE, form_type", args).use { cursor ->
                while (cursor.moveToNext()) result += DictionaryWordForm(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4))
            }
            return result
        }

        private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    private fun databaseOnIo(): SQLiteDatabase = synchronized(dbLock) {
        val target = File(context.filesDir, DATABASE_NAME)
        val installedMarker = File(context.filesDir, VERSION_MARKER).takeIf { it.isFile }?.readText()?.trim()
        val bundled = readBundledMetadata(context)
        val updateFailedForSameAsset = failedBundleMarker == bundled.markerValue && db != null && target.isFile && target.length() > 0L
        if (!updateFailedForSameAsset && (db == null || installedMarker != bundled.markerValue || !target.isFile || target.length() == 0L)) {
            db?.close()
            db = null
            ensureBundledDatabase(context)
            val markerAfterInstall = File(context.filesDir, VERSION_MARKER).takeIf { it.isFile }?.readText()?.trim()
            failedBundleMarker = if (markerAfterInstall == bundled.markerValue) null else bundled.markerValue
            db = SQLiteDatabase.openDatabase(target.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        }
        db!!
    }
}

internal fun normalizeDictionaryQuery(word: String): String = word.trim().lowercase(Locale.ROOT)
