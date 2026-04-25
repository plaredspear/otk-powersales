package com.otoki.powersales.sap.outbound.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration
import java.util.Base64

@Configuration
@EnableConfigurationProperties(SapOutboundProperties::class)
class SapOutboundRestClientConfig(
    private val properties: SapOutboundProperties
) {

    @Bean(name = ["sapOutboundRestClient"])
    fun sapOutboundRestClient(): RestClient {
        val requestFactory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(Duration.ofMillis(properties.connectTimeoutMs.toLong()))
            setReadTimeout(Duration.ofMillis(properties.readTimeoutMs.toLong()))
        }

        val builder = RestClient.builder()
            .requestFactory(requestFactory)
            .defaultHeader("Content-Type", "${MediaType.APPLICATION_JSON_VALUE}; charset=UTF-8")

        if (properties.baseUrl.isNotBlank()) {
            builder.baseUrl(properties.baseUrl)
        }

        if (properties.username.isNotBlank() || properties.password.isNotBlank()) {
            val token = Base64.getEncoder()
                .encodeToString("${properties.username}:${properties.password}".toByteArray(Charsets.UTF_8))
            builder.defaultHeader("Authorization", "Basic $token")
        }

        return builder.build()
    }
}
