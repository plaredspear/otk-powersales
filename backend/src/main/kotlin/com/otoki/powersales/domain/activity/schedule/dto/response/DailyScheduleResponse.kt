package com.otoki.powersales.domain.activity.schedule.dto.response

/**
 * 일간 일정 상세 조회 응답 DTO
 */
data class DailyScheduleResponse(
    val date: String, // YYYY-MM-DD 형식
    val dayOfWeek: String,
    val memberName: String,
    val employeeCode: String?,
    val workingType: String? = null,
    val reportProgress: ReportProgressDto,
    val accounts: List<DisplayWorkScheduleItemDto>
)

/**
 * 보고 진행 상황 DTO
 */
data class ReportProgressDto(
    val completed: Int,
    val total: Int,
    val workType: String
)

/**
 * 거래처 일정 항목 DTO
 */
data class DisplayWorkScheduleItemDto(
    val accountId: Long,
    val accountName: String,
    val workType1: String,
    val workType2: String,
    val workType3: String,
    val isRegistered: Boolean,
    /**
     * 레거시 화면에 노출되던 항목인지 여부.
     *
     * 레거시 myDaily 는 출근등록 전이면서 진열·행사를 동시에 보유한 날에 한해
     * 한쪽(진열 임시 보유 시 진열, 그 외 행사)만 표시하고 반대편을 버렸다.
     * 신규는 그날 방문할 매장을 놓치지 않도록 버려지던 쪽도 함께 내려주되,
     * 이 플래그를 false 로 표시해 화면에서 부차 항목으로 구분하고
     * 보고완료 카운터(reportProgress) 집계에서는 제외한다.
     */
    val isLegacyVisible: Boolean = true
)
