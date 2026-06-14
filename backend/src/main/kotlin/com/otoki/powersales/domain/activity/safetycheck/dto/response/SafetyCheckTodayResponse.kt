package com.otoki.powersales.domain.activity.safetycheck.dto.response

import java.time.LocalDateTime

data class SafetyCheckTodayResponse(
    val completed: Boolean,
    val submittedAt: LocalDateTime? = null
)
