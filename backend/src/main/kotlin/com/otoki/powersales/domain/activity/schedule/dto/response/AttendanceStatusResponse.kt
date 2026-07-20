package com.otoki.powersales.domain.activity.schedule.dto.response

/**
 * 출근 현황 응답 DTO
 */
data class AttendanceStatusResponse(
    val totalCount: Int,
    val registeredCount: Int,
    val statusList: List<AttendanceStatusItem>,
    val currentDate: String
)

/**
 * 출근 현황 항목
 */
data class AttendanceStatusItem(
    val scheduleId: Long,
    val accountName: String,
    val workCategory: String,
    val status: String,
    /**
     * 근무유형4 (TMS.workingCategory4 = SF SecondWorkType__c) 표시명.
     *
     * 레거시 home.jsp `#popPlace3` 은 등록 완료 행에 한해 이 값이 있을 때만
     * "완료 (상온)" 처럼 괄호 줄을 덧붙인다. 행사 스케줄에만 존재하므로
     * 진열 근무는 null 이다.
     */
    val secondWorkType: String? = null
)
