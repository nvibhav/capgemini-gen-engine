package com.capgemini.genengine.controller

import com.capgemini.genengine.model.GenerativeRequest
import com.capgemini.genengine.model.GenerativeResponse
import com.capgemini.genengine.service.GenEngineRestService
import com.capgemini.genengine.service.GenEngineWebSocketService
import kotlinx.coroutines.flow.Flow
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/generative")
class GenEngineController(
    private val restService: GenEngineRestService,
    private val webSocketService: GenEngineWebSocketService,
) {
    /**
     * Synchronous endpoint using REST API
     * Returns complete response at once
     */
    @PostMapping("/completion/sync")
    suspend fun generateSyncCompletion(
        @RequestBody request: GenerativeRequest
    ): GenerativeResponse {
        return restService.generateCompletion(request)
    }

    /**
     * Asynchronous endpoint using WebSocket API
     * Returns complete response after streaming
     */
    @PostMapping("/completion/async")
    suspend fun generateAsyncCompletion(
        @RequestBody request: GenerativeRequest
    ): GenerativeResponse {
        return webSocketService.generateCompletionFromWebSocket(request)
    }

    /**
     * Streaming endpoint using WebSocket API
     * Returns Server-Sent Events stream
     */
    @PostMapping("/completion/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    suspend fun generateStreamingCompletion(
        @RequestBody request: GenerativeRequest
    ): Flow<String> {
        return webSocketService.generateStreamingCompletion(request)
    }

    /**
     * Get Available Models
     */
    @GetMapping("/models")
    suspend fun getModels(): List<String> {
        return restService.getAvailableModels()
    }
}
