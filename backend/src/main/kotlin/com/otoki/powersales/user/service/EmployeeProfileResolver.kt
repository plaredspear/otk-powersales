package com.otoki.powersales.user.service

import com.otoki.powersales.platform.auth.repository.ProfileRepository
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.organization.repository.OrganizationRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Employee 정보 기반 Profile.id 동적 산출 도메인 서비스.
 *
 * 레거시 매핑: SF `AppointmentTriggerHanlder.cls:233-365` (updateUser future) 의 10개 분기.
 * 동작 요약 (입력 / 분기 / 외부 호출 / 부수 효과):
 *  - 입력: `Employee.costCenterCode` + `Employee.jikchak`
 *  - 외부 호출: `OrganizationRepository.findFirstByOrgCodeCascade()` — Level5/4/3 cascade
 *  - 분기: 10개 (마케팅 → 지원실 → 판매전략팀 → BS/SP팀 → 조장/팀장 → 지점장/팀장 →
 *           사업부장/실장 → 본부장 → 영업부장 → 영업사원 default)
 *  - 부수 효과: 없음 (순수 산출). User.profile_id 갱신은 호출자(AppointmentInsertService 등) 책임.
 *  - 신규 차이: SF `if (orgInfoTmp != null)` 가드를 명시적 `Staff` 디폴트로 강화 (silently skip 대신).
 */
@Service
class EmployeeProfileResolver(
    private val organizationRepository: OrganizationRepository,
    private val profileRepository: ProfileRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 사원의 Profile.name 산출 — SF AppointmentTriggerHanlder 10개 분기 정합.
     *
     * Org__c lookup 실패 시 `Staff` 디폴트 (SF 레거시 `orgInfoTmp == null` 시 갱신 skip 동등).
     * `SystemAdmin` 케이스는 본 분기 외 — 운영자가 별도 set.
     */
    private fun resolveProfileName(employee: Employee): String {
        val costCenterCodeRaw = employee.costCenterCode
        if (costCenterCodeRaw.isNullOrEmpty()) return PROFILE_STAFF
        val org = organizationRepository.findFirstByOrgCodeCascade(costCenterCodeRaw)
            ?: return PROFILE_STAFF
        val orgCodeLevel3 = org.orgCodeLevel3
        val jikchak = employee.jikchak.orEmpty()
        val costCenterCode = costCenterCodeRaw

        return when {
            orgCodeLevel3 == "5066" -> PROFILE_MARKETING
            orgCodeLevel3 == "3475" && !(jikchak.contains("판매") || jikchak == "조장") -> PROFILE_STAFF
            orgCodeLevel3 == "3472" -> PROFILE_STAFF
            costCenterCode in BS_SP_TEAM_CODES -> PROFILE_STAFF
            jikchak.contains("조장") || jikchak == "판매팀장" -> PROFILE_TEAM_LEADER
            jikchak.contains("지점장") || jikchak.contains("팀장") -> PROFILE_BRANCH_MANAGER
            jikchak.contains("부장") && (jikchak.contains("사업") || jikchak == "실장") -> PROFILE_BUSINESS_DIRECTOR
            jikchak == "본부장" -> PROFILE_DIVISION_HEAD
            jikchak.contains("부장") -> PROFILE_SALES_MANAGER
            else -> PROFILE_SALES_REP
        }
    }

    /**
     * 사원의 Profile.id 산출 — Profile.name → id lookup.
     *
     * Profile entity 부재 시 null 반환 — 호출자가 silently skip. dev/prod 는 SF Stage1 Profile 적재가, local 은 LocalDataInitializer 가 12종 보장.
     */
    fun resolveProfileId(employee: Employee): Long? {
        val name = resolveProfileName(employee)
        val profile = profileRepository.findByName(name)
        if (profile == null) {
            log.warn("[EmployeeProfileResolver] Profile.name='{}' lookup 실패 — SF Stage1 적재 또는 LocalDataInitializer 시드 누락 의심", name)
            return null
        }
        return profile.id
    }

    companion object {
        // SF AppointmentTriggerHanlder:336-339 — BS팀/SP팀 cost center code (직책 무관 STAFF)
        private val BS_SP_TEAM_CODES: Set<String> = setOf("5397", "5398", "5639")

        // Profile.name SoT — SystemAdminProfilePolicy.REQUIRED_PROFILE_NAMES 와 1:1 정합.
        private const val PROFILE_MARKETING = "8.마케팅"
        private const val PROFILE_STAFF = "9. Staff"
        private const val PROFILE_TEAM_LEADER = "6.조장"
        private const val PROFILE_BRANCH_MANAGER = "4.지점장"
        private const val PROFILE_SALES_MANAGER = "3.영업부장"
        private const val PROFILE_BUSINESS_DIRECTOR = "2.사업부장"
        private const val PROFILE_DIVISION_HEAD = "1.본부장"
        private const val PROFILE_SALES_REP = "5.영업사원"
    }
}
