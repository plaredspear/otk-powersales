package com.otoki.powersales.organization.repository

import com.otoki.powersales.organization.entity.Organization
import org.springframework.data.jpa.repository.JpaRepository

interface OrganizationRepository : JpaRepository<Organization, Long>, OrganizationRepositoryCustom {
    // 단일 컬럼 derived 메서드 — 단일 레벨만 정확히 lookup 해야 하는 케이스 한정.
    // cascade (Level5→4 또는 Level5→4→3) 가 필요하면 OrganizationRepositoryCustom 의
    // `findFirstByCostCenterCascade` / `findFirstByOrgCodeCascade` 사용.
    fun findFirstByCostCenterLevel5(costCenterLevel5: String): Organization?
    fun findFirstByCostCenterLevel4(costCenterLevel4: String): Organization?

    /**
     * Level3 코스트센터 기준 하위 Level5 조직 목록 조회 (영업지원실 다중 지점)
     */
    fun findByCostCenterLevel3(costCenterLevel3: String): List<Organization>

    fun findFirstByOrgCodeLevel5(orgCodeLevel5: String): Organization?
    fun findFirstByOrgCodeLevel4(orgCodeLevel4: String): Organization?
    fun findFirstByOrgCodeLevel3(orgCodeLevel3: String): Organization?
}
