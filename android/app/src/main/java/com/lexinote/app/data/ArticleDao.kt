package com.lexinote.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    @Transaction
    suspend fun savePhonetic(
        articleId: Long,
        start: Int,
        end: Int,
        word: String,
        phonetic: String,
        sourceType: String = "word",
        sourceLemma: String? = null
    ): Boolean {
        val article = checkNotNull(getById(articleId)) { "文章不存在" }
        require(phonetic.isNotBlank()) { "音标为空" }
        require(start >= 0 && end > start && end <= article.content.length && article.content.substring(start, end) == word) { "文章内容已改变，请重新选择单词" }
        require(sourceType == "word" || sourceType == "lemma") { "未知音标来源" }
        require((sourceType == "word" && sourceLemma == null) || (sourceType == "lemma" && !sourceLemma.isNullOrBlank())) { "音标来源信息不完整" }
        val old = findAnnotation(articleId, start, end)
        val now = System.currentTimeMillis()
        if (old != null && old.phonetic == phonetic && old.phoneticSourceType == sourceType && old.phoneticSourceLemma == sourceLemma) return false
        if (old == null) insertAnnotation(AnnotationEntity(articleId = articleId, startOffset = start, endOffset = end, word = word, phonetic = phonetic, createdAt = now, phoneticSourceType = sourceType, phoneticSourceLemma = sourceLemma))
        else updateAnnotation(old.copy(phonetic = phonetic, phoneticSourceType = sourceType, phoneticSourceLemma = sourceLemma, updatedAt = now))
        return true
    }
    @Insert suspend fun insertAnnotation(annotation: AnnotationEntity): Long
    @Update suspend fun updateAnnotation(annotation: AnnotationEntity)
    @Query("DELETE FROM annotations WHERE id = :id") suspend fun deleteAnnotation(id: Long)
    @Query("SELECT * FROM annotations WHERE articleId = :articleId ORDER BY startOffset") fun observeAnnotations(articleId: Long): Flow<List<AnnotationEntity>>
    @Query("SELECT * FROM annotations WHERE articleId = :articleId ORDER BY startOffset") suspend fun getAnnotations(articleId: Long): List<AnnotationEntity>
    @Query("SELECT * FROM annotations WHERE articleId = :articleId AND startOffset = :start AND endOffset = :end LIMIT 1") suspend fun findAnnotation(articleId: Long, start: Int, end: Int): AnnotationEntity?
    @Transaction
    suspend fun clearPhonetic(articleId: Long, start: Int, end: Int, word: String): Boolean {
        val article = checkNotNull(getById(articleId)) { "文章不存在" }
        require(start >= 0 && end > start && end <= article.content.length && article.content.substring(start, end) == word) { "文章内容已改变，请重新选择单词" }
        val annotation = findAnnotation(articleId, start, end) ?: return false
        require(annotation.word == word) { "标注位置与单词不匹配" }
        if (annotation.phonetic.isBlank()) return false
        if (annotation.meaning.isNullOrBlank()) {
            deleteAnnotation(annotation.id)
        } else {
            updateAnnotation(annotation.copy(
                phonetic = "",
                phoneticSourceType = "word",
                phoneticSourceLemma = null,
                updatedAt = System.currentTimeMillis()
            ))
        }
        return true
    }
    @Transaction
    suspend fun clearMeaning(articleId: Long, start: Int, end: Int, word: String): Boolean {
        val article = checkNotNull(getById(articleId)) { "文章不存在" }
        require(start >= 0 && end > start && end <= article.content.length && article.content.substring(start, end) == word) { "文章内容已改变，请重新选择单词" }
        val annotation = findAnnotation(articleId, start, end) ?: return false
        require(annotation.word == word) { "标注位置与单词不匹配" }
        if (annotation.meaning.isNullOrBlank()) return false
        if (annotation.phonetic.isBlank()) deleteAnnotation(annotation.id)
        else updateAnnotation(annotation.copy(meaning = null, updatedAt = System.currentTimeMillis()))
        return true
    }
    @Transaction
    suspend fun clearAnnotation(articleId: Long, start: Int, end: Int, word: String): Boolean {
        val article = checkNotNull(getById(articleId)) { "文章不存在" }
        require(start >= 0 && end > start && end <= article.content.length && article.content.substring(start, end) == word) { "文章内容已改变，请重新选择单词" }
        val annotation = findAnnotation(articleId, start, end) ?: return false
        require(annotation.word == word) { "标注位置与单词不匹配" }
        deleteAnnotation(annotation.id)
        return true
    }
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

    @Query("SELECT nextNumber FROM article_title_sequence WHERE id = 1") suspend fun getNextTitleNumber(): Int?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveTitleSequence(sequence: ArticleTitleSequence)
    @Query("SELECT EXISTS(SELECT 1 FROM articles WHERE title = :title)") suspend fun titleExists(title: String): Boolean

    @Transaction
    suspend fun insertNewArticle(article: ArticleEntity): Long {
        if (article.title.isNotBlank()) return insert(article)
        var number = (getNextTitleNumber() ?: 1).coerceAtLeast(1)
        var generated = "未命名$number"
        while (titleExists(generated)) {
            number += 1
            generated = "未命名$number"
        }
        saveTitleSequence(ArticleTitleSequence(nextNumber = number + 1))
        return insert(article.copy(title = generated))
    }
}
