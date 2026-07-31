package com.otoki.powersales.external.ovip.service

import com.otoki.powersales.external.ovip.auth.service.OvipJwtCodec
import com.otoki.powersales.external.ovip.auth.config.OvipAuthProperties
import io.jsonwebtoken.JwtException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Date

@DisplayName("OvipJwtCodec 테스트")
class OvipJwtCodecTest {

    private val signingKey = "test-ovip-jwt-signing-key-with-at-least-256-bits-of-entropy-1234"
    private val codec = OvipJwtCodec(
        OvipAuthProperties(
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
            val issued = codec.issue("client-1", listOf("ovip.write"), now)

            assertThat(issued.token).isNotBlank()
            assertThat(issued.expiresIn).isEqualTo(86400)
            assertThat(issued.jti).isNotBlank()

            val claims = codec.parse(issued.token)
            assertThat(claims.subject).isEqualTo("client-1")
            assertThat(claims.issuer).isEqualTo(OvipJwtCodec.ISSUER)
            assertThat(claims.get("scope", String::class.java)).isEqualTo("ovip.write")
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
            val expiredCodec = OvipJwtCodec(
                OvipAuthProperties(jwtSigningKey = signingKey, tokenTtlSeconds = 1)
            )
            val issued = expiredCodec.issue("client-1", listOf("ovip.write"), past)
            assertThatThrownBy { codec.parse(issued.token) }
                .isInstanceOf(JwtException::class.java)
        }

        @Test
        @DisplayName("서명 변조 - JwtException")
        fun parse_tampered() {
            val issued = codec.issue("client-1", listOf("ovip.write"))
            // 서명(마지막 segment)의 **첫 글자**를 바꾼다. 끝 글자는 base64url 패딩 비트를 포함해
            // 다른 문자여도 같은 바이트로 디코딩될 수 있어(= 변조가 무효화되어 검증 통과) 간헐 실패한다.
            val body = issued.token.substringBeforeLast('.')
            val signature = issued.token.substringAfterLast('.')
            val tampered = "$body.${if (signature.first() == 'A') 'B' else 'A'}${signature.drop(1)}"
            assertThatThrownBy { codec.parse(tampered) }
                .isInstanceOf(JwtException::class.java)
        }

        @Test
        @DisplayName("다른 키로 서명된 토큰 - JwtException")
        fun parse_wrongKey() {
            val otherCodec = OvipJwtCodec(
                OvipAuthProperties(
                    jwtSigningKey = "different-signing-key-with-at-least-256-bits-of-entropy-AAAA",
                    tokenTtlSeconds = 86400
                )
            )
            val issued = otherCodec.issue("client-1", listOf("ovip.write"))
            assertThatThrownBy { codec.parse(issued.token) }
                .isInstanceOf(JwtException::class.java)
        }
    }
}
