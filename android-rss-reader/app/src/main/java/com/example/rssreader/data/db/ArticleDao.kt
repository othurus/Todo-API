package com.example.rssreader.data.db

import androidx.room.*
import com.example.rssreader.data.model.Article
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {
    @Query("SELECT * FROM articles ORDER BY publishedAt DESC, fetchedAt DESC")
    fun getAllArticles(): Flow<List<Article>>

    @Query("SELECT * FROM articles WHERE feedId = :feedId ORDER BY publishedAt DESC, fetchedAt DESC")
    fun getArticlesForFeed(feedId: Long): Flow<List<Article>>

    @Query("SELECT * FROM articles WHERE isBookmarked = 1 ORDER BY publishedAt DESC, fetchedAt DESC")
    fun getBookmarkedArticles(): Flow<List<Article>>

    @Query("SELECT * FROM articles WHERE isRead = 0 ORDER BY publishedAt DESC, fetchedAt DESC")
    fun getUnreadArticles(): Flow<List<Article>>

    @Query("SELECT * FROM articles WHERE id = :id")
    suspend fun getArticleById(id: Long): Article?

    @Query("SELECT * FROM articles WHERE guid = :guid LIMIT 1")
    suspend fun getArticleByGuid(guid: String): Article?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArticle(article: Article): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArticles(articles: List<Article>)

    @Update
    suspend fun updateArticle(article: Article)

    @Query("UPDATE articles SET isRead = 1 WHERE feedId = :feedId")
    suspend fun markAllReadForFeed(feedId: Long)

    @Query("UPDATE articles SET isRead = 1")
    suspend fun markAllRead()

    @Query("UPDATE articles SET isRead = :isRead WHERE id = :id")
    suspend fun setReadState(id: Long, isRead: Boolean)

    @Query("UPDATE articles SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun setBookmarkState(id: Long, isBookmarked: Boolean)

    @Query("DELETE FROM articles WHERE feedId = :feedId AND isBookmarked = 0")
    suspend fun deleteArticlesForFeed(feedId: Long)

    @Query("""
        SELECT * FROM articles
        WHERE (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%')
        ORDER BY publishedAt DESC, fetchedAt DESC
    """)
    fun searchArticles(query: String): Flow<List<Article>>

    @Query("DELETE FROM articles WHERE fetchedAt < :cutoffTime AND isBookmarked = 0 AND isRead = 1")
    suspend fun deleteOldArticles(cutoffTime: Long)
}
