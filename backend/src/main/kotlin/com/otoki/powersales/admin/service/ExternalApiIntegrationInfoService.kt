package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.response.ExternalApiIntegrationInfo
import com.otoki.powersales.admin.dto.response.ExternalApiIntegrationInfoResponse
import com.otoki.powersales.external.sap.SapConstants
import com.otoki.powersales.external.sap.outbound.config.SapOutboundProperties
import com.otoki.powersales.external.sf.outbound.SfOutboundProperties
import org.springframework.stereotype.Service

/**
 * 외부 API 연동 정보 조회 서비스 (개발자 도구 — 외부 API 테스트).
 *
 * web 의 각 외부 API 테스트 탭이 "요청 시 필수 연동 정보"(외부 시스템 endpoint / HTTP method / 인증 방식)를
 * 노출하도록, 현재 환경에 주입된 실제 설정값([SfOutboundProperties], [SapOutboundProperties])을 조회해
 * 탭별 메타로 조립한다.
 *
 * 인증 secret(비밀번호 / client secret)은 절대 응답에 포함하지 않고 방식만 표기한다.
 * SAP 7개 인터페이스는 모두 `sap.outbound.base-url` + `/{interfaceId}` 구조이며 interfaceId 만 다르다.
 */
@Service
class ExternalApiIntegrationInfoService(
    private val sfOutboundProperties: SfOutboundProperties,
    private val sapOutboundProperties: SapOutboundProperties,
) {

    fun getIntegrationInfo(): ExternalApiIntegrationInfoResponse {
        val items = buildList {
            add(naverGeocode())
            add(claimRegist())
            add(claimMasterSync())
            add(logisticsClaimMasterSync())
            add(salesProgressRateMasterSync())
            add(staffReviewSync())
            addAll(sapInterfaces())
        }
        return ExternalApiIntegrationInfoResponse(items)
    }

    private fun naverGeocode() = ExternalApiIntegrationInfo(
        key = "naver-geocode",
        externalSystem = "Naver Cloud Platform (Maps Geocode)",
        endpoint = NAVER_GEOCODE_ENDPOINT,
        httpMethod = "GET",
        authType = "NCP API Key (x-ncp-apigw-api-key-id / x-ncp-apigw-api-key 헤더)",
        note = "query 파라미터로 주소 전송. 환경변수 prefix: app.naver.geocode.*",
    )

    private fun claimRegist() = ExternalApiIntegrationInfo(
        key = "claim-regist",
        externalSystem = "Salesforce (Apex REST)",
        endpoint = joinUrl(sfOutboundProperties.apexBaseUrl, "/ClaimRegist"),
        httpMethod = "POST",
        authType = "OAuth2 Password Grant (Bearer) — token: ${blankOr(sfOutboundProperties.oauth.tokenUrl)}",
        note = "Content-Type: application/json. 401 시 토큰 재발급 후 1회 재시도. 환경변수 prefix: sf.outbound.*",
    )

    // key 는 web 탭 식별자("SF 클레임 상태 업데이트" 탭 = claim-status-update)에 의도적으로 맞춘다.
    // 백엔드 endpoint/DTO/서비스는 인터페이스 실체에 맞춰 claim-master-sync 계열로 명명하여 두 갈래로 갈린다.
    private fun claimMasterSync() = ExternalApiIntegrationInfo(
        key = "claim-status-update",
        externalSystem = "Salesforce (Apex REST)",
        endpoint = joinUrl(sfOutboundProperties.apexBaseUrl, "/IF_SendClaimToPWS"),
        httpMethod = "POST",
        authType = "OAuth2 Password Grant (Bearer) — token: ${blankOr(sfOutboundProperties.oauth.tokenUrl)}",
        note = "Content-Type: application/json. Request body: { MOD_DT } (YYYYMMDD). SF → PWS 클레임 마스터 조회. 환경변수 prefix: sf.outbound.*",
    )

    // key 는 web 탭 식별자("SF 물류 클레임 상태 업데이트" 탭 = logistics-claim-status-update)에 의도적으로 맞춘다.
    // 백엔드 endpoint/DTO/서비스는 인터페이스 실체에 맞춰 logistics-claim-master-sync 계열로 명명하여 두 갈래로 갈린다.
    private fun logisticsClaimMasterSync() = ExternalApiIntegrationInfo(
        key = "logistics-claim-status-update",
        externalSystem = "Salesforce (Apex REST)",
        endpoint = joinUrl(sfOutboundProperties.apexBaseUrl, "/IF_SendLogisticsClaimToPWS"),
        httpMethod = "POST",
        authType = "OAuth2 Password Grant (Bearer) — token: ${blankOr(sfOutboundProperties.oauth.tokenUrl)}",
        note = "Content-Type: application/json. Request body: { MOD_DT } (YYYYMMDD). SF → PWS 물류 클레임 마스터 조회. 환경변수 prefix: sf.outbound.*",
    )

    // key 는 web 탭 식별자("SF 거래처목표등록마스터 동기화" 탭 = sales-progress-rate-master-sync)에 맞춘다.
    private fun salesProgressRateMasterSync() = ExternalApiIntegrationInfo(
        key = "sales-progress-rate-master-sync",
        externalSystem = "Salesforce (Apex REST)",
        endpoint = joinUrl(sfOutboundProperties.apexBaseUrl, "/IF_salesprogresssend"),
        httpMethod = "POST",
        authType = "OAuth2 Password Grant (Bearer) — token: ${blankOr(sfOutboundProperties.oauth.tokenUrl)}",
        note = "Content-Type: application/json. Request body: { MOD_DT } (YYYYMMDD). SF → PWS 거래처목표등록마스터 조회. 환경변수 prefix: sf.outbound.*",
    )

    // key 는 web 탭 식별자("SF 사원평가 마스터 동기화" 탭 = staff-review-sync)에 맞춘다.
    private fun staffReviewSync() = ExternalApiIntegrationInfo(
        key = "staff-review-sync",
        externalSystem = "Salesforce (Apex REST)",
        endpoint = joinUrl(sfOutboundProperties.apexBaseUrl, "/IF_SendStaffReviewToPWS"),
        httpMethod = "POST",
        authType = "OAuth2 Password Grant (Bearer) — token: ${blankOr(sfOutboundProperties.oauth.tokenUrl)}",
        note = "Content-Type: application/json. Request body: { MOD_DT } (YYYYMMDD). SF → PWS 사원평가 마스터 조회. 환경변수 prefix: sf.outbound.*",
    )

    private fun sapInterfaces(): List<ExternalApiIntegrationInfo> {
        val sap = { key: String, title: String, interfaceId: String, note: String ->
            ExternalApiIntegrationInfo(
                key = key,
                externalSystem = "SAP ($title)",
                endpoint = joinUrl(sapOutboundProperties.baseUrl, "/$interfaceId"),
                httpMethod = "POST",
                authType = if (sapOutboundProperties.username.isNotBlank()) {
                    "HTTP Basic (username: ${sapOutboundProperties.username})"
                } else {
                    "HTTP Basic (미설정)"
                },
                note = "interfaceId: $interfaceId. $note 환경변수 prefix: sap.outbound.*",
            )
        }
        return listOf(
            sap("loan-inquiry", "여신 한도 조회", SapConstants.SAP_INTERFACE_LOAN_INQUIRY, "동기 조회."),
            sap("order-request-detail", "주문요청 상세 조회", SapConstants.SAP_INTERFACE_ORDER_REQUEST_DETAIL, "동기 조회."),
            sap("order-request-cancel", "주문 취소", SapConstants.SAP_INTERFACE_ORDER_REQUEST_CANCEL, "송신만 수행."),
            sap(
                "order-request-register",
                "주문 등록",
                SapConstants.SAP_INTERFACE_ORDER_REQUEST_REGIST,
                "Outbox 패턴 — SapOutboxWorker 가 비동기로 실제 송신.",
            ),
            sap("attendance", "일반 출근", SapConstants.SAP_INTERFACE_ATTENDANCE, "페이지 단위 송신."),
            sap("display-master", "진열 마스터", SapConstants.SAP_INTERFACE_DISPLAY_MASTER, "페이지 단위 송신."),
            sap("ppt-master", "전문행사조 마스터", SapConstants.SAP_INTERFACE_PPT_MASTER, "월 단위 첫 페이지 송신."),
        )
    }

    /** base URL 과 suffix 를 결합. base 가 비어 있으면 미설정 표기. */
    private fun joinUrl(base: String, suffix: String): String {
        if (base.isBlank()) return "(미설정) $suffix"
        return base.trimEnd('/') + suffix
    }

    private fun blankOr(value: String): String = value.ifBlank { "(미설정)" }

    companion object {
        private const val NAVER_GEOCODE_ENDPOINT =
            "https://maps.apigw.ntruss.com/map-geocode/v2/geocode"
    }
}
