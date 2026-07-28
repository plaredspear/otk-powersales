package com.otoki.powersales.domain.org.organization.branchmapping

import com.otoki.powersales.domain.org.organization.branchmapping.repository.BranchMappingRepository
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component

/**
 * Spec #810 — SF `Util.getIncludedBranchCode(List<String>)` 의 backend 대응 헬퍼.
 *
 * 입력 = 조직코드 (컬럼 명명은 `cost_center_code` 이나 **실제 값은 OrgCode** —
 * [com.otoki.powersales.domain.org.organization.service.OrgCostCenterMatchService] KDoc 참조).
 * 출력 = 입력 + `BranchMapping` 확장 코드의 합집합 Set.
 *
 * ## ⚠️ 확장 결과는 "이력 코드" 만이 아니다
 * `BranchMapping.includedBranchCodes` 에는 폐기된 옛 코드뿐 아니라 **현행 타 조직 코드**(롤업)와
 * 동일 조직 별칭(`E{N}` ↔ `{N}`)이 섞여 있다. 따라서 [expand] 결과를 "구 코드까지 넓힌 같은 지점"
 * 으로 단정하면 안 되며, 롤업 행에서는 다른 조직의 데이터가 함께 조회된다.
 * 예: `5829`(retail3영업부) → `5826,5827,5828` (인천1·2·3지점). 성격 4종과 롤업 6건의 실측 목록은
 * [com.otoki.powersales.domain.org.organization.branchmapping.entity.BranchMapping] KDoc 참조.
 *
 * ## 화이트리스트 판정에 쓰지 말 것
 * 지점 스코프 가드는 **확장 전 원본 코드**로 판정하고, [expand] 는 판정 통과 후 실제 조회 필터에만
 * 적용한다 (`AdminTeamScheduleService.getMembers` / `AdminAttendInfoService.getMembers` 가 이 순서).
 * 화이트리스트 자체를 확장하면 롤업 행이 권한 범위를 넓히고, 자기 자신을 포함하지 않는 행
 * (예: `5829`)은 오히려 판정에서 탈락한다.
 *
 * 실제 확장 결과는 `시스템 > 지점 코드 맵핑` 화면에서 조직명과 함께 확인할 수 있다
 * ([com.otoki.powersales.domain.org.organization.branchmapping.service.AdminBranchMappingService]).
 *
 * ## 캐시 정책
 * `@PostConstruct` 부팅 1회 메모리 캐시 (DB `branch_mapping` 테이블 조회). 운영 중 BranchMapping 변경 없다고 가정
 * (테이블은 SF 마이그레이션 Stage1 CSV 로 적재 — admin UI 도입 시 무효화 메커니즘 별도).
 *
 * **주의**: 테이블이 Stage1 CSV (런타임 admin 트리거) 로 채워지므로, 빈 DB 로 부팅한 신규 환경은
 * Stage1 적재 직후 본 캐시가 stale (빈 상태) 로 남는다. 적재 후 1회 재부팅하거나 [reload] 로 갱신.
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
    // 런타임 reload (Stage1 적재 후) 가시성 보장 — @Volatile 로 참조 교체를 다른 스레드에 즉시 노출.
    @Volatile
    private var cache: Map<String, Set<String>> = emptyMap()

    @PostConstruct
    fun init() {
        reload()
    }

    /**
     * DB `branch_mapping` 테이블에서 캐시 재빌드.
     *
     * 부팅 시 [init] 1회 + SF 마이그레이션 Stage1 `BranchMapping` 적재 직후 (Stage1CopyController)
     * 호출. 빈 DB 로 부팅한 신규 환경이 Stage1 적재 후 stale (빈) 캐시로 남는 것을 방지.
     */
    fun reload() {
        cache = repository.findAll().associate { entity ->
            entity.branchCode to splitIncluded(entity.includedBranchCodes)
        }
    }

    /**
     * 현재 메모리 캐시 스냅샷 — 지점 코드 맵핑 조회 화면(`시스템 > 지점 코드 맵핑`) 진단용.
     *
     * DB 원본이 아니라 **런타임 캐시**를 그대로 노출한다. 캐시가 DB 와 어긋나 있으면(빈 DB 부팅 후
     * Stage1 적재 → [reload] 미호출 등, 클래스 KDoc 의 stale 시나리오) 화면이 그 사실을 그대로
     * 보여주는 것이 목적이므로, 여기서 DB 를 재조회하지 않는다.
     */
    fun snapshot(): Map<String, Set<String>> = cache

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
