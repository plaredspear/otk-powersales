package com.otoki.powersales.external.rdp.service

import com.otoki.powersales.external.rdp.auth.service.RdpJwtCodec
import com.otoki.powersales.external.rdp.auth.config.RdpAuthProperties
import io.jsonwebtoken.JwtException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Date

@DisplayName("RdpJwtCodec 테스트")
class RdpJwtCodecTest {

    private val signingKey = "test-rdp-jwt-signing-key-with-at-least-256-bits-of-entropy-1234"
    private val codec = RdpJwtCodec(
        RdpAuthProperties(
            jwtSigningKey = signingKey,
            tokenTtlSeconds = 86400
        )
    )

    @Nested
    @DisplayName("issue - 토큰 발급")
    inner class Issue {

        @Test
        @DisplayName("정상 발급 - subject/scope/iss/exp 설정")
        fun issue_success() {
            val now = Date()
            val issued = codec.issue("client-1", listOf("rdp.write"), now)

            assertThat(issued.token).isNotBlank()
            assertThat(issued.expiresIn).isEqualTo(86400)
            assertThat(issued.jti).isNotBlank()

            val claims = codec.parse(issued.token)
            assertThat(claims.subject).isEqualTo("client-1")
            assertThat(claims.issuer).isEqualTo(RdpJwtCodec.ISSUER)
            assertThat(claims.get("scope", String::class.java)).isEqualTo("rdp.write")
            assertThat(claims.expiration.time - claims.issuedAt.time).isEqualTo(86_400_000L)
            assertThat(claims.id).isEqualTo(issued.jti)
        }
    }

    @Nested
    @DisplayName("parse - 토큰 검증")
    inner class Parse {

        @Test
        @DisplayName("만료된 토큰 - JwtException")
        fun parse_expired() {
            val past = Date(System.currentTimeMillis() - 1_000_000L)
            val expiredCodec = RdpJwtCodec(
                RdpAuthProperties(jwtSigningKey = signingKey, tokenTtlSeconds = 1)
            )
            val issued = expiredCodec.issue("client-1", listOf("rdp.write"), past)
            assertThatThrownBy { codec.parse(issued.token) }
                .isInstanceOf(JwtException::class.java)
        }

        @Test
        @DisplayName("서명 변조 - JwtException")
        fun parse_tampered() {
            val issued = codec.issue("client-1", listOf("rdp.write"))
            val tampered = issued.token.dropLast(2) + "AB"
            assertThatThrownBy { codec.parse(tampered) }
                .isInstanceOf(JwtException::class.java)
        }

        @Test
        @DisplayName("다른 키로 서명된 토큰 - JwtException")
        fun parse_wrongKey() {
            val otherCodec = RdpJwtCodec(
                RdpAuthProperties(
                    jwtSigningKey = "different-signing-key-with-at-least-256-bits-of-entropy-AAAA",
                    tokenTtlSeconds = 86400
                )
            )
            val issued = otherCodec.issue("client-1", listOf("rdp.write"))
            assertThatThrownBy { codec.parse(issued.token) }
                .isInstanceOf(JwtException::class.java)
        }
    }
}
