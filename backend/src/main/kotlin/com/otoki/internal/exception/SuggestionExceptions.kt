package com.otoki.internal.exception

import org.springframework.http.HttpStatus

/**
 * 기존제품 선택 시 제품 코드 필수
 */
class ProductRequiredForExistingException : BusinessException(
    errorCode = "PRODUCT_REQUIRED_FOR_EXISTING",
    message = "기존제품 선택 시 제품 코드는 필수입니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 유효하지 않은 사진 파일
 */
class InvalidPhotoException(detail: String = "사진 파일 형식이 올바르지 않습니다") : BusinessException(
    errorCode = "INVALID_PHOTO",
    message = detail,
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 사진 용량 초과
 */
class PhotoSizeExceededException : BusinessException(
    errorCode = "PHOTO_SIZE_EXCEEDED",
    message = "사진 용량이 초과되었습니다 (최대 10MB)",
    httpStatus = HttpStatus.BAD_REQUEST
)
