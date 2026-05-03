package com.example.rssreader.ui.screens

import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.rssreader.data.model.Article
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    article: Article,
    onBack: () -> Unit,
    onToggleBookmark: () -> Unit
) {
    val context = LocalContext.current
    var loadingProgress by remember { mutableStateOf(0) }
    var showWebView by remember { mutableStateOf(false) }

    val hasContent = article.content.isNotBlank() || article.description.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        article.feedTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onToggleBookmark) {
                        Icon(
                            if (article.isBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (article.isBookmarked) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.link))
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in browser")
                    }
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "${article.title}\n${article.link}")
                        }
                        context.startActivity(Intent.createChooser(intent, "Share article"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            AnimatedVisibility(visible = loadingProgress in 1..99) {
                LinearProgressIndicator(
                    progress = { loadingProgress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (showWebView && article.link.isNotEmpty()) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                setSupportZoom(true)
                                builtInZoomControls = true
                                displayZoomControls = false
                                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            }
                            webViewClient = WebViewClient()
                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    loadingProgress = newProgress
                                }
                            }
                            loadUrl(article.link)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                ArticleContent(
                    article = article,
                    onOpenBrowser = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.link))
                        context.startActivity(intent)
                    },
                    onShowWebView = { showWebView = true }
                )
            }
        }
    }
}

@Composable
private fun ArticleContent(
    article: Article,
    onOpenBrowser: () -> Unit,
    onShowWebView: () -> Unit
) {
    val scrollState = rememberScrollState()
    val content = article.content.ifBlank { article.description }
    val htmlContent = buildArticleHtml(article, content)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (article.author != null) {
                    Text(
                        text = article.author,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        " · ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Text(
                    text = article.publishedAt?.let {
                        SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(Date(it))
                    } ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        if (content.isNotBlank()) {
            val isDark = isSystemInDarkTheme()
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.apply {
                            javaScriptEnabled = false
                            defaultFontSize = 16
                            useWideViewPort = true
                            loadWithOverviewMode = true
                        }
                        webViewClient = WebViewClient()
                        isScrollContainer = false
                        overScrollMode = WebView.OVER_SCROLL_NEVER
                        loadDataWithBaseURL(
                            article.link.ifEmpty { null },
                            buildStyledHtml(htmlContent, isDark),
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 10000.dp)
                    .padding(horizontal = 8.dp)
            )
        }

        Column(modifier = Modifier.padding(16.dp)) {
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onShowWebView,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Language, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Load Full Page")
                }
                OutlinedButton(
                    onClick = onOpenBrowser,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.OpenInBrowser, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Open in Browser")
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

private fun buildArticleHtml(article: Article, content: String): String {
    return if (content.contains("<") && content.contains(">")) {
        content
    } else {
        "<p>${content.replace("\n\n", "</p><p>").replace("\n", "<br>")}</p>"
    }
}

private fun buildStyledHtml(content: String, isDark: Boolean): String {
    val bg = if (isDark) "#121212" else "#FFFFFF"
    val text = if (isDark) "#E0E0E0" else "#212121"
    val link = if (isDark) "#90CAF9" else "#1565C0"
    val muted = if (isDark) "#9E9E9E" else "#616161"

    return """
        <!DOCTYPE html>
        <html>
        <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=5.0">
        <style>
            * { box-sizing: border-box; }
            body {
                font-family: -apple-system, 'Helvetica Neue', Arial, sans-serif;
                font-size: 16px;
                line-height: 1.7;
                color: $text;
                background: $bg;
                margin: 0;
                padding: 8px 16px 24px;
                word-wrap: break-word;
            }
            h1, h2, h3, h4, h5, h6 {
                color: $text;
                margin-top: 1.5em;
                margin-bottom: 0.5em;
                line-height: 1.3;
            }
            p { margin: 0 0 1em; }
            a { color: $link; text-decoration: none; }
            img {
                max-width: 100%;
                height: auto;
                border-radius: 8px;
                display: block;
                margin: 12px auto;
            }
            blockquote {
                border-left: 4px solid $link;
                margin: 1em 0;
                padding: 4px 16px;
                color: $muted;
                font-style: italic;
            }
            code {
                background: ${if (isDark) "#2d2d2d" else "#f5f5f5"};
                padding: 2px 6px;
                border-radius: 4px;
                font-size: 0.9em;
                font-family: monospace;
            }
            pre {
                background: ${if (isDark) "#2d2d2d" else "#f5f5f5"};
                padding: 12px;
                border-radius: 8px;
                overflow-x: auto;
            }
            pre code { background: none; padding: 0; }
            figure { margin: 1em 0; }
            figcaption { text-align: center; color: $muted; font-size: 0.85em; }
            table { width: 100%; border-collapse: collapse; margin: 1em 0; }
            th, td { border: 1px solid ${if (isDark) "#444" else "#ddd"}; padding: 8px; text-align: left; }
            th { background: ${if (isDark) "#2d2d2d" else "#f5f5f5"}; }
            iframe { max-width: 100%; }
        </style>
        </head>
        <body>$content</body>
        </html>
    """.trimIndent()
}
