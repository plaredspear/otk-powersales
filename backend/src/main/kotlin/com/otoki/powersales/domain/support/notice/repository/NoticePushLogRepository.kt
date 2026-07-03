package com.otoki.powersales.domain.support.notice.repository

import com.otoki.powersales.domain.support.notice.entity.NoticePushLog
import org.springframework.data.jpa.repository.JpaRepository

interface NoticePushLogRepository : JpaRepository<NoticePushLog, Long> {

    /** 이 공지의 누적 push 발송 횟수(중복 발송 경고용). */
    fun countByNoticeId(noticeId: Long): Long

    /** 이 공지의 가장 최근 push 발송 이력(마지막 발송 시각/건수 표시용). */
    fun findFirstByNoticeIdOrderByCreatedAtDesc(noticeId: Long): NoticePushLog?
}
