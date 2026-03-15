package com.otoki.internal.schedule.dto.request

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

/**
 * 출근 등록 요청 DTO
 */
data class CommuteRequest(

    @field:NotNull(message = "스케줄 ID는 필수입니다")
    @field:Positive(message = "스케줄 ID는 양수여야 합니다")
    val scheduleId: Long?,

    @field:NotNull(message = "위도는 필수입니다")
    @field:DecimalMin(value = "-90", message = "위도는 -90 이상이어야 합니다")
    @field:DecimalMax(value = "90", message = "위도는 90 이하여야 합니다")
    val latitude: Double?,

    @field:NotNull(message = "경도는 필수입니다")
    @field:DecimalMin(value = "-180", message = "경도는 -180 이상이어야 합니다")
    @field:DecimalMax(value = "180", message = "경도는 180 이하여야 합니다")
    val longitude: Double?,

    val workType: String? = null
)
