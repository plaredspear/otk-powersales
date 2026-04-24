package com.otoki.powersales.productexpiration.exception

import com.otoki.powersales.common.exception.BusinessException
import org.springframework.http.HttpStatus

class ProductExpirationNotFoundException : BusinessException(
    errorCode = "PRODUCT_EXPIRATION_NOT_FOUND",
    message = "유통기한을 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

class ProductExpirationForbiddenException : BusinessException(
    errorCode = "PRODUCT_EXPIRATION_FORBIDDEN",
    message = "해당 유통기한에 대한 권한이 없습니다",
    httpStatus = HttpStatus.FORBIDDEN
)

class InvalidAlertDateException : BusinessException(
    errorCode = "INVALID_ALERT_DATE",
    message = "알림일은 유통기한 이전이어야 합니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class ProductExpirationAccountNotFoundException : BusinessException(
    errorCode = "ACCOUNT_NOT_FOUND",
    message = "거래처를 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

class InvalidProductExpirationDateRangeException(detail: String? = null) : BusinessException(
    errorCode = "INVALID_DATE_RANGE",
    message = detail ?: "날짜 범위가 올바르지 않습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)
