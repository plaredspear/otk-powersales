package com.otoki.powersales.domain.activity.order.dto.response

import java.time.OffsetDateTime

/**
 * 본인 주문요청 목록 응답 DTO.
 *
 * 백엔드는 페이징 없이 전체 결과를 반환하고, 모바일이 클라이언트 슬라이스로 분할한다 (레거시 동등).
 *
 * @property items 검색 조건에 매칭된 모든 항목
 * @property total items.size — 클라이언트 페이지 계산용
 * @property truncated 결과가 응답 라인 수 상한(2000건)에 도달해 잘린 경우 true
 * @property fetchedAt 서버 조회 시각 (Asia/Seoul)
 */
data class OrderRequestListResponse(
    val items: List<OrderRequestSummaryResponse>,
    val total: Int,
    val truncated: Boolean,
    val fetchedAt: OffsetDateTime,
)
