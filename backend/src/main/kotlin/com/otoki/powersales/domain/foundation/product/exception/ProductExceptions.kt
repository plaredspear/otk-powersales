package com.otoki.powersales.domain.foundation.product.exception

import com.otoki.powersales.common.exception.BusinessException

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

/**
 * 제품을 찾을 수 없음
 */
class ProductNotFoundException(productCode: String) : BusinessException(
    errorCode = "PRODUCT_NOT_FOUND",
    message = "제품을 찾을 수 없습니다: $productCode",
    httpStatus = HttpStatus.NOT_FOUND
)
