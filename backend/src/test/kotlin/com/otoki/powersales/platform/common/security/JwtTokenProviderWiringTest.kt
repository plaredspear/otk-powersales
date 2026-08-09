package com.otoki.powersales.platform.common.security

import com.otoki.powersales.platform.auth.token.RefreshTokenStore
import com.otoki.powersales.platform.common.util.TimeZones
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import java.time.LocalDateTime
import java.util.Date

/**
 * `jwt.min-issued-at` 이 **Spring 컨테이너를 통해 실제로 주입되는지** 검증한다.
 *
 * [JwtTokenProviderTest] 는 생성자를 직접 호출하므로, 프로퍼티 바인딩이 깨져도 통과한다.
 * 그런데 이 파라미터는 Kotlin 기본값(`= ""`)을 가진 선택 파라미터라, 바인딩이 실패하면 예외가
 * 아니라 **조용히 기본값("")으로 떨어져 컷오프가 비활성**된다 — 마이그레이션 컷오버에서
 * "설정했는데 아무도 로그아웃되지 않는" 형태로만 드러나는 실패다. 그 경로를 여기서 못 박는다.
 */
@SpringJUnitConfig(classes = [JwtTokenProvider::class, JwtTokenProviderWiringTest.MockDeps::class])
@TestPropertySource(
    properties = [
        "jwt.secret=wiring-test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256",
        "jwt.expiration=3600000",
        "jwt.refresh-expiration=604800000",
        "jwt.refresh-expiration-long=5184000000",
        // 컷오프 활성 조건: 이미 지난 시각. 공백 구분자 표기도 함께 검증한다.
        "jwt.min-issued-at=2020-01-01 00:00:00",
    ]
)
@DisplayName("JwtTokenProvider Spring 주입 — jwt.min-issued-at 바인딩")
class JwtTokenProviderWiringTest {

    @Configuration
    class MockDeps {
        @Bean
        fun redisTemplate(): RedisTemplate<String, String> = mockk(relaxed = true)

        @Bean
        fun refreshTokenStore(): RefreshTokenStore = mockk(relaxed = true)
    }

    @Test
    @DisplayName("설정값이 주입되어 컷오프 이전 발급 토큰이 거부된다")
    fun cutoffIsAppliedWhenInjectedBySpring(@Autowired provider: JwtTokenProvider) {
        // Given: 컷오프(2020-01-01) 이전에 발급된 토큰
        val issued = Date.from(
            LocalDateTime.of(2019, 12, 31, 23, 59, 0).atZone(TimeZones.SEOUL_ZONE).toInstant()
        )
        val token = Jwts.builder()
            .subject("1")
            .claim("type", "access")
            .issuedAt(issued)
            // 만료가 아니라 컷오프로 탈락하는지 보려면 만료를 넉넉히 잡는다
            .expiration(Date(System.currentTimeMillis() + 3_600_000))
            .signWith(
                Keys.hmacShaKeyFor(
                    "wiring-test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256".toByteArray()
                )
            )
            .compact()

        // Then
        assertThat(provider.isIssuedBeforeCutoff(token)).isTrue()
        assertThat(provider.validateToken(token)).isFalse()
    }

    @Test
    @DisplayName("컷오프 이후 발급 토큰(=재로그인 세션)은 통과한다")
    fun tokenIssuedAfterCutoffPasses(@Autowired provider: JwtTokenProvider) {
        val token = provider.createAccessToken(1L, "여사원")

        assertThat(provider.isIssuedBeforeCutoff(token)).isFalse()
        assertThat(provider.validateToken(token)).isTrue()
    }
}
