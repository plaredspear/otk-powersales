package com.otoki.internal.exception

import org.springframework.http.HttpStatus

/**
 * 유효하지 않은 공지사항 카테고리
 */
class InvalidNoticeCategoryException : BusinessException(
    errorCode = "INVALID_CATEGORY",
    message = "유효하지 않은 분류입니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 공지사항 게시물을 찾을 수 없음
 */
class NoticePostNotFoundException : BusinessException(
    errorCode = "NOTICE_NOT_FOUND",
    message = "공지사항을 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)
