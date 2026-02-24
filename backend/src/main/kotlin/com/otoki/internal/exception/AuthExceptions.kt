package com.otoki.internal.exception

import com.otoki.internal.common.exception.BusinessException

import org.springframework.http.HttpStatus

/**
 * 인증 실패 (사번/비밀번호 불일치)
 */
class InvalidCredentialsException : BusinessException(
    errorCode = "INVALID_CREDENTIALS",
    message = "사번 또는 비밀번호가 올바르지 않습니다",
    httpStatus = HttpStatus.UNAUTHORIZED
)

/**
 * 현재 비밀번호 불일치
 */
class InvalidCurrentPasswordException : BusinessException(
    errorCode = "INVALID_CURRENT_PASSWORD",
    message = "현재 비밀번호가 올바르지 않습니다",
    httpStatus = HttpStatus.UNAUTHORIZED
)

/**
 * 비밀번호 형식 오류
 */
class InvalidPasswordFormatException(detail: String? = null) : BusinessException(
    errorCode = "INVALID_PASSWORD_FORMAT",
    message = detail ?: "비밀번호 형식이 올바르지 않습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 유효하지 않은 토큰
 */
class InvalidTokenException : BusinessException(
    errorCode = "INVALID_TOKEN",
    message = "유효하지 않은 Refresh Token입니다",
    httpStatus = HttpStatus.UNAUTHORIZED
)

/**
 * 사용자 없음
 */
class UserNotFoundException : BusinessException(
    errorCode = "USER_NOT_FOUND",
    message = "사용자를 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

/**
 * 단말기 불일치
 */
class DeviceMismatchException : BusinessException(
    errorCode = "DEVICE_MISMATCH",
    message = "등록된 단말기와 다른 기기입니다. 관리자에게 문의하세요",
    httpStatus = HttpStatus.FORBIDDEN
)

class TermsNotFoundException : BusinessException(
    errorCode = "TERMS_NOT_FOUND",
    message = "활성화된 약관이 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

class GpsConsentRequiredException : BusinessException(
    errorCode = "GPS_CONSENT_REQUIRED",
    message = "GPS 사용 동의가 필요합니다",
    httpStatus = HttpStatus.FORBIDDEN
)

/**
 * Refresh Token 재사용 감지 (탈취 의심)
 */
class TokenReuseDetectedException : BusinessException(
    errorCode = "TOKEN_REUSE_DETECTED",
    message = "보안 이상이 감지되었습니다. 다시 로그인해주세요",
    httpStatus = HttpStatus.UNAUTHORIZED
)
