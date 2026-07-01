package com.otoki.powersales.domain.support.notice.enums

/**
 * 공지사항 발행 상태.
 * - DRAFT: 임시저장 — 관리자만 조회 가능(모바일/홈 미노출).
 * - PUBLISHED: 발행 — 모바일 앱(사원/여사원)에 노출.
 *
 * SF 원본 메타에는 없는 신규 로컬 상태(저장/발행 분리 도입). DB 저장은 enum name.
 * 기존 데이터는 마이그레이션으로 전부 PUBLISHED 로 간주(이미 노출되던 공지).
 */
enum class NoticeStatus(
    val displayName: String,
    val apiCode: String
) {
    DRAFT("임시저장", "DRAFT"),
    PUBLISHED("발행", "PUBLISHED");

    companion object {
        fun fromApiCodeOrNull(code: String?): NoticeStatus? {
            if (code.isNullOrBlank()) return null
            return entries.find { it.apiCode == code }
        }
    }
}
