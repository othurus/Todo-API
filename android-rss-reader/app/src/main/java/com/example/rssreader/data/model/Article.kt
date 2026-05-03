package com.example.rssreader.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "articles",
    foreignKeys = [ForeignKey(
        entity = Feed::class,
        parentColumns = ["id"],
        childColumns = ["feedId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("feedId"), Index("guid", unique = true)]
)
data class Article(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val guid: String,
    val feedId: Long,
    val feedTitle: String = "",
    val title: String,
    val link: String,
    val description: String = "",
    val content: String = "",
    val author: String? = null,
    val publishedAt: Long? = null,
    val imageUrl: String? = null,
    val isRead: Boolean = false,
    val isBookmarked: Boolean = false,
    val fetchedAt: Long = System.currentTimeMillis()
)
