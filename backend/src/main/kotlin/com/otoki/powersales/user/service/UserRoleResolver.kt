package com.otoki.powersales.user.service

import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.organization.repository.OrganizationRepository
import com.otoki.powersales.platform.auth.permission.SalesSupportTeam2Policy
import org.springframework.stereotype.Service

/**
 * Spec #759 — 영업지원실 / 영업본부 소속 여부 산출 도메인 서비스.
 *
 * 레거시 매핑: SF `CurrentUserBranchNameList.cls:42-51` `isSalesSupportOffice()` 등에서
 *   `UserRole.Name LIKE '%영업지원%' OR UserRole.Name == '영업본부'` 패턴.
 *
 * 신규 backend 는 SF UserRole Object 를 보존하지 않으므로 `Organization` cascade lookup
 * 후 매칭 row 의 `OrgNameLevel4` / `OrgNameLevel3` 둘 다 검사한다.
 *  - Level4 후보: "영업지원1팀" 등 → contains("영업지원") 매칭
 *  - Level3 후보: "영업본부" → 정확 일치
 *
 * **영업지원2팀 제외 (2026-08-02)**: [SalesSupportTeam2Policy] 참조. 조직명이 `"영업지원2팀"` /
 * 상위 `"영업지원실"` 이라 종전엔 부분일치로 전사가 열렸으나, 일반 지점과 동일한 스코프로 되돌린다.
 *
 * 동작 요약:
 *  - 입력: `Employee.costCenterCode`
 *  - 외부 호출: `OrganizationRepository.findFirstByOrgCodeCascade()`
 *  - 분기: 영업지원2팀 제외 → 4가지 (Level4 contains / Level3 contains / Level4 == 영업본부 / Level3 == 영업본부)
 *  - 부수 효과: 없음 (순수 산출).
 */
@Service
class UserRoleResolver(
    private val organizationRepository: OrganizationRepository,
) {

    /**
     * Employee 의 영업지원실 / 영업본부 소속 여부 산출.
     *
     * 영업지원2팀([SalesSupportTeam2Policy]) 은 제외 — 전사가 아니라 본인 지점 스코프.
     * Org__c lookup 실패 시 `false` 반환.
     */
    fun isSalesSupport(employee: Employee): Boolean {
        val costCenterCode = employee.costCenterCode
        if (costCenterCode.isNullOrEmpty()) return false
        if (SalesSupportTeam2Policy.isTeam2(costCenterCode)) return false
        // SF AppointmentTriggerHanlder.cls:264-271, 297-311 동등 — org_code Level5 → Level4 → Level3 cascade.
        // cascade 정책은 OrganizationRepository.findFirstByOrgCodeCascade 에 응집.
        val org = organizationRepository.findFirstByOrgCodeCascade(costCenterCode)
            ?: return false
        val level4 = org.orgNameLevel4.orEmpty()
        val level3 = org.orgNameLevel3.orEmpty()
        // 조직코드 정확일치와 별개로 조직명으로도 차단 — 향후 영업지원2팀 하위에 Level5 조직이 생겨
        // costCenterCode 가 4889 가 아닌 사원이 배치돼도 상위 "영업지원실" 부분일치로 전사가 열리지 않게 한다.
        if (level4 == SalesSupportTeam2Policy.ORG_NAME) return false
        return level4.contains(SALES_SUPPORT_KEYWORD) || level3.contains(SALES_SUPPORT_KEYWORD) ||
            level4 == SALES_HQ_NAME || level3 == SALES_HQ_NAME
    }

    /**
     * 마이그레이션 helper — SF `UserRole.Name` 직접 평가.
     *
     * SF `CurrentUserBranchNameList.cls:44` 의 `UserRole.Name LIKE '%영업지원%' OR == '영업본부'` 동등.
     * SalesforceMigrationTool 의 User 적재 시 본 함수로 초기값 산출 (별도 spec).
     *
     * [isSalesSupport] 와 동일하게 영업지원2팀([SalesSupportTeam2Policy]) 은 제외 — 적재 초기값과
     * 런타임 산출값이 갈라지지 않게 한다.
     */
    fun isSalesSupportFromUserRoleName(userRoleName: String?): Boolean {
        if (userRoleName.isNullOrEmpty()) return false
        if (userRoleName == SalesSupportTeam2Policy.ORG_NAME) return false
        return userRoleName.contains(SALES_SUPPORT_KEYWORD) || userRoleName == SALES_HQ_NAME
    }

    companion object {
        private const val SALES_SUPPORT_KEYWORD = "영업지원"
        private const val SALES_HQ_NAME = "영업본부"
    }
}
