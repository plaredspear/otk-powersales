package com.otoki.powersales.domain.activity.suggestion.exception

import com.otoki.powersales.platform.common.exception.BusinessException
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

/**
 * SF `/ProposalRegist` 전송 실패로 등록을 취소했을 때 (레거시 `suggestProc` 의 `RESULT_CODE != 200` 분기 정합).
 *
 * 레거시는 SF 가 유일한 저장소라 SF 가 거부하면 사용자에게 오류(E4/E5)를 반환하고 아무것도 남기지 않았다.
 * 신규도 동일하게 맞춰, DB 에 선반영된 제안/첨부와 S3 객체를 되돌린 뒤 이 예외로 등록 실패를 알린다.
 *
 * [message] 는 SF 가 준 `RESULT_MSG` 를 그대로 노출한다 — "잘못된 값입니다. (ProductCode)" 처럼 사용자가
 * 입력을 고칠 수 있는 정보라서 삼키지 않는다. SF 응답이 없는 호출 실패(타임아웃/OAuth)면 일반 안내 문구.
 *
 * 400 Bad Request — 비즈니스 검증 실패이므로 401(세션 만료 오인) / 500 을 쓰지 않는다.
 */
class SuggestionSfRegistFailedException(message: String) : BusinessException(
    errorCode = "SUGGESTION_SF_REGIST_FAILED",
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
