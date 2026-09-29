package com.example.data.backend

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SearchResultItem(
    val title: String,
    val snippet: String,
    val url: String,
    val source: String = "Web / Google"
)

class SearchIntegrationService(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    /**
     * Triggers native Google / Android Web Search intent
     */
    fun launchGoogleSearchIntent(query: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query.trim())
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query.trim())}")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(browserIntent)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Performs a live HTTP query search to fetch web knowledge results
     */
    suspend fun executeLiveQuery(query: String): List<SearchResultItem> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return@withContext emptyList()

        val results = mutableListOf<SearchResultItem>()
        try {
            val encoded = Uri.encode(cleanQuery)
            // Use DuckDuckGo Instant Answer API for fast, reliable JSON search results without API keys
            val request = Request.Builder()
                .url("https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1")
                .header("User-Agent", "AndroidRoleVault/1.0")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val json = JSONObject(body)
                    val heading = json.optString("Heading", cleanQuery)
                    val abstractText = json.optString("AbstractText", "")
                    val abstractUrl = json.optString("AbstractURL", "https://www.google.com/search?q=$encoded")

                    if (abstractText.isNotBlank()) {
                        results.add(
                            SearchResultItem(
                                title = heading,
                                snippet = abstractText,
                                url = abstractUrl,
                                source = "Instant Answer Engine"
                            )
                        )
                    }

                    val relatedTopics = json.optJSONArray("RelatedTopics")
                    if (relatedTopics != null) {
                        for (i in 0 until minOf(relatedTopics.length(), 4)) {
                            val topic = relatedTopics.optJSONObject(i)
                            if (topic != null && topic.has("Text")) {
                                val text = topic.getString("Text")
                                val firstUrl = topic.optString("FirstURL", "")
                                results.add(
                                    SearchResultItem(
                                        title = text.take(50) + "...",
                                        snippet = text,
                                        url = firstUrl.ifBlank { "https://www.google.com/search?q=$encoded" },
                                        source = "Web Topic"
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // If offline or network error, provide structured fallback search entry
        }

        if (results.isEmpty()) {
            results.add(
                SearchResultItem(
                    title = "Google Search: $cleanQuery",
                    snippet = "Klik om de actuele zoekresultaten rechtstreeks in Google te openen voor '$cleanQuery'.",
                    url = "https://www.google.com/search?q=${Uri.encode(cleanQuery)}",
                    source = "Google Direct"
                )
            )
        }

        results
    }
}
