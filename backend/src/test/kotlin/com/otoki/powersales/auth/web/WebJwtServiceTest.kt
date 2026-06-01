package com.otoki.powersales.auth.web

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("WebJwtService - impersonated_by claim round-trip (Spec #851)")
class WebJwtServiceTest {

    // HS256 서명용 32+ byte secret.
    private val service = WebJwtService(
        secret = "test-secret-key-for-web-jwt-service-spec-851-0123456789",
        accessExpiration = 1_800_000L,
        refreshExpiration = 7 * 24 * 60 * 60 * 1000L,
    )

    private fun principal(userId: Long): WebUserPrincipal = WebUserPrincipal(
        userId = userId,
        usernameValue = "u$userId@otokims.co.kr",
        employeeCode = "S00$userId",
        employeeId = userId,
        role = null,
        costCenterCode = null,
        profileName = "5.영업사원",
        profileId = null,
        isSalesSupport = false,
        passwordChangeRequired = false,
        permissions = emptySet(),
        encodedPassword = "",
        grantedAuthorities = emptyList(),
        active = true,
    )

    @Test
    @DisplayName("일반 access 토큰 — impersonated_by claim 부재 → 추출 시 null")
    fun normalAccessToken_noImpersonatedBy() {
        val token = service.createAccessToken(principal(1234L), role = null)

        assertThat(service.getImpersonatedByFromToken(token)).isNull()
        assertThat(service.getUserIdFromToken(token)).isEqualTo(1234L)
    }

    @Test
    @DisplayName("대행 access 토큰 — user_id=대상, impersonated_by=관리자 round-trip")
    fun impersonationAccessToken_roundTrip() {
        // 대행 모델: subject/user_id = 대상(1234), impersonated_by = 관리자(7)
        val token = service.createAccessToken(principal(1234L), role = null, impersonatedBy = 7L)

        assertThat(service.getUserIdFromToken(token)).isEqualTo(1234L)
        assertThat(service.getImpersonatedByFromToken(token)).isEqualTo(7L)
    }

    @Test
    @DisplayName("일반 refresh 토큰 — impersonated_by claim 부재 → 추출 시 null")
    fun normalRefreshToken_noImpersonatedBy() {
        val token = service.createRefreshToken("u1234@otokims.co.kr", 1234L, "fam", "tok")

        assertThat(service.getImpersonatedByFromToken(token)).isNull()
    }

    @Test
    @DisplayName("대행 refresh 토큰 — impersonated_by 보존 round-trip")
    fun impersonationRefreshToken_roundTrip() {
        val token = service.createRefreshToken("u1234@otokims.co.kr", 1234L, "fam", "tok", impersonatedBy = 7L)

        assertThat(service.getUserIdFromToken(token)).isEqualTo(1234L)
        assertThat(service.getImpersonatedByFromToken(token)).isEqualTo(7L)
    }
}
