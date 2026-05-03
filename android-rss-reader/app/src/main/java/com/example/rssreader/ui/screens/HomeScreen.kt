package com.example.rssreader.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.rssreader.data.model.Article
import com.example.rssreader.data.model.Feed
import com.example.rssreader.ui.viewmodel.FeedFilter
import com.example.rssreader.ui.viewmodel.MainUiState
import com.example.rssreader.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    onArticleClick: (Article) -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showAddFeedDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<Feed?>(null) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            FeedDrawer(
                feeds = uiState.feeds,
                selectedFilter = uiState.selectedFilter,
                totalUnreadCount = uiState.totalUnreadCount,
                onFilterSelected = { filter ->
                    viewModel.setFilter(filter)
                    scope.launch { drawerState.close() }
                },
                onAddFeed = { showAddFeedDialog = true },
                onDeleteFeed = { showDeleteConfirm = it }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopBar(
                    uiState = uiState,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onRefresh = { viewModel.refreshCurrentFeed() },
                    onMarkAllRead = { viewModel.markAllRead() },
                    onSearchToggle = { viewModel.setSearchActive(!uiState.isSearchActive) },
                    onSearchQuery = { viewModel.setSearchQuery(it) }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddFeedDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Feed")
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                val articles = if (uiState.isSearchActive && uiState.searchQuery.isNotEmpty()) {
                    uiState.searchResults
                } else {
                    uiState.articles
                }

                if (articles.isEmpty() && !uiState.isRefreshing) {
                    EmptyState(
                        filter = uiState.selectedFilter,
                        onAddFeed = { showAddFeedDialog = true }
                    )
                } else {
                    ArticleList(
                        articles = articles,
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { viewModel.refreshCurrentFeed() },
                        onArticleClick = onArticleClick,
                        onToggleBookmark = { viewModel.toggleBookmark(it) },
                        onMarkRead = { viewModel.markArticleRead(it.id) }
                    )
                }

                if (uiState.errorMessage != null) {
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        action = {
                            TextButton(onClick = { viewModel.clearError() }) {
                                Text("Dismiss")
                            }
                        }
                    ) { Text(uiState.errorMessage) }
                }
            }
        }
    }

    if (showAddFeedDialog) {
        AddFeedDialog(
            isLoading = uiState.isAddingFeed,
            error = uiState.addFeedError,
            onAdd = { url ->
                viewModel.addFeed(url)
                if (!uiState.isAddingFeed && uiState.addFeedError == null) {
                    showAddFeedDialog = false
                }
            },
            onDismiss = {
                showAddFeedDialog = false
                viewModel.clearError()
            }
        )
    }

    showDeleteConfirm?.let { feed ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Remove Feed") },
            text = { Text("Remove \"${feed.title}\" and all its articles?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFeed(feed)
                    showDeleteConfirm = null
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    uiState: MainUiState,
    onMenuClick: () -> Unit,
    onRefresh: () -> Unit,
    onMarkAllRead: () -> Unit,
    onSearchToggle: () -> Unit,
    onSearchQuery: (String) -> Unit
) {
    Column {
        TopAppBar(
            title = {
                if (uiState.isSearchActive) {
                    TextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQuery,
                        placeholder = { Text("Search articles...") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = when (val f = uiState.selectedFilter) {
                            is FeedFilter.All -> "All Articles"
                            is FeedFilter.Unread -> "Unread"
                            is FeedFilter.Bookmarks -> "Saved"
                            is FeedFilter.ByFeed -> f.feed.title
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                }
            },
            actions = {
                IconButton(onClick = onSearchToggle) {
                    Icon(
                        if (uiState.isSearchActive) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Search"
                    )
                }
                if (!uiState.isSearchActive) {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Mark all as read") },
                            leadingIcon = { Icon(Icons.Default.DoneAll, null) },
                            onClick = { onMarkAllRead(); showMenu = false }
                        )
                    }
                }
            }
        )
        if (uiState.isRefreshing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedDrawer(
    feeds: List<Feed>,
    selectedFilter: FeedFilter,
    totalUnreadCount: Int,
    onFilterSelected: (FeedFilter) -> Unit,
    onAddFeed: () -> Unit,
    onDeleteFeed: (Feed) -> Unit
) {
    ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.RssFeed,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "RSS Reader",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Article, null) },
            label = { Text("All Articles") },
            badge = { if (totalUnreadCount > 0) UnreadBadge(totalUnreadCount) },
            selected = selectedFilter is FeedFilter.All,
            onClick = { onFilterSelected(FeedFilter.All) },
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.FiberNew, null) },
            label = { Text("Unread") },
            selected = selectedFilter is FeedFilter.Unread,
            onClick = { onFilterSelected(FeedFilter.Unread) },
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Default.Bookmark, null) },
            label = { Text("Saved") },
            selected = selectedFilter is FeedFilter.Bookmarks,
            onClick = { onFilterSelected(FeedFilter.Bookmarks) },
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "MY FEEDS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(onClick = onAddFeed, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Add feed", modifier = Modifier.size(16.dp))
            }
        }

        LazyColumn {
            items(feeds, key = { it.id }) { feed ->
                FeedDrawerItem(
                    feed = feed,
                    isSelected = selectedFilter is FeedFilter.ByFeed && (selectedFilter as FeedFilter.ByFeed).feed.id == feed.id,
                    onClick = { onFilterSelected(FeedFilter.ByFeed(feed)) },
                    onDelete = { onDeleteFeed(feed) }
                )
            }
        }
    }
}

