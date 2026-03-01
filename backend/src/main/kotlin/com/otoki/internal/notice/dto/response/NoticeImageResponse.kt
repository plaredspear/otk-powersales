package com.otoki.internal.notice.dto.response

/**
 * 공지사항 게시물 이미지 Response
 */
data class NoticeImageResponse(
    val id: Long,
    val url: String,
    val sortOrder: Int
)
