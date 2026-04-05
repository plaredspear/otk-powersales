package com.otoki.internal.schedule.exception

import com.otoki.internal.common.exception.BusinessException
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
 * GPS 거리 초과
 */
class DistanceExceededException(distanceKm: Double) : BusinessException(
    errorCode = "DISTANCE_EXCEEDED",
    message = "현재 위치가 거래처에서 너무 멀리 떨어져 있습니다 (${String.format("%.1f", distanceKm)}km)",
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
 * 진열마스터 미존재
 */
class DisplayScheduleNotFoundException : BusinessException(
    errorCode = "DISPLAY_SCHEDULE_NOT_FOUND",
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
 * 오늘이 마스터 기간 범위 밖
 */
class DisplayScheduleOutOfRangeException : BusinessException(
    errorCode = "DISPLAY_SCHEDULE_OUT_OF_RANGE",
    message = "진열마스터의 유효 기간이 아닙니다",
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
