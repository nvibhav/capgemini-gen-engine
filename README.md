# Capgemini GenEngine

A modern, reactive Java/Kotlin Spring Boot application that provides a unified API for generative AI completions. It supports multiple integration modes (synchronous REST, asynchronous WebSocket, and server-sent event streaming) with OpenAI-compatible and WebSocket-based generative AI backends.

## Overview

Capgemini GenEngine is a versatile generative AI integration layer designed to:
- Consume generative AI services through multiple protocols (REST and WebSocket)
- Expose a consistent, developer-friendly API for AI completions
- Support multiple streaming and non-streaming interaction patterns
- Provide real-time feedback with Server-Sent Events (SSE) and heartbeat mechanisms
- Handle resilience and error scenarios gracefully

## Features

### 🚀 Multiple Completion Modes
- **Synchronous**: Traditional request-response pattern for complete responses
- **Asynchronous**: WebSocket-based completion that aggregates streamed tokens
- **Streaming**: Real-time Server-Sent Events with automatic heartbeat support

### 🔄 Protocol Support
- **OpenAI-Compatible REST API**: Standard chat completions endpoint
- **WebSocket API**: Advanced multi-modal interactions with streaming responses
- **Server-Sent Events (SSE)**: Real-time event streaming to clients

### 🛡️ Production-Ready Features
- **Reactive/Non-blocking**: Built on Spring WebFlux and Kotlin coroutines
- **Distributed Tracing**: Micrometer tracing with Brave instrumentation
- **Health Monitoring**: Spring Actuator for application health and metrics
- **Circuit Breaker**: Resilience4j for fault tolerance
- **OpenAPI Documentation**: Interactive Swagger UI via SpringDoc

### ⚙️ Configuration
- Environment-based API key management
- Configurable backend URLs (REST and WebSocket)
- Debug logging for transparency
- Easy deployment via Spring Boot

## Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| **Language** | Kotlin | 2.3.20 |
| **Framework** | Spring Boot | 4.0.5 |
| **Async Runtime** | Project Reactor | Latest (via Spring Cloud) |
| **Coroutines** | Kotlinx Coroutines | Latest (via Spring Cloud) |
| **Cloud Features** | Spring Cloud | 2025.1.1 |
| **Tracing** | Micrometer + Brave | Latest (via Spring Boot) |
| **Circuit Breaking** | Resilience4j | Latest (via Spring Cloud) |
| **JSON Processing** | Jackson | Latest (via Spring Boot) |
| **Testing** | JUnit 5 + Kotest Extensions | Latest (via Spring Boot) |

## Project Structure

```
src/main/
├── kotlin/com/capgemini/genengine/
│   ├── CapgeminiGenEngineApplication.kt    # Spring Boot entry point
│   ├── controller/
│   │   └── GenEngineController.kt          # REST API endpoints
│   ├── service/
│   │   ├── GenEngineRestService.kt         # OpenAI-compatible REST client
│   │   └── GenEngineWebSocketService.kt    # WebSocket client & streaming
│   ├── config/
│   │   └── GenEngineConfig.kt              # Configuration & Spring beans
│   ├── model/
│   │   └── Models.kt                       # Data models & DTOs
│   └── streaming/
│       ├── StreamingEvent.kt               # Event types for streaming
│       └── HeartbeatFlow.kt                # Heartbeat logic for SSE
└── resources/
    ├── application.yaml                    # Application configuration
    └── banner.txt                          # Spring Boot banner

src/test/
├── kotlin/...                              # Unit & integration tests
```

## API Endpoints

### POST /api/v1/generative/completion/sync
Synchronous completion endpoint. Returns the complete response after the backend finishes generation.

**Request Body:**
```json
{
  "prompt": "Your prompt text here",
  "model": "anthropic.claude-sonnet-4-6",
  "useStreaming": false
}
```

**Response:**
```json
{
  "content": "Generated response text",
  "model": "anthropic.claude-sonnet-4-6",
  "streaming": false
}
```

### POST /api/v1/generative/completion/async
Asynchronous completion endpoint. Internally uses WebSocket streaming but aggregates the full response before returning.

**Request Body:** Same as `/sync`

**Response:** Same as `/sync` (with `streaming: true`)

### POST /api/v1/generative/completion/stream
Streaming completion endpoint using Server-Sent Events (SSE). Returns tokens in real-time as they are generated.

**Request Body:** Same as `/sync`

**Response:** Streaming response with events:
- `event: token` - Contains a generated token
- `event: heartbeat` - Periodic keep-alive signal (every 10 seconds)
- `event: error` - Error message if something fails
- `event: done` - Stream completion signal

**Example SSE Stream:**
```
data: "Hello"
event: token

data: " there"
event: token

data: "ping"
event: heartbeat

data: "done"
event: done
```

### GET /api/v1/generative/models
Retrieves the list of available models from the backend.

**Response:**
```json
[
  "anthropic.claude-sonnet-4-6",
  "anthropic.claude-haiku-4-5",
  "gpt-4"
]
```

## Configuration

Configuration is managed through environment variables and the `application.yaml` file.

### Environment Variables

```bash
# Required
export API_KEY="your-api-key-here"

# Optional (defaults provided in application.yaml)
export GEN_ENGINE_OPENAI_BASE_URL="https://openai.generative.engine.capgemini.com/v1"
export GEN_ENGINE_WEBSOCKET_URL="wss://ws.generative.engine.capgemini.com"
```

### Application Properties

