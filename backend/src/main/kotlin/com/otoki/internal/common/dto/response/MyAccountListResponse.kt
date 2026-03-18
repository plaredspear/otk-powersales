package com.otoki.internal.common.dto.response

/**
 * 내 거래처 목록 응답 DTO
 */
data class MyAccountListResponse(
    val stores: List<MyAccountInfo>,
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
    val representativeName: String?,
    val phoneNumber: String?
)
