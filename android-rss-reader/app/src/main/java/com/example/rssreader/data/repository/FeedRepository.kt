package com.example.rssreader.data.repository

import com.example.rssreader.data.db.ArticleDao
import com.example.rssreader.data.db.FeedDao
import com.example.rssreader.data.model.Article
import com.example.rssreader.data.model.Feed
import com.example.rssreader.data.network.FeedFetcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class FeedRepository(
    private val feedDao: FeedDao,
    private val articleDao: ArticleDao,
    private val fetcher: FeedFetcher = FeedFetcher()
) {
    val allFeeds: Flow<List<Feed>> = feedDao.getAllFeeds()
    val allArticles: Flow<List<Article>> = articleDao.getAllArticles()
    val unreadArticles: Flow<List<Article>> = articleDao.getUnreadArticles()
    val bookmarkedArticles: Flow<List<Article>> = articleDao.getBookmarkedArticles()
    val totalUnreadCount: Flow<Int> = feedDao.getTotalUnreadCount()

    fun getArticlesForFeed(feedId: Long): Flow<List<Article>> =
        articleDao.getArticlesForFeed(feedId)

    fun getUnreadCountForFeed(feedId: Long): Flow<Int> =
        feedDao.getUnreadCountForFeed(feedId)

    fun searchArticles(query: String): Flow<List<Article>> =
        articleDao.searchArticles(query)

    suspend fun addFeed(url: String): Result<Feed> {
        val existing = feedDao.getFeedByUrl(url)
        if (existing != null) return Result.failure(Exception("Feed already added"))

        val result = fetcher.fetch(url)
        return result.map { parsed ->
            val feed = Feed(
                url = url,
                title = parsed.title.ifEmpty { url },
                description = parsed.description,
                siteUrl = parsed.siteUrl,
                imageUrl = parsed.imageUrl
            )
            val feedId = feedDao.insertFeed(feed)
            val articles = parsed.articles.map { it.toArticle(feedId, feed.title) }
            articleDao.insertArticles(articles)
            feed.copy(id = feedId)
        }
    }

    suspend fun discoverAndAddFeed(siteUrl: String): Result<Feed> {
        val discoveryResult = fetcher.discoverFeedUrl(siteUrl)
        val feedUrl = discoveryResult.getOrElse { return addFeed(siteUrl) }
        return addFeed(feedUrl)
    }

    suspend fun refreshFeed(feed: Feed): Result<Int> {
        val result = fetcher.fetch(feed.url)
        return result.map { parsed ->
            val articles = parsed.articles.map { it.toArticle(feed.id, feed.title) }
            var newCount = 0
            articles.forEach { article ->
                val inserted = articleDao.insertArticle(article)
                if (inserted != -1L) newCount++
            }
            val updatedFeed = feed.copy(
                title = parsed.title.ifEmpty { feed.title },
                description = parsed.description.ifEmpty { feed.description },
                siteUrl = parsed.siteUrl.ifEmpty { feed.siteUrl },
                imageUrl = parsed.imageUrl ?: feed.imageUrl,
                lastRefreshed = System.currentTimeMillis()
            )
            feedDao.updateFeed(updatedFeed)
            newCount
        }
    }

    suspend fun refreshAllFeeds(): Map<Long, Result<Int>> {
        val feeds = feedDao.getAllFeeds().first()
        return feeds.associate { feed -> feed.id to refreshFeed(feed) }
    }

    suspend fun deleteFeed(feed: Feed) {
        feedDao.deleteFeed(feed)
    }

    suspend fun markArticleRead(articleId: Long) {
        articleDao.setReadState(articleId, true)
    }

    suspend fun markArticleUnread(articleId: Long) {
        articleDao.setReadState(articleId, false)
    }

    suspend fun toggleBookmark(article: Article) {
        articleDao.setBookmarkState(article.id, !article.isBookmarked)
    }

    suspend fun markAllReadForFeed(feedId: Long) {
        articleDao.markAllReadForFeed(feedId)
    }

    suspend fun markAllRead() {
        articleDao.markAllRead()
    }

    suspend fun pruneOldArticles(daysToKeep: Int = 30) {
        val cutoff = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        articleDao.deleteOldArticles(cutoff)
    }
}

private fun com.example.rssreader.data.network.ParsedArticle.toArticle(feedId: Long, feedTitle: String) = Article(
    guid = guid,
    feedId = feedId,
    feedTitle = feedTitle,
    title = title,
    link = link,
    description = description,
    content = content,
    author = author,
    publishedAt = publishedAt,
    imageUrl = imageUrl
)
