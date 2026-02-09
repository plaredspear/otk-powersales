package com.otoki.internal.exception

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
