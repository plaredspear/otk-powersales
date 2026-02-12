package com.otoki.internal.dto.response

/**
 * 교육 게시물 목록 + 페이지네이션 Response
 */
data class EducationPostListResponse(
    val content: List<EducationPostSummaryResponse>,
    val totalCount: Long,
    val totalPages: Int,
    val currentPage: Int,  // 1부터 시작
    val size: Int
)
