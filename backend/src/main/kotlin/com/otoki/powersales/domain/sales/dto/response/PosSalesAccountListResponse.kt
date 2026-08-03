package com.otoki.powersales.domain.sales.dto.response

import com.otoki.powersales.admin.dto.SelectorBranchResult

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
 *
 * @property distributionChannel 유통형태 라벨 (예 "01 대형마트(3대)") — 조회 조건과 동일 조합 규칙.
 * @property accountType 거래처유형(ABC유형) 라벨 (예 "6111 이마트").
 * @property branchCode 거래처에 적재된 원본 지점 코드 — 상위 조직/별칭 코드일 수 있어 셀렉터 값과 다를 수 있다.
 * @property selectorBranchCode 지점 셀렉터에서 자동 선택할 코드 — 지점 미선택 상태로 거래처를 먼저 고른
 *   경우 화면이 이 값으로 지점 체크박스를 채운다. 역산 불가(`selectorBranchStatus` 가 RESOLVED 아님) 면 null.
 * @property selectorBranchStatus 역산 결과 코드
 *   ([com.otoki.powersales.admin.dto.SelectorBranchResult] — RESOLVED / AMBIGUOUS / OUT_OF_SCOPE).
 */
data class PosSalesAccountItem(
    val accountId: Long,
    val accountName: String?,
    val sapAccountCode: String?,
    val distributionChannel: String?,
    val accountType: String?,
    val branchCode: String?,
    val branchName: String?,
    val selectorBranchCode: String? = null,
    val selectorBranchName: String? = null,
    val selectorBranchStatus: String = SelectorBranchResult.STATUS_OUT_OF_SCOPE,
)
