package com.otoki.powersales.common.dto.response

import com.otoki.powersales.domain.foundation.account.entity.Account

/**
 * 내 거래처 목록 응답 DTO
 */
data class MyAccountListResponse(
    val accounts: List<MyAccountInfo>,
    val totalCount: Int
)

/**
 * 내 거래처 정보
 */
data class MyAccountInfo(
    val accountId: Long,
    val accountName: String,
    val accountCode: String,
    val address: String?,
    val addressDetail: String?,
    val representativeName: String?,
    val phoneNumber: String?
) {
    companion object {
        fun from(account: Account): MyAccountInfo = MyAccountInfo(
            accountId = account.id.toLong(),
            accountName = account.name ?: "",
            accountCode = account.externalKey ?: "",
            address = account.address1,
            addressDetail = account.address2,
            representativeName = account.representative,
            phoneNumber = account.phone
        )
    }
}
