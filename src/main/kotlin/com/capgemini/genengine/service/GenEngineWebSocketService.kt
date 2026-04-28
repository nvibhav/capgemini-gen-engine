package com.capgemini.genengine.service

import com.capgemini.genengine.config.GenEngineProperties
import com.capgemini.genengine.model.*
import com.capgemini.genengine.streaming.StreamingEvent
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactive.asFlow
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.socket.WebSocketMessage
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
    private val log = LoggerFactory.getLogger(GenEngineWebSocketService::class.java)
    private val client = ReactorNettyWebSocketClient()

    companion object {
        private val STREAM_IDLE_TIMEOUT: Duration = Duration.ofSeconds(30L)
        private val RETRY_FIRST_BACKOFF: Duration = Duration.ofSeconds(2L)
        private val RETRY_MAX_BACKOFF: Duration = Duration.ofSeconds(10L)
        private const val MAX_RETRIES: Long = 3L
    }

    /**
     * Stream responses from the WebSocket API
     * with retry/backoff resilience and graceful lifecycle semantics.
     */

    fun streamTokens(request: GenerativeRequest): Flow<StreamingEvent> = flow {
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

        val flux: Flux<StreamingEvent> = Flux.create { outerSink ->
            // IMPORTANT: observe cancellation
            outerSink.onCancel {
                log.atInfo().log("Client cancelled stream, websocket will close")
            }
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
                    .handle<StreamingEvent> { message: WebSocketMessage, innerSink ->
                        try {
                            val node = objectMapper.readTree(message.payloadAsText)
                            val content = node.path("data").path("content").asText(null)
                            if (!content.isNullOrBlank()) {
                                innerSink.next(StreamingEvent.Token(content))
                            }
                        } catch (e: Exception) {
                            log.atError().log("Error parsing response: {}", e.message)
                            // Ignore malformed payload OR emit an Error event if you prefer
                        }
                    }
                    .timeout(Duration.ofSeconds(30L))
                    .onErrorResume { e ->
                        Mono.just(
                            StreamingEvent.Error(
                                e.message ?: "WebSocket stream failed"
                            )
                        )
                    }
                    .concatWithValues(StreamingEvent.Done)
                    .doOnNext(outerSink::next)
                    .doOnComplete(outerSink::complete)
                send.thenMany(receive).then()
            }.subscribe(
                { /* no-op */ },
                { e ->
                    log.atError().log("WebSocket execution failed.", e)
                    outerSink.next(
                        StreamingEvent.Error(
                            e.message ?: "WebSocket execution failed."
                        )
                    )
                    outerSink.next(StreamingEvent.Done)
                    outerSink.complete()
                }
            )
        }

        flux.asFlow().collect { emit(it) }
    }

    suspend fun generateCompletionFromWebSocket(request: GenerativeRequest): GenerativeResponse {
        val content = buildString {
            streamTokens(request).collect { event ->
                when (event) {
                    is StreamingEvent.Token -> append(event.value)
                    is StreamingEvent.Error -> log.atWarn()
                        .log("Streaming error while building full response: {}", event.message)

                    is StreamingEvent.Heartbeat -> { /* no-op */
                    }

                    is StreamingEvent.Done -> { /* no-op */
                    }
                }
            }
        }

        return GenerativeResponse(
            content = content,
            model = request.model ?: "anthropic.claude-sonnet-4-6",
            streaming = true
        )
    }
}
