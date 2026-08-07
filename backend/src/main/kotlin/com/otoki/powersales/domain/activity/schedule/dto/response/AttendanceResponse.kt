package com.otoki.powersales.domain.activity.schedule.dto.response

import com.otoki.powersales.domain.activity.schedule.enums.AttendanceType
import java.time.LocalDate

/**
 * 출근 등록 응답 DTO
 *
 * - `distanceKm`: Spec #585 Q4 — 응답에 실제 거리 미노출(항상 `0.0`). 거리 값은 서버 로그/감사 로그에만 기록.
 * - `gpsSkipped`/`gpsSkipReason`: Spec #586 — ABC 코드 면제 정책 적용 여부. 모바일 화면 노출 X, 운영 디버깅/감사 용도.
 * - `secondWorkType`: 근무유형4 (SF `TeamMemberSchedule.SecondWorkType__c`). 행사는 행사마스터 제품유형
 *   (상온/라면/만두/냉동/냉장), 진열은 진열마스터 근무형태4 (상온/냉동/냉장) 에서 파생된 표시 전용 문자열.
 *   레거시와 동일하게 서버가 파생하고 사용자는 선택하지 않는다 (출근등록 시 온도 선택 UI 는 2024-02-26 폐기 —
 *   `home.jsp:711-725`, `IF_REST_MOBILE_WorkReport.cls:76`). 값이 없으면 null (표시 생략).
 * - `attendanceType`: Spec #587 — `REGULAR | DISPLAY | EVENT`.
 * - `displayWorkScheduleId` / `scheduleStartDate` / `scheduleEndDate`: Spec #587 P1-B §1.6 — DISPLAY 분기에서만 채움.
 * - `eventScheduleId` / `scheduleWorkingDate` / `promotionEmployeeId`: Spec #587 P2-B §1.5 — EVENT 분기에서만 채움.
 *   `promotionEmployeeId` 는 PromotionEmployee 매핑 row PK (사원 마스터 PK 인 employeeId 와는 별개 — 원칙 7).
 */
data class AttendanceRegisterResponse(
    val scheduleId: Long,
    val accountName: String,
    val workType: String?,
    val secondWorkType: String? = null,
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
