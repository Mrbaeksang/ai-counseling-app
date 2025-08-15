package com.aicounseling.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class AiCounselingApplication

fun main(args: Array<String>) {
    runApplication<AiCounselingApplication>(*args)
}