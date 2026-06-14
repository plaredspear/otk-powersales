package com.otoki.powersales.domain.org.organization.service

import com.otoki.powersales.domain.org.organization.repository.OrganizationRepository
import org.springframework.stereotype.Service
import java.util.Optional

/**
 * Org 조직 코드 기준 CostCenter 코드 매칭 Service.
 *
 * Employee 의 `costCenterCode` 필드(레거시 명명상 "CostCenterCode" 이나 실제 값은 OrgCode)를 입력받아,
 * Organization 의 `orgCodeLevel2~5` 중 매칭되는 레벨의 `costCenterLevel*` 코드를 반환한다.
 *
 * ## 레거시 매핑
 * - SF Apex: `OrgCCCodeMatchHelper.cls#getMatchingCCCode`
 * - 호출처:
 *   - `IF_REST_MOBILE_ProposalRegist.cls:184` — Proposal (Suggestion) 등록 시 `OrgCostCenterCode__c` 매핑
 *   - `SalesDiaryTriggerHandler.cls:46` — SalesDiary BeforeInsert trigger 의 CostCenter 매핑
 * - origin spec: #666
 *
 * ## 레거시 동작 요약
 * 1. 입력: `inputValue` (호출자 측 `Employee.CostCenterCode__c` — 실제 의미상 OrgCode)
 * 2. 정적 Map 캐시 조회 (트랜잭션 스코프) → 캐시 히트 시 즉시 반환
 * 3. Org__c 조회: WHERE `OrgCodeLevel2/3/4/5 = :inputValue` LIMIT 1
 * 4. 매칭 분기 (Level5 → Level4 → Level3 → Level2 순):
 *    - inputValue == OrgCodeLevel5 → CostCenterLevel5 반환
 *    - inputValue == OrgCodeLevel4 → CostCenterLevel4 반환
 *    - inputValue == OrgCodeLevel3 → CostCenterLevel3 반환
 *    - inputValue == OrgCodeLevel2 → CostCenterLevel2 반환
 * 5. 결과 없으면 빈 문자열(`""`) 반환. 매칭 분기 모두 실패 시 null (Apex `else if` chain 종료)
 * 6. 캐시에 결과 저장 후 반환
 *
 * ## 신규 차이
 * - **반환 타입**: `String` (null/빈 문자열 혼용) → **`Optional<String>`**. 호출자 측에서 `.orElse(null)` 로 레거시 swallow 패턴 재현 가능. 이유: 결과 부재 의미 명시화 + Kotlin 호출 시 호환. 스펙 #666 Q3 옵션 1.
 * - **매칭 결과 없음 표현**: 빈 문자열(`""`) → **`Optional.empty()`**. 빈 문자열·null 혼용 제거. 이유: 결과 없음의 단일 표현. 스펙 #666 Q3 옵션 1.
 * - **캐시**: 정적 Map 캐시 → **캐시 미적용**. 이유: 신규 시스템 캐시 인프라(Caffeine 등) 미도입 상태. Organization 조회 빈도와 latency 검증 후 별도 후속 스펙으로 캐시 도입 검토. 스펙 #666 Q4 옵션 2(대체 채택).
 * - **try/catch swallow**: 호출자 측 패턴 폐지 → **service 는 Optional 반환만 책임**. 호출자가 `.orElse(null)` 또는 `.orElseThrow()` 로 명시적 처리. 스펙 #666 Q3 옵션 1.
 */
@Service
class OrgCostCenterMatchService(
    private val organizationRepository: OrganizationRepository,
) {

    /**
     * Employee 의 CostCenterCode(실제 OrgCode 값) 를 받아 매칭 organization 의 CostCenter 코드를 반환.
     *
     * 매칭 우선순위: Level5 → Level4 → Level3 → Level2 (레거시 동등).
     * 결과 없거나 매칭 실패 시 `Optional.empty()` 반환.
     */
    fun findMatchingCostCenterCode(employeeCostCenterCode: String): Optional<String> {
        if (employeeCostCenterCode.isBlank()) return Optional.empty()

        val org = organizationRepository.findFirstByAnyOrgCodeLevel(employeeCostCenterCode)
            ?: return Optional.empty()

        val matched = when (employeeCostCenterCode) {
            org.orgCodeLevel5 -> org.costCenterLevel5
            org.orgCodeLevel4 -> org.costCenterLevel4
            org.orgCodeLevel3 -> org.costCenterLevel3
            org.orgCodeLevel2 -> org.costCenterLevel2
            else -> null
        }

        return Optional.ofNullable(matched)
    }
}
