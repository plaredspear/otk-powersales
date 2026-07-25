package com.otoki.powersales.domain.activity.schedule.dto.response

/**
 * 진열사원 투입기준 마스터 목록 화면 조회 조건 로드 응답.
 *
 * 행사마스터 [PromotionListMetaResponse] 와 동일한 "조회 조건 로드" 표준 패턴 —
 * 목록 진입 시 화면이 필요로 하는 조회 조건(필터 옵션)과 기본값을 한 번에 내려준다.
 * 기존에는 상태 필터(전체/유효/예정/종료)가 프론트 상수로 하드코딩되어 있었다.
 *
 * 공통 구조 규약(구조는 통일, 내용은 화면별 자유):
 * - [filters]: 조회 조건 목록. 각 항목의 type/options 페이로드는 화면별 자유.
 * - [defaults]: 최초 조회 기본값. 클라이언트가 기본값을 추측하지 않도록 서버가 단일 출처로 제공.
 *
 * 본 화면은 지점/권한 의존 조건이 없어(전사 공통 마스터) 컨트롤러의 권한 조립 단계 없이
 * 서비스 산출을 그대로 반환한다.
 */
data class EmployeeInputCriteriaMasterListMetaResponse(
    val filters: List<EmployeeInputCriteriaMasterFilterMeta>,
    val defaults: EmployeeInputCriteriaMasterListDefaults,
)

/** 조회 조건 1개의 메타. SELECT 계열만 [options] 를 채우고, TEXT/DATE 는 null. */
data class EmployeeInputCriteriaMasterFilterMeta(
    val key: String,
    val type: EmployeeInputCriteriaMasterFilterType,
    val options: List<EmployeeInputCriteriaMasterFilterOption>? = null,
)

enum class EmployeeInputCriteriaMasterFilterType {
    TEXT,
    SELECT,
    DATE,
}

data class EmployeeInputCriteriaMasterFilterOption(
    val value: String,
    val label: String,
)

/** 목록 최초 조회 기본값. */
data class EmployeeInputCriteriaMasterListDefaults(
    val status: String,
)
