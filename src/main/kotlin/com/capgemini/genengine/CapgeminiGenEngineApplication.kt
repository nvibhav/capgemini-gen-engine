package com.capgemini.genengine

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class CapgeminiGenEngineApplication

fun main(args: Array<String>) {
    runApplication<CapgeminiGenEngineApplication>(*args)
}
