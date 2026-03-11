package com.otoki.internal.sap.repository

import com.otoki.internal.sap.entity.Organization

interface OrganizationRepositoryCustom {

    fun searchForAdmin(
        keyword: String?,
        level: String?,
        branchCodes: List<String>?
    ): List<Organization>
}
