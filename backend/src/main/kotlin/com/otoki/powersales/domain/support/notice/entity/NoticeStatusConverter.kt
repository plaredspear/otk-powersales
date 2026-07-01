package com.otoki.powersales.domain.support.notice.entity

import com.otoki.powersales.domain.support.notice.enums.NoticeStatus
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/**
 * Notice.status ↔ DB 컬럼 변환. DB 저장값은 enum name(DRAFT/PUBLISHED) — 마이그레이션 기본값과 정합.
 * null(구 데이터 방어) 은 마이그레이션에서 PUBLISHED 로 채워지나, 만일을 위해 null 로 읽어도 조회 필터가
 * `status = PUBLISHED` 이므로 모바일엔 노출되지 않는다(안전측).
 */
@Converter(autoApply = false)
class NoticeStatusConverter : AttributeConverter<NoticeStatus?, String?> {

    override fun convertToDatabaseColumn(attribute: NoticeStatus?): String? =
        attribute?.name

    override fun convertToEntityAttribute(dbData: String?): NoticeStatus? =
        dbData?.let { runCatching { NoticeStatus.valueOf(it) }.getOrNull() }
}
