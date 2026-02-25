package com.otoki.internal.schedule.dto.request

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

/**
 * 출근 등록 요청 DTO
 */
data class CommuteRequest(

    @field:NotBlank(message = "스케줄 sfid는 필수입니다")
    val scheduleSfid: String,

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
