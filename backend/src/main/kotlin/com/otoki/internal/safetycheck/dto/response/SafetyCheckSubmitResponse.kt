package com.otoki.internal.safetycheck.dto.response

import java.time.LocalDateTime

data class SafetyCheckSubmitResponse(
    val submissionId: Long,
    val submittedAt: LocalDateTime,
    val safetyCheckCompleted: Boolean = true
)
