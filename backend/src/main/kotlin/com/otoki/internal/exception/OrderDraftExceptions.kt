package com.otoki.internal.exception

import org.springframework.http.HttpStatus

/**
 * 임시 저장된 주문서를 찾을 수 없음
 */
class DraftNotFoundException : BusinessException(
    errorCode = "DRAFT_NOT_FOUND",
    message = "임시 저장된 주문서가 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

/**
 * 거래처를 찾을 수 없음
 */
class ClientNotFoundException : BusinessException(
    errorCode = "CLIENT_NOT_FOUND",
    message = "거래처를 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

/**
 * 제품을 찾을 수 없음
 */
class ProductNotFoundException(productCode: String? = null) : BusinessException(
    errorCode = "PRODUCT_NOT_FOUND",
    message = if (productCode != null) "제품을 찾을 수 없습니다: $productCode" else "제품을 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

/**
 * 잘못된 납기일
 */
class InvalidDeliveryDateException : BusinessException(
    errorCode = "INVALID_DATE",
    message = "납기일은 오늘 이후여야 합니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 주문서 유효성 검증 실패
 */
class OrderValidationFailedException(message: String = "주문서 유효성 검증에 실패했습니다") : BusinessException(
    errorCode = "VALIDATION_FAILED",
    message = message,
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 즐겨찾기에 이미 추가된 제품
 */
class AlreadyFavoritedException : BusinessException(
    errorCode = "ALREADY_FAVORITED",
    message = "이미 즐겨찾기에 추가된 제품입니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 즐겨찾기에 없는 제품
 */
class FavoriteNotFoundException : BusinessException(
    errorCode = "FAVORITE_NOT_FOUND",
    message = "즐겨찾기에 없는 제품입니다",
    httpStatus = HttpStatus.NOT_FOUND
)

/**
 * 주문일 범위 오류
 */
class InvalidOrderDateRangeException : BusinessException(
    errorCode = "INVALID_DATE_RANGE",
    message = "주문일 종료일은 시작일 이후여야 합니다",
    httpStatus = HttpStatus.BAD_REQUEST
)
