package com.otoki.internal.exception

import org.springframework.http.HttpStatus

/**
 * 거래처가 오늘 스케줄에 없는 경우
 */
class StoreNotFoundException : BusinessException(
    errorCode = "STORE_NOT_FOUND",
    message = "해당 거래처가 오늘 스케줄에 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

/**
 * 이미 출근등록이 완료된 거래처
 */
class AlreadyRegisteredException : BusinessException(
    errorCode = "ALREADY_REGISTERED",
    message = "해당 거래처에 이미 출근등록이 완료되었습니다",
    httpStatus = HttpStatus.CONFLICT
)

/**
 * 격고 근무자 등록 한도 초과
 */
class RegistrationLimitExceededException : BusinessException(
    errorCode = "REGISTRATION_LIMIT_EXCEEDED",
    message = "격고 근무자의 일일 등록 한도(2건)를 초과했습니다",
    httpStatus = HttpStatus.CONFLICT
)

/**
 * 유효하지 않은 근무 유형
 */
class InvalidWorkTypeException : BusinessException(
    errorCode = "INVALID_WORK_TYPE",
    message = "유효하지 않은 근무 유형입니다. ROOM_TEMP 또는 REFRIGERATED만 가능합니다",
    httpStatus = HttpStatus.BAD_REQUEST
)
