package com.capgemini.genengine.service

import com.capgemini.genengine.model.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@Service
class GenEngineRestService(
    private val webClient: WebClient,
) {
    private val log = LoggerFactory.getLogger(GenEngineRestService::class.java)

    /**
     * Synchronous call to OpenAI-compatible completions endpoint
     * Uses REST API for non-streaming responses
     */
    suspend fun generateCompletion(request: GenerativeRequest): GenerativeResponse {
        val chatRequest = ChatCompletionRequest(
            model = request.model ?: "anthropic.claude-sonnet-4-6",
            messages = listOf(
                Message(
                    role = "user",
                    content = request.prompt
                ),
            ),
        )

        log.atDebug().log("Request to OpenAI endpoint: {}", chatRequest)

        val response = webClient.post()
            .uri("/chat/completions")
            .bodyValue(chatRequest)
            .retrieve()
            .awaitBody<ChatCompletionResponse>()

        log.atDebug().log("Response from OpenAI endpoint: {}", response)

        return GenerativeResponse(
            content = response.choices.firstOrNull()?.message?.content ?: "",
            model = response.model,
            streaming = false,
        )
    }

    /**
     * Gets available models from the API
     */
    suspend fun getAvailableModels(): List<String> {
        val response = webClient.get()
            .uri("/models")
            .retrieve()
            .awaitBody<ModelsListResponse>()

        log.atDebug().log("Response from OpenAI endpoint: {}", response)

        return response.data.stream()
            .map { modelData -> modelData.id }
            .toList()
    }
}
