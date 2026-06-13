package com.otoki.powersales.domain.sales.sfsync

/**
 * SF `SalesProgressRateMaster__c` 한 건의 fetch 결과 (거래처목표등록마스터 sync 입력).
 *
 * SF Apex/REST 응답을 역직렬화한 raw 표현. upsert 매칭 키는 [externalKey] (`연+월+거래처코드`).
 * SF 응답이 ExternalKey 를 비워 보내는 경우 sync 서비스가 `targetYear + targetMonth + accountCode`
 * 로 재조합한다 (SF Trigger `beforeInsertSetExternerKey` 동등 — month leftPad 없음).
 *
 * Formula 필드(거래처명/지점명/유형, TargetSum, ProgressRate)는 신규 DB 에서 산출하므로 fetch 대상 아님.
 */
data class SalesProgressRateMasterFetchDto(
    /** SF 18자리 Id. 감사/추적용 — upsert 키 아님. */
    val sfid: String?,
    /** SF Name (SPR-xxxxxxxx). */
    val name: String?,
    /** SF ExternalKey__c (`연+월+거래처코드`). null/blank 면 sync 서비스가 재조합. */
    val externalKey: String?,
    val targetYear: String?,
    val targetMonth: String?,
    /** 거래처 ExternalKey (= AccountCode__c). 신규 DB account FK resolve 키. */
    val accountCode: String?,
    val rtTargetAmount: Double?,
    val frTargetAmount: Double?,
    val rmTargetAmount: Double?,
    val foTargetAmount: Double?,
    /** SF TargetSumAmount__c (운영 미사용 컬럼이나 적재 정합 위해 보존). */
    val targetSumAmount: Double?,
    val currentMonthSalesAmount: Double?,
    val previousMonthSalesAmount: Double?,
    val businessRate: Double?,
    val accountBranchView: String?,
    val accountBranchCode: String?,
    /** SF IsDeleted — fetch 결과에 포함될 경우 보존. */
    val isDeleted: Boolean?,
)
