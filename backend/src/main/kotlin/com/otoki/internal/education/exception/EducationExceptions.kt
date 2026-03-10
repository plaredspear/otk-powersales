package com.otoki.internal.education.exception

import com.otoki.internal.common.exception.BusinessException

import org.springframework.http.HttpStatus

/**
 * 유효하지 않은 교육 카테고리
 */
class InvalidEducationCategoryException : BusinessException(
    errorCode = "INVALID_CATEGORY",
    message = "유효하지 않은 카테고리입니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 교육 게시물을 찾을 수 없음
 */
class EducationPostNotFoundException : BusinessException(
    errorCode = "POST_NOT_FOUND",
    message = "게시물을 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

/**
 * 첨부파일 수 초과
 */
class FileLimitExceededException : BusinessException(
    errorCode = "FILE_LIMIT_EXCEEDED",
    message = "첨부파일은 최대 20개까지 가능합니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 파일 크기 초과
 */
class FileSizeExceededException : BusinessException(
    errorCode = "FILE_SIZE_EXCEEDED",
    message = "파일 크기가 50MB를 초과합니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 잘못된 파일 키
 */
class InvalidFileKeyException : BusinessException(
    errorCode = "INVALID_FILE_KEY",
    message = "유효하지 않은 파일 키입니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 잘못된 파라미터
 */
class InvalidEducationParameterException(detail: String? = null) : BusinessException(
    errorCode = "INVALID_PARAMETER",
    message = detail ?: "입력값이 올바르지 않습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)
