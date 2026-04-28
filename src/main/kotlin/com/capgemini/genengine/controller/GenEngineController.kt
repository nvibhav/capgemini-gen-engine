package com.capgemini.genengine.controller

import com.capgemini.genengine.model.GenerativeRequest
import com.capgemini.genengine.model.GenerativeResponse
import com.capgemini.genengine.service.GenEngineRestService
import com.capgemini.genengine.service.GenEngineWebSocketService
import com.capgemini.genengine.streaming.StreamingEvent
import com.capgemini.genengine.streaming.withHeartbeat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.*
import java.time.Duration

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
    ): Flow<ServerSentEvent<String>> {
        val eventStream = withHeartbeat(
            webSocketService.streamTokens(request),
            interval = Duration.ofSeconds(10L)
        )
        return eventStream.map { event ->
            when (event) {
                is StreamingEvent.Token -> ServerSentEvent.builder(event.value)
                    .event("token")
                    .build()

                is StreamingEvent.Heartbeat -> ServerSentEvent.builder("ping")
                    .event("heartbeat")
                    .build()

                is StreamingEvent.Error -> ServerSentEvent.builder(event.message)
                    .event("error")
                    .build()

                is StreamingEvent.Done -> ServerSentEvent.builder("done")
                    .event("done")
                    .build()
            }
        }
    }

    /**
     * Get Available Models
     */
    @GetMapping("/models")
    suspend fun getModels(): List<String> {
        return restService.getAvailableModels()
    }
}
