package com.otoki.powersales.schedule.dto.request

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

/**
 * 출근 등록 요청 DTO
 *
 * scheduleId와 displayWorkScheduleId는 상호 배타적:
 * - 둘 다 null이면 ATTENDANCE_TARGET_REQUIRED 에러
 * - 둘 다 값이 있으면 ATTENDANCE_TARGET_CONFLICT 에러
 * 서비스 레이어에서 검증한다.
 *
 * latitude/longitude 범위(±90 / ±180) 검증은 [com.otoki.powersales.schedule.service.AttendanceService]
 * 에서 [com.otoki.powersales.schedule.exception.InvalidCoordsException] (errorCode `ATT_INVALID_COORDS`)
 * 으로 처리한다 (Spec #585 §4).
 */
data class AttendanceRegisterRequest(

    @field:Positive(message = "스케줄 ID는 양수여야 합니다")
    val scheduleId: Long? = null,

    @field:Positive(message = "진열마스터 ID는 양수여야 합니다")
    val displayWorkScheduleId: Long? = null,

    @field:NotNull(message = "위도는 필수입니다")
    val latitude: Double?,

    @field:NotNull(message = "경도는 필수입니다")
    val longitude: Double?,

    val workType: String? = null
)
