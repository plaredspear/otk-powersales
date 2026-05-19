package com.otoki.powersales.notice.dto.response

import java.time.LocalDateTime

/**
 * 공지사항 게시물 요약 Response (목록용)
 */
data class NoticePostSummaryResponse(
    val id: Long,
    val category: String,           // enum name (e.g., "COMPANY")
    val categoryName: String,       // enum displayName (e.g., "회사공지")
    val title: String,
    val createdAt: LocalDateTime?
)
