package com.otoki.powersales.domain.activity.schedule.dto.response

/**
 * 진열스케줄마스터 목록 화면 조회 조건 로드 응답 — "권한 기반 조건/UI 제어" 표준 패턴.
 *
 * 목록 진입 시 화면이 필요로 하는 조회 조건(필터 옵션)과 기본값을 한 번에 내려준다.
 * 기존 `/branches`(지점 셀렉터) 단독 호출 + web 하드코딩(근무유형3/확정상태 옵션)으로 분산되어 있던
 * 조건 로드를 단일 응답으로 통합한다. 행사마스터 목록 meta(`PromotionListMetaResponse`)와 동일한
 * 공통 구조 규약을 따른다.
 *
 * 공통 구조 규약(구조는 통일, 내용은 화면별 자유):
 * - [filters]: 조회 조건 목록. 각 항목의 type/options 페이로드는 화면별 자유.
 * - [defaults]: 최초 조회 기본값. 클라이언트가 기본값을 추측하지 않도록 서버가 단일 출처로 제공.
 *
 * 지점(branchCode) 옵션은 권한/스코프에 따라 달라진다(전사 권한자 34개 / 그 외 본인 지점 1건).
 * 이 산출은 목록/엑셀 조회 스코프 가드와 동일한 [com.otoki.powersales.admin.service.WhitelistBranchScopeResolver]
 * 를 공유하여 셀렉터-조회 간 스코프 드리프트를 방지한다.
 */
data class ScheduleListMetaResponse(
    val filters: List<ScheduleFilterMeta>,
    val defaults: ScheduleListDefaults,
)

/** 조회 조건 1개의 메타. SELECT 계열만 [options] 를 채우고, TEXT/DATE 는 null. */
data class ScheduleFilterMeta(
    val key: String,
    val type: ScheduleFilterType,
    val options: List<ScheduleFilterOption>? = null,
)

enum class ScheduleFilterType {
    TEXT,
    SELECT,
    DATE,
}

data class ScheduleFilterOption(
    val value: String,
    val label: String,
)

/** 목록 최초 조회 기본값. */
data class ScheduleListDefaults(
    val pageSize: Int,
    val sort: String,
)
