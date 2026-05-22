package com.otoki.powersales.schedule.exception

import com.otoki.powersales.common.exception.BusinessException
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

class TeamScheduleRangeTooWideException : BusinessException(
    errorCode = "RANGE_TOO_WIDE",
    message = "조회 기간은 최대 92일까지 지정할 수 있습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

class TeamScheduleInvalidRangeException : BusinessException(
    errorCode = "INVALID_RANGE",
    message = "종료일은 시작일보다 같거나 뒤 날짜여야 합니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 다건 삭제 endpoint 의 행 수 상한 (100건) 초과 (Spec #691 Q1 옵션 1).
 * legacy `MassDeleteTmScheduleListButton.page:7` 의 client-side 100건 차단을 server-side 로 강화.
 */
class TeamScheduleMassDeleteRowLimitExceededException : BusinessException(
    errorCode = "ROW_LIMIT_EXCEEDED",
    message = "한 번에 100개 이하로만 삭제 가능합니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 다건 삭제 endpoint 에서 `findAllById` 결과가 요청 ids 와 다름 (일부 ids 미존재) — Spec #691.
 * legacy `MassDeleteTmScheduleController.cls:33` 클라이언트 ID 신뢰 보안 결함 회피 — 서버 측 ID 재검증.
 */
class TeamScheduleNotFoundPartialException(val missingIds: List<Long>) : BusinessException(
    errorCode = "TEAM_SCHEDULE_NOT_FOUND_PARTIAL",
    message = "일부 일정이 존재하지 않습니다 (ids=${missingIds.joinToString(",")})",
    httpStatus = HttpStatus.NOT_FOUND
)
