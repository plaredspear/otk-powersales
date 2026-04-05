package com.otoki.internal.common.dto.response

/**
 * 출근 거래처 목록 응답 DTO
 */
data class AccountListResponse(
    val safetyCheckCompleted: Boolean,
    val accounts: List<AccountInfo>,
    val totalCount: Int,
    val registeredCount: Int,
    val currentDate: String,
    val registrationDeadline: String = "17:00",
    val isRegistrationClosed: Boolean = false
)

/**
 * 거래처 정보 (GPS 좌표 포함)
 */
data class AccountInfo(
    val scheduleId: Long? = null,
    val displayWorkScheduleId: Long? = null,
    val accountId: Int?,
    val accountName: String,
    val accountTypeCode: String?,
    val workCategory: String,
    val workCategory2: String? = null,
    val workCategory3: String? = null,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    val isRegistered: Boolean,
    val source: String = "schedule"
)
