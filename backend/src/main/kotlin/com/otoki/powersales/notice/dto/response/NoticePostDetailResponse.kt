package com.otoki.powersales.notice.dto.response

import java.time.LocalDateTime

/**
 * 공지사항 게시물 상세 Response
 */
data class NoticePostDetailResponse(
    val id: Long,
    val category: String,
    val categoryName: String,
    val title: String,
    val content: String,
    val branch: String?,
    val branchCode: String?,
    val createdAt: LocalDateTime?,
    val images: List<NoticeImageResponse>
)
