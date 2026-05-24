package com.otoki.powersales.organization.branchmapping

import com.otoki.powersales.organization.branchmapping.repository.BranchMappingRepository
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component

/**
 * Spec #810 — SF `Util.getIncludedBranchCode(List<String>)` 의 backend 대응 헬퍼.
 *
 * 입력 도메인 = **조직코드 (cost_center_code)** 단일 도메인.
 * 출력 = 입력 + BranchMapping 으로 확장된 이력 코드의 합집합 Set.
 *
 * ## 캐시 정책
 * `@PostConstruct` 부팅 1회 메모리 캐시. 운영 중 BranchMapping 변경 없다고 가정 (admin UI 도입 시 무효화 메커니즘 별도).
 *
 * ## SF 동작 대응
 * | SF `Util.cls:162-175` | backend |
 * |--------|--------|
 * | 입력 `orgValues` Set 초기화 | `result.addAll(branchCodes)` |
 * | `WHERE BranchCode__c IN :orgValues` SOQL | `cache[code]` Map lookup |
 * | `IncludedBranchCode__c.split(',')` 후 합집합 | `cache` 값 (이미 split + trim 된 Set) `addAll` |
 * | 매칭 없으면 자기 자신만 반환 | 동일 (pass-through) |
 */
@Component
class BranchCodeExpander(
    private val repository: BranchMappingRepository,
) {
    private lateinit var cache: Map<String, Set<String>>

    @PostConstruct
    fun init() {
        cache = repository.findAll().associate { entity ->
            entity.branchCode to splitIncluded(entity.includedBranchCodes)
        }
    }

    fun expand(branchCodes: Collection<String>): Set<String> {
        val result = mutableSetOf<String>()
        result.addAll(branchCodes)
        for (code in branchCodes) {
            cache[code]?.let { result.addAll(it) }
        }
        return result
    }

    companion object {
        /**
         * SF `IncludedBranchCode__c.split(',')` 대응 — 공백 / 빈 토큰 제거.
         * `KAM1` (BC=5721) 의 `"5721,E5721, 5466, 5693,5721,5466"` 같은 공백 포함 데이터 대응.
         */
        internal fun splitIncluded(csv: String): Set<String> =
            csv.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()
    }
}
