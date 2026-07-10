package com.otoki.powersales.external.common.outboundlog

import com.otoki.powersales.external.sap.SapConstants

/**
 * 외부 HTTP outbound 호출의 uri 를 "탭 식별 key" 로 해석하는 SoT.
 *
 * `ExternalApiLog.endpointKey` 적재(인터셉터) 와 조회 필터가 동일한 규칙을 공유한다.
 * key 값은 web "외부 API 테스트" 페이지의 탭 key + `ExternalApiIntegrationInfoService` 의
 * `ExternalApiIntegrationInfo.key` 와 의도적으로 일치시켜, 각 탭이 자기 key 로 자신의 호출 이력만
 * 정확히 조회할 수 있게 한다 (uri 문자열 LIKE 매칭이 아니라 endpoint_key 동등 비교).
 *
 * 분류 기준:
 * - SAP: uri 의 마지막 path segment(= interfaceId `SDxxxxx`) → key
 * - SF : uri 의 마지막 path segment(= Apex REST resource) → key. OAuth 토큰 발급은 `sf-oauth-token`
 * - Naver: host 기반 → `naver-geocode`
 *
 * 매칭되지 않는 호출은 `null` 을 반환한다 (분류 불가 — "전체" 필터로만 노출).
 */
object ExternalApiEndpointKeyResolver {

    // SAP interfaceId → 탭 key (ExternalApiIntegrationInfoService.sapInterfaces 와 1:1)
    private val SAP_INTERFACE_TO_KEY: Map<String, String> = mapOf(
        SapConstants.SAP_INTERFACE_LOAN_INQUIRY to "loan-inquiry",
        SapConstants.SAP_INTERFACE_ORDER_REQUEST_DETAIL to "order-request-detail",
        SapConstants.SAP_INTERFACE_ORDER_REQUEST_CANCEL to "order-request-cancel",
        SapConstants.SAP_INTERFACE_ORDER_REQUEST_REGIST to "order-request-register",
        SapConstants.SAP_INTERFACE_ATTENDANCE to "attendance",
        SapConstants.SAP_INTERFACE_DISPLAY_MASTER to "display-master",
        SapConstants.SAP_INTERFACE_PPT_MASTER to "ppt-master",
    )

    // SF Apex REST resource(마지막 segment) → 탭 key (ExternalApiIntegrationInfoService 의 SF 항목과 1:1)
    private val SF_RESOURCE_TO_KEY: Map<String, String> = mapOf(
        "ClaimRegist" to "claim-regist",
        // 운영 물류클레임 등록(제안하기 > 물류클레임 dual-write) 이 실제 호출하는 endpoint.
        "ProposalRegist" to "logistics-claim-regist",
        "IF_SendClaimToPWS" to "claim-status-update",
        "IF_SendLogisticsClaimToPWS" to "logistics-claim-status-update",
        "IF_salesprogresssend" to "sales-progress-rate-master-sync",
        "IF_SendStaffReviewToPWS" to "staff-review-sync",
    )

    private const val NAVER_GEOCODE_HOST = "maps.apigw.ntruss.com"
    private const val SF_OAUTH_KEY = "sf-oauth-token"
    private const val NAVER_GEOCODE_KEY = "naver-geocode"

    /** web 외부 API 테스트 탭에서 필터 셀렉터로 노출할 수 있는 전체 key 목록 (resolver SoT). */
    val ALL_KEYS: List<String> =
        SAP_INTERFACE_TO_KEY.values.toList() +
            SF_RESOURCE_TO_KEY.values.toList() +
            listOf(SF_OAUTH_KEY, NAVER_GEOCODE_KEY)

    /**
     * @param target 호출 대상 시스템 ([ExternalApiTarget] 상수)
     * @param uri    호출 uri (query string 포함 가능)
     * @return 탭 식별 key. 분류 불가 시 null.
     */
    fun resolve(target: String, uri: String): String? {
        val lastSegment = lastPathSegment(uri)
        return when (target) {
            ExternalApiTarget.SAP -> SAP_INTERFACE_TO_KEY[lastSegment]
            ExternalApiTarget.SF -> SF_RESOURCE_TO_KEY[lastSegment]
                ?: if (isOAuthTokenUri(uri)) SF_OAUTH_KEY else null
            ExternalApiTarget.NAVER -> if (uri.contains(NAVER_GEOCODE_HOST)) NAVER_GEOCODE_KEY else null
            else -> null
        }
    }

    /** uri 의 path 마지막 segment(query string 제외). 예: `http://h:5/RESTAdapter/SD03040?x=1` → `SD03040`. */
    private fun lastPathSegment(uri: String): String {
        val path = uri.substringBefore('?').substringBefore('#').trimEnd('/')
        return path.substringAfterLast('/')
    }

    /** SF OAuth2 토큰 발급 endpoint 여부 (`.../oauth2/token`). */
    private fun isOAuthTokenUri(uri: String): Boolean {
        val path = uri.substringBefore('?')
        return path.contains("/oauth2/token") || path.endsWith("/token")
    }
}
