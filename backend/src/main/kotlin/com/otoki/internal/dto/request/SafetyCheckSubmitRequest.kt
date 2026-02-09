package com.otoki.internal.dto.request

import jakarta.validation.constraints.NotEmpty

data class SafetyCheckSubmitRequest(
    @field:NotEmpty(message = "체크 항목이 비어있습니다")
    val checkedItemIds: List<Long>
)
