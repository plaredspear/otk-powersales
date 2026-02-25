package com.otoki.internal.schedule.exception

import com.otoki.internal.common.exception.BusinessException
import org.springframework.http.HttpStatus

/**
 * 스케줄을 찾을 수 없는 경우
 */
class ScheduleNotFoundException : BusinessException(
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
 * GPS 거리 초과
 */
class DistanceExceededException(distanceKm: Double) : BusinessException(
    errorCode = "DISTANCE_EXCEEDED",
    message = "현재 위치가 거래처에서 너무 멀리 떨어져 있습니다 (${String.format("%.1f", distanceKm)}km)",
    httpStatus = HttpStatus.BAD_REQUEST
)
