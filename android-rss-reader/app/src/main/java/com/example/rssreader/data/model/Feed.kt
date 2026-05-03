package com.example.rssreader.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feeds")
data class Feed(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String = "",
    val description: String = "",
    val siteUrl: String = "",
    val imageUrl: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val lastRefreshed: Long? = null,
    val category: String = "Uncategorized",
    val isActive: Boolean = true
)
