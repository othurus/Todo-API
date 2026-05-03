package com.example.rssreader.data.network

import android.util.Xml
import com.example.rssreader.data.model.Article
import com.example.rssreader.data.model.Feed
import org.jsoup.Jsoup
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale

data class ParsedFeed(
    val title: String,
    val description: String,
    val siteUrl: String,
    val imageUrl: String?,
    val articles: List<ParsedArticle>
)

data class ParsedArticle(
    val guid: String,
    val title: String,
    val link: String,
    val description: String,
    val content: String,
    val author: String?,
    val publishedAt: Long?,
    val imageUrl: String?
)

class RssParser {

    private val dateFormats = listOf(
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ENGLISH),
        SimpleDateFormat("dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
    )

    fun parse(inputStream: InputStream, feedUrl: String): ParsedFeed {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            setInput(inputStream, null)
        }

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                return when (parser.name?.lowercase()) {
                    "rss", "rdf:rdf" -> parseRss(parser, feedUrl)
                    "feed" -> parseAtom(parser, feedUrl)
                    else -> parseRss(parser, feedUrl)
                }
            }
            eventType = parser.next()
        }
        return ParsedFeed("", "", feedUrl, null, emptyList())
    }

    private fun parseRss(parser: XmlPullParser, feedUrl: String): ParsedFeed {
        var feedTitle = ""
        var feedDescription = ""
        var feedSiteUrl = ""
        var feedImageUrl: String? = null
        val articles = mutableListOf<ParsedArticle>()

        var inChannel = false
        var inItem = false
        var inImage = false

        var itemTitle = ""
        var itemLink = ""
        var itemDescription = ""
        var itemContent = ""
        var itemGuid = ""
        var itemAuthor: String? = null
        var itemPubDate: Long? = null
        var itemImageUrl: String? = null

        var currentText = StringBuilder()

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name?.lowercase() ?: ""

            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentText = StringBuilder()
                    when {
                        tagName == "channel" -> inChannel = true
                        tagName == "item" -> {
                            inItem = true
                            itemTitle = ""; itemLink = ""; itemDescription = ""
                            itemContent = ""; itemGuid = ""; itemAuthor = null
                            itemPubDate = null; itemImageUrl = null
                        }
                        tagName == "image" && !inItem -> inImage = true
                        tagName == "enclosure" && inItem -> {
                            val type = parser.getAttributeValue(null, "type") ?: ""
                            if (type.startsWith("image/")) {
                                itemImageUrl = parser.getAttributeValue(null, "url")
                            }
                        }
                        tagName == "media:content" && inItem -> {
                            val medium = parser.getAttributeValue(null, "medium") ?: ""
                            val url = parser.getAttributeValue(null, "url") ?: ""
                            if ((medium == "image" || url.contains(Regex("\\.(jpg|jpeg|png|webp|gif)", RegexOption.IGNORE_CASE))) && itemImageUrl == null) {
                                itemImageUrl = url
                            }
                        }
                        tagName == "media:thumbnail" && inItem -> {
                            if (itemImageUrl == null) {
                                itemImageUrl = parser.getAttributeValue(null, "url")
                            }
                        }
                    }
                }
                XmlPullParser.TEXT -> currentText.append(parser.text ?: "")
                XmlPullParser.CDSECT -> currentText.append(parser.text ?: "")
                XmlPullParser.END_TAG -> {
                    val text = currentText.toString().trim()
                    when {
                        tagName == "item" && inItem -> {
                            val guid = itemGuid.ifEmpty { itemLink.ifEmpty { "$feedUrl-$itemTitle" } }
                            if (itemTitle.isNotEmpty() || itemLink.isNotEmpty()) {
                                articles.add(ParsedArticle(
                                    guid = guid,
                                    title = itemTitle,
                                    link = itemLink,
                                    description = itemDescription,
                                    content = itemContent.ifEmpty { itemDescription },
                                    author = itemAuthor,
                                    publishedAt = itemPubDate,
                                    imageUrl = itemImageUrl ?: extractImageFromHtml(itemDescription)
                                ))
                            }
                            inItem = false
                        }
                        tagName == "image" && !inItem -> inImage = false
                        tagName == "title" -> when {
                            inItem -> itemTitle = text
                            inChannel && !inImage -> feedTitle = text
                        }
                        tagName == "link" -> when {
                            inItem -> if (itemLink.isEmpty()) itemLink = text
                            inChannel && !inImage -> feedSiteUrl = text
                        }
                        tagName == "description" || tagName == "summary" -> when {
                            inItem -> itemDescription = text
                            inChannel -> feedDescription = text
                        }
                        (tagName == "content:encoded" || tagName == "content") && inItem -> {
                            itemContent = text
                        }
                        tagName == "guid" && inItem -> itemGuid = text
                        (tagName == "dc:creator" || tagName == "author") && inItem -> itemAuthor = text
                        tagName == "pubdate" && inItem -> itemPubDate = parseDate(text)
                        (tagName == "url" || tagName == "uri") && inImage -> feedImageUrl = text
                    }
                    currentText = StringBuilder()
                }
            }
            eventType = parser.next()
        }

        return ParsedFeed(feedTitle, feedDescription, feedSiteUrl, feedImageUrl, articles)
    }

    private fun parseAtom(parser: XmlPullParser, feedUrl: String): ParsedFeed {
        var feedTitle = ""
        var feedDescription = ""
        var feedSiteUrl = ""
        var feedImageUrl: String? = null
        val articles = mutableListOf<ParsedArticle>()

        var inEntry = false
        var inAuthor = false
        var entryTitle = ""
        var entryLink = ""
        var entrySummary = ""
        var entryContent = ""
        var entryId = ""
        var entryAuthor: String? = null
        var entryPublished: Long? = null
        var entryUpdated: Long? = null
        var entryImageUrl: String? = null

        var currentText = StringBuilder()

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name?.lowercase() ?: ""

            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentText = StringBuilder()
                    when {
                        tagName == "entry" -> {
                            inEntry = true
                            entryTitle = ""; entryLink = ""; entrySummary = ""
                            entryContent = ""; entryId = ""; entryAuthor = null
                            entryPublished = null; entryUpdated = null; entryImageUrl = null
                        }
                        tagName == "author" -> inAuthor = true
                        tagName == "link" -> {
                            val rel = parser.getAttributeValue(null, "rel") ?: "alternate"
                            val href = parser.getAttributeValue(null, "href") ?: ""
                            val type = parser.getAttributeValue(null, "type") ?: ""
                            if (rel == "alternate" || rel == "related") {
                                if (inEntry) entryLink = href
                                else feedSiteUrl = href
                            } else if (rel == "enclosure" && type.startsWith("image/") && inEntry) {
                                entryImageUrl = href
                            }
                        }
                        tagName == "media:content" && inEntry -> {
                            val medium = parser.getAttributeValue(null, "medium") ?: ""
                            val url = parser.getAttributeValue(null, "url") ?: ""
                            if ((medium == "image" || url.contains(Regex("\\.(jpg|jpeg|png|webp|gif)", RegexOption.IGNORE_CASE))) && entryImageUrl == null) {
                                entryImageUrl = url
                            }
                        }
                        tagName == "media:thumbnail" && inEntry -> {
                            if (entryImageUrl == null) {
                                entryImageUrl = parser.getAttributeValue(null, "url")
                            }
                        }
                    }
                }
                XmlPullParser.TEXT -> currentText.append(parser.text ?: "")
                XmlPullParser.CDSECT -> currentText.append(parser.text ?: "")
                XmlPullParser.END_TAG -> {
                    val text = currentText.toString().trim()
                    when {
                        tagName == "entry" && inEntry -> {
                            val guid = entryId.ifEmpty { entryLink }
                            if (entryTitle.isNotEmpty() || entryLink.isNotEmpty()) {
                                articles.add(ParsedArticle(
                                    guid = guid,
                                    title = entryTitle,
                                    link = entryLink,
                                    description = entrySummary,
                                    content = entryContent.ifEmpty { entrySummary },
                                    author = entryAuthor,
                                    publishedAt = entryPublished ?: entryUpdated,
                                    imageUrl = entryImageUrl ?: extractImageFromHtml(entrySummary)
                                ))
                            }
                            inEntry = false
                        }
                        tagName == "author" -> inAuthor = false
                        tagName == "title" -> when {
                            inEntry -> entryTitle = text
                            else -> feedTitle = text
                        }
                        tagName == "summary" && inEntry -> entrySummary = text
                        tagName == "content" && inEntry -> entryContent = text
                        tagName == "subtitle" && !inEntry -> feedDescription = text
                        tagName == "id" && inEntry -> entryId = text
                        tagName == "name" && inAuthor -> entryAuthor = text
                        tagName == "published" && inEntry -> entryPublished = parseDate(text)
                        tagName == "updated" -> {
                            val parsed = parseDate(text)
                            if (inEntry) entryUpdated = parsed
                        }
                        tagName == "icon" && !inEntry -> feedImageUrl = text
                        tagName == "logo" && !inEntry -> if (feedImageUrl == null) feedImageUrl = text
                    }
                    currentText = StringBuilder()
                }
            }
            eventType = parser.next()
        }

        return ParsedFeed(feedTitle, feedDescription, feedSiteUrl, feedImageUrl, articles)
    }

    private fun parseDate(dateStr: String): Long? {
        if (dateStr.isBlank()) return null
        for (format in dateFormats) {
            try {
                return format.parse(dateStr)?.time
            } catch (_: Exception) {}
        }
        return null
    }

    private fun extractImageFromHtml(html: String): String? {
        if (html.isBlank()) return null
        return try {
            val doc = Jsoup.parse(html)
            doc.select("img[src]").firstOrNull()?.attr("src")
        } catch (_: Exception) {
            null
        }
    }
}
