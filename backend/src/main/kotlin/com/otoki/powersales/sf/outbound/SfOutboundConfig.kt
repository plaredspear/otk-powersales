package com.otoki.powersales.sf.outbound

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration

/**
 * SF outbound 인프라 Configuration — Spec #829.
 *
 * - [SfOutboundProperties] 활성화 (sf.outbound.* prefix)
 * - SF outbound 호출 전용 [RestClient] 빈 등록 (`sfOutboundRestClient`).
 *   * connect timeout 5s, read timeout 30s (Heroku ApiService 의 120s 보다 짧게 — 사용자가 admin 화면에서 기다리는 시간 단축).
 *   * Base URL / Auth 헤더는 호출 시점에 동적으로 부착 (Token 캐시 invalidate 시 헤더 재설정 필요).
 */
@Configuration
@EnableConfigurationProperties(SfOutboundProperties::class)
class SfOutboundConfig {

    @Bean(name = ["sfOutboundRestClient"])
    fun sfOutboundRestClient(): RestClient {
        val factory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(Duration.ofSeconds(5))
            setReadTimeout(Duration.ofSeconds(30))
        }
        return RestClient.builder()
            .requestFactory(factory)
            .build()
    }
}
