package com.otoki.powersales.organization.repository

import com.otoki.powersales.organization.entity.Organization
import org.springframework.data.jpa.repository.JpaRepository

interface OrganizationRepository : JpaRepository<Organization, Long>, OrganizationRepositoryCustom {
    fun findFirstByCostCenterLevel5(costCenterLevel5: String): Organization?
    fun findFirstByCostCenterLevel4(costCenterLevel4: String): Organization?

    /**
     * Level3 코스트센터 기준 하위 Level5 조직 목록 조회 (영업지원실 다중 지점)
     */
    fun findByCostCenterLevel3(costCenterLevel3: String): List<Organization>
}
