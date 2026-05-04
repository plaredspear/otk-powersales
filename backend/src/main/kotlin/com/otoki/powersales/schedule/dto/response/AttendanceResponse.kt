package com.otoki.powersales.schedule.dto.response

/**
 * 출근 등록 응답 DTO
 *
 * - `distanceKm`: Spec #585 Q4 — 응답에 실제 거리 미노출(항상 `0.0`). 거리 값은 서버 로그/감사 로그에만 기록.
 * - `gpsSkipped`/`gpsSkipReason`: Spec #586 — ABC 코드 면제 정책 적용 여부. 모바일 화면 노출 X, 운영 디버깅/감사 용도.
 */
data class AttendanceRegisterResponse(
    val scheduleId: Long,
    val accountName: String,
    val workType: String?,
    val distanceKm: Double,
    val totalCount: Int,
    val registeredCount: Int,
    val gpsSkipped: Boolean = false,
    val gpsSkipReason: String? = null
)
