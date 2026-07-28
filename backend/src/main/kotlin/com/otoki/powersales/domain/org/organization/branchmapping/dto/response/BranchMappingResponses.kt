package com.otoki.powersales.domain.org.organization.branchmapping.dto.response

/**
 * 지점 코드 맵핑 조회 화면(`시스템 > 지점 코드 맵핑`) 응답 DTO.
 *
 * `branch_mapping` 원본 CSV 를 그대로 보여주는 것이 목적이 아니라, 각 코드가 **현행 조직에서 무엇인지**
 * 를 붙여 "이력 코드인지 / 현행 타 조직을 끌어오는 롤업인지" 를 눈으로 판별할 수 있게 하는 것이 목적이다.
 * (`BranchCodeExpander` / `BranchMapping` 의 KDoc 은 오랫동안 "이력 합집합" 으로만 서술해 왔으나,
 * 실제 운영 데이터에는 이력 외에 롤업·이중코드가 섞여 있다.)
 */

/** 확장 코드 1건 — 코드 + 현행 조직명. */
data class BranchMappingExpandedCode(
    val code: String,
    /** 현행 조직명. `null` 이면 현행 조직에 없는 코드 = 조직 개편으로 폐기된 이력 코드. */
    val orgName: String?,
    /** 이 확장 코드가 매핑 행의 지점코드 자기 자신인지 여부 — 화면 강조용. */
    val isSelf: Boolean,
)

/**
 * 매핑 성격 분류 — [BranchMappingListItem.type] 의 값 집합.
 *
 * 판정 근거는 `IncludedBranchCode__c` 운영 데이터 74건 실측. 우선순위대로 평가하며 첫 매치를 채택한다.
 */
enum class BranchMappingType(val label: String) {
    /** 확장 결과가 자기 자신뿐 — 확장 효과 없음. 예: 울산2지점 `5767` → `5767`. */
    NONE("없음"),

    /**
     * 자기 자신을 포함하지 않거나, 자기 자신 외에 **현행 조직으로 해석되는 코드**가 섞여 있다.
     * 지점 스코프에 타 조직 데이터를 끌어오므로 사용처별 검토가 필요하다.
     * 예: retail3영업부 `5829` → `5826,5827,5828` (인천1·2·3), cvs전략 `5694` → `5691~5694`.
     */
    ROLLUP("롤업"),

    /**
     * `E{N}` 과 `{N}` 이 쌍으로 존재 — 동일 조직이 레벨에 따라 갖는 별칭이며 양쪽 다 현역이다.
     * 예: E-BIZ1팀 `E5706` → `5706,E5706`.
     */
    DUAL_CODE("이중코드"),

    /**
     * 자기 자신 + 현행 조직에서 해석되지 않는 코드(= 폐기된 옛 코드).
     * 예: 강북1지점 `5815` → `5452,5815` (5452 는 2025-05-01 조직 개편 전 코드).
     */
    LEGACY("이력"),
}

/** 지점 코드 맵핑 목록 1행. */
data class BranchMappingListItem(
    /** `branch_mapping.branch_code` — 확장의 입력 키. */
    val branchCode: String,
    /** `branch_mapping.label` — SF `BranchMapping__mdt` 의 운영자 작성 라벨. 조직명과 일치하지 않을 수 있다. */
    val label: String?,
    /** [branchCode] 의 현행 조직명. `null` 이면 지점코드 자체가 현행 조직에 없다. */
    val orgName: String?,
    val type: BranchMappingType,
    /** 사람이 읽는 유형명 — web 이 enum 매핑 테이블을 따로 두지 않도록 함께 내린다. */
    val typeLabel: String,
    /** `BranchCodeExpander.expand(branchCode)` 런타임 결과 (코드 오름차순). */
    val expandedCodes: List<BranchMappingExpandedCode>,
    val expandedCount: Int,
    /** 확장 결과 중 현행 조직에서 해석되지 않은 코드 수 — 0 보다 크면 이력 코드를 품고 있다. */
    val unresolvedCount: Int,
    /** `branch_mapping.included_branch_codes` 원본 CSV — 공백/중복이 있는 그대로. 진단용. */
    val rawIncludedBranchCodes: String,
)

/**
 * 지점 코드 맵핑 목록 응답.
 *
 * 74건 규모라 페이징 없이 전건 + 집계를 함께 반환한다 (영업일관리마스터 응답과 동일 패턴).
 */
data class BranchMappingListResponse(
    val content: List<BranchMappingListItem>,
    /** 유형별 건수 — 화면 상단 요약 표기용. key 는 [BranchMappingType.name]. */
    val typeCounts: Map<String, Int>,
    /**
     * `BranchCodeExpander` 런타임 캐시가 비어 있는지 여부.
     *
     * DB 에 `branch_mapping` 행이 있는데 캐시가 비어 있으면 부팅 이후 Stage1 적재가 이뤄져
     * 캐시가 stale 인 상태다 (`BranchCodeExpander` KDoc 의 경고 시나리오). 화면이 경고를 띄우도록
     * 플래그로 내린다.
     */
    val cacheEmpty: Boolean,
)
