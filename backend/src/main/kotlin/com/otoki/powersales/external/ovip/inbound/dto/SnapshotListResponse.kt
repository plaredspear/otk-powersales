package com.otoki.powersales.external.ovip.inbound.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 전량 스냅샷 단건 응답 봉투 — **페이지네이션 없이 전건**을 한 번에 반환하는 조회용.
 *
 * 거래처/MFEIS 가 쓰는 [SnapshotPageResponse] 와 달리 `nextCursor`/`hasNext` 가 없다. 조직처럼
 * 페이지를 나누는 것이 오히려 스냅샷 일관성을 해치는 대상에 사용한다 (근거는
 * [com.otoki.powersales.domain.org.organization.repository.OrganizationRepositoryCustom.findAllSnapshot] KDoc).
 *
 * `totalCount` 는 `items.size` 와 항상 같다 — 클라이언트가 수신 건수를 검증할 때 배열을 세지 않고도
 * 쓸 수 있도록 명시 필드로 둔다.
 *
 * @property items      전건 row 목록
 * @property totalCount 반환 건수
 */
data class SnapshotListResponse<T>(
    @JsonProperty("items")
    val items: List<T>,

    @JsonProperty("totalCount")
    val totalCount: Int
) {
    companion object {
        fun <T> of(items: List<T>): SnapshotListResponse<T> =
            SnapshotListResponse(items = items, totalCount = items.size)
    }
}
