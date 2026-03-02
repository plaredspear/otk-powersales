package com.otoki.internal.order.exception

import com.otoki.internal.common.exception.BusinessException

import org.springframework.http.HttpStatus

/**
 * 거래처를 찾을 수 없음
 */
class ClientNotFoundException : BusinessException(
    errorCode = "CLIENT_NOT_FOUND",
    message = "거래처를 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

/**
 * 거래처 접근 권한 없음 (담당 거래처가 아닌 경우)
 */
class ForbiddenClientAccessException : BusinessException(
    errorCode = "FORBIDDEN",
    message = "접근 권한이 없습니다",
    httpStatus = HttpStatus.FORBIDDEN
)
