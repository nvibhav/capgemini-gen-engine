package com.capgemini.genengine.streaming

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import java.time.Duration
import kotlin.time.Duration.Companion.milliseconds

fun withHeartbeat(
    stream: Flow<StreamingEvent>,
    interval: Duration = Duration.ofSeconds(10L)
): Flow<StreamingEvent> = channelFlow {
    // Job collecting the main stream
    val mainJob = launch {
        stream.collect { event ->
            send(event)

            // Stops everything on Done
            if (event is StreamingEvent.Done) {
                close()
            }
        }
    }

    // Job emitting heartbeats
    val heartbeatJob = launch {
        while (true) {
            delay(interval.toMillis().milliseconds)
            send(StreamingEvent.Heartbeat)
        }
    }

    awaitClose {
        // Ensure both jobs are cancelled cleanly
        mainJob.cancel()
        heartbeatJob.cancel()
    }
}
