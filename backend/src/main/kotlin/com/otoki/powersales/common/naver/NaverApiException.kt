package com.otoki.powersales.common.naver

import com.otoki.powersales.common.exception.BusinessException
import org.springframework.http.HttpStatus

/**
 * Naver Cloud Map Geocode API 호출 실패 예외 (Spec #638 P1-B).
 *
 * 외부 API 5xx / 타임아웃 / 네트워크 오류를 단일 코드 `NAVER_GEOCODE_API_FAILED` + HTTP 502 로
 * 통합 매핑한다. `BusinessException` 상속으로 `GlobalExceptionHandler` 가 자동 처리.
 *
 * `common/naver/` 패키지에 둠 — Naver Geocode API cross-cutting 외부 어댑터 컨벤션 정합.
 */
class NaverApiException(
    cause: Throwable? = null
) : BusinessException(
    errorCode = "NAVER_GEOCODE_API_FAILED",
    message = "Naver Geocode API 호출 실패",
    httpStatus = HttpStatus.BAD_GATEWAY,
    cause = cause
)
