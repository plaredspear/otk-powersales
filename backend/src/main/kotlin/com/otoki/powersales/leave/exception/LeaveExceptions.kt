package com.otoki.powersales.leave.exception

import com.otoki.powersales.common.exception.BusinessException
import org.springframework.http.HttpStatus

class HolidayDateDuplicateException : BusinessException(
    errorCode = "HOLIDAY_DATE_DUPLICATE",
    message = "해당 날짜에 이미 공휴일이 등록되어 있습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class InvalidHolidayTypeException : BusinessException(
    errorCode = "INVALID_HOLIDAY_TYPE",
    message = "유효하지 않은 공휴일 유형입니다. 허용 값: 공휴일, 주말, 기타",
    httpStatus = HttpStatus.BAD_REQUEST
)

class HolidayNotFoundException : BusinessException(
    errorCode = "HOLIDAY_NOT_FOUND",
    message = "공휴일을 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)
