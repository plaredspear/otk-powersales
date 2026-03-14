package com.otoki.internal.admin.dto.request

import jakarta.validation.constraints.NotBlank

data class ScheduleConfirmRequest(
    @field:NotBlank(message = "upload_id는 필수입니다")
    val uploadId: String
)
