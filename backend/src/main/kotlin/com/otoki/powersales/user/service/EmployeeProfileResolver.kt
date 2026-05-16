package com.otoki.powersales.user.service

import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.organization.repository.OrganizationRepository
import com.otoki.powersales.user.entity.ProfileType
import org.springframework.stereotype.Service

/**
 * Spec #759 — Employee 정보 기반 ProfileType 동적 산출 도메인 서비스.
 *
 * 레거시 매핑: SF `AppointmentTriggerHanlder.cls:233-365` (updateUser future) 의 10개 분기.
 * 동작 요약 (입력 / 분기 / 외부 호출 / 부수 효과):
 *  - 입력: `Employee.costCenterCode` + `Employee.jikchak`
 *  - 외부 호출: `OrganizationRepository.findFirstByOrgCodeCascade()` — Level5/4/3 cascade
 *  - 분기: 10개 (마케팅 → 지원실 → 판매전략팀 → BS/SP팀 → 조장/팀장 → 지점장/팀장 →
 *           사업부장/실장 → 본부장 → 영업부장 → 영업사원 default)
 *  - 부수 효과: 없음 (순수 산출). User cache 갱신은 호출자(AppointmentInsertService 등) 책임.
 *  - 신규 차이: SF `if (orgInfoTmp != null)` 가드를 명시적 `STAFF` 디폴트로 강화 (silently skip 대신).
 */
@Service
class EmployeeProfileResolver(
    private val organizationRepository: OrganizationRepository,
) {

    /**
     * 사원의 ProfileType 산출.
     *
     * Org__c lookup 실패 시 `STAFF` 디폴트 (SF 레거시 `orgInfoTmp == null` 시 갱신 skip 동등).
     * `SystemAdmin` 케이스는 본 분기 외 — 운영자가 별도 set.
     */
    fun resolve(employee: Employee): ProfileType {
        val costCenterCodeRaw = employee.costCenterCode
        if (costCenterCodeRaw.isNullOrEmpty()) return ProfileType.STAFF
        // SF AppointmentTriggerHanlder.cls:264-271, 297-311 동등 — org_code Level5 → Level4 → Level3 cascade.
        // cascade 정책은 OrganizationRepository.findFirstByOrgCodeCascade 에 응집.
        val org = organizationRepository.findFirstByOrgCodeCascade(costCenterCodeRaw)
            ?: return ProfileType.STAFF
        val orgCodeLevel3 = org.orgCodeLevel3
        val jikchak = employee.jikchak.orEmpty()
        val costCenterCode = costCenterCodeRaw

        return when {
            orgCodeLevel3 == "5066" -> ProfileType.MARKETING
            orgCodeLevel3 == "3475" && !(jikchak.contains("판매") || jikchak == "조장") -> ProfileType.STAFF
            orgCodeLevel3 == "3472" -> ProfileType.STAFF
            costCenterCode in BS_SP_TEAM_CODES -> ProfileType.STAFF
            jikchak.contains("조장") || jikchak == "판매팀장" -> ProfileType.TEAM_LEADER
            jikchak.contains("지점장") || jikchak.contains("팀장") -> ProfileType.BRANCH_MANAGER
            jikchak.contains("부장") && (jikchak.contains("사업") || jikchak == "실장") -> ProfileType.BUSINESS_DIRECTOR
            jikchak == "본부장" -> ProfileType.DIVISION_HEAD
            jikchak.contains("부장") -> ProfileType.SALES_MANAGER
            else -> ProfileType.SALES_REP
        }
    }

    companion object {
        // SF AppointmentTriggerHanlder:336-339 — BS팀/SP팀 cost center code (직책 무관 STAFF)
        private val BS_SP_TEAM_CODES: Set<String> = setOf("5397", "5398", "5639")
    }
}
