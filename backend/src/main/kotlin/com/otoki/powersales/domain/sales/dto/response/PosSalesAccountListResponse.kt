package com.otoki.powersales.domain.sales.dto.response

/**
 * POS매출 1단 — 거래처 조회 결과 (외부 POS DB 미접촉, 메인 DB Account 만).
 *
 * 「POS매출」 web admin 2단 조회의 1단계. 지점/거래처명/유통형태/거래처유형 조건으로 메인 DB
 * 거래처 목록만 반환한다 (POS 집계 없음). 운영자가 이 목록에서 거래처 N건(최대
 * [com.otoki.powersales.domain.sales.service.PosSalesAdminQueryService.MAX_SELECTABLE_ACCOUNTS])
 * 을 선택하면 2단 `/list` 가 선택 거래처만 외부 POS DB 로 집계한다.
 *
 * @property totalElements 조건에 매칭된 전체 거래처 수 — 화면 안내/상한 판단용
 */
data class PosSalesAccountListResponse(
    val totalElements: Int,
    val items: List<PosSalesAccountItem>,
)

/**
 * POS매출 1단 거래처 1행 — POS 집계 없는 순수 거래처 메타.
 */
data class PosSalesAccountItem(
    val accountId: Long,
    val accountName: String?,
    val sapAccountCode: String?,
    val branchCode: String?,
    val branchName: String?,
)
