package com.otoki.powersales.domain.sales.sfsync

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * SF `IF_salesprogresssend` Response 한 건의 raw 역직렬화 표현 ("알라딘 거래처목표 마스터 API" 문서 정합).
 *
 * PDF Response 표의 PascalCase 필드명을 [JsonProperty] 로 바인딩한다. SF 응답에 문서 외 필드가 추가로
 * 오더라도 무시하도록 [JsonIgnoreProperties] `ignoreUnknown=true` 를 둔다.
 *
 * 거래처목표 금액/영업률/진행률 등은 문서상 모두 String 타입이라 SF 가 숫자를 문자열로 보낼 수 있으므로,
 * [toFetchDto] 에서 안전 파싱([parseDouble])하여 [SalesProgressRateMasterFetchDto] 로 변환한다.
 * 'FOTartgetAmount'/'RMTartgetAmount' 의 오타(Tartget)는 문서 표기를 그대로 따른 것이며, SF 실제 응답
 * key 철자가 다르면 해당 필드가 null 로 매핑되므로 운영 응답 확인 후 정정한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SalesProgressRateMasterSfRecord(
    @JsonProperty("Id") val id: String? = null,
    @JsonProperty("Name") val name: String? = null,
    @JsonProperty("ExternalKey") val externalKey: String? = null,
    @JsonProperty("AccountCode") val accountCode: String? = null,
    @JsonProperty("BusinessRate") val businessRate: String? = null,
    @JsonProperty("FOTartgetAmount") val foTargetAmount: String? = null,
    @JsonProperty("FRTargetAmount") val frTargetAmount: String? = null,
    @JsonProperty("RMTartgetAmount") val rmTargetAmount: String? = null,
    @JsonProperty("RTTargetAmount") val rtTargetAmount: String? = null,
    @JsonProperty("TargetMonth") val targetMonth: String? = null,
    @JsonProperty("TargetSumAmount") val targetSumAmount: String? = null,
    @JsonProperty("TargetYear") val targetYear: String? = null,
    @JsonProperty("ProgressRate") val progressRate: String? = null,
) {

    /** PDF String 필드 → [SalesProgressRateMasterFetchDto] (숫자 필드는 안전 파싱). */
    fun toFetchDto(): SalesProgressRateMasterFetchDto = SalesProgressRateMasterFetchDto(
        sfid = id?.takeIf { it.isNotBlank() },
        name = name?.takeIf { it.isNotBlank() },
        externalKey = externalKey?.takeIf { it.isNotBlank() },
        targetYear = targetYear?.takeIf { it.isNotBlank() },
        targetMonth = targetMonth?.takeIf { it.isNotBlank() },
        accountCode = accountCode?.takeIf { it.isNotBlank() },
        rtTargetAmount = parseDouble(rtTargetAmount),
        frTargetAmount = parseDouble(frTargetAmount),
        rmTargetAmount = parseDouble(rmTargetAmount),
        foTargetAmount = parseDouble(foTargetAmount),
        targetSumAmount = parseDouble(targetSumAmount),
        // 문서 Response 에 없는 컬럼(현/전월 실적·지점). 신규 DB 가 산출/보강하므로 fetch 비대상 → null.
        currentMonthSalesAmount = null,
        previousMonthSalesAmount = null,
        businessRate = parseDouble(businessRate),
        accountBranchView = null,
        accountBranchCode = null,
        isDeleted = null,
    )

    private fun parseDouble(raw: String?): Double? =
        raw?.takeIf { it.isNotBlank() }?.replace(",", "")?.trim()?.toDoubleOrNull()
}
