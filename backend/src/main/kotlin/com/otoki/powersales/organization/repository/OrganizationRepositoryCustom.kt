package com.otoki.powersales.organization.repository

import com.otoki.powersales.organization.entity.Organization

interface OrganizationRepositoryCustom {

    fun searchForAdmin(
        keyword: String?,
        level: String?,
        branchCodes: List<String>?
    ): List<Organization>

    fun expandCostCenterCodes(costCenterCodes: List<String>): List<String>

    fun findFirstByAnyOrgCodeLevel(orgCode: String): Organization?
}
