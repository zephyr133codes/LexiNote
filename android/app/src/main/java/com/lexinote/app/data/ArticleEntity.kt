package com.lexinote.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val lastReadAt: Long? = null
)
