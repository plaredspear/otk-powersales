package com.otoki.powersales.domain.support.education.dto.response

import java.time.LocalDateTime

/**
 * 교육 게시물 요약 Response (목록용)
 */
data class EducationPostSummaryResponse(
    val id: String,
    val title: String,
    val createdAt: LocalDateTime?
)
