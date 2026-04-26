package com.otoki.powersales.safetycheck.dto.response

import java.time.LocalDateTime

data class SafetyCheckSubmitResponse(
    val submittedAt: LocalDateTime,
    val safetyCheckCompleted: Boolean = true
)
