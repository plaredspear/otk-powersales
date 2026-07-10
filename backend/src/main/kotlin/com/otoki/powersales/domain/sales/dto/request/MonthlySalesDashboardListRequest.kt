package com.otoki.powersales.domain.sales.dto.request

/**
 * 월매출 대시보드 list endpoint 요청 파라미터.
 *
 * year/month + costCenterCodes 는 필수 (인접 admin endpoint 컨벤션).
 * accountIds / accountGroup 은 추가 필터. page / size / sort 는 페이징.
 *
 * 거래처 검색:
 * - [customerKeyword]: 거래처명 OR 거래처코드(externalKey) 통합 부분일치 (행사마스터 accountName 정합).
 * - [distributionChannels]: 유통형태 라벨 (예 "01 대형마트(3대)") 다중 정확일치 — POS매출 정합.
 * - [accountTypes]: 거래처유형(ABC유형) 라벨 (예 "6111 이마트") 다중 정확일치 — POS매출 정합.
 *
 * [targetRegistration]: 목표등록 구분 — 거래처목표등록마스터(SalesProgressRateMaster)의
 * 해당 (거래처, 연, 월) row 존재유무. "registered"=row 존재, "unregistered"=row 미존재, null=전체.
 *
 * [deploymentFilter]: 근무등록 구분 — 선택 지점 범위에서 여사원이 근무등록(출근등록)한 거래처 기준
 * (MFEIS keySet). "deployed"=근무등록 거래처, "undeployed"=미등록 거래처, null/기타=전체.
 */
data class MonthlySalesDashboardListRequest(
    val year: Int,
    val month: Int,
    val costCenterCodes: List<String>,
    val accountIds: List<Long> = emptyList(),
    val accountGroup: String? = null,
    val customerKeyword: String? = null,
    val distributionChannels: List<String> = emptyList(),
    val accountTypes: List<String> = emptyList(),
    val targetRegistration: String? = null,
    val deploymentFilter: String? = null,
    val page: Int = 0,
    val size: Int = 20,
    val sort: String? = null,
)
