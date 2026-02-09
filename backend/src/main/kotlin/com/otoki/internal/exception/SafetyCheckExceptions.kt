package com.otoki.internal.exception

import org.springframework.http.HttpStatus

/**
 * 동일 날짜에 이미 안전점검을 제출한 경우
 */
class AlreadySubmittedException : BusinessException(
    errorCode = "ALREADY_SUBMITTED",
    message = "오늘 안전점검이 이미 제출되었습니다",
    httpStatus = HttpStatus.CONFLICT
)

/**
 * 필수 항목이 모두 체크되지 않은 경우
 */
class RequiredItemsMissingException : BusinessException(
    errorCode = "REQUIRED_ITEMS_MISSING",
    message = "필수 항목이 모두 체크되지 않았습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)
