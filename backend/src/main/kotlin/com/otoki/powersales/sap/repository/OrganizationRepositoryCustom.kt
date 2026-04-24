package com.otoki.powersales.sap.repository

import com.otoki.powersales.sap.entity.Organization

interface OrganizationRepositoryCustom {

    fun searchForAdmin(
        keyword: String?,
        level: String?,
        branchCodes: List<String>?
    ): List<Organization>

    fun expandCostCenterCodes(costCenterCodes: List<String>): List<String>
}
