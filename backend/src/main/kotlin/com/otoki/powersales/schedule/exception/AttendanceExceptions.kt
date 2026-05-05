package com.otoki.powersales.schedule.exception

import com.otoki.powersales.common.exception.BusinessException
import org.springframework.http.HttpStatus

/**
 * 스케줄을 찾을 수 없는 경우
 */
class TeamMemberScheduleNotFoundException : BusinessException(
    errorCode = "SCHEDULE_NOT_FOUND",
    message = "스케줄을 찾을 수 없습니다",
    httpStatus = HttpStatus.NOT_FOUND
)

/**
 * 이미 출근 등록된 거래처
 */
class AlreadyRegisteredException : BusinessException(
    errorCode = "ALREADY_REGISTERED",
    message = "이미 출근 등록된 거래처입니다",
    httpStatus = HttpStatus.CONFLICT
)

/**
 * 안전점검 미완료 상태에서 출근등록 시도
 */
class SafetyCheckRequiredException : BusinessException(
    errorCode = "SAFETY_CHECK_REQUIRED",
    message = "안전점검을 먼저 완료해 주세요",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * GPS 거리 초과 (Spec #585).
 *
 * 거리 값은 응답에 노출하지 않는다 (Q4). 서버 로그/감사 로그에만 기록한다.
 */
class DistanceExceededException : BusinessException(
    errorCode = "ATT_GPS_DISTANCE_EXCEEDED",
    message = "거래처와의 거리가 허용 범위를 초과했습니다.",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 거래처 좌표 누락 (Spec #585 §3-3).
 *
 * 다음 케이스에서 발생: ① null ② 빈 문자열 ③ 공백 ④ Double 파싱 실패 ⑤ 위경도 범위 초과.
 */
class AccountCoordsMissingException : BusinessException(
    errorCode = "ATT_ACCOUNT_COORDS_MISSING",
    message = "거래처 위경도 정보가 없어 출근 등록이 불가합니다.",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 사원 현재 위치 좌표 무효 (Spec #585 §4).
 *
 * currentLat ∉ [-90, 90] 또는 currentLng ∉ [-180, 180].
 */
class InvalidCoordsException : BusinessException(
    errorCode = "ATT_INVALID_COORDS",
    message = "현재 위치 좌표가 유효 범위를 벗어났습니다.",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * schedule_id, display_work_schedule_id 둘 다 null
 */
class AttendanceTargetRequiredException : BusinessException(
    errorCode = "ATTENDANCE_TARGET_REQUIRED",
    message = "schedule_id 또는 display_work_schedule_id 중 하나를 전달해야 합니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * schedule_id, display_work_schedule_id 둘 다 값 있음
 */
class AttendanceTargetConflictException : BusinessException(
    errorCode = "ATTENDANCE_TARGET_CONFLICT",
    message = "schedule_id와 display_work_schedule_id는 동시에 전달할 수 없습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 진열마스터 미존재 (Spec #587 P1-B — `ATT_DISPLAY_SCHEDULE_NOT_FOUND`)
 */
class DisplayScheduleNotFoundException : BusinessException(
    errorCode = "ATT_DISPLAY_SCHEDULE_NOT_FOUND",
    message = "존재하지 않는 진열마스터입니다",
    httpStatus = HttpStatus.NOT_FOUND
)

/**
 * 진열마스터 미확정
 */
class DisplayScheduleNotConfirmedException : BusinessException(
    errorCode = "DISPLAY_SCHEDULE_NOT_CONFIRMED",
    message = "확정되지 않은 진열마스터입니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 오늘이 마스터 기간 범위 밖 (Spec #587 P1-B — `ATT_DISPLAY_SCHEDULE_DATE_OUT_OF_RANGE`)
 */
class DisplayScheduleOutOfRangeException : BusinessException(
    errorCode = "ATT_DISPLAY_SCHEDULE_DATE_OUT_OF_RANGE",
    message = "진열마스터의 유효 기간이 아닙니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 진열마스터 본인 할당 아님 (Spec #587 P1-B §1.2 step 2).
 * `display_work_schedule.employee_id != currentEmployeeId`.
 */
class DisplayScheduleNotAssignedException : BusinessException(
    errorCode = "ATT_DISPLAY_SCHEDULE_NOT_ASSIGNED",
    message = "본인에게 할당된 진열마스터가 아닙니다",
    httpStatus = HttpStatus.FORBIDDEN
)

/**
 * 진열 출근 중복 (Spec #587 P1-B §1.2 step 4 / Q6).
 * 동일 `(employee_id, working_date, working_category3)` 조합으로 이미 출근 등록됨.
 */
class DisplayAttendanceDuplicateException : BusinessException(
    errorCode = "ATT_DISPLAY_DUPLICATE",
    message = "동일한 근무유형으로 이미 출근 등록된 일자입니다",
    httpStatus = HttpStatus.CONFLICT
)

/**
 * 행사 일정 미존재 (Spec #587 P2-B §1.2 step 1).
 * `eventScheduleId` 로 지정된 TMS row 가 존재하지 않거나 is_deleted=true.
 */
class EventScheduleNotFoundException : BusinessException(
    errorCode = "ATT_EVENT_SCHEDULE_NOT_FOUND",
    message = "존재하지 않는 행사 일정입니다",
    httpStatus = HttpStatus.NOT_FOUND
)

/**
 * 행사 일정 본인 할당 아님 (Spec #587 P2-B §1.2 step 2 / Q4 보안 강화).
 * `team_member_schedule.employee_id != currentEmployeeId`.
 */
class EventScheduleNotAssignedException : BusinessException(
    errorCode = "ATT_EVENT_SCHEDULE_NOT_ASSIGNED",
    message = "본인에게 할당된 행사 일정이 아닙니다",
    httpStatus = HttpStatus.FORBIDDEN
)

/**
 * 행사 일정 일자 불일치 (Spec #587 P2-B §1.2 step 3 / Q4).
 * `team_member_schedule.working_date != LocalDate.now()`.
 */
class EventScheduleDateMismatchException : BusinessException(
    errorCode = "ATT_EVENT_SCHEDULE_DATE_MISMATCH",
    message = "오늘 일자의 행사 일정만 출근 등록할 수 있습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 행사 출근 중복 (Spec #587 P2-B §1.2 step 4).
 * 이미 `commute_log_id` 가 채워진 일정에 다시 출근 등록 시도.
 */
class EventAttendanceDuplicateException : BusinessException(
    errorCode = "ATT_EVENT_DUPLICATE",
    message = "이미 출근 등록된 행사 일정입니다",
    httpStatus = HttpStatus.CONFLICT
)

/**
 * 진열/행사 분기 동시 입력 (Spec #587 P2-B §1.1).
 * `displayWorkScheduleId` 와 `eventScheduleId` 가 동시에 전달됨.
 */
class AttendanceDualBranchException : BusinessException(
    errorCode = "ATT_DUAL_BRANCH",
    message = "진열과 행사 분기를 동시에 지정할 수 없습니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 출근등록 마감 시간(17:00) 초과
 */
class AttendanceTimeExceededException : BusinessException(
    errorCode = "ATTENDANCE_TIME_EXCEEDED",
    message = "출근등록은 17시 이전에만 가능합니다",
    httpStatus = HttpStatus.BAD_REQUEST
)

/**
 * 대휴 날짜에 출근 등록 시도
 */
class AttendanceDayOffConflictException : BusinessException(
    errorCode = "ATTENDANCE_DAY_OFF_CONFLICT",
    message = "대휴가 예정된 날짜에는 출근 등록이 불가합니다",
    httpStatus = HttpStatus.BAD_REQUEST
)
