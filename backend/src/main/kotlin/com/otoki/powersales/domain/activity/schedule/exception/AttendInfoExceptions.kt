package com.otoki.powersales.domain.activity.schedule.exception

import com.otoki.powersales.platform.common.exception.BusinessException
import org.springframework.http.HttpStatus

class AttendInfoNotFoundException : BusinessException(
    errorCode = "ATTEND_INFO_NOT_FOUND",
    message = "근태정보를 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

class InvalidAttendInfoStatusException : BusinessException(
    errorCode = "INVALID_ATTEND_INFO_STATUS",
    message = "상태는 N 또는 Y 만 입력 가능합니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class InvalidAttendInfoDateException : BusinessException(
    errorCode = "INVALID_ATTEND_INFO_DATE",
    message = "시작일·종료일은 yyyyMMdd 형식으로 입력해주세요",
    httpStatus = HttpStatus.BAD_REQUEST
)

class InvalidAttendInfoDateRangeException : BusinessException(
    errorCode = "INVALID_ATTEND_INFO_DATE_RANGE",
    message = "종료일은 시작일 이후여야 합니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class InvalidAttendInfoTypeException : BusinessException(
    errorCode = "INVALID_ATTEND_INFO_TYPE",
    message = "유효하지 않은 근태유형 코드입니다 (허용: 10/14/20/90/120/133)",
    httpStatus = HttpStatus.BAD_REQUEST
)
