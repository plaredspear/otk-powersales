package com.otoki.powersales.schedule.exception

import com.otoki.powersales.common.exception.BusinessException
import org.springframework.http.HttpStatus

// ===== 입력 검증 (HTTP 400) =====

class LeaderScheduleInvalidWorkingTypeException : BusinessException(
    errorCode = "INVALID_WORKING_TYPE",
    message = "조장 대리 등록은 근무 유형만 가능합니다.",
    httpStatus = HttpStatus.BAD_REQUEST
)

class LeaderScheduleInvalidWorkCategory2Exception : BusinessException(
    errorCode = "INVALID_WORK_CATEGORY2",
    message = "근무 분류 2는 '전담' 만 허용됩니다.",
    httpStatus = HttpStatus.BAD_REQUEST
)

class LeaderScheduleMissingWorkCategory3Exception : BusinessException(
    errorCode = "MISSING_WORK_CATEGORY3",
    message = "근무 분류 3을 고정/격고/순회 중 선택해야 합니다.",
    httpStatus = HttpStatus.BAD_REQUEST
)

class LeaderScheduleAccountRequiredException : BusinessException(
    errorCode = "ACCOUNT_REQUIRED",
    message = "거래처를 선택해야 합니다.",
    httpStatus = HttpStatus.BAD_REQUEST
)

// ===== 권한 (HTTP 403) =====

class LeaderScheduleNotLeaderException : BusinessException(
    errorCode = "NOT_LEADER",
    message = "조장 권한이 필요합니다.",
    httpStatus = HttpStatus.FORBIDDEN
)

class LeaderScheduleNotTeamMemberException : BusinessException(
    errorCode = "NOT_TEAM_MEMBER",
    message = "본인 팀원만 일정을 등록할 수 있습니다.",
    httpStatus = HttpStatus.FORBIDDEN
)

class LeaderScheduleNotLeaderAccountException : BusinessException(
    errorCode = "NOT_LEADER_ACCOUNT",
    message = "본인이 담당하는 거래처만 선택할 수 있습니다.",
    httpStatus = HttpStatus.FORBIDDEN
)

// ===== 대상 직원 (HTTP 403/404) =====

class LeaderScheduleTargetEmployeeInactiveException : BusinessException(
    errorCode = "TARGET_EMPLOYEE_INACTIVE",
    message = "휴직/퇴직 상태의 직원은 일정을 등록할 수 없습니다.",
    httpStatus = HttpStatus.FORBIDDEN
)

class LeaderScheduleTargetEmployeeNotFoundException : BusinessException(
    errorCode = "TARGET_EMPLOYEE_NOT_FOUND",
    message = "대상 직원 정보를 찾을 수 없습니다.",
    httpStatus = HttpStatus.NOT_FOUND
)

// ===== 일정 충돌 (HTTP 409) =====

class LeaderScheduleDuplicateLeaveException : BusinessException(
    errorCode = "DUPLICATE_LEAVE_SCHEDULE",
    message = "해당 날짜에 예정된 연차/대휴 일정이 존재합니다.",
    httpStatus = HttpStatus.CONFLICT
)

class LeaderScheduleDuplicateWorkException : BusinessException(
    errorCode = "DUPLICATE_WORK_SCHEDULE",
    message = "해당 날짜에 예정된 근무 일정이 존재합니다.",
    httpStatus = HttpStatus.CONFLICT
)

class LeaderScheduleCategory3LimitExceededException : BusinessException(
    errorCode = "CATEGORY3_LIMIT_EXCEEDED",
    message = "해당 직원 및 선택한 날짜에 같은 유형의 일정이 이미 존재합니다.",
    httpStatus = HttpStatus.CONFLICT
)

class LeaderScheduleCategory3ConflictException : BusinessException(
    errorCode = "CATEGORY3_CONFLICT",
    message = "동일 날짜와 직원으로 다른 유형의 일정이 존재합니다.",
    httpStatus = HttpStatus.CONFLICT
)

// ===== 일정 수정/삭제 (P7 진열 일정변경) =====

class LeaderScheduleNotFoundException : BusinessException(
    errorCode = "SCHEDULE_NOT_FOUND",
    message = "일정을 찾을 수 없습니다.",
    httpStatus = HttpStatus.NOT_FOUND
)

/**
 * 진열 일정만 조장이 수정/삭제 가능 (행사 일정은 admin promotion 도메인 소유 — spec #679/#571).
 */
class LeaderScheduleNotDisplayScheduleException : BusinessException(
    errorCode = "NOT_DISPLAY_SCHEDULE",
    message = "진열 일정만 수정/삭제할 수 있습니다.",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 진열마스터(`display_work_schedule`)와 연결된 일정은 수정/삭제 불가 (레거시 `checkDisplayMaster` 동등).
 */
class LeaderScheduleDisplayMasterLinkedException : BusinessException(
    errorCode = "DISPLAY_MASTER_LINKED",
    message = "진열마스터가 존재하여 수정/삭제할 수 없습니다.",
    httpStatus = HttpStatus.CONFLICT
)

/**
 * 출근 등록된 일정은 수정/삭제 불가 (레거시 `deleteblock` 동등 — 출근 보고 완료 일정 보호).
 */
class LeaderScheduleAlreadyAttendedException : BusinessException(
    errorCode = "SCHEDULE_ALREADY_ATTENDED",
    message = "출근 등록된 일정은 수정/삭제할 수 없습니다.",
    httpStatus = HttpStatus.CONFLICT
)
