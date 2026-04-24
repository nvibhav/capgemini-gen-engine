package com.capgemini.genengine.model

import com.fasterxml.jackson.annotation.JsonProperty

// OpenAI-compatible REST API models
data class ChatCompletionRequest(
    val model: String = "anthropic.claude-sonnet-4-6",
    val messages: List<Message>,
    val temperature: Double? = 0.6,
    @JsonProperty("max_tokens") val maxTokens: Int? = 512,
    @JsonProperty("top_p") val topP: Double? = 0.9,
)

data class Message(
    val role: String,
    val content: String,
)

data class ChatCompletionResponse(
    @JsonProperty("id_") val id: String,
    val model: String,
    val choices: List<Choice>,
    val usage: Usage?,
)

data class Choice(
    val index: Int,
    val message: Message,
    @JsonProperty("finish_reason") val finishReason: String?,
)

data class Usage(
    @JsonProperty("prompt_tokens") val promptTokens: Int,
    @JsonProperty("completion_tokens") val completionTokens: Int,
    @JsonProperty("total_tokens") val totalTokens: Int,
)

data class ModelsListResponse(
    val data: List<ModelData>,
    @JsonProperty("object") val name: String,
)

data class ModelData(
    val id: String,
    @JsonProperty("object") val name: String,
    val created: Int,
    @JsonProperty("owned_by") val ownedBy: String,
)

// WebSocket API Models
data class WebSocketRequest(
    val action: String = "run",
    val modelInterface: String = "multimodal",
    val data: WebSocketData,
)

data class WebSocketData(
    val mode: String = "chain",
    val text: String,
    val modelName: String = "anthropic.claude-sonnet-4-6",
    val provider: String = "bedrock",
    val files: List<String> = emptyList(),
    val modelKwargs: ModelKwargs = ModelKwargs(),
)

data class ModelKwargs(
    val maxTokens: Int = 512,
    val temperature: Double = 0.6,
    val streaming: Boolean = true,
    val topP: Double = 0.9,
)

data class WebSocketResponse(
    val data: WebSocketResponseData?,
)

data class WebSocketResponseData(
    val content: String?
)

// Service request/response DTOs
data class GenerativeRequest(
    val prompt: String,
    val model: String? = null,
    val useStreaming: Boolean = false,
)

data class GenerativeResponse(
    val content: String,
    val model: String,
    val streaming: Boolean,
)
