package com.otoki.internal.exception

import org.springframework.http.HttpStatus

// 공유 예외 클래스 — 원래 OrderDraftExceptions.kt, InspectionExceptions.kt 등에 정의되었으나
// 해당 파일이 주석 처리됨에 따라 활성 코드에서 참조하는 예외를 분리 (Spec 69)

class InvalidOrderParameterException(detail: String) : BusinessException(
    errorCode = "INVALID_ORDER_PARAMETER",
    message = detail,
    httpStatus = HttpStatus.BAD_REQUEST
)

class ProductNotFoundException(productCode: String? = null) : BusinessException(
    errorCode = "PRODUCT_NOT_FOUND",
    message = if (productCode != null) "제품을 찾을 수 없습니다: $productCode" else "제품을 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

class AlreadyFavoritedException : BusinessException(
    errorCode = "ALREADY_FAVORITED",
    message = "이미 즐겨찾기에 추가된 제품입니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class FavoriteNotFoundException : BusinessException(
    errorCode = "FAVORITE_NOT_FOUND",
    message = "즐겨찾기에 없는 제품입니다",
    httpStatus = HttpStatus.NOT_FOUND
)

/**
 * 파일 저장 오류
 */
class FileStorageException(
    detail: String,
    cause: Throwable? = null
) : BusinessException(
    errorCode = "FILE_STORAGE_ERROR",
    message = detail,
    httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
    cause = cause
)

/**
 * 유효하지 않은 파일
 */
class InvalidFileException(detail: String) : BusinessException(
    errorCode = "INVALID_FILE",
    message = detail,
    httpStatus = HttpStatus.BAD_REQUEST
)
