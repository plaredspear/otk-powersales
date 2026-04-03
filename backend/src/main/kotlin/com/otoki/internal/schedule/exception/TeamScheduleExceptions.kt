package com.otoki.internal.schedule.exception

import com.otoki.internal.common.exception.BusinessException
import org.springframework.http.HttpStatus

class TeamScheduleEmployeeOnLeaveException : BusinessException(
    errorCode = "EMPLOYEE_ON_LEAVE",
    message = "해당 여사원은 휴직 상태입니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class TeamScheduleEmployeeResignedException : BusinessException(
    errorCode = "EMPLOYEE_RESIGNED",
    message = "해당 여사원은 퇴직하였습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class TeamScheduleConflictException(detail: String) : BusinessException(
    errorCode = "SCHEDULE_CONFLICT",
    message = detail,
    httpStatus = HttpStatus.BAD_REQUEST
)

class TeamScheduleNotFoundException : BusinessException(
    errorCode = "NOT_FOUND",
    message = "일정을 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

class TeamScheduleEmployeeNotFoundException : BusinessException(
    errorCode = "NOT_FOUND",
    message = "사원을 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

class TeamScheduleAccountNotFoundException : BusinessException(
    errorCode = "NOT_FOUND",
    message = "거래처를 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

class TeamScheduleDeleteForbiddenException : BusinessException(
    errorCode = "FORBIDDEN",
    message = "지점장님은 스케줄 편집 권한이 없습니다",
    httpStatus = HttpStatus.FORBIDDEN
)

class TeamScheduleWorkReportDeleteException : BusinessException(
    errorCode = "WORK_REPORT_DELETE_CONSTRAINT",
    message = "근무등록이 완료된 일정은 삭제할 수 없습니다",
    httpStatus = HttpStatus.CONFLICT
)
