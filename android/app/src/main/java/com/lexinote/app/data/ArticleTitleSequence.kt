package com.lexinote.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "article_title_sequence")
data class ArticleTitleSequence(
    @PrimaryKey val id: Int = 1,
    val nextNumber: Int = 1
)
