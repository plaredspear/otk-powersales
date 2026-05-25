package com.otoki.powersales.common.naver

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Duration

/**
 * Naver Cloud Map Geocode API 클라이언트.
 *
 * cross-cutting 외부 어댑터 — Naver Geocode API 가 거래처 좌표 보강 외에도 (예: 주문 배송지 검증)
 * 향후 재사용 가능하므로 `common/naver/` 하위에 배치한다 (#637 §5).
 *
 * 호출:
 *   GET https://maps.apigw.ntruss.com/map-geocode/v2/geocode?query={URL-encoded address}
 *   Headers:
 *     X-NCP-APIGW-API-KEY-ID: ${NAVER_GEOCODE_CLIENT_ID}
 *     X-NCP-APIGW-API-KEY:    ${NAVER_GEOCODE_CLIENT_SECRET}
 *     Accept: application/json
 *
 * 에러 처리: 본 클래스는 예외를 throw 하지 않고 실패 시 `null` 을 반환한다 — 호출자가
 * 거래처별 배치 진행을 막지 않도록 (#637 §2.5 정책).
 */
@Component
class NaverGeocodeClient(
    @Value("\${app.naver.geocode.client-id:}") private val clientId: String,
    @Value("\${app.naver.geocode.client-secret:}") private val clientSecret: String
) {

    private val log = LoggerFactory.getLogger(NaverGeocodeClient::class.java)

    private val restClient: RestClient = run {
        val factory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(Duration.ofMillis(CONNECT_TIMEOUT_MS))
            setReadTimeout(Duration.ofMillis(READ_TIMEOUT_MS))
        }
        RestClient.builder()
            .requestFactory(factory)
            .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
            .build()
    }

    /**
     * 주소 문자열을 Naver Geocode API 로 변환한다.
     *
     * @return 응답 DTO. 호출 실패 / 응답 파싱 실패 시 `null`.
     */
    fun geocode(address: String): NaverGeocodeResponse? {
        val encoded = URLEncoder.encode(address, StandardCharsets.UTF_8)
        val uri = "$ENDPOINT?query=$encoded"
        return try {
            restClient.get()
                .uri(uri)
                .header("X-NCP-APIGW-API-KEY-ID", clientId)
                .header("X-NCP-APIGW-API-KEY", clientSecret)
                .retrieve()
                .body<NaverGeocodeResponse>()
        } catch (ex: Exception) {
            log.warn("Naver Geocode API 호출 실패 — address={} cause={}", address, ex.message)
            null
        }
    }

    /**
     * 주소 문자열을 Naver Geocode API 로 변환하여 원본 JSON 응답 본문을 그대로 반환한다.
     *
     * admin 변환 테스트 도구가 Naver 응답을 가공 없이 노출하기 위해 사용 (#638).
     *
     * @return 응답 본문 raw JSON 문자열. 호출 실패 시 `null`.
     */
    fun geocodeRaw(address: String): String? {
        val encoded = URLEncoder.encode(address, StandardCharsets.UTF_8)
        val uri = "$ENDPOINT?query=$encoded"
        return try {
            restClient.get()
                .uri(uri)
                .header("X-NCP-APIGW-API-KEY-ID", clientId)
                .header("X-NCP-APIGW-API-KEY", clientSecret)
                .retrieve()
                .body<String>()
        } catch (ex: Exception) {
            log.warn("Naver Geocode API 호출 실패 — address={} cause={}", address, ex.message)
            null
        }
    }

    companion object {
        private const val ENDPOINT = "https://maps.apigw.ntruss.com/map-geocode/v2/geocode"
        private const val CONNECT_TIMEOUT_MS = 3000L
        private const val READ_TIMEOUT_MS = 5000L
    }
}
