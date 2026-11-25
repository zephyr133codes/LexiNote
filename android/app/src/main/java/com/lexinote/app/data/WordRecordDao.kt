package com.lexinote.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WordRecordDao {
    @Query("SELECT * FROM word_records WHERE isStarred = 1 ORDER BY starredAt DESC") fun observeStarred(): Flow<List<WordRecord>>
    @Query("SELECT * FROM word_records WHERE isStarred = 1 AND isMastered = 0 ORDER BY starredAt DESC") fun observeNewWords(): Flow<List<WordRecord>>
    @Query("SELECT * FROM word_records WHERE viewCount > 0 ORDER BY lastViewedAt DESC") fun observeHistory(): Flow<List<WordRecord>>
    @Query("SELECT * FROM word_records WHERE normalizedWord = :word LIMIT 1") suspend fun get(word: String): WordRecord?
    @Transaction suspend fun recordQuery(word: String) {
        val normalized = word.trim().lowercase(java.util.Locale.ROOT)
        if (normalized.isBlank()) return
        val old = get(normalized); val now = System.currentTimeMillis()
        if (old == null) insert(WordRecord(normalized, word.trim(), now, 1))
        else update(old.copy(displayWord = word.trim(), lastViewedAt = now, viewCount = old.viewCount + 1))
    }
    @Transaction suspend fun setStarred(word: String, value: Boolean) {
        val normalized = word.trim().lowercase(java.util.Locale.ROOT); val now = System.currentTimeMillis()
        val old = get(normalized)
        if (old == null) insert(WordRecord(normalized, word.trim(), now, 0, value, if (value) now else null))
        else update(old.copy(isStarred = value, starredAt = if (value) now else null))
    }
    @Transaction suspend fun toggleStar(word: String) {
        val normalized = word.trim().lowercase(java.util.Locale.ROOT)
        if (normalized.isBlank()) return
        val old = get(normalized)
        val now = System.currentTimeMillis()
        if (old == null) insert(WordRecord(normalized, word.trim(), now, 0, true, now))
        else {
            val starred = !old.isStarred
            update(old.copy(isStarred = starred, starredAt = if (starred) now else null))
        }
    }
    @Query("UPDATE word_records SET isMastered = :value, masteredAt = :time WHERE normalizedWord = :word") suspend fun setMastered(word: String, value: Boolean, time: Long?): Int
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(record: WordRecord)
    @Update suspend fun update(record: WordRecord)
}
