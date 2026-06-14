package com.otoki.powersales.platform.common.service.dto

/**
 * 시스템 공통 코드 마스터 UPSERT 도메인 입력 커맨드.
 *
 * - UPSERT 키: `companyCode + ';' + groupCode + ';' + detailCode` (= [com.otoki.powersales.platform.common.entity.SystemCodeMaster.externalKey])
 */
data class SystemCodeMasterUpsertCommand(
    val companyCode: String?,
    val groupCode: String?,
    val detailCode: String?,
    val groupCodeName: String?,
    val detailCodeName: String?,
    val seq: String?
)
