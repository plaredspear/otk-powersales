package com.otoki.internal.exception

import org.springframework.http.HttpStatus

/**
 * 유통기한 정보를 찾을 수 없음
 */
class ShelfLifeNotFoundException : BusinessException(
    errorCode = "NOT_FOUND",
    message = "유통기한 정보를 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

/**
 * 유통기한 접근 권한 없음 (타인의 데이터)
 */
class ShelfLifeForbiddenException : BusinessException(
    errorCode = "FORBIDDEN",
    message = "접근 권한이 없습니다",
    httpStatus = HttpStatus.FORBIDDEN
)

/**
 * 알림 날짜가 유통기한 이전이 아님
 */
class InvalidAlertDateException : BusinessException(
    errorCode = "INVALID_ALERT_DATE",
    message = "알림 날짜는 유통기한 이전이어야 합니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 유효하지 않은 유통기한 조회 기간 (toDate < fromDate 또는 6개월 초과)
 */
class InvalidShelfLifeDateRangeException(detail: String) : BusinessException(
    errorCode = "INVALID_DATE_RANGE",
    message = detail,
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 동일 사용자+거래처+제품 중복 등록
 */
class DuplicateShelfLifeException : BusinessException(
    errorCode = "DUPLICATE_ENTRY",
    message = "이미 등록된 유통기한입니다",
    httpStatus = HttpStatus.CONFLICT
)

/**
 * 유통기한 등록 시 거래처를 찾을 수 없음
 */
class ShelfLifeStoreNotFoundException : BusinessException(
    errorCode = "STORE_NOT_FOUND",
    message = "거래처를 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

/**
 * 제품을 찾을 수 없음 (productCode로 조회 실패)
 */
class ShelfLifeProductNotFoundException : BusinessException(
    errorCode = "PRODUCT_NOT_FOUND",
    message = "제품을 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)
