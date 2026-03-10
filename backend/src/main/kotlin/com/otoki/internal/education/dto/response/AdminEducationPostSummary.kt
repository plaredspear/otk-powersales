package com.otoki.internal.education.dto.response

/**
 * Admin 교육 목록 항목 DTO (attachment_count 포함)
 */
data class AdminEducationPostSummary(
    val eduId: String,
    val eduTitle: String,
    val eduCode: String,
    val eduCodeNm: String,
    val instDate: String,
    val attachmentCount: Int
)
