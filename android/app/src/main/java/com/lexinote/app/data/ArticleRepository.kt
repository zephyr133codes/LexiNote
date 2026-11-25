package com.lexinote.app.data

import kotlinx.coroutines.flow.Flow
import android.util.Log

class ArticleRepository(private val dao: ArticleDao) {
    fun observeAnnotations(articleId: Long) = dao.observeAnnotations(articleId)
    suspend fun savePhonetic(articleId: Long, start: Int, end: Int, word: String, phonetic: String) {
        Log.d("LexiNoteAnnotation", "save request articleId=$articleId range=[$start,$end) word=$word phonetic=$phonetic")
        dao.savePhonetic(articleId, start, end, word, phonetic)
        Log.d("LexiNoteAnnotation", "save complete articleId=$articleId")
    }
    fun observeAll(): Flow<List<ArticleEntity>> = dao.observeAll()
    fun observeRecentlyRead(): Flow<List<ArticleEntity>> = dao.observeRecentlyRead()
    suspend fun get(id: Long) = dao.getById(id)
    suspend fun save(id: Long?, title: String, content: String, invalidateAnnotations: Boolean = false): Long {
        val now = System.currentTimeMillis()
        return if (id == null) dao.insert(ArticleEntity(title = title, content = content, createdAt = now, updatedAt = now, lastReadAt = now))
        else {
            val old = checkNotNull(dao.getById(id)) { "文章已不存在" }
            dao.updateArticleAndAnnotations(old.copy(title = title, content = content, updatedAt = now, lastReadAt = now), invalidateAnnotations)
            id
        }
    }
    suspend fun markRead(id: Long) = dao.markRead(id, System.currentTimeMillis())
    suspend fun delete(id: Long) = dao.delete(id)
}
