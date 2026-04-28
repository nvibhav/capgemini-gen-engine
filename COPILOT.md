# Capgemini GenEngine - Copilot Guide

This document provides guidance for AI agents (Copilot) working on the Capgemini GenEngine codebase. It covers project structure, design patterns, conventions, and common development tasks.

## Project Context

**Project Name:** Capgemini GenEngine

**Purpose:** A reactive Spring Boot application that provides a unified API for generative AI completions, supporting multiple protocols (REST, WebSocket) and interaction modes (sync, async, streaming).

**Key Technologies:**
- Kotlin 2.3.20 with Spring Boot 4.0.5
- Spring WebFlux for reactive, non-blocking I/O
- Kotlin Coroutines for structured concurrency
- Spring Cloud 2025.1.1 for distributed features
- Project Reactor (Flux/Mono) underneath WebFlux

**JVM Target:** Java 24 (Java 25 not yet supported by Kotlin)

## Codebase Structure

### Core Modules

```
src/main/kotlin/com/capgemini/genengine/
│
├── CapgeminiGenEngineApplication.kt
│   └── Spring Boot entry point with @SpringBootApplication and @ConfigurationPropertiesScan
│
├── controller/GenEngineController.kt
│   └── REST API endpoints for three completion modes + models endpoint
│       ├── POST /api/v1/generative/completion/sync
│       ├── POST /api/v1/generative/completion/async
│       ├── POST /api/v1/generative/completion/stream (SSE)
│       └── GET /api/v1/generative/models
│
├── service/
│   ├── GenEngineRestService.kt
│   │   ├── generateCompletion(request) - OpenAI-compatible REST API calls
│   │   └── getAvailableModels() - Fetch available models
│   │
│   └── GenEngineWebSocketService.kt
│       ├── streamTokens(request) - Returns Flow<StreamingEvent> from WebSocket
│       └── generateCompletionFromWebSocket(request) - Aggregates stream into full response
│
├── config/GenEngineConfig.kt
│   ├── GenEngineProperties - Configuration properties (apiKey, URLs)
│   └── WebClientConfig - WebClient bean setup with headers and base URL
│
├── model/Models.kt
│   ├── OpenAI-compatible models: ChatCompletionRequest, ChatCompletionResponse, etc.
│   ├── WebSocket models: WebSocketRequest, WebSocketData, WebSocketResponse
│   └── Service DTOs: GenerativeRequest, GenerativeResponse
│
└── streaming/
    ├── StreamingEvent.kt - Sealed interface for streaming events (Token, Error, Done, Heartbeat)
    └── HeartbeatFlow.kt - Heartbeat logic and event stream composition
```

### Configuration & Resources

```
src/main/resources/
├── application.yaml
│   ├── Spring application name
│   ├── gen-engine properties (API key, URLs)
│   └── Logging configuration
│
└── banner.txt - Spring Boot startup banner
```

### Testing

```
src/test/kotlin/... - Unit and integration tests
```

## Key Design Patterns

### 1. **Reactive Streams with Kotlin Flow**
- Services return `Flow<T>` or `suspend fun` for async operations
- Controller methods are `suspend fun` to leverage coroutine benefits
- WebSocket streaming uses `Flow<StreamingEvent>` with Reactor underneath

**Example:**
```kotlin
fun streamTokens(request: GenerativeRequest): Flow<StreamingEvent> = flow {
    // Implementation returns Flow for reactive, composable operations
}

// Controller uses suspend fun
suspend fun generateStreamingCompletion(
    @RequestBody request: GenerativeRequest
): Flow<ServerSentEvent<String>> { }
```

### 2. **Configuration Properties**
- Properties are injected via `@ConfigurationProperties` annotation
- Loaded from environment variables via `${VAR_NAME}` syntax
- Data class with mutable vars for Spring binding

**Example:**
```kotlin
@ConfigurationProperties(prefix = "gen-engine")
data class GenEngineProperties(
    var apiKey: String = "",
    var openaiBaseUrl: String = "...",
    var websocketUrl: String = "..."
)
```

### 3. **Service Layer Pattern**
- `RestService` for OpenAI-compatible REST calls
- `WebSocketService` for WebSocket communication
- Both return consistent response models via DTOs

### 4. **Event-Driven Streaming**
- Three types of backend events: Token, Error, Done
- Heartbeat events injected by controller for keep-alive
- SSE transport with event types: `token`, `heartbeat`, `error`, `done`

### 5. **Error Handling**
- WebSocket errors emit `StreamingEvent.Error` instead of throwing
- Timeouts handled with `.timeout(Duration)` operator
- Malformed responses logged and gracefully ignored (no hard failures)

## Important Conventions

