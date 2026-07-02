package com.otoki.powersales.domain.support.notice.dto.response

/**
 * 공지 push 발송 결과 응답.
 *
 * @property targetCount 발송 대상 토큰 수
 * @property successCount 발송 성공 수
 * @property failureCount 발송 실패 수 (무효 토큰 등)
 */
data class NoticePushResultResponse(
    val targetCount: Int,
    val successCount: Int,
    val failureCount: Int
)
