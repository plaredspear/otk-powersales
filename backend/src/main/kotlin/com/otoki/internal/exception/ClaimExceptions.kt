package com.otoki.internal.exception

import org.springframework.http.HttpStatus

/**
 * 클레임 종류1을 찾을 수 없음
 */
class ClaimCategoryNotFoundException : BusinessException(
    errorCode = "CATEGORY_NOT_FOUND",
    message = "클레임 종류를 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

/**
 * 클레임 종류2를 찾을 수 없음
 */
class ClaimSubcategoryNotFoundException : BusinessException(
    errorCode = "SUBCATEGORY_NOT_FOUND",
    message = "클레임 세부 종류를 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

/**
 * 구매 방법을 찾을 수 없음
 */
class PurchaseMethodNotFoundException : BusinessException(
    errorCode = "PURCHASE_METHOD_NOT_FOUND",
    message = "구매 방법을 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

/**
 * 요청사항을 찾을 수 없음
 */
class RequestTypeNotFoundException : BusinessException(
    errorCode = "REQUEST_TYPE_NOT_FOUND",
    message = "요청사항을 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

/**
 * 유효하지 않은 기한 종류
 */
class InvalidDateTypeException : BusinessException(
    errorCode = "INVALID_DATE_TYPE",
    message = "유효하지 않은 기한 종류입니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 구매 정보 불완전
 */
class PurchaseInfoRequiredException : BusinessException(
    errorCode = "PURCHASE_INFO_REQUIRED",
    message = "구매 금액 입력 시 구매 방법과 영수증 사진은 필수입니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 유효하지 않은 파라미터
 */
class InvalidParameterException(detail: String) : BusinessException(
    errorCode = "INVALID_PARAMETER",
    message = detail,
    httpStatus = HttpStatus.BAD_REQUEST
)
