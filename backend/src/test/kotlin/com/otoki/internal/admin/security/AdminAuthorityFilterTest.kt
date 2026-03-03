package com.otoki.internal.admin.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.otoki.internal.common.entity.User
import com.otoki.internal.common.entity.UserRole
import com.otoki.internal.common.repository.UserRepository
import com.otoki.internal.common.security.UserPrincipal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("AdminAuthorityFilter 테스트")
class AdminAuthorityFilterTest {

    @Mock
    private lateinit var userRepository: UserRepository

    private lateinit var filter: AdminAuthorityFilter
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper().apply {
            propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
        filter = AdminAuthorityFilter(userRepository, objectMapper)
        SecurityContextHolder.clearContext()
    }

    @Nested
    @DisplayName("허용된 권한으로 접근")
    inner class AllowedAuthority {

        @Test
        @DisplayName("조장 권한 - 정상 통과")
        fun allowJojang() {
            setAuthentication(1L)
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(createUser(1L, "조장")))

            val request = MockHttpServletRequest()
            val response = MockHttpServletResponse()
            val chain = MockFilterChain()

            filter.doFilter(request, response, chain)

            assertThat(response.status).isEqualTo(200)
            assertThat(chain.request).isNotNull
        }

        @Test
        @DisplayName("영업지원실 권한 - 정상 통과")
        fun allowSalesSupport() {
            setAuthentication(2L)
            whenever(userRepository.findById(2L)).thenReturn(Optional.of(createUser(2L, "영업지원실")))

            val request = MockHttpServletRequest()
            val response = MockHttpServletResponse()
            val chain = MockFilterChain()

            filter.doFilter(request, response, chain)

            assertThat(response.status).isEqualTo(200)
            assertThat(chain.request).isNotNull
        }
    }

    @Nested
    @DisplayName("비허용 권한으로 접근")
    inner class ForbiddenAuthority {

        @Test
        @DisplayName("appAuthority가 null - 403 Forbidden")
        fun nullAuthority() {
            setAuthentication(1L)
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(createUser(1L, null)))

            val request = MockHttpServletRequest()
            val response = MockHttpServletResponse()
            val chain = MockFilterChain()

            filter.doFilter(request, response, chain)

            assertThat(response.status).isEqualTo(403)
            assertThat(response.contentAsString).contains("FORBIDDEN")
            assertThat(chain.request).isNull()
        }

        @Test
        @DisplayName("허용 목록에 없는 권한 - 403 Forbidden")
        fun unknownAuthority() {
            setAuthentication(1L)
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(createUser(1L, "일반사원")))

            val request = MockHttpServletRequest()
            val response = MockHttpServletResponse()
            val chain = MockFilterChain()

            filter.doFilter(request, response, chain)

            assertThat(response.status).isEqualTo(403)
            assertThat(response.contentAsString).contains("관리자 권한이 없습니다")
            assertThat(chain.request).isNull()
        }

        @Test
        @DisplayName("사용자 미존재 - 403 Forbidden")
        fun userNotFound() {
            setAuthentication(999L)
            whenever(userRepository.findById(999L)).thenReturn(Optional.empty())

            val request = MockHttpServletRequest()
            val response = MockHttpServletResponse()
            val chain = MockFilterChain()

            filter.doFilter(request, response, chain)

            assertThat(response.status).isEqualTo(403)
            assertThat(chain.request).isNull()
        }
    }

    @Nested
    @DisplayName("인증되지 않은 요청")
    inner class Unauthenticated {

        @Test
        @DisplayName("SecurityContext 비어있음 - 필터 통과 (Security가 401 처리)")
        fun noAuthentication() {
            val request = MockHttpServletRequest()
            val response = MockHttpServletResponse()
            val chain = MockFilterChain()

            filter.doFilter(request, response, chain)

            assertThat(response.status).isEqualTo(200)
            assertThat(chain.request).isNotNull
        }
    }

    private fun setAuthentication(userId: Long) {
        val principal = UserPrincipal(userId, UserRole.LEADER)
        val auth = UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
        SecurityContextHolder.getContext().authentication = auth
    }

    private fun createUser(id: Long, appAuthority: String?): User {
        return User(
            id = id,
            employeeId = "12345678",
            name = "테스트",
            appAuthority = appAuthority
        )
    }
}
