package com.otoki.internal.dto.response

/**
 * 오늘 출근 거래처 목록 응답 DTO
 */
data class StoreListResponse(
    val workerType: String,
    val stores: List<StoreInfo>,
    val totalCount: Int,
    val registeredCount: Int,
    val currentDate: String
)

/**
 * 거래처 정보
 */
data class StoreInfo(
    val storeId: Long,
    val storeName: String,
    val storeCode: String,
    val workCategory: String,
    val address: String?,
    val isRegistered: Boolean,
    val registeredWorkType: String?
)
