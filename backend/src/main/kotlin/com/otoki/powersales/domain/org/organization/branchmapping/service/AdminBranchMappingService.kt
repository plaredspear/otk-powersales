package com.otoki.powersales.domain.org.organization.branchmapping.service

import com.otoki.powersales.domain.org.organization.branchmapping.BranchCodeExpander
import com.otoki.powersales.domain.org.organization.branchmapping.dto.response.BranchMappingExpandedCode
import com.otoki.powersales.domain.org.organization.branchmapping.dto.response.BranchMappingListItem
import com.otoki.powersales.domain.org.organization.branchmapping.dto.response.BranchMappingListResponse
import com.otoki.powersales.domain.org.organization.branchmapping.dto.response.BranchMappingType
import com.otoki.powersales.domain.org.organization.branchmapping.entity.BranchMapping
import com.otoki.powersales.domain.org.organization.branchmapping.repository.BranchMappingRepository
import com.otoki.powersales.domain.org.organization.repository.OrganizationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 지점 코드 맵핑 조회 서비스 — `시스템 > 지점 코드 맵핑` 화면 전용 (조회 전용).
 *
 * ## 목적
 * `BranchCodeExpander` 가 지점 스코프 조회에서 코드를 어떻게 확장하는지 운영자가 확인할 수 있게 한다.
 * 확장 결과 코드마다 **현행 조직명을 붙여** 이력 코드(현행 조직에 없음)와 롤업(현행 타 조직을 끌어옴)을
 * 구분해 보여주는 것이 핵심이다.
 *
 * ## 코드 도메인 주의
 * `BranchMapping` 의 코드는 명명(`cost_center_code`)과 달리 실제로는 **OrgCode** 다
 * (`OrgCostCenterMatchService` KDoc — "레거시 명명상 CostCenterCode 이나 실제 값은 OrgCode").
 * 확장 입력값의 출처인 `OrganizationRepositoryCustomImpl.fetchTeamScheduleBranches` 도
 * `org_cd5`/`org_cd4` 로 `BranchResponse.branchCode` 를 만든다. 따라서 조직명 해석은 cc_cd 계열이
 * 아니라 org_cd 계열([OrganizationRepository.findOrgNamesByAnyOrgCodeLevel])로 매칭해야 한다.
 *
 * ## 캐시 vs DB
 * 확장 결과는 [BranchCodeExpander.snapshot] 의 **런타임 캐시**를 쓴다. DB 를 다시 읽어 계산하면
 * 실제 조회에 적용되는 값과 화면이 달라져 stale 진단이 불가능해지기 때문이다.
 */
