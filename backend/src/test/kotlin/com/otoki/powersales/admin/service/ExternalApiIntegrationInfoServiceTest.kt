package com.otoki.powersales.admin.service

import com.otoki.powersales.external.sap.outbound.config.SapOutboundProperties
import com.otoki.powersales.external.sf.outbound.SfOutboundProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ExternalApiIntegrationInfoService (외부 API 연동 정보) 테스트")
class ExternalApiIntegrationInfoServiceTest {

    private fun service(
        sf: SfOutboundProperties = SfOutboundProperties(
            apexBaseUrl = "https://ottogi.my.salesforce.com/services/apexrest/mobile",
            oauth = SfOutboundProperties.OAuthProps(
                tokenUrl = "https://login.salesforce.com/services/oauth2/token",
                clientId = "CID",
                clientSecret = "SECRET-XYZ",
                username = "interface@otg.com",
                password = "SUPER-SECRET-PW",
            ),
        ),
        sap: SapOutboundProperties = SapOutboundProperties(
            baseUrl = "https://sap.example.com/rest",
            username = "sapuser",
            password = "SAP-SECRET-PW",
        ),
    ) = ExternalApiIntegrationInfoService(sf, sap)

    @Test
    @DisplayName("SF ClaimRegist — endpoint = apexBaseUrl + /ClaimRegist, POST, Bearer")
    fun claimRegistInfo() {
        val info = service().getIntegrationInfo().items.first { it.key == "claim-regist" }

        assertThat(info.externalSystem).contains("Salesforce")
        assertThat(info.endpoint)
            .isEqualTo("https://ottogi.my.salesforce.com/services/apexrest/mobile/ClaimRegist")
        assertThat(info.httpMethod).isEqualTo("POST")
        assertThat(info.authType).contains("OAuth2", "Bearer")
    }

    @Test
    @DisplayName("SF IF_SendClaimToPWS(클레임 마스터 조회) — key=claim-status-update, /IF_SendClaimToPWS, POST")
    fun claimMasterSyncInfo() {
        val info = service().getIntegrationInfo().items.first { it.key == "claim-status-update" }

        assertThat(info.externalSystem).contains("Salesforce")
        assertThat(info.endpoint)
            .isEqualTo("https://ottogi.my.salesforce.com/services/apexrest/mobile/IF_SendClaimToPWS")
        assertThat(info.httpMethod).isEqualTo("POST")
        assertThat(info.authType).contains("OAuth2", "Bearer")
        assertThat(info.note).contains("MOD_DT")
    }

    @Test
    @DisplayName("SF IF_SendLogisticsClaimToPWS(물류 클레임 마스터 조회) — key=logistics-claim-status-update, /IF_SendLogisticsClaimToPWS, POST")
    fun logisticsClaimMasterSyncInfo() {
        val info = service().getIntegrationInfo().items.first { it.key == "logistics-claim-status-update" }

        assertThat(info.externalSystem).contains("Salesforce")
        assertThat(info.endpoint)
            .isEqualTo("https://ottogi.my.salesforce.com/services/apexrest/mobile/IF_SendLogisticsClaimToPWS")
        assertThat(info.httpMethod).isEqualTo("POST")
        assertThat(info.authType).contains("OAuth2", "Bearer")
        assertThat(info.note).contains("MOD_DT")
    }

    @Test
    @DisplayName("SAP 인터페이스 7개 — endpoint = baseUrl + /{interfaceId}, POST, Basic")
    fun sapInfos() {
        val items = service().getIntegrationInfo().items
        val loan = items.first { it.key == "loan-inquiry" }

        assertThat(loan.endpoint).isEqualTo("https://sap.example.com/rest/SD03040")
        assertThat(loan.httpMethod).isEqualTo("POST")
        assertThat(loan.authType).contains("Basic")
        // SAP 7개 모두 포함
        assertThat(items.filter { it.externalSystem.startsWith("SAP") }).hasSize(7)
    }

    @Test
    @DisplayName("Naver Geocode — 상수 endpoint, GET")
    fun naverInfo() {
        val info = service().getIntegrationInfo().items.first { it.key == "naver-geocode" }

        assertThat(info.endpoint).isEqualTo("https://maps.apigw.ntruss.com/map-geocode/v2/geocode")
        assertThat(info.httpMethod).isEqualTo("GET")
    }

    @Test
    @DisplayName("인증 secret(비밀번호/clientSecret)은 응답 어디에도 노출되지 않음")
    fun noSecretLeak() {
        val items = service().getIntegrationInfo().items
        val serialized = items.joinToString("|") {
            "${it.endpoint}|${it.authType}|${it.note}|${it.externalSystem}"
        }

        assertThat(serialized).doesNotContain("SECRET-XYZ")
        assertThat(serialized).doesNotContain("SUPER-SECRET-PW")
        assertThat(serialized).doesNotContain("SAP-SECRET-PW")
    }

    @Test
    @DisplayName("baseUrl 미설정 시 endpoint 에 (미설정) 표기")
    fun blankBaseUrl() {
        val svc = service(
            sf = SfOutboundProperties(apexBaseUrl = ""),
            sap = SapOutboundProperties(baseUrl = ""),
        )
        val items = svc.getIntegrationInfo().items

        assertThat(items.first { it.key == "claim-regist" }.endpoint).contains("(미설정)")
        assertThat(items.first { it.key == "loan-inquiry" }.endpoint).contains("(미설정)")
    }
}
