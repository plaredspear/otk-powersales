package com.otoki.powersales.domain.activity.safetycheck.dto.response

import java.time.LocalDateTime

data class SafetyCheckSubmitResponse(
    val submittedAt: LocalDateTime,
    val safetyCheckCompleted: Boolean = true
)
