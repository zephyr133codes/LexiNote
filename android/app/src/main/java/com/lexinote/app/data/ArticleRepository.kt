package com.lexinote.app.data

import kotlinx.coroutines.flow.Flow

class ArticleRepository(private val dao: ArticleDao) {
    fun observeAll(): Flow<List<ArticleEntity>> = dao.observeAll()
    fun observeRecentlyRead(): Flow<List<ArticleEntity>> = dao.observeRecentlyRead()
    suspend fun get(id: Long) = dao.getById(id)
    suspend fun save(id: Long?, title: String, content: String): Long {
        val now = System.currentTimeMillis()
        return if (id == null) dao.insert(ArticleEntity(title = title, content = content, createdAt = now, updatedAt = now, lastReadAt = now))
        else {
            val old = checkNotNull(dao.getById(id)) { "文章已不存在" }
            dao.update(old.copy(title = title, content = content, updatedAt = now, lastReadAt = now))
            id
        }
    }
    suspend fun markRead(id: Long) = dao.markRead(id, System.currentTimeMillis())
    suspend fun delete(id: Long) = dao.delete(id)
}
