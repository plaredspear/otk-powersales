package com.otoki.internal.dto.response

/**
 * 내 거래처 목록 응답 DTO
 */
data class MyStoreListResponse(
    val stores: List<MyStoreInfo>,
    val totalCount: Int
)

/**
 * 내 거래처 정보
 */
data class MyStoreInfo(
    val storeId: Long,
    val storeName: String,
    val storeCode: String,
    val address: String?,
    val representativeName: String?,
    val phoneNumber: String?
)
