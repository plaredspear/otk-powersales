package com.otoki.powersales.suggestion.exception

import com.otoki.powersales.common.exception.BusinessException
import org.springframework.http.HttpStatus

/**
 * BR1~BR7 — Category 분기 검증 위반 (P2-B §2.4).
 *
 * 레거시 `ProposalTriggerHandler.cls` 의 `addError` 호출이 신규에서 400 Bad Request 로 처리.
 */
class SuggestionValidationException(message: String) : BusinessException(
    errorCode = "SUGGESTION_VALIDATION",
    message = message,
    httpStatus = HttpStatus.BAD_REQUEST
)

class SuggestionNotFoundException : BusinessException(
    errorCode = "SUGGESTION_NOT_FOUND",
    message = "제안을 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

class SuggestionAccessDeniedException : BusinessException(
    errorCode = "SUGGESTION_ACCESS_DENIED",
    message = "본인이 등록한 제안만 접근할 수 있습니다",
    httpStatus = HttpStatus.FORBIDDEN
)

class InvalidSuggestionIdException : BusinessException(
    errorCode = "INVALID_PARAMETER",
    message = "유효하지 않은 제안 ID 입니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class InvalidSuggestionPhotoIdException : BusinessException(
    errorCode = "INVALID_PARAMETER",
    message = "유효하지 않은 사진 ID 입니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class SuggestionPhotoNotFoundException : BusinessException(
    errorCode = "SUGGESTION_PHOTO_NOT_FOUND",
    message = "사진을 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)
