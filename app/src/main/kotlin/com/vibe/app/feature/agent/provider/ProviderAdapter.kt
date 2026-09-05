package com.vibe.app.feature.agent.provider

import com.vibe.app.data.database.entity.PlatformV2
import com.vibe.app.feature.agent.AgentModelEvent
import com.vibe.app.feature.agent.AgentModelRequest
import kotlinx.coroutines.flow.Flow

/**
 * Result of a quick connectivity test for a provider.
 */
data class TestResult(val ok: Boolean, val info: String? = null, val latencyMs: Long? = null)

/**
 * Adapter abstraction for a model provider (OpenAI, Anthropic, Gemini, OpenRouter, etc.).
 */
interface ProviderAdapter {
    suspend fun testConnection(platform: PlatformV2): TestResult

    fun streamTurn(request: AgentModelRequest, platform: PlatformV2): Flow<AgentModelEvent>
}
