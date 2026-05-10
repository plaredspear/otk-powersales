package com.otoki.powersales.common.service.dto

/**
 * 시스템 공통 코드 UPSERT 실패 행.
 *
 * - [identifier] : 식별자 (UPSERT 키 또는 부분 키 값. 필수 필드 누락 시 null)
 * - [reason] : 실패 사유 (예: `"CompanyCode 필수"`, `"GroupCode 필수"`)
 */
data class SystemCodeMasterUpsertFailedRow(
    val identifier: String?,
    val reason: String
)
