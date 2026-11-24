package com.lexinote.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    @Insert suspend fun insert(article: ArticleEntity): Long
    @Update suspend fun update(article: ArticleEntity)
    @Query("SELECT * FROM articles WHERE id = :id") suspend fun getById(id: Long): ArticleEntity?
    @Query("SELECT * FROM articles ORDER BY updatedAt DESC") fun observeAll(): Flow<List<ArticleEntity>>
    @Query("SELECT * FROM articles WHERE lastReadAt IS NOT NULL ORDER BY lastReadAt DESC LIMIT 1") fun observeRecentlyRead(): Flow<List<ArticleEntity>>
    @Query("UPDATE articles SET lastReadAt = :readAt WHERE id = :id") suspend fun markRead(id: Long, readAt: Long)
    @Query("DELETE FROM articles WHERE id = :id") suspend fun delete(id: Long)
}
