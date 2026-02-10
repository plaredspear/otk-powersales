package com.otoki.internal.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

/**
 * 출근등록 요청 DTO
 */
data class AttendanceRequest(

    @field:NotNull(message = "거래처 ID는 필수입니다")
    @field:Positive(message = "거래처 ID는 양수여야 합니다")
    val storeId: Long?,

    @field:NotBlank(message = "근무 유형은 필수입니다")
    val workType: String?
)
