package com.otoki.internal.common.dto.response

/**
 * 출근 거래처 목록 응답 DTO
 */
data class StoreListResponse(
    val stores: List<StoreInfo>,
    val totalCount: Int,
    val registeredCount: Int,
    val currentDate: String
)

/**
 * 거래처 정보 (GPS 좌표 포함)
 */
data class StoreInfo(
    val scheduleSfid: String,
    val storeSfid: String?,
    val storeName: String,
    val storeTypeCode: String?,
    val workCategory: String,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    val isRegistered: Boolean
)
