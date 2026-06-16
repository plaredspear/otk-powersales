package com.otoki.powersales.external.sap.outbound.config

import com.otoki.powersales.external.common.outboundlog.ExternalApiLogInterceptor
import com.otoki.powersales.external.common.outboundlog.ExternalApiTarget
import com.otoki.powersales.external.common.outboundlog.service.ExternalApiLogService
import com.otoki.powersales.platform.common.util.TimeZones
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
    private val properties: SapOutboundProperties,
    private val externalApiLogService: ExternalApiLogService
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
        return restClientBuilder(sapOutboundObjectMapper).build()
    }

    /**
     * SAP outbound RestClient 의 converter / interceptor / 인증 헤더 구성을 한곳에 모은 builder.
     *
     * Bean 생성과 테스트(MockRestServiceServer bind)가 동일한 converter 체인을 공유하도록 별도 메서드로
     * 분리한다 — converter 구성 회귀는 빌드된 RestClient 가 아니라 builder 단계에서만 검증 가능하다.
     */
    internal fun restClientBuilder(sapOutboundObjectMapper: JsonMapper): RestClient.Builder {
        val factory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(Duration.ofMillis(properties.connectTimeoutMs.toLong()))
            setReadTimeout(Duration.ofMillis(properties.readTimeoutMs.toLong()))
        }

        val jsonConverter = JacksonJsonHttpMessageConverter(sapOutboundObjectMapper)

        val builder = RestClient.builder()
            .requestFactory(factory)
            .requestInterceptor(ExternalApiLogInterceptor(ExternalApiTarget.SAP, externalApiLogService))
            .configureMessageConverters { configurer ->
                // 기본 converter 체인을 등록(registerDefaults)한 뒤 JSON 슬롯만 SAP 전용 KST mapper
                // converter 로 교체한다. registerDefaults() 없이 withJsonConverter() 만 호출하면
                // 기본 체인이 비어 JSON 외 converter 가 모두 사라지고, RestClient body writer 의
                // converter 후보 탐색에서 `mapOf` 단일 엔트리가 `java.util.Collections$SingletonMap`
                // 으로 떨어지는 페이로드(SD03040 등)가 `No HttpMessageConverter for ... SingletonMap`
                // 으로 직렬화 실패한다.
                configurer.registerDefaults()
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

        return builder
    }
}
