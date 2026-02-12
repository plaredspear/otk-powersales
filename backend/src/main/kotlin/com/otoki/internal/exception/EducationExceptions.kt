package com.otoki.internal.exception

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
