package com.otoki.powersales.schedule.dto.response

import com.otoki.powersales.schedule.enums.AttendanceType
import java.time.LocalDate

/**
 * 출근 등록 응답 DTO
 *
 * - `distanceKm`: Spec #585 Q4 — 응답에 실제 거리 미노출(항상 `0.0`). 거리 값은 서버 로그/감사 로그에만 기록.
 * - `gpsSkipped`/`gpsSkipReason`: Spec #586 — ABC 코드 면제 정책 적용 여부. 모바일 화면 노출 X, 운영 디버깅/감사 용도.
 * - `attendanceType`: Spec #587 — `REGULAR | DISPLAY | EVENT`.
 * - `displayWorkScheduleId` / `scheduleStartDate` / `scheduleEndDate`: Spec #587 P1-B §1.6 — DISPLAY 분기에서만 채움.
 * - `eventScheduleId` / `scheduleWorkingDate` / `promotionEmployeeId`: Spec #587 P2-B §1.5 — EVENT 분기에서만 채움.
 *   `promotionEmployeeId` 는 PromotionEmployee 매핑 row PK (사원 마스터 PK 인 employeeId 와는 별개 — 원칙 7).
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
    val eventScheduleId: Long? = null,
    val scheduleWorkingDate: LocalDate? = null,
    val promotionEmployeeId: Long? = null,
)
