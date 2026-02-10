package com.otoki.internal.exception

import org.springframework.http.HttpStatus

/**
 * 잘못된 주문 파라미터
 */
class InvalidOrderParameterException(detail: String) : BusinessException(
    errorCode = "INVALID_PARAMETER",
    message = detail,
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 잘못된 납기일 범위
 */
class InvalidDateRangeException : BusinessException(
    errorCode = "INVALID_DATE_RANGE",
    message = "납기일 종료일은 시작일 이후여야 합니다",
    httpStatus = HttpStatus.BAD_REQUEST
)
