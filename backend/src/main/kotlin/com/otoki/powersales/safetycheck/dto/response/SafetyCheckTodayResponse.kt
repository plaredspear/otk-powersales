package com.otoki.powersales.safetycheck.dto.response

import java.time.LocalDateTime

data class SafetyCheckTodayResponse(
    val completed: Boolean,
    val submittedAt: LocalDateTime? = null
)
