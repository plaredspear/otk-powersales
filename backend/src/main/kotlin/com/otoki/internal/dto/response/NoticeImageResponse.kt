package com.otoki.internal.dto.response

/**
 * 공지사항 게시물 이미지 Response
 */
data class NoticeImageResponse(
    val id: Long,
    val url: String,
    val sortOrder: Int
)
