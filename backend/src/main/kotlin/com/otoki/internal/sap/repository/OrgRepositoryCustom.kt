package com.otoki.internal.sap.repository

import com.otoki.internal.sap.entity.Org

interface OrgRepositoryCustom {

    fun searchForAdmin(
        keyword: String?,
        level: String?,
        branchCodes: List<String>?
    ): List<Org>
}
