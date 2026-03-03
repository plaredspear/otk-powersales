package com.otoki.internal.shelflife.exception

import com.otoki.internal.common.exception.BusinessException
import org.springframework.http.HttpStatus

class ShelfLifeNotFoundException : BusinessException(
    errorCode = "NOT_FOUND",
    message = "유통기한 데이터를 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

class ShelfLifeForbiddenException : BusinessException(
    errorCode = "FORBIDDEN",
    message = "권한이 없습니다",
    httpStatus = HttpStatus.FORBIDDEN
)

class InvalidAlertDateException : BusinessException(
    errorCode = "INVALID_ALERT_DATE",
    message = "알림일은 유통기한 이전이어야 합니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class InvalidShelfLifeDateRangeException(detail: String? = null) : BusinessException(
    errorCode = "INVALID_DATE_RANGE",
    message = detail ?: "날짜 범위가 올바르지 않습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)
