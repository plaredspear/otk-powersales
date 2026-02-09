package com.otoki.internal.dto

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
 */
data class ErrorDetail(
    val code: String,
    val message: String
)