### Naming Conventions
- **Services:** `<Feature>Service.kt` (e.g., `GenEngineRestService`)
- **Controllers:** `<Feature>Controller.kt` (e.g., `GenEngineController`)
- **Data Models:** Domain objects in `model/Models.kt` grouped by concern
- **Config:** `<Feature>Config.kt` or properties data classes
- **Packages:** `com.capgemini.genengine.<layer>.<feature>`

### API Versioning
- Current API version: `/api/v1/generative`
- All endpoints follow this base path
- Request/response models use semantic field names (no abbreviations)

### Logging
- Use SLF4J with LoggerFactory
- Structured logging with `.atDebug().log(message, args)` pattern
- Log level: DEBUG for development, adjust in `application.yaml`

**Example:**
```kotlin
private val log = LoggerFactory.getLogger(GenEngineRestService::class.java)
log.atDebug().log("Request to OpenAI endpoint: {}", chatRequest)
```

### Resource Management
- WebSocket connections closed gracefully via `onCancel {}` handlers
- No manual connection cleanup needed (Reactor handles it)
- Cancellation observed and logged for debugging

## Building & Testing

### Build Project
```bash
./gradlew build
```

### Run Tests
```bash
./gradlew test
```

### Run Application
```bash
export API_KEY="your-key"
./gradlew bootRun
```

### Build Docker Image
```bash
./gradlew bootBuildImage
```

## Adding New Features

### Adding a New Completion Endpoint

1. **Add endpoint to controller:**
   ```kotlin
   @PostMapping("/completion/my-mode")
   suspend fun generateMyModeCompletion(
       @RequestBody request: GenerativeRequest
   ): GenerativeResponse { }
   ```

2. **Add service method:**
   ```kotlin
   suspend fun generateMyMode(request: GenerativeRequest): GenerativeResponse { }
   ```

3. **Follow existing patterns:**
   - Use `suspend fun` for async operations
   - Return DTOs (GenerativeRequest/GenerativeResponse)
   - Handle errors gracefully with logging
   - Document endpoint with comments

### Adding a New Configuration Property

1. **Add to GenEngineProperties:**
   ```kotlin
   var myNewProperty: String = "default-value"
   ```

2. **Use in application.yaml:**
   ```yaml
   gen-engine:
     my-new-property: ${MY_NEW_PROPERTY}
   ```

3. **Inject and use:**
   ```kotlin
   class MyService(private val properties: GenEngineProperties) {
       fun doSomething() {
           val value = properties.myNewProperty
       }
   }
   ```

### Adding a New Streaming Event Type

1. **Add to StreamingEvent sealed interface:**
   ```kotlin
   sealed interface StreamingEvent {
       data class MyEvent(val data: String) : StreamingEvent
       // ... other cases
   }
   ```

2. **Handle in stream processing:**
   ```kotlin
   // In service that emits events
   emit(StreamingEvent.MyEvent("data"))
   
   // In controller that maps events
   is StreamingEvent.MyEvent -> ServerSentEvent.builder(event.data)
       .event("my-event")
       .build()
   ```

## Testing Guidelines

### Unit Tests
- Test service methods in isolation
- Mock WebClient and WebSocket responses
- Verify logging calls
- Test error paths

### Integration Tests
- Test full request-response flow
- Use `@WebFluxTest` or `@SpringBootTest`
- Mock external services
- Verify SSE event order

### Key Testing Areas
1. **REST Service** - ChatCompletionResponse parsing, models endpoint
2. **WebSocket Service** - Token streaming, error handling, timeouts
3. **Controller** - Endpoint mapping, request validation, response format
4. **Streaming** - Event order, heartbeat injection, cancellation

## Common Tasks for Agents

### Task: Debug a Streaming Request
1. Check logs in `GenEngineWebSocketService.streamTokens()`
2. Verify WebSocket URL in properties
3. Check API key format and authorization header
4. Review error events in the SSE stream
5. Check timeout (30 seconds) if stream stops abruptly

### Task: Add Support for a New Model
1. Update default model in `GenerativeRequest.model` if needed
2. Test with `GenEngineRestService.getAvailableModels()` to verify availability
3. Document new model in README
4. No code changes needed - already model-agnostic

### Task: Improve Error Handling
1. Review `GenEngineWebSocketService` error handling:
   - `.onErrorResume()` catches timeout/connection errors
   - Malformed JSON caught in `.handle<StreamingEvent>()` block
   - Consider emitting `StreamingEvent.Error` vs. silent ignore
2. Add specific error types as `StreamingEvent` subclasses if needed
3. Update logging to capture error context

