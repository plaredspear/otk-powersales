package com.otoki.powersales.domain.activity.schedule.sap

/**
 * 진열 마스터(DISPLAY) SAP 송신 페이로드 빌드용 row projection (Spec #588 P2-B).
 *
 * 레거시 `Batch_TeamMemberMasterSchedule.cls:42-66` 결과 셋과 동등.
 * `display_work_schedule` + `employee` + `account` join 결과.
 */
data class DisplayMasterSapPayloadRow(
    val displayWorkScheduleId: Long,
    val employeeCode: String?,
    val accountExternalKey: String?,
    val typeOfWork1: String?,
    val typeOfWork3: String?,
    val typeOfWork5: String?
)
