package com.otoki.internal.education.dto.response

/**
 * Admin 교육 목록 응답 DTO
 */
data class AdminEducationListResponse(
    val content: List<AdminEducationPostSummary>,
    val totalCount: Long,
    val totalPages: Int,
    val currentPage: Int,
    val size: Int
)
