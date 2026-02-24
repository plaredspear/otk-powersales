package com.otoki.internal.shelflife.exception

import com.otoki.internal.common.exception.BusinessException

/* --- 전체 주석 처리: V1 Entity 리매핑 (Spec 77) ---
 * ShelfLifeService/Controller 비활성화로 예외 클래스도 주석 처리.
 * Service 재작성 시 함께 활성화.

import org.springframework.http.HttpStatus

class ShelfLifeNotFoundException : BusinessException(
    errorCode = "NOT_FOUND",
    message = "유통기한 정보를 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

class ShelfLifeForbiddenException : BusinessException(
    errorCode = "FORBIDDEN",
    message = "접근 권한이 없습니다",
    httpStatus = HttpStatus.FORBIDDEN
)

class InvalidAlertDateException : BusinessException(
    errorCode = "INVALID_ALERT_DATE",
    message = "알림 날짜는 유통기한 이전이어야 합니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class InvalidShelfLifeDateRangeException(detail: String) : BusinessException(
    errorCode = "INVALID_DATE_RANGE",
    message = detail,
    httpStatus = HttpStatus.BAD_REQUEST
)

class DuplicateShelfLifeException : BusinessException(
    errorCode = "DUPLICATE_ENTRY",
    message = "이미 등록된 유통기한입니다",
    httpStatus = HttpStatus.CONFLICT
)

class ShelfLifeStoreNotFoundException : BusinessException(
    errorCode = "STORE_NOT_FOUND",
    message = "거래처를 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

class ShelfLifeProductNotFoundException : BusinessException(
    errorCode = "PRODUCT_NOT_FOUND",
    message = "제품을 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

--- */
