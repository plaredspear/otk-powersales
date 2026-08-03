package com.otoki.powersales.domain.sales.dto.request

/**
 * POS매출 1단(거래처 조회) endpoint 요청 파라미터 — 외부 POS DB 미접촉.
 *
 * 「POS매출」 web admin 2단 조회의 1단계. 메인 DB Account 만으로 조건에 맞는 거래처 목록을
 * 조회한다 (POS 집계 없음 → 즉시 응답). 운영자가 이 목록에서 거래처를 선택하면 2단
 * [PosSalesDashboardListRequest] 가 선택 거래처만 외부 POS DB 로 집계한다.
 *
 * 필터 해소 위치 (모두 메인 DB Account):
 * - costCenterCodes: 지점 (선택 — 비우면 지점 필터 없이 권한 범위 전체. 단 customerKeyword 는 필수가 된다)
 * - customerKeyword: 거래처명 부분일치
 * - distributionChannels: 유통형태 = 거래처유형마스터 코드 (마스터 이름으로 Account.accountType 매칭)
 * - accountTypes: 거래처유형 = ABC유형 라벨
 */
data class PosSalesAccountListRequest(
    /**
     * 지점 조회 코드 (이미 권한 판정 + `BranchMapping` 확장이 끝난 값). 빈 목록 = 지점 필터 미적용 —
     * 거래처명으로 먼저 검색하는 경로에서 쓰인다.
     */
    val costCenterCodes: List<String> = emptyList(),
    val customerKeyword: String? = null,
    /** 유통형태 — 거래처유형마스터 코드 (예 "06" = 슈퍼) 다중 선택. 비우면 전체. */
    val distributionChannels: List<String> = emptyList(),
    /** 거래처유형 라벨 (ABC유형코드+ABC유형 조합, 예 "6111 이마트"). 비우면 전체. */
    val accountTypes: List<String> = emptyList(),
)
