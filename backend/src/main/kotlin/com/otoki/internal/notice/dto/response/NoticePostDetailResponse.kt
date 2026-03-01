package com.otoki.internal.notice.dto.response

/**
 * 공지사항 게시물 상세 Response
 */
data class NoticePostDetailResponse(
    val id: Long,
    val category: String,
    val categoryName: String,
    val title: String,
    val content: String,
    val createdAt: String,
    val images: List<NoticeImageResponse>
)
