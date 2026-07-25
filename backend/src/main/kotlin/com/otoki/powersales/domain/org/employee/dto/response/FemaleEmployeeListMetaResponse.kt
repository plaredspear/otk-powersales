package com.otoki.powersales.domain.org.employee.dto.response

/**
 * 여사원 현황 목록 화면 조회 조건 로드 응답 — "권한 기반 조건/UI 제어" 표준 패턴.
 *
 * 목록 진입 시 화면이 필요로 하는 조회 조건(필터 옵션)과 기본값을 한 번에 내려준다.
 * 기존 `/branches`(지점 셀렉터) 1회 호출 + web 하드코딩(재직상태 / 근무형태1 / 근무형태3 /
 * 전문행사조 옵션)으로 분산되어 있던 조건 로드를 단일 응답으로 통합한다.
 *
 * 공통 구조 규약(구조는 통일, 내용은 화면별 자유) — 행사마스터
 * [com.otoki.powersales.domain.activity.promotion.dto.response.PromotionListMetaResponse] 와 동일:
 * - [filters]: 조회 조건 목록. 각 항목의 type/options 페이로드는 화면별 자유.
 * - [defaults]: 최초 조회 기본값. 클라이언트가 기본값을 추측하지 않도록 서버가 단일 출처로 제공.
 *
 * 지점(costCenterCode) 옵션은 권한/스코프에 따라 달라진다(전사 권한자 다건 / 조장 등 본인 지점 1건).
 * 이 산출은 목록/엑셀 조회 스코프 가드와 동일한
 * [com.otoki.powersales.domain.activity.schedule.service.WomenScheduleBranchResolver] 를 공유하여
 * 셀렉터-조회 간 스코프 드리프트를 방지한다.
 */
data class FemaleEmployeeListMetaResponse(
    val filters: List<FemaleEmployeeFilterMeta>,
    val defaults: FemaleEmployeeListDefaults,
)

/** 조회 조건 1개의 메타. SELECT 계열만 [options] 를 채우고, TEXT 는 null. */
data class FemaleEmployeeFilterMeta(
    val key: String,
    val type: FemaleEmployeeFilterType,
    val options: List<FemaleEmployeeFilterOption>? = null,
)

enum class FemaleEmployeeFilterType {
    TEXT,
    SELECT,
}

data class FemaleEmployeeFilterOption(
    val value: String,
    val label: String,
)

/** 목록 최초 조회 기본값. */
data class FemaleEmployeeListDefaults(
    val pageSize: Int,
    val sort: String,
)
