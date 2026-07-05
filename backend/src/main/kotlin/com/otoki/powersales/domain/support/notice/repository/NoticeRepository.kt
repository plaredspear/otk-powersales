package com.otoki.powersales.domain.support.notice.repository

import com.otoki.powersales.domain.support.notice.entity.Notice
import org.springframework.data.jpa.repository.JpaRepository

interface NoticeRepository : JpaRepository<Notice, Long>, NoticeRepositoryCustom {

    /**
     * 본문(contents)에 특정 문자열이 포함된 공지 조회. base64 인라인 이미지 일회성 정규화 마이그레이션
     * ([com.otoki.powersales.domain.support.notice.service.NoticeService.migrateInlineBase64Images]) 대상 선별용.
     * contents 는 TEXT 컬럼이라 LIKE 스캔이지만 일회성 관리자 작업이라 허용.
     */
    fun findByContentsContaining(fragment: String): List<Notice>
}