Configuration properties in `application.yaml`:
```yaml
gen-engine:
  api-key: ${API_KEY}
  openai-base-url: https://openai.generative.engine.capgemini.com/v1
  websocket-url: wss://ws.generative.engine.capgemini.com

logging:
  level:
    com.capgemini.genengine: DEBUG
```

## Building and Running

### Prerequisites
- Java 24+ (Java 25 not yet supported by Kotlin 2.3.20)
- Gradle 8.0+ (included via Gradle Wrapper)

### Build
```bash
./gradlew build
```

### Run
```bash
# Set API key (required)
export API_KEY="your-api-key"

# Start the application
./gradlew bootRun

# Application will be available at http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
```

### Run Tests
```bash
./gradlew test
```

### Build Docker Image
```bash
./gradlew bootBuildImage
```

## Usage Examples

### Using cURL

#### Synchronous Request
```bash
curl -X POST http://localhost:8080/api/v1/generative/completion/sync \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "What is the capital of France?",
    "model": "anthropic.claude-sonnet-4-6"
  }'
```

#### Streaming Request
```bash
curl -X POST http://localhost:8080/api/v1/generative/completion/stream \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Write a short poem about spring"
  }'
```

### Using JavaScript/Fetch API

```javascript
// Streaming with EventSource pattern
const eventSource = new EventSource(
  'http://localhost:8080/api/v1/generative/completion/stream'
);

eventSource.addEventListener('token', (event) => {
  console.log('Token:', event.data);
});

eventSource.addEventListener('heartbeat', (event) => {
  console.log('Heartbeat received');
});

eventSource.addEventListener('done', (event) => {
  console.log('Stream completed');
  eventSource.close();
});

eventSource.addEventListener('error', (event) => {
  console.error('Stream error:', event.data);
  eventSource.close();
});
```

### Using Python

```python
import httpx
import json

async with httpx.AsyncClient() as client:
    async with client.stream(
        "POST",
        "http://localhost:8080/api/v1/generative/completion/stream",
        json={"prompt": "Hello, world!"},
    ) as response:
        async for line in response.aiter_lines():
            print(line)
```

## Architecture

### Request Flow

1. **REST Controller** receives the request
2. **Service Layer** routes to appropriate backend:
   - REST Service → OpenAI-compatible endpoint (for sync)
   - WebSocket Service → WebSocket endpoint (for async & streaming)
3. **Event Streaming** (for `/stream` endpoint):
   - Tokens are collected and wrapped in SSE events
   - Heartbeat events are injected automatically
   - Client receives real-time events
4. **Response** is returned with appropriate format

### Streaming Architecture

The streaming system uses:
- **Kotlin Coroutines Flows** for composable, non-blocking pipelines
- **Reactor Flux/Mono** underneath for reactive operations
- **Server-Sent Events** as the HTTP transport mechanism
- **Automatic Heartbeat** to keep connections alive and detect stale clients

## Monitoring & Observability

The application includes:

### Actuator Endpoints
- `/actuator/health` - Application health status
- `/actuator/metrics` - Application metrics
- `/actuator/env` - Environment properties
- `/actuator/loggers` - Logger configuration

### Distributed Tracing
Tracing is configured with Micrometer and Brave. Export traces to your observability platform:
- Jaeger
- Zipkin
- Cloud Trace
- Other Micrometer-compatible backends

### Logging
Default log level is DEBUG for `com.capgemini.genengine` package. Adjust in `application.yaml` as needed:
```yaml
logging:
  level:
    com.capgemini.genengine: INFO  # Change to INFO for less verbose output
```

## Error Handling

The application handles errors gracefully:

| Error Type | Behavior |
|-----------|----------|
| **WebSocket Connection Failure** | Returns `StreamingEvent.Error` in the event stream |
| **Malformed Response** | Logs error and continues processing (gracefully degrades) |
| **Timeout** | 30-second timeout per connection, returns error event |
| **Invalid Request** | HTTP 400 Bad Request with details |
| **Server Error** | HTTP 500 with error context |

## Performance Considerations

- **Non-blocking I/O**: All operations are async, no thread blocking
- **Connection Pooling**: WebClient uses connection pooling by default
- **Streaming**: Tokens are streamed immediately, no buffering delay
- **Heartbeat**: 10-second interval to detect stale connections without excessive overhead

## Contributing

1. Create a feature branch
2. Make your changes with clear commit messages
3. Ensure tests pass: `./gradlew test`
4. Submit a pull request

## License

Internal Capgemini project.

## Support

For issues or questions:
- Check the logs: `logging.level.com.capgemini.genengine: DEBUG` in `application.yaml`
- Review API endpoint documentation: `/swagger-ui.html` when running
- Inspect actuator endpoints: `http://localhost:8080/actuator`

## Deployment

### Spring Boot Deployment
Standard Spring Boot deployment options are supported:
- Traditional WAR to application servers
- Executable JAR with embedded server
- Docker containers (OCI image via `bootBuildImage`)
- Cloud platforms (Heroku, Cloud Run, App Engine, etc.)

### Environment Setup
Ensure these are set in your deployment environment:
- `API_KEY` - Your authentication token for the backend service
- Port exposure (default: 8080)
- Sufficient memory for reactive streams (default: 512MB)

## Roadmap

Potential enhancements:
- [ ] Rate limiting per API key
- [ ] Request/response caching
- [ ] Model-specific parameter validation
- [ ] Advanced authentication (OAuth2, mTLS)
- [ ] Batch processing endpoints
- [ ] Request history/audit logging
