package com.example.rssreader.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.rssreader.data.model.Article
import com.example.rssreader.data.model.Feed
import com.example.rssreader.data.repository.FeedRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class FeedFilter {
    object All : FeedFilter()
    object Unread : FeedFilter()
    object Bookmarks : FeedFilter()
    data class ByFeed(val feed: Feed) : FeedFilter()
}

data class MainUiState(
    val feeds: List<Feed> = emptyList(),
    val articles: List<Article> = emptyList(),
    val selectedFilter: FeedFilter = FeedFilter.All,
    val selectedArticle: Article? = null,
    val isRefreshing: Boolean = false,
    val isAddingFeed: Boolean = false,
    val addFeedError: String? = null,
    val totalUnreadCount: Int = 0,
    val searchQuery: String = "",
    val searchResults: List<Article> = emptyList(),
    val isSearchActive: Boolean = false,
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(private val repository: FeedRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _selectedFilter = MutableStateFlow<FeedFilter>(FeedFilter.All)

    init {
        viewModelScope.launch {
            repository.allFeeds.collect { feeds ->
                _uiState.update { it.copy(feeds = feeds) }
            }
        }
        viewModelScope.launch {
            repository.totalUnreadCount.collect { count ->
                _uiState.update { it.copy(totalUnreadCount = count) }
            }
        }
        viewModelScope.launch {
            _selectedFilter.flatMapLatest { filter ->
                when (filter) {
                    is FeedFilter.All -> repository.allArticles
                    is FeedFilter.Unread -> repository.unreadArticles
                    is FeedFilter.Bookmarks -> repository.bookmarkedArticles
                    is FeedFilter.ByFeed -> repository.getArticlesForFeed(filter.feed.id)
                }
            }.collect { articles ->
                _uiState.update { it.copy(articles = articles) }
            }
        }
    }

    fun setFilter(filter: FeedFilter) {
        _selectedFilter.value = filter
        _uiState.update { it.copy(selectedFilter = filter, isSearchActive = false, searchQuery = "") }
    }

    fun selectArticle(article: Article?) {
        _uiState.update { it.copy(selectedArticle = article) }
        if (article != null && !article.isRead) {
            viewModelScope.launch { repository.markArticleRead(article.id) }
        }
    }

    fun addFeed(url: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isAddingFeed = true, addFeedError = null) }
            val result = if (url.contains("/rss") || url.contains("/feed") || url.contains(".xml") || url.contains("atom")) {
                repository.addFeed(url)
            } else {
                repository.discoverAndAddFeed(url)
            }
            result.fold(
                onSuccess = { _uiState.update { it.copy(isAddingFeed = false, addFeedError = null) } },
                onFailure = { e -> _uiState.update { it.copy(isAddingFeed = false, addFeedError = e.message) } }
            )
        }
    }

    fun deleteFeed(feed: Feed) {
        viewModelScope.launch {
            if (_uiState.value.selectedFilter is FeedFilter.ByFeed &&
                (_uiState.value.selectedFilter as FeedFilter.ByFeed).feed.id == feed.id) {
                setFilter(FeedFilter.All)
            }
            repository.deleteFeed(feed)
        }
    }

    fun refreshCurrentFeed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            val filter = _uiState.value.selectedFilter
            when (filter) {
                is FeedFilter.ByFeed -> {
                    repository.refreshFeed(filter.feed).onFailure { e ->
                        _uiState.update { it.copy(errorMessage = "Refresh failed: ${e.message}") }
                    }
                }
                else -> {
                    val feeds = _uiState.value.feeds
                    feeds.forEach { feed ->
                        repository.refreshFeed(feed)
                    }
                }
            }
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun toggleBookmark(article: Article) {
        viewModelScope.launch { repository.toggleBookmark(article) }
    }

    fun markArticleRead(articleId: Long) {
        viewModelScope.launch { repository.markArticleRead(articleId) }
    }

    fun markAllRead() {
        viewModelScope.launch {
            val filter = _uiState.value.selectedFilter
            if (filter is FeedFilter.ByFeed) {
                repository.markAllReadForFeed(filter.feed.id)
            } else {
                repository.markAllRead()
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList()) }
            return
        }
        viewModelScope.launch {
            repository.searchArticles(query).collect { results ->
                _uiState.update { it.copy(searchResults = results) }
            }
        }
    }

    fun setSearchActive(active: Boolean) {
        _uiState.update { it.copy(isSearchActive = active, searchQuery = "", searchResults = emptyList()) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null, addFeedError = null) }
    }

    class Factory(private val repository: FeedRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MainViewModel(repository) as T
    }
}
