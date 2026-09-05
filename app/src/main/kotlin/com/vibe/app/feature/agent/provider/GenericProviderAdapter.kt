package com.vibe.app.feature.agent.provider

import com.vibe.app.data.database.entity.PlatformV2
import com.vibe.app.feature.agent.AgentModelEvent
import com.vibe.app.feature.agent.AgentModelRequest
import com.vibe.app.feature.agent.AgentModelEvent.*
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.HttpHeaders
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.system.measureTimeMillis

/**
 * Generic provider adapter that attempts common paths on a user-provided apiUrl.
 * This is a best-effort fallback for custom endpoints and vendors that follow
 * an OpenAI-like interface.
 */
class GenericProviderAdapter(
    private val httpClient: io.ktor.client.HttpClient,
    private val decryptToken: (String?) -> String?
) : ProviderAdapter {

    private val candidatePaths = listOf(
        "/v1/responses",
        "/v1/chat/completions",
        "/v1/completions",
        "/v1/chat/completions/stream",
        "" // raw base URL
    )

    override suspend fun testConnection(platform: PlatformV2): TestResult {
        val token = decryptToken(platform.token)
        var lastError: String? = null
        val latency = measureTimeMillis {
            for (path in candidatePaths) {
                val url = buildUrl(platform.apiUrl, path)
                try {
                    val payload = "{\"model\":\"${escape(platform.model)}\",\"input\":\"Say OK\"}"

                    val resp: HttpResponse = httpClient.post(url) {
                        header(HttpHeaders.Authorization, "Bearer $token")
                        contentType(ContentType.Application.Json)
                        setBody(payload)
                    }
                    val bodyText = resp.bodyAsText()
                    if (resp.status.value in 200..299) {
                        return TestResult(ok = true, info = bodyText.take(1024), latencyMs = latency)
                    } else {
                        lastError = "HTTP ${resp.status.value}: ${bodyText.take(1024)}"
                    }
                } catch (e: Exception) {
                    lastError = e.message ?: "unknown"
                }
            }
        }
        return TestResult(ok = false, info = lastError, latencyMs = latency)
    }

    override fun streamTurn(request: AgentModelRequest, platform: PlatformV2): Flow<AgentModelEvent> = flow {
        // Fallback: non-streaming request to first successful path
        val token = decryptToken(platform.token)
        val path = candidatePaths.first()
        val url = buildUrl(platform.apiUrl, path)
        try {
            val payload = "{\"model\":\"${escape(platform.model)}\",\"input\":\"${escape(request.instructions ?: "Say OK")}\"}"
            val resp: HttpResponse = httpClient.post(url) {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
            val body = resp.bodyAsText()
            emit(Completed(finalText = body, responseId = null, reasoningContent = null))
        } catch (e: Exception) {
            emit(Failed(message = e.message ?: "request failed"))
        }
    }

    private fun buildUrl(base: String, path: String): String {
        val b = base.trimEnd('/')
        return if (path.isBlank()) b else "$b$path"
    }

    private fun escape(s: String): String = s.replace("\"", "\\\"")
}
