package com.otoki.powersales.schedule.dto.response

import com.otoki.powersales.account.entity.Account

/**
 * 조장의 본인 거래처 목록 응답 DTO (Spec #554 P1-B §3.5.2).
 */
data class LeaderAccountListResponse(
    val id: Int,
    val name: String?,
    val address1: String?,
    val branchCode: String?,
    val accountGroup: String?,
    val accountType: String?
) {
    companion object {
        fun from(entity: Account): LeaderAccountListResponse =
            LeaderAccountListResponse(
                id = entity.id,
                name = entity.name,
                address1 = entity.address1,
                branchCode = entity.branchCode,
                accountGroup = entity.accountGroup,
                accountType = entity.accountType?.displayName
            )
    }
}
