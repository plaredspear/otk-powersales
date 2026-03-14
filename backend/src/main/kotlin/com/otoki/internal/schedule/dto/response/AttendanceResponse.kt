package com.otoki.internal.schedule.dto.response

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 출근 등록 응답 DTO
 */
data class CommuteResponse(
    @JsonProperty("schedule_sfid")
    val teamMemberScheduleSfid: String,
    val storeName: String,
    val workType: String?,
    val distanceKm: Double,
    val totalCount: Int,
    val registeredCount: Int
)
