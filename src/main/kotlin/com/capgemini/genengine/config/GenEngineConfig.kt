package com.capgemini.genengine.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@ConfigurationProperties(prefix = "gen-engine")
data class GenEngineProperties(
    var apiKey: String = "",
    var openaiBaseUrl: String = "https://openai.generative.engine.capgemini.com/v1",
    var websocketUrl: String = "wss://ws.generative.engine.capgemini.com"
)

@Configuration
class WebClientConfig(
    val properties: GenEngineProperties
) {
    @Bean
    fun webClient(): WebClient {
        return WebClient.builder()
            .baseUrl(properties.openaiBaseUrl)
            .defaultHeader(AUTHORIZATION, "Bearer ${properties.apiKey}")
            .defaultHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build()
    }

    @Bean
    fun objectMapper(): ObjectMapper {
        return ObjectMapper()
    }
}
