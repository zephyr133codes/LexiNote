package com.lexinote.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    @Transaction
    suspend fun savePhonetic(articleId: Long, start: Int, end: Int, word: String, phonetic: String) {
        val article = checkNotNull(getById(articleId)) { "文章不存在" }
        require(phonetic.isNotBlank()) { "音标为空" }
        require(start >= 0 && end > start && end <= article.content.length && article.content.substring(start, end) == word) { "文章内容已改变，请重新选择单词" }
        val old = findAnnotation(articleId, start, end)
        val now = System.currentTimeMillis()
        if (old == null) insertAnnotation(AnnotationEntity(articleId = articleId, startOffset = start, endOffset = end, word = word, phonetic = phonetic, createdAt = now))
        else updateAnnotation(old.copy(phonetic = phonetic, updatedAt = now))
    }
    @Insert suspend fun insertAnnotation(annotation: AnnotationEntity): Long
    @Update suspend fun updateAnnotation(annotation: AnnotationEntity)
    @Query("SELECT * FROM annotations WHERE articleId = :articleId ORDER BY startOffset") fun observeAnnotations(articleId: Long): Flow<List<AnnotationEntity>>
    @Query("SELECT * FROM annotations WHERE articleId = :articleId ORDER BY startOffset") suspend fun getAnnotations(articleId: Long): List<AnnotationEntity>
    @Query("SELECT * FROM annotations WHERE articleId = :articleId AND startOffset = :start AND endOffset = :end LIMIT 1") suspend fun findAnnotation(articleId: Long, start: Int, end: Int): AnnotationEntity?
    @Query("DELETE FROM annotations WHERE articleId = :articleId") suspend fun deleteAnnotations(articleId: Long)
    @Transaction
    suspend fun updateArticleAndAnnotations(article: ArticleEntity, invalidateAnnotations: Boolean) {
        if (invalidateAnnotations) deleteAnnotations(article.id)
        update(article)
    }
    @Insert suspend fun insert(article: ArticleEntity): Long
    @Update suspend fun update(article: ArticleEntity)
    @Query("SELECT * FROM articles WHERE id = :id") suspend fun getById(id: Long): ArticleEntity?
    @Query("SELECT * FROM articles ORDER BY updatedAt DESC") fun observeAll(): Flow<List<ArticleEntity>>
    @Query("SELECT * FROM articles WHERE lastReadAt IS NOT NULL ORDER BY lastReadAt DESC LIMIT 1") fun observeRecentlyRead(): Flow<List<ArticleEntity>>
    @Query("UPDATE articles SET lastReadAt = :readAt WHERE id = :id") suspend fun markRead(id: Long, readAt: Long)
    @Query("DELETE FROM articles WHERE id = :id") suspend fun delete(id: Long)
}
