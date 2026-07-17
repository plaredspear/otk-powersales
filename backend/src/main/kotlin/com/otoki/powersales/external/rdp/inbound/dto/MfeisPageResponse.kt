package com.otoki.powersales.external.rdp.inbound.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * MFEIS keyset 페이지네이션 응답 봉투.
 *
 * @property items       이번 페이지 row projection 목록
 * @property nextCursor  다음 페이지 조회 시 넘길 커서(마지막 row 의 PK). null 이면 마지막 페이지
 * @property hasNext     다음 페이지 존재 여부
 */
data class MfeisPageResponse(
    @JsonProperty("items")
    val items: List<MfeisScheduleRow>,

    @JsonProperty("nextCursor")
    val nextCursor: Long?,

    @JsonProperty("hasNext")
    val hasNext: Boolean
)
