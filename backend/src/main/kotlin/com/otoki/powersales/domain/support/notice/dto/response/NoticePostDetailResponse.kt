package com.otoki.powersales.domain.support.notice.dto.response

import java.time.LocalDateTime

/**
 * 공지사항 게시물 상세 Response
 */
data class NoticePostDetailResponse(
    val id: Long,
    val scope: String?,
    val category: String,
    val categoryName: String,
    val status: String,             // 발행 상태 apiCode (DRAFT/PUBLISHED)
    val statusName: String,         // 발행 상태 표시명 (임시저장/발행)
    val title: String,
    val content: String,
    val branch: String?,
    val branchCode: String?,
    val createdAt: LocalDateTime?,
    val images: List<NoticeImageResponse>
)
