package com.lexinote.app.data

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "annotations", foreignKeys = [ForeignKey(entity = ArticleEntity::class, parentColumns = ["id"], childColumns = ["articleId"], onDelete = ForeignKey.CASCADE)], indices = [Index(value = ["articleId", "startOffset", "endOffset"], unique = true)])
data class AnnotationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val articleId: Long,
    val startOffset: Int,
    val endOffset: Int,
    val word: String,
    val phonetic: String,
    val meaning: String? = null,
    val createdAt: Long,
    val updatedAt: Long = createdAt,
    @ColumnInfo(defaultValue = "'word'") val phoneticSourceType: String = "word",
    val phoneticSourceLemma: String? = null
)
