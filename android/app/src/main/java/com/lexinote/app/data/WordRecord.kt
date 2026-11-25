package com.lexinote.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "word_records", indices = [Index(value = ["isStarred", "starredAt"]), Index(value = ["lastViewedAt"])])
data class WordRecord(
    @PrimaryKey val normalizedWord: String,
    val displayWord: String,
    val lastViewedAt: Long,
    val viewCount: Long,
    val isStarred: Boolean = false,
    val starredAt: Long? = null,
    val isMastered: Boolean = false,
    val masteredAt: Long? = null
)
