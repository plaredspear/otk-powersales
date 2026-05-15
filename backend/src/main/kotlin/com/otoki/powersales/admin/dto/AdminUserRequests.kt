package com.otoki.powersales.admin.dto

/**
 * User 활성/비활성 토글 요청.
 */
data class UpdateUserActiveStatusRequest(
    val isActive: Boolean
)
