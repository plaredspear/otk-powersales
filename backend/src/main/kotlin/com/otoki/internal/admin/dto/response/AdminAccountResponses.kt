package com.otoki.internal.admin.dto.response

import com.otoki.internal.sap.entity.Account

data class AccountListResponse(
    val content: List<AccountListItem>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

data class AccountListItem(
    val externalKey: String?,
    val name: String?,
    val abcType: String?,
    val branchCode: String?,
    val branchName: String?,
    val employeeCode: String?,
    val address1: String?,
    val phone: String?,
    val accountStatusName: String?
) {
    companion object {
        fun from(account: Account): AccountListItem = AccountListItem(
            externalKey = account.externalKey,
            name = account.name,
            abcType = account.abcType,
            branchCode = account.branchCode,
            branchName = account.branchName,
            employeeCode = account.employeeCode,
            address1 = account.address1,
            phone = account.phone,
            accountStatusName = account.accountStatusName
        )
    }
}
