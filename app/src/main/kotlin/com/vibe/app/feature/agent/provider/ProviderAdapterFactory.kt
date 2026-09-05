package com.vibe.app.feature.agent.provider

import com.vibe.app.data.database.entity.PlatformV2
import com.vibe.app.data.model.ClientType
import com.vibe.app.data.network.NetworkClient

/**
 * Factory that returns a ProviderAdapter appropriate for the given platform.
 * New adapters should be added here and wired via DI if they need special dependencies.
 */
class ProviderAdapterFactory(
    private val networkClient: NetworkClient,
    private val decryptToken: (String?) -> String?
) {
    fun forPlatform(platform: PlatformV2): ProviderAdapter {
        val client = networkClient()
        return when (platform.compatibleType) {
            ClientType.OPENAI -> OpenAiProviderAdapter(client, decryptToken)
            ClientType.ANTHROPIC -> GenericProviderAdapter(client, decryptToken) // Replace with AnthropicAdapter when available
            ClientType.QWEN -> GenericProviderAdapter(client, decryptToken) // Replace with QwenAdapter
            ClientType.KIMI -> GenericProviderAdapter(client, decryptToken)
            ClientType.MINIMAX -> GenericProviderAdapter(client, decryptToken)
            ClientType.DEEPSEEK -> GenericProviderAdapter(client, decryptToken)
            ClientType.GEMINI -> GenericProviderAdapter(client, decryptToken) // Best-effort; may require special auth
            ClientType.OPENROUTER -> GenericProviderAdapter(client, decryptToken) // OpenRouter is often compatible
            ClientType.NVIDIA -> GenericProviderAdapter(client, decryptToken)
            ClientType.CUSTOM -> GenericProviderAdapter(client, decryptToken)
        }
    }
}
