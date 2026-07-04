package com.otoki.powersales.domain.sales.dto.request

/**
 * POS매출 1단(거래처 조회) endpoint 요청 파라미터 — 외부 POS DB 미접촉.
 *
 * 「POS매출」 web admin 2단 조회의 1단계. 메인 DB Account 만으로 조건에 맞는 거래처 목록을
 * 조회한다 (POS 집계 없음 → 즉시 응답). 운영자가 이 목록에서 거래처를 선택하면 2단
 * [PosSalesDashboardListRequest] 가 선택 거래처만 외부 POS DB 로 집계한다.
 *
 * 필터 해소 위치 (모두 메인 DB Account):
 * - costCenterCodes: 지점 (필수)
 * - customerKeyword: 거래처명 부분일치
 * - distributionChannels: 유통형태 라벨 (거래처상태코드+거래처타입)
 * - accountTypes: 거래처유형 = ABC유형 라벨
 */
data class PosSalesAccountListRequest(
    val costCenterCodes: List<String>,
    val customerKeyword: String? = null,
    /** 유통형태 라벨 (거래처상태코드+거래처타입 조합, 예 "02 슈퍼"). 비우면 전체. */
    val distributionChannels: List<String> = emptyList(),
    /** 거래처유형 라벨 (ABC유형코드+ABC유형 조합, 예 "6111 이마트"). 비우면 전체. */
    val accountTypes: List<String> = emptyList(),
)
