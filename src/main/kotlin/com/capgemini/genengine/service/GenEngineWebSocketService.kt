package com.capgemini.genengine.service

import com.capgemini.genengine.config.GenEngineProperties
import com.capgemini.genengine.model.*
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactive.asFlow
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.net.URI
import java.time.Duration

const val X_API_KEY = "x-api-key"

@Service
class GenEngineWebSocketService(
    private val properties: GenEngineProperties,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(GenEngineWebSocketService::class.java)
    private val client = ReactorNettyWebSocketClient()

    /**
     * Stream responses from the WebSocket API
     * Asynchronous call with streaming support
     */
    fun generateStreamingCompletion(request: GenerativeRequest): Flow<String> = flow {
        val wsRequest = WebSocketRequest(
            data = WebSocketData(
                text = request.prompt,
                modelName = request.model ?: "anthropic.claude-sonnet-4-6",
                modelKwargs = ModelKwargs(streaming = true),
            )
        )

        val uri = URI.create(properties.websocketUrl)
        val headers = HttpHeaders().apply {
            set(X_API_KEY, properties.apiKey)
        }

        val flux = Flux.create { sink ->
            client.execute(uri, headers) { session ->
                // Send initial request
                val send = session.send(
                    Mono.just(
                        session.textMessage(
                            objectMapper.writeValueAsString(wsRequest)
                        )
                    )
                )

                // Received streamed message
                val receive = session.receive()
                    .map { message ->
                        try {
                            val node = objectMapper.readTree(message.payloadAsText)
                            node.path("data").path("content").asText("")
                        } catch (e: Exception) {
                            logger.atError().log("Error parsing response: ${e.message}")
                            "Error parsing response: ${e.message}"
                        }
                    }
                    .filter { it.isNotBlank() }
                    .map { it }
                    .timeout(Duration.ofSeconds(30L), Flux.empty())
                    .doOnNext { sink.next(it) }
                    .doOnError { sink.error(it) }
                    .doOnComplete { sink.complete() }
                send.thenMany(receive).then()
            }.subscribe()
        }

        flux.asFlow().collect { emit(it) }
    }

    suspend fun generateCompletionFromWebSocket(request: GenerativeRequest): GenerativeResponse {
        val content = buildString {
            generateStreamingCompletion(request).collect { append(it) }
        }

        return GenerativeResponse(
            content = content,
            model = request.model ?: "anthropic.claude-sonnet-4-6",
            streaming = true
        )
    }
}
