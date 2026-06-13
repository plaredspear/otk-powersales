package com.otoki.powersales.domain.foundation.account.service.dto

/**
 * 거래처 카테고리 마스터 UPSERT 도메인 입력 커맨드.
 *
 * 외부 채널(SAP 인바운드 등) 의 페이로드를 [com.otoki.powersales.domain.foundation.account.service.AccountCategoryUpsertService] 가 받기 위한 도메인 용어 모델.
 *
 * - [accountCode] : UPSERT 키 (= [com.otoki.powersales.domain.foundation.account.entity.AccountCategoryMaster.accountCode])
 */
data class AccountCategoryUpsertCommand(
    val accountCode: String?,
    val name: String?
)