@Service
@Transactional(readOnly = true)
class AdminBranchMappingService(
    private val branchMappingRepository: BranchMappingRepository,
    private val organizationRepository: OrganizationRepository,
    private val branchCodeExpander: BranchCodeExpander,
) {

    /**
     * 지점 코드 맵핑 전건 조회.
     *
     * @param keyword 양방향 검색어. 지점코드 / 라벨 / 확장 코드(원본 CSV) / 조직명 중 하나라도 부분 일치하면
     *   해당 행을 포함한다. 확장 코드까지 매칭 대상이라 "이 코드를 품고 있는 매핑 행" 역방향 조회가 가능하다
     *   (예: `5826` 검색 → 인천1지점 자신 + 그를 포함하는 retail3영업부 행).
     */
    fun getBranchMappings(keyword: String?): BranchMappingListResponse {
        val mappings = branchMappingRepository.findAllByOrderByBranchCodeAsc()
        val cache = branchCodeExpander.snapshot()

        // 지점코드 + 모든 확장 코드를 한 번에 모아 조직명 IN 1회 조회 (코드별 단건 조회 시 수백 쿼리).
        val allCodes = mappings.flatMapTo(mutableSetOf()) { mapping ->
            branchCodeExpander.expand(setOf(mapping.branchCode)) + mapping.branchCode
        }
        val orgNames = organizationRepository.findOrgNamesByAnyOrgCodeLevel(allCodes)

        val items = mappings.map { mapping -> toItem(mapping, orgNames) }
        val filtered = if (keyword.isNullOrBlank()) items else items.filter { it.matches(keyword.trim()) }

        return BranchMappingListResponse(
            content = filtered,
            typeCounts = filtered.groupingBy { it.type.name }.eachCount(),
            // DB 에 행이 있는데 캐시가 비어 있으면 Stage1 적재 후 reload 미실행 (stale) 상태.
            cacheEmpty = mappings.isNotEmpty() && cache.isEmpty(),
        )
    }

    private fun toItem(mapping: BranchMapping, orgNames: Map<String, String>): BranchMappingListItem {
        val branchCode = mapping.branchCode
        val expanded = branchCodeExpander.expand(setOf(branchCode)).sorted()
        val expandedCodes = expanded.map { code ->
            BranchMappingExpandedCode(
                code = code,
                orgName = orgNames[code],
                isSelf = code == branchCode,
            )
        }
        val type = classify(branchCode, expandedCodes)
        return BranchMappingListItem(
            branchCode = branchCode,
            label = mapping.label,
            orgName = orgNames[branchCode],
            type = type,
            typeLabel = type.label,
            expandedCodes = expandedCodes,
            expandedCount = expandedCodes.size,
            unresolvedCount = expandedCodes.count { it.orgName == null },
            rawIncludedBranchCodes = mapping.includedBranchCodes,
        )
    }

    /**
     * 매핑 성격 판정 — 운영 데이터 74건 실측 기준. 첫 매치 채택.
     *
     * 1. 확장이 자기 자신뿐 → [BranchMappingType.NONE]
     * 2. 자기 자신 미포함 (예: retail3 `5829` → `5826,5827,5828`) → [BranchMappingType.ROLLUP]
     * 3. 자기 자신 외에 현행 조직으로 해석되는 코드 존재 → [BranchMappingType.ROLLUP]
     *    단 `E{N}` ↔ `{N}` 쌍만 있는 경우는 동일 조직의 별칭이므로 [BranchMappingType.DUAL_CODE]
     * 4. 그 외 (해석 안 되는 코드 = 폐기 코드 포함) → [BranchMappingType.LEGACY]
     */
    private fun classify(branchCode: String, expanded: List<BranchMappingExpandedCode>): BranchMappingType {
        if (expanded.size <= 1) return BranchMappingType.NONE
        if (expanded.none { it.isSelf }) return BranchMappingType.ROLLUP

        val others = expanded.filterNot { it.isSelf }
        if (others.all { isDualCodePair(branchCode, it.code) }) return BranchMappingType.DUAL_CODE
        if (others.any { it.orgName != null }) return BranchMappingType.ROLLUP
        return BranchMappingType.LEGACY
    }

    /** `E5706` ↔ `5706` 처럼 `E` 접두 유무만 다른 동일 조직 별칭인지. */
    private fun isDualCodePair(a: String, b: String): Boolean =
        a.removePrefix(DUAL_CODE_PREFIX) == b.removePrefix(DUAL_CODE_PREFIX)

    /** 지점코드 / 라벨 / 조직명 / 원본 CSV / 확장 코드·조직명 전체를 대상으로 부분 일치. */
    private fun BranchMappingListItem.matches(keyword: String): Boolean {
        val haystack = buildList {
            add(branchCode)
            label?.let { add(it) }
            orgName?.let { add(it) }
            add(rawIncludedBranchCodes)
            expandedCodes.forEach { expandedCode ->
                add(expandedCode.code)
                expandedCode.orgName?.let { add(it) }
            }
        }
        return haystack.any { it.contains(keyword, ignoreCase = true) }
    }

    companion object {
        /** SF 운영 데이터의 이중코드 접두 (`E5706` ↔ `5706`). */
        private const val DUAL_CODE_PREFIX = "E"
    }
}
