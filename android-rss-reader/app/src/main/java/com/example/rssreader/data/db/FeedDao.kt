package com.example.rssreader.data.db

import androidx.room.*
import com.example.rssreader.data.model.Feed
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedDao {
    @Query("SELECT * FROM feeds WHERE isActive = 1 ORDER BY title ASC")
    fun getAllFeeds(): Flow<List<Feed>>

    @Query("SELECT * FROM feeds WHERE id = :id")
    suspend fun getFeedById(id: Long): Feed?

    @Query("SELECT * FROM feeds WHERE url = :url LIMIT 1")
    suspend fun getFeedByUrl(url: String): Feed?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeed(feed: Feed): Long

    @Update
    suspend fun updateFeed(feed: Feed)

    @Delete
    suspend fun deleteFeed(feed: Feed)

    @Query("DELETE FROM feeds WHERE id = :id")
    suspend fun deleteFeedById(id: Long)

    @Query("SELECT COUNT(*) FROM articles WHERE feedId = :feedId AND isRead = 0")
    fun getUnreadCountForFeed(feedId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM articles WHERE isRead = 0")
    fun getTotalUnreadCount(): Flow<Int>
}
