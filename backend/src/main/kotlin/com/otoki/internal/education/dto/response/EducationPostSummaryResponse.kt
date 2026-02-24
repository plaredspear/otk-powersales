package com.otoki.internal.education.dto.response

/**
 * 교육 게시물 요약 Response (목록용)
 */
data class EducationPostSummaryResponse(
    val id: String,
    val title: String,
    val createdAt: String  // ISO 8601 형식
)
