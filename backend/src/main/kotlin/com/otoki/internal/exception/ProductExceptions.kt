package com.otoki.internal.exception

import org.springframework.http.HttpStatus

/**
 * 유효하지 않은 검색 파라미터
 */
class InvalidSearchParameterException(detail: String) : BusinessException(
    errorCode = "INVALID_PARAMETER",
    message = detail,
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 유효하지 않은 검색 유형
 */
class InvalidSearchTypeException : BusinessException(
    errorCode = "INVALID_SEARCH_TYPE",
    message = "유효하지 않은 검색 유형입니다",
    httpStatus = HttpStatus.BAD_REQUEST
)
