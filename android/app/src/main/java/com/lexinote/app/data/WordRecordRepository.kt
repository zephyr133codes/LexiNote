package com.lexinote.app.data

class WordRecordRepository(private val dao: WordRecordDao) {
    fun starred() = dao.observeStarred()
    fun newWords() = dao.observeNewWords()
    fun history() = dao.observeHistory()
    suspend fun get(word: String) = dao.get(word.trim().lowercase(java.util.Locale.ROOT))
    suspend fun recordQuery(word: String) = dao.recordQuery(word)
    suspend fun setStarred(word: String, value: Boolean) = dao.setStarred(word, value)
    suspend fun toggleStar(word: String) = dao.toggleStar(word)
    suspend fun setMastered(word: String, value: Boolean): Boolean = dao.setMastered(word.trim().lowercase(java.util.Locale.ROOT), value, if (value) System.currentTimeMillis() else null) > 0
}
