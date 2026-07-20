package com.otoki.powersales.external.ovip.inbound.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * MFEIS(월별 여사원 통합일정) 전량 스냅샷 조회 요청.
 *
 * 부하 방지 설계:
 * - `year` + `month` 필수 → 전체 스캔을 API 계약에서 차단 (기존 (year, month, ...) 복합 인덱스 활용)
 * - `cursor` (PK keyset) → offset 저하 없이 대량을 순차 인출. 첫 요청은 null/생략
 * - `size` → 서버 상한(ovip.inbound.mfeis.max-page-size) 으로 clamp
 *
 * 전량 스냅샷 방식: 요청마다 year+month 고정 → 단일 연월 안에서 PK 커서가 이동하므로
 * PK 단일 커서로 충분(경계 문제 없음). 클라이언트는 nextCursor 가 null 이 될 때까지 반복 호출한다.
 */
data class MfeisSearchRequest(
    @JsonProperty("year")
    val year: String?,

    @JsonProperty("month")
    val month: String?,

    /** 직전 응답의 nextCursor (PK). 첫 요청은 null/생략 → 처음부터. */
    @JsonProperty("cursor")
    val cursor: Long? = null,

    /** 페이지 크기. null/0 이하면 기본값, 상한 초과면 상한으로 clamp. */
    @JsonProperty("size")
    val size: Int? = null
)
