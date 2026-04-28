package com.capgemini.genengine.streaming

sealed interface StreamingEvent {
    data class Token(val value: String) : StreamingEvent
    data class Error(val message: String) : StreamingEvent
    data object Done : StreamingEvent
    data object Heartbeat : StreamingEvent
}
