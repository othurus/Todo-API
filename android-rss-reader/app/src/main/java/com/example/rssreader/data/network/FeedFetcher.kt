package com.example.rssreader.data.network

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import java.util.concurrent.TimeUnit

class FeedFetcher(private val client: OkHttpClient = defaultClient()) {

    suspend fun fetch(url: String): Result<ParsedFeed> {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "RssReader/1.0 (+https://github.com/example/rssreader)")
                .header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml, */*")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }

            val body = response.body ?: return Result.failure(Exception("Empty response body"))
            val parsed = body.byteStream().use { stream ->
                RssParser().parse(stream, url)
            }
            Result.success(parsed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun discoverFeedUrl(siteUrl: String): Result<String> {
        return try {
            val request = Request.Builder()
                .url(siteUrl)
                .header("User-Agent", "RssReader/1.0")
                .build()

            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return Result.failure(Exception("Empty response"))

            val feedPattern = Regex(
                """<link[^>]+type=["'](application/rss\+xml|application/atom\+xml)["'][^>]*href=["']([^"']+)["']""",
                RegexOption.IGNORE_CASE
            )
            val match = feedPattern.find(html)
            if (match != null) {
                val feedHref = match.groupValues[2]
                val feedUrl = if (feedHref.startsWith("http")) feedHref
                    else resolveUrl(siteUrl, feedHref)
                Result.success(feedUrl)
            } else {
                val commonPaths = listOf("/feed", "/rss", "/atom.xml", "/feed.xml", "/rss.xml", "/index.xml")
                val base = siteUrl.trimEnd('/')
                for (path in commonPaths) {
                    try {
                        val probeUrl = "$base$path"
                        val probeReq = Request.Builder().url(probeUrl).head().build()
                        val probeResp = client.newCall(probeReq).execute()
                        if (probeResp.isSuccessful) return Result.success(probeUrl)
                    } catch (_: Exception) {}
                }
                Result.failure(Exception("No feed found at $siteUrl"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun resolveUrl(base: String, relative: String): String {
        return if (relative.startsWith("/")) {
            val uri = java.net.URI(base)
            "${uri.scheme}://${uri.host}$relative"
        } else {
            "$base/$relative"
        }
    }

    companion object {
        private fun defaultClient() = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }
}
