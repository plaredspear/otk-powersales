package com.otoki.powersales.external.rdp.inbound.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 마스터 전량 스냅샷 keyset 페이지네이션 응답 봉투 (거래처 / 사원 공용).
 *
 * MFEIS 의 [MfeisPageResponse] 와 동일한 3필드 계약이며, row 타입만 제네릭으로 일반화했다.
 * (MFEIS 는 선행 구현이라 전용 타입을 유지 — 기존 클라이언트 계약 불변.)
 *
 * @property items       이번 페이지 row 목록
 * @property nextCursor  다음 페이지 조회 시 넘길 커서(마지막 row 의 PK). null 이면 마지막 페이지
 * @property hasNext     다음 페이지 존재 여부
 */
data class SnapshotPageResponse<T>(
    @JsonProperty("items")
    val items: List<T>,

    @JsonProperty("nextCursor")
    val nextCursor: Long?,

    @JsonProperty("hasNext")
    val hasNext: Boolean
)
