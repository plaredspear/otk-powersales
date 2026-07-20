package com.otoki.powersales.external.ovip.inbound.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 마스터 전량 스냅샷 조회 요청 (커서 페이지네이션 계열 공용).
 *
 * MFEIS([MfeisSearchRequest]) 는 `year` + `month` 를 필수로 강제해 전체 스캔을 API 계약에서 차단하지만,
 * 거래처는 **마스터 테이블이라 그런 자연 파티션 축이 없다**. 전량 스냅샷 인출 자체가 목적이므로
 * 필수 필터를 두지 않고, 부하 방어는 아래 두 축으로만 수행한다.
 *
 * - `cursor` (PK keyset) → offset 저하 없이 대량을 순차 인출. 첫 요청은 null/생략
 * - `size` → 서버 상한(ovip.inbound.account.max-page-size) 으로 clamp
 *
 * 클라이언트는 nextCursor 가 null 이 될 때까지 반복 호출한다. 장애 시 같은 커서를 재요청하면
 * 누락 없이 재개된다 (요청일 의존 없음).
 */
data class SnapshotSearchRequest(
    /** 직전 응답의 nextCursor (PK). 첫 요청은 null/생략 → 처음부터. */
    @JsonProperty("cursor")
    val cursor: Long? = null,

    /** 페이지 크기. null/0 이하면 기본값, 상한 초과면 상한으로 clamp. */
    @JsonProperty("size")
    val size: Int? = null
)
