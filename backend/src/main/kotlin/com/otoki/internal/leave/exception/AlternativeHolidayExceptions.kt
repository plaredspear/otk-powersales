package com.otoki.internal.leave.exception

import com.otoki.internal.common.exception.BusinessException
import org.springframework.http.HttpStatus

class AltHolidayConfirmDateIsHolidayException : BusinessException(
    errorCode = "ALT_HOLIDAY_CONFIRM_DATE_IS_HOLIDAY",
    message = "해당 날짜는 공휴일이므로 대체휴무를 신청할 수 없습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class AltHolidayConfirmDateIsWeekendException : BusinessException(
    errorCode = "ALT_HOLIDAY_CONFIRM_DATE_IS_WEEKEND",
    message = "해당 날짜는 주말이므로 대체휴무를 신청할 수 없습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class AltHolidayActualDateIsWeekdayException : BusinessException(
    errorCode = "ALT_HOLIDAY_ACTUAL_DATE_IS_WEEKDAY",
    message = "대상일이 평일(공휴일 아님)이므로 대체휴무를 신청할 수 없습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class AltHolidayNoWorkScheduleException : BusinessException(
    errorCode = "ALT_HOLIDAY_NO_WORK_SCHEDULE",
    message = "대상일에 해당 사원의 근무 스케줄이 없습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class AltHolidayDuplicateException : BusinessException(
    errorCode = "ALT_HOLIDAY_DUPLICATE",
    message = "동일 사원의 동일 대상일에 이미 대체휴무 신청이 존재합니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class AltHolidayInvalidStatusException : BusinessException(
    errorCode = "ALT_HOLIDAY_INVALID_STATUS",
    message = "현재 상태에서는 해당 작업을 수행할 수 없습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class AltHolidayNotFoundException : BusinessException(
    errorCode = "ALT_HOLIDAY_NOT_FOUND",
    message = "대체휴무를 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

class ChangeReasonRequiredException : BusinessException(
    errorCode = "CHANGE_REASON_REQUIRED",
    message = "변경 사유는 필수입니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class EmployeeNotFoundException : BusinessException(
    errorCode = "EMPLOYEE_NOT_FOUND",
    message = "사원을 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)
