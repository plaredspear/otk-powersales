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

class TeamScheduleDisplayMasterLinkException : BusinessException(
    errorCode = "DISPLAY_MASTER_LINK_CONSTRAINT",
    message = "진열마스터가 연결된 일정은 수정/삭제할 수 없습니다",
    httpStatus = HttpStatus.CONFLICT
)

class TeamSchedulePastDateChangeException : BusinessException(
    errorCode = "PAST_DATE_CHANGE_NOT_ALLOWED",
    message = "과거 근무일자의 날짜는 변경할 수 없습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class TeamScheduleAccountRequiredException : BusinessException(
    errorCode = "ACCOUNT_REQUIRED",
    message = "근무 유형이 '근무'인 일정에는 거래처를 지정해야 합니다",
    httpStatus = HttpStatus.BAD_REQUEST
)
