package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.request.NaverGeocodeTestRequest
import com.otoki.powersales.admin.dto.response.NaverGeocodeTestResponse
import com.otoki.powersales.platform.common.naver.NaverApiException
import com.otoki.powersales.platform.common.naver.NaverGeocodeClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Naver Geocode 변환 테스트 서비스 (Spec #638 P1-B — 신규 도입, 레거시 미존재).
 *
 * ## 레거시 매핑
 * - 레거시 SF Apex `Batch_AccountLatLong.cls` 는 batch 컨텍스트에서만 Naver API 호출 — 단건 변환 도구 부재
 * - origin spec: #638
 *
 * ## 신규 도입 동작
 * 1. 입력: 주소 문자열 1건 (도로명/지번, 1~200자)
 * 2. `NaverGeocodeClient.geocodeRaw(address)` 호출 → Naver API 응답 본문을 raw JSON 문자열 그대로 반환
 * 3. client 가 null 반환 시 (5xx / timeout / 네트워크 오류) → `NaverApiException` throw → GlobalExceptionHandler 가 502 + `NAVER_GEOCODE_API_FAILED` 응답 매핑
 * 4. INFO 로그: `NAVER_GEOCODE_TEST user={} address={} responseLength={}` — 운영자 식별 + 입력 + 응답 길이 (CloudWatch 검색 가능)
 *
 * ## 신규 차이 — 동등 (생략, 레거시 미존재)
 *
 * 거래처 데이터 변경 없음 (read-only 외부 호출). 운영 도구 성격상 별도 audit 적재 없이 slf4j INFO 로그만 적용
 * — `legacy-deviation.md §7 시스템·인프라` (#637 도입 정책) 와 동일 패턴.
 */
@Service
class AdminNaverGeocodeService(
    private val naverGeocodeClient: NaverGeocodeClient
) {

    private val log = LoggerFactory.getLogger(AdminNaverGeocodeService::class.java)

    fun test(userId: Long, request: NaverGeocodeTestRequest): NaverGeocodeTestResponse {
        val rawResponse = naverGeocodeClient.geocodeRaw(request.address)
            ?: throw NaverApiException()

        log.info(
            "NAVER_GEOCODE_TEST user={} address={} responseLength={}",
            userId, request.address, rawResponse.length
        )

        return NaverGeocodeTestResponse(
            input = request.address,
            rawResponse = rawResponse
        )
    }
}
