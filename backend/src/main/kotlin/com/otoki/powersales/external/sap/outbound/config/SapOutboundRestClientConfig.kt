package com.otoki.powersales.external.sap.outbound.config

import com.otoki.powersales.common.util.TimeZones
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.web.client.RestClient
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule
import java.time.Duration
import java.util.Base64
import java.util.TimeZone

@Configuration
@EnableConfigurationProperties(SapOutboundProperties::class)
class SapOutboundRestClientConfig(
    private val properties: SapOutboundProperties
) {

    /**
     * SAP outbound 전용 ObjectMapper — KST(Asia/Seoul) 직렬화로 격리.
     *
     * 전역 ObjectMapper 의 `jackson.time-zone` 은 신규 시스템의 wire format 정책(UTC)을 따르지만,
     * SAP 외부 시스템은 KST `+09:00` offset 을 기대하므로 본 RestClient 전용 mapper 가 별도로 처리한다.
     *
     * 현재 모든 SAP outbound DTO 의 datetime 필드는 String 으로 명시 변환되어 있어 본 mapper 가
     * 실제로 적용되는 경우는 없지만, 향후 DTO 에 `LocalDateTime` 필드가 추가되어도 자동으로 `+09:00`
     * offset 으로 직렬화되어 SAP wire format 호환성이 보장된다.
     */
    @Bean(name = ["sapOutboundObjectMapper"])
    fun sapOutboundObjectMapper(): JsonMapper {
        return JsonMapper.builder()
            .addModule(kotlinModule())
            .defaultTimeZone(TimeZone.getTimeZone(TimeZones.SEOUL_ZONE))
            .build()
    }

    @Bean(name = ["sapOutboundRestClient"])
    fun sapOutboundRestClient(sapOutboundObjectMapper: JsonMapper): RestClient {
        val factory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(Duration.ofMillis(properties.connectTimeoutMs.toLong()))
            setReadTimeout(Duration.ofMillis(properties.readTimeoutMs.toLong()))
        }

        val jsonConverter = JacksonJsonHttpMessageConverter(sapOutboundObjectMapper)

        val builder = RestClient.builder()
            .requestFactory(factory)
            .configureMessageConverters { configurer ->
                configurer.withJsonConverter(jsonConverter)
            }
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
