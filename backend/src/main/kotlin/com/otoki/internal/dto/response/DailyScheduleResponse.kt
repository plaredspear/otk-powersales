package com.otoki.internal.dto.response

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 일간 일정 상세 조회 응답 DTO
 */
data class DailyScheduleResponse(
    val date: String, // YYYY-MM-DD 형식
    @JsonProperty("day_of_week")
    val dayOfWeek: String,
    @JsonProperty("member_name")
    val memberName: String,
    @JsonProperty("employee_number")
    val employeeNumber: String,
    @JsonProperty("report_progress")
    val reportProgress: ReportProgressDto,
    val stores: List<StoreScheduleItemDto>
)

/**
 * 보고 진행 상황 DTO
 */
data class ReportProgressDto(
    val completed: Int,
    val total: Int,
    @JsonProperty("work_type")
    val workType: String
)

/**
 * 거래처 일정 항목 DTO
 */
data class StoreScheduleItemDto(
    @JsonProperty("store_id")
    val storeId: Long,
    @JsonProperty("store_name")
    val storeName: String,
    @JsonProperty("work_type_1")
    val workType1: String,
    @JsonProperty("work_type_2")
    val workType2: String,
    @JsonProperty("work_type_3")
    val workType3: String,
    @JsonProperty("is_registered")
    val isRegistered: Boolean
)
