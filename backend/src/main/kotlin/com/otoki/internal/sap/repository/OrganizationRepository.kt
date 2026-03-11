package com.otoki.internal.sap.repository

import com.otoki.internal.sap.entity.Organization
import org.springframework.data.jpa.repository.JpaRepository

interface OrganizationRepository : JpaRepository<Organization, Long>, OrganizationRepositoryCustom {
    fun findFirstByCostCenterLevel5(costCenterLevel5: String): Organization?
    fun findFirstByCostCenterLevel4(costCenterLevel4: String): Organization?
}
