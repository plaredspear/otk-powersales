package com.otoki.powersales.education.dto.response

import java.time.LocalDateTime

/**
 * Admin 교육 목록 항목 DTO (attachment_count 포함)
 */
data class AdminEducationPostSummary(
    val eduId: String,
    val eduTitle: String,
    val eduCode: String,
    val eduCodeNm: String,
    val instDate: LocalDateTime?,
    val attachmentCount: Int
)
