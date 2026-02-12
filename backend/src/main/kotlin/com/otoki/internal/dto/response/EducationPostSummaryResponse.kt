package com.otoki.internal.dto.response

/**
 * 교육 게시물 요약 Response (목록용)
 */
data class EducationPostSummaryResponse(
    val id: Long,
    val title: String,
    val createdAt: String  // ISO 8601 형식
)
