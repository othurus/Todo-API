package com.example.rssreader

import android.app.Application
import com.example.rssreader.data.db.AppDatabase
import com.example.rssreader.data.network.FeedFetcher
import com.example.rssreader.data.repository.FeedRepository

class RssReaderApp : Application() {

    val database by lazy { AppDatabase.getInstance(this) }

    val repository by lazy {
        FeedRepository(
            feedDao = database.feedDao(),
            articleDao = database.articleDao(),
            fetcher = FeedFetcher()
        )
    }
}
