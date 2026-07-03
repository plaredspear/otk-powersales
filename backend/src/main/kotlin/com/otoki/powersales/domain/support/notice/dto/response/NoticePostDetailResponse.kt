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
    val images: List<NoticeImageResponse>,
    /** push 누적 발송 횟수 (0=미발송). 중복 발송 경고 판단용. */
    val pushSentCount: Long = 0,
    /** 마지막 push 발송 이력 (미발송이면 null). */
    val lastPush: NoticePushInfo? = null
)

/**
 * 공지 push 발송 이력 요약 (상세 응답 내포).
 */
data class NoticePushInfo(
    val sentAt: LocalDateTime?,
    val targetCount: Int,
    val successCount: Int,
    val failureCount: Int
)
