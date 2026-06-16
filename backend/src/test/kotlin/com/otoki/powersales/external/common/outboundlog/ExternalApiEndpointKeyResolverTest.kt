package com.otoki.powersales.external.common.outboundlog

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ExternalApiEndpointKeyResolver 테스트")
class ExternalApiEndpointKeyResolverTest {

    @Test
    @DisplayName("SAP — interfaceId(uri 마지막 segment) 로 탭 key 분류")
    fun sapInterface() {
        assertThat(ExternalApiEndpointKeyResolver.resolve(ExternalApiTarget.SAP, "http://sap:50000/RESTAdapter/SD03040"))
            .isEqualTo("loan-inquiry")
        assertThat(ExternalApiEndpointKeyResolver.resolve(ExternalApiTarget.SAP, "http://sap:50000/RESTAdapter/SD03052"))
            .isEqualTo("order-request-detail")
        assertThat(ExternalApiEndpointKeyResolver.resolve(ExternalApiTarget.SAP, "http://sap:50000/RESTAdapter/SD03300"))
            .isEqualTo("ppt-master")
    }

    @Test
    @DisplayName("SAP — query string 이 붙어도 마지막 path segment 로 분류")
    fun sapWithQueryString() {
        assertThat(ExternalApiEndpointKeyResolver.resolve(ExternalApiTarget.SAP, "http://sap/RESTAdapter/SD03040?trace=1"))
            .isEqualTo("loan-inquiry")
    }

    @Test
    @DisplayName("SF — Apex REST resource 로 탭 key 분류")
    fun sfResource() {
        assertThat(ExternalApiEndpointKeyResolver.resolve(ExternalApiTarget.SF, "https://x.my.salesforce.com/services/apexrest/mobile/ClaimRegist"))
            .isEqualTo("claim-regist")
        assertThat(ExternalApiEndpointKeyResolver.resolve(ExternalApiTarget.SF, "https://x.my.salesforce.com/services/apexrest/IF_SendStaffReviewToPWS"))
            .isEqualTo("staff-review-sync")
    }

    @Test
    @DisplayName("SF — OAuth2 토큰 발급은 sf-oauth-token 으로 분류")
    fun sfOAuthToken() {
        assertThat(ExternalApiEndpointKeyResolver.resolve(ExternalApiTarget.SF, "https://login.salesforce.com/services/oauth2/token"))
            .isEqualTo("sf-oauth-token")
    }

    @Test
    @DisplayName("Naver — geocode host 로 분류")
    fun naverGeocode() {
        assertThat(ExternalApiEndpointKeyResolver.resolve(ExternalApiTarget.NAVER, "https://maps.apigw.ntruss.com/map-geocode/v2/geocode?query=서울"))
            .isEqualTo("naver-geocode")
    }

    @Test
    @DisplayName("분류 불가 호출은 null")
    fun unresolved() {
        assertThat(ExternalApiEndpointKeyResolver.resolve(ExternalApiTarget.SAP, "http://sap/RESTAdapter/SD99999"))
            .isNull()
        assertThat(ExternalApiEndpointKeyResolver.resolve(ExternalApiTarget.SF, "https://x.my.salesforce.com/services/apexrest/UnknownResource"))
            .isNull()
        assertThat(ExternalApiEndpointKeyResolver.resolve("UNKNOWN", "http://whatever/x"))
            .isNull()
    }

    @Test
    @DisplayName("ALL_KEYS 는 SAP 7 + SF 5 + sf-oauth-token + naver-geocode = 14개")
    fun allKeys() {
        assertThat(ExternalApiEndpointKeyResolver.ALL_KEYS).hasSize(14)
        assertThat(ExternalApiEndpointKeyResolver.ALL_KEYS).contains(
            "loan-inquiry", "staff-review-sync", "sf-oauth-token", "naver-geocode",
        )
    }
}
