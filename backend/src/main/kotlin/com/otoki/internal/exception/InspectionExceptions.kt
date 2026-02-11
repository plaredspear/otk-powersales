package com.otoki.internal.exception

import org.springframework.http.HttpStatus

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

/**
 * 테마를 찾을 수 없음
 */
class ThemeNotFoundException : BusinessException(
    errorCode = "THEME_NOT_FOUND",
    message = "테마를 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

/**
 * 현장 유형을 찾을 수 없음
 */
class FieldTypeNotFoundException : BusinessException(
    errorCode = "FIELD_TYPE_NOT_FOUND",
    message = "현장 유형을 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

/**
 * 현장 점검을 찾을 수 없음
 */
class InspectionNotFoundException : BusinessException(
    errorCode = "INSPECTION_NOT_FOUND",
    message = "점검 정보를 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

/**
 * 필수 필드 누락
 */
class MissingRequiredFieldException(detail: String) : BusinessException(
    errorCode = "MISSING_REQUIRED_FIELD",
    message = detail,
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 잘못된 분류
 */
class InvalidCategoryException : BusinessException(
    errorCode = "INVALID_CATEGORY",
    message = "유효하지 않은 분류입니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 사진 파일 필수
 */
class PhotoRequiredException : BusinessException(
    errorCode = "PHOTO_REQUIRED",
    message = "사진을 1장 이상 첨부해주세요",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 사진 개수 초과
 */
class PhotoCountExceededException : BusinessException(
    errorCode = "PHOTO_COUNT_EXCEEDED",
    message = "사진은 최대 2장까지 첨부 가능합니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 접근 권한 없음
 */
class InspectionForbiddenException : BusinessException(
    errorCode = "FORBIDDEN",
    message = "접근 권한이 없습니다",
    httpStatus = HttpStatus.FORBIDDEN
)
