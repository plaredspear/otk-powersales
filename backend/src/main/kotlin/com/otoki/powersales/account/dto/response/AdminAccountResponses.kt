package com.otoki.powersales.account.dto.response

import com.otoki.powersales.account.entity.Account

data class AccountListResponse(
    val content: List<AccountListItem>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

data class AccountListItem(
    val id: Int,
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
            id = account.id,
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

/**
 * 관리자 웹 신규 거래처 등록 응답 DTO. (Spec #640)
 *
 * `account_group` 은 자동 set `'9999'` 고정. `branch_code` 는 Employee.cost_center_code 직접 사용으로
 * 등록 시점에 항상 비-NULL. `branch_name` 은 Organization 매칭 결과의 deepest non-blank,
 * 매칭 실패 시 NULL.
 */
data class AdminAccountCreateResponse(
    val id: Int,
    val name: String,
    val accountGroup: String,
    val employeeCode: String,
    val branchCode: String?,
    val branchName: String?
) {
    companion object {
        fun from(account: Account): AdminAccountCreateResponse = AdminAccountCreateResponse(
            id = account.id,
            name = account.name ?: "",
            accountGroup = account.accountGroup ?: "",
            employeeCode = account.employeeCode ?: "",
            branchCode = account.branchCode,
            branchName = account.branchName
        )
    }
}
