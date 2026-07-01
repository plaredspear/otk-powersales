package com.otoki.powersales.domain.sales.dto.request

/**
 * 월매출 대시보드 list endpoint 요청 파라미터.
 *
 * year/month + costCenterCodes 는 필수 (인접 admin endpoint 컨벤션).
 * accountIds / accountGroup 은 추가 필터. page / size / sort 는 페이징.
 *
 * 거래처 검색 3종 (월별 여사원 통합일정·행사마스터 정합):
 * - [customerKeyword]: 거래처명 OR 거래처코드(externalKey) 통합 부분일치 (행사마스터 accountName 정합).
 * - [distributionKeyword]: 유통형태 (거래처상태코드 부분일치 OR 거래처유형명 IN) — 통합일정 정합.
 * - [accountTypeKeyword]: 거래처유형 (ABC유형코드 OR ABC유형 부분일치) — 통합일정 정합.
 *
 * [targetRegistration]: 목표등록 구분 — 거래처목표등록마스터(SalesProgressRateMaster)의
 * 해당 (거래처, 연, 월) row 존재유무. "registered"=row 존재, "unregistered"=row 미존재, null=전체.
 */
data class MonthlySalesDashboardListRequest(
    val year: Int,
    val month: Int,
    val costCenterCodes: List<String>,
    val accountIds: List<Long> = emptyList(),
    val accountGroup: String? = null,
    val customerKeyword: String? = null,
    val distributionKeyword: String? = null,
    val accountTypeKeyword: String? = null,
    val targetRegistration: String? = null,
    val page: Int = 0,
    val size: Int = 20,
    val sort: String? = null,
)