@Composable
private fun FeedDrawerItem(
    feed: Feed,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showOptions by remember { mutableStateOf(false) }
    NavigationDrawerItem(
        icon = {
            if (feed.imageUrl != null) {
                AsyncImage(
                    model = feed.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.RssFeed, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        },
        label = { Text(feed.title.ifEmpty { feed.url }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        selected = isSelected,
        onClick = onClick,
        badge = {
            IconButton(
                onClick = { showOptions = true },
                modifier = Modifier.size(20.dp)
            ) {
                Icon(Icons.Default.MoreVert, null, modifier = Modifier.size(16.dp))
            }
            DropdownMenu(expanded = showOptions, onDismissRequest = { showOptions = false }) {
                DropdownMenuItem(
                    text = { Text("Remove feed", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                    onClick = { onDelete(); showOptions = false }
                )
            }
        },
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

@Composable
private fun UnreadBadge(count: Int) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArticleList(
    articles: List<Article>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onArticleClick: (Article) -> Unit,
    onToggleBookmark: (Article) -> Unit,
    onMarkRead: (Article) -> Unit
) {
    val pullRefreshState = rememberPullToRefreshState()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(articles, key = { it.id }) { article ->
                ArticleCard(
                    article = article,
                    onClick = { onArticleClick(article) },
                    onToggleBookmark = { onToggleBookmark(article) },
                    onMarkRead = { onMarkRead(article) }
                )
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }
            item { Spacer(Modifier.height(80.dp)) }
        }

        PullToRefreshContainer(
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

    LaunchedEffect(pullRefreshState.isRefreshing) {
        if (pullRefreshState.isRefreshing) {
            onRefresh()
            pullRefreshState.endRefresh()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArticleCard(
    article: Article,
    onClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    onMarkRead: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) {
                onMarkRead()
            }
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd)
                MaterialTheme.colorScheme.primaryContainer else Color.Transparent
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                    Icon(Icons.Default.DoneAll, contentDescription = "Mark read",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        },
        enableDismissFromStartToEnd = !article.isRead,
        enableDismissFromEndToStart = false
    ) {
        ArticleCardContent(article, onClick, onToggleBookmark)
    }
}

@Composable
private fun ArticleCardContent(
    article: Article,
    onClick: () -> Unit,
    onToggleBookmark: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (!article.isRead) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp, end = 10.dp)
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        } else {
            Spacer(Modifier.width(18.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = article.feedTitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (article.isRead) FontWeight.Normal else FontWeight.SemiBold,
                color = if (article.isRead) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            val cleanDesc = article.description
                .replace(Regex("<[^>]+>"), "")
                .replace(Regex("\\s+"), " ")
                .trim()
            if (cleanDesc.isNotBlank()) {
                Text(
                    text = cleanDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = article.publishedAt?.let { formatDate(it) } ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                if (article.author != null) {
                    Text(
                        text = "by ${article.author}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (article.imageUrl != null) {
                AsyncImage(
                    model = article.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(4.dp))
            }
            IconButton(
                onClick = onToggleBookmark,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    if (article.isBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = if (article.isBookmarked) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyState(filter: FeedFilter, onAddFeed: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            when (filter) {
                is FeedFilter.Bookmarks -> Icons.Default.Bookmark
                is FeedFilter.Unread -> Icons.Default.FiberNew
                else -> Icons.Default.RssFeed
            },
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = when (filter) {
                is FeedFilter.Bookmarks -> "No saved articles"
                is FeedFilter.Unread -> "You're all caught up!"
                is FeedFilter.ByFeed -> "No articles yet"
                else -> "No feeds added yet"
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (filter is FeedFilter.All) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Add your first RSS feed to get started",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onAddFeed) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add Feed")
            }
        }
    }
}

@Composable
private fun AddFeedDialog(
    isLoading: Boolean,
    error: String?,
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Add RSS Feed") },
        text = {
            Column {
                Text(
                    "Enter a feed URL or website address",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    placeholder = { Text("https://example.com/feed") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
                if (isLoading) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(url.trim()) },
                enabled = url.isNotBlank() && !isLoading
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Cancel") }
        }
    )
}

private fun formatDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 604_800_000 -> "${diff / 86_400_000}d ago"
        else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}
