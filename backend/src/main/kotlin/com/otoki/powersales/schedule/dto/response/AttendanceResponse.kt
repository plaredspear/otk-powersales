package com.otoki.powersales.schedule.dto.response

import com.otoki.powersales.schedule.entity.AttendanceType
import java.time.LocalDate

/**
 * 출근 등록 응답 DTO
 *
 * - `distanceKm`: Spec #585 Q4 — 응답에 실제 거리 미노출(항상 `0.0`). 거리 값은 서버 로그/감사 로그에만 기록.
 * - `gpsSkipped`/`gpsSkipReason`: Spec #586 — ABC 코드 면제 정책 적용 여부. 모바일 화면 노출 X, 운영 디버깅/감사 용도.
 * - `attendanceType`: Spec #587 — `REGULAR | DISPLAY` (P2-B 에서 `EVENT` 추가).
 * - `displayWorkScheduleId` / `scheduleStartDate` / `scheduleEndDate`: Spec #587 P1-B §1.6 — DISPLAY 분기에서만 채움.
 */
data class AttendanceRegisterResponse(
    val scheduleId: Long,
    val accountName: String,
    val workType: String?,
    val distanceKm: Double,
    val totalCount: Int,
    val registeredCount: Int,
    val gpsSkipped: Boolean = false,
    val gpsSkipReason: String? = null,
    val attendanceType: AttendanceType = AttendanceType.REGULAR,
    val displayWorkScheduleId: Long? = null,
    val scheduleStartDate: LocalDate? = null,
    val scheduleEndDate: LocalDate? = null,
)
