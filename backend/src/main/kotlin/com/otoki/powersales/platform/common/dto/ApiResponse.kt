package com.otoki.powersales.platform.common.dto

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDateTime

/**
 * 공통 API 응답 구조
 */
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ErrorDetail? = null,
    val message: String? = null,
    val timestamp: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        /**
         * 성공 응답 생성
         */
        fun <T> success(data: T, message: String? = null): ApiResponse<T> {
            return ApiResponse(
                success = true,
                data = data,
                message = message
            )
        }

        /**
         * 에러 응답 생성
         */
        fun <T> error(code: String, message: String): ApiResponse<T> {
            return ApiResponse(
                success = false,
                error = ErrorDetail(code, message)
            )
        }

        /**
         * 에러 응답 생성 (ErrorDetail 직접 전달)
         */
        fun <T> error(errorDetail: ErrorDetail): ApiResponse<T> {
            return ApiResponse(
                success = false,
                error = errorDetail
            )
        }
    }
}

/**
 * 에러 상세 정보
 *
 * `details` 는 도메인별 추가 메타(예: missing_ids 목록) 를 담는 옵션 필드다.
 * null 일 경우 응답 JSON 에서 키 자체가 누락된다 (`@JsonInclude(NON_NULL)`).
 */
data class ErrorDetail(
    val code: String,
    val message: String,
    @field:JsonInclude(JsonInclude.Include.NON_NULL)
    val details: Map<String, Any>? = null
)
