package com.satory.graphenosai.llm

import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Model discovery for self-hosted OpenAI-compatible servers.
 * Queries GET {baseUrl}/models — supported by llama-server, Ollama (/v1),
 * vLLM, LM Studio, Hermes API, etc.
 */
object SelfHostedModels {

    private const val TAG = "SelfHostedModels"
    private const val TIMEOUT_MS = 10000

    /**
     * Fetch available model IDs from an OpenAI-compatible endpoint.
     * Must be called from a background thread.
     *
     * @param baseUrl e.g. "http://host:8080/v1" (with or without /chat/completions)
     * @return sorted list of model IDs; empty on any failure (caller shows fallback UI)
     */
    fun fetch(baseUrl: String, apiKey: String?): List<String> {
        if (baseUrl.isBlank()) return emptyList()
        val root = baseUrl.trimEnd('/').removeSuffix("/chat/completions").trimEnd('/')
        return try {
            val conn = (URL("$root/models").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                if (!apiKey.isNullOrBlank()) {
                    setRequestProperty("Authorization", "Bearer $apiKey")
                }
                setRequestProperty("Accept", "application/json")
            }
            try {
                val code = conn.responseCode
                if (code !in 200..299) {
                    Log.w(TAG, "GET $root/models -> HTTP $code")
                    return emptyList()
                }
                parse(conn.inputStream.bufferedReader().use { it.readText() })
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Model discovery failed for $root: ${e.message}")
            emptyList()
        }
    }

    /** Parse {"data":[{"id":...}]} or {"models":[{"id":...}]}/{"models":["id",...]}. */
    internal fun parse(body: String): List<String> {
        return try {
            val root = JSONObject(body)
            val ids = mutableListOf<String>()
            root.optJSONArray("data")?.let { arr ->
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.optString("id")
                        ?.takeIf { it.isNotBlank() }?.let { ids.add(it) }
                }
            }
            if (ids.isEmpty()) {
                root.optJSONArray("models")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        when (val el = arr.opt(i)) {
                            is JSONObject -> el.optString("id")
                                .takeIf { it.isNotBlank() }?.let { ids.add(it) }
                            is String -> if (el.isNotBlank()) ids.add(el)
                        }
                    }
                }
            }
            ids.distinct().sorted()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse /models response: ${e.message}")
            emptyList()
        }
    }
}
