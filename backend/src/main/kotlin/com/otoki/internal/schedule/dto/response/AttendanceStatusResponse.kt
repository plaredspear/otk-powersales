package com.otoki.internal.schedule.dto.response

/**
 * 출근 현황 응답 DTO
 */
data class CommuteStatusResponse(
    val totalCount: Int,
    val registeredCount: Int,
    val statusList: List<CommuteStatusItem>,
    val currentDate: String
)

/**
 * 출근 현황 항목
 */
data class CommuteStatusItem(
    val scheduleSfid: String,
    val storeName: String,
    val workCategory: String,
    val status: String
)
