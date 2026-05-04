package com.otoki.powersales.admin.integration

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 관리자 등록 API 의 미인증 흐름 통합 테스트. (Spec #581 P1-B, 후속 of #579 §5.2 #2)
 *
 * `@WebMvcTest` 슬라이스가 `addFilters=false` + `@MockitoBean adminAuthorityFilter` 로 보안 체인을
 * 비활성화하므로, 미인증 흐름을 검증하려면 전체 `@SpringBootTest` 컨텍스트가 필요하다.
 *
 * 본 테스트는 다음 컴포넌트의 종단 흐름이 정상 동작함을 회귀 차단한다:
 * - SecurityConfig: `/api/v1/admin/employees` 가 `authenticated()` 정책 하에 보호됨
 * - JwtAuthenticationFilter: 토큰 부재 시 SecurityContext 미설정
 * - JwtAuthenticationEntryPoint: 인증 누락 시 401 + ApiResponse(error.code="UNAUTHORIZED") 응답
 *
 * 참고: 본 스펙(P1-B §3.1) 작성 시 errorCode 가 `INVALID_CREDENTIALS` 라고 가정했으나,
 * 실제 `JwtAuthenticationEntryPoint` 구현은 토큰 부재 시 `UNAUTHORIZED` 를 반환한다. 운영 코드
 * 변경은 본 Part 의 스코프 외 (§4.2) 이므로, 현 시스템 동작에 맞춰 `UNAUTHORIZED` 로 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("관리자 등록 API 미인증 통합 테스트 (Spec #581 P1-B)")
class AdminEmployeeRegisterIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var redisTemplate: RedisTemplate<String, String>

    @Test
    @DisplayName("Authorization 헤더 없음 - POST /api/v1/admin/employees -> 401 + UNAUTHORIZED")
    fun unauthenticatedRegister_returns401() {
        val validBody = """
            {
              "employeeCode": "MANUAL-TEST01",
              "name": "테스트관리자",
              "password": "Password123!",
              "passwordConfirm": "Password123!"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/admin/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
    }
}