### Task: Add Request Validation
1. Add `@Valid` and `@Constraints` to DTOs in `Models.kt`
2. Spring will validate before reaching controller
3. Add `data class constructor annotations:
   ```kotlin
   data class GenerativeRequest(
       @NotBlank val prompt: String,
       val model: String? = null,
       val useStreaming: Boolean = false,
   )
   ```

### Task: Add Metrics/Monitoring
1. Inject `MeterRegistry` from Micrometer
2. Use counter/timer APIs:
   ```kotlin
   meterRegistry.counter("genengine.completions", "type", "sync").increment()
   ```
3. Metrics appear in `/actuator/metrics`

### Task: Handle Backpressure
1. Streams automatically handle backpressure via Reactor
2. If needed, add `.onBackpressureBuffer()` or `.onBackpressureLatest()`
3. WebSocket handles client disconnection via `onCancel{}`

## Dependencies to Know

### Core Spring Boot
- `spring-boot-starter-webflux` - Reactive web framework
- `spring-boot-starter-actuator` - Health, metrics endpoints
- `spring-boot-devtools` - Hot reload during development

### Reactive & Async
- `reactor-kotlin-extensions` - Kotlin extensions for Project Reactor
- `kotlinx-coroutines-reactor` - Coroutines integration with Reactor

### Cloud & Resilience
- `spring-cloud-starter-circuitbreaker-reactor-resilience4j` - Circuit breaker pattern
- `spring-boot-micrometer-tracing-brave` - Distributed tracing

### API & Docs
- `springdoc-openapi-starter-webflux-ui` - Swagger/OpenAPI UI

### JSON Processing
- `jackson-module-kotlin` - Kotlin serialization support

## Important Considerations

### Kotlin-Specific
- Use `data class` for immutable models
- Prefer `suspend fun` for async operations in Spring context
- Use `Flow` for reactive sequences (not `Mono`/`Flux` directly in business logic)
- Sealed interfaces for type-safe pattern matching (like `StreamingEvent`)

### Spring WebFlux Specific
- All operations are non-blocking; never use `.block()` or `.blockingGet()`
- Return reactive types (`Mono`, `Flux`, `Flow`, `suspend fun`)
- Timeouts configured at operation level, not thread level

### WebSocket Specific
- Connections are managed by the WebSocket client
- Always observe cancellation to clean up resources
- Handle partial messages gracefully (JSON parsing errors)
- Implement keep-alive via heartbeats to detect stale connections

### Configuration
- Sensitive values (API_KEY) come from environment, not code
- Properties are not reloaded at runtime; restart required for changes
- Prefix `gen-engine:` used for all application-specific properties

## File Organization Principles

1. **One logical concept per file** (mostly followed; Models.kt is exception by necessity)
2. **Package by feature** (`controller`, `service`, `config`, `model`, `streaming`)
3. **Test structure mirrors source** (`src/test` mirrors `src/main` structure)
4. **Configuration separate from business logic** (`config` package isolated)
5. **DTOs grouped by protocol** (OpenAI models vs. WebSocket models vs. Service DTOs)

## Documentation Standards

### Code Comments
- Document complex logic (e.g., retry backoff, timeout values)
- Explain "why", not "what" (code shows what)
- Document assumptions (e.g., "Assumes API returns one choice")
- Mark TODOs/FIXMEs clearly for future work

### JavaDoc/KDoc
- Document public methods with intent and parameters
- Example: GenEngineController methods have KDoc for each endpoint
- Document exceptions/errors that can occur

### Inline Guidance
- Use `IMPORTANT:` prefix for critical information
- Explain non-obvious side effects

## Version Management

- **Spring Boot:** 4.0.5 (latest major version)
- **Kotlin:** 2.3.20 (Java 25 not supported yet)
- **Spring Cloud:** 2025.1.1
- **Dependencies:** Managed via Gradle dependency management plugin

Always check compatibility when upgrading dependencies.

## Quick Reference

| Task | How To |
|------|--------|
| Add endpoint | Add method to controller, implement in service, follow suspend pattern |
| Change API behavior | Modify service logic, maintain DTO contracts |
| Add logging | Use `LoggerFactory.getLogger()` and structured logging |
| Handle errors | Use `.onErrorResume()` or emit `StreamingEvent.Error` |
| Test code | Use `@SpringBootTest` or `@WebFluxTest` with mocks |
| Deploy | Use `./gradlew bootBuildImage` for Docker |
| Monitor app | Check `/actuator/health` and `/actuator/metrics` |
| Debug stream | Enable DEBUG logging, check timestamps and event order |

## Related Documentation

- See `README.md` for user-facing documentation
- See `HELP.md` for Spring Boot generated reference links
- See source code for implementation details and patterns in use

## For More Help

- Review existing service implementations for patterns
- Check Spring WebFlux and Kotlin Coroutines documentation
- Look at error handling patterns in GenEngineWebSocketService
- Refer to test files for integration test examples
