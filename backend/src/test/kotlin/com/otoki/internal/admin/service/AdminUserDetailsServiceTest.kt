package com.otoki.internal.admin.service

import com.otoki.internal.common.entity.User
import com.otoki.internal.common.entity.UserRole
import com.otoki.internal.common.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.springframework.security.authentication.DisabledException
import org.springframework.security.core.userdetails.UsernameNotFoundException
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@DisplayName("AdminUserDetailsService 테스트")
class AdminUserDetailsServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @InjectMocks
    private lateinit var adminUserDetailsService: AdminUserDetailsService

    @Nested
    @DisplayName("loadUserByUsername - 사번으로 사용자 조회")
    inner class LoadUserByUsernameTests {

        @Test
        @DisplayName("정상 조회 (LEADER) - 재직 + 로그인 활성 → AdminUserDetails 반환")
        fun loadUser_leader_success() {
            val user = createUser(appAuthority = "조장")
            whenever(userRepository.findByEmployeeId("20030117")).thenReturn(Optional.of(user))

            val result = adminUserDetailsService.loadUserByUsername("20030117")

            assertThat(result.username).isEqualTo("20030117")
            assertThat(result.displayName).isEqualTo("홍길동")
            assertThat(result.role).isEqualTo(UserRole.LEADER)
            assertThat(result.isEnabled).isTrue()
            assertThat(result.authorities).anyMatch { it.authority == "ROLE_LEADER" }
        }

        @Test
        @DisplayName("정상 조회 (ADMIN) - 지점장 → ROLE_ADMIN 반환")
        fun loadUser_admin_success() {
            val user = createUser(appAuthority = "지점장")
            whenever(userRepository.findByEmployeeId("20030117")).thenReturn(Optional.of(user))

            val result = adminUserDetailsService.loadUserByUsername("20030117")

            assertThat(result.role).isEqualTo(UserRole.ADMIN)
            assertThat(result.authorities).anyMatch { it.authority == "ROLE_ADMIN" }
        }

        @Test
        @DisplayName("정상 조회 (USER) - 일반 사원 → ROLE_USER 반환 (SecurityConfig에서 접근 제어)")
        fun loadUser_user_success() {
            val user = createUser(appAuthority = null)
            whenever(userRepository.findByEmployeeId("20030117")).thenReturn(Optional.of(user))

            val result = adminUserDetailsService.loadUserByUsername("20030117")

            assertThat(result.role).isEqualTo(UserRole.USER)
            assertThat(result.authorities).anyMatch { it.authority == "ROLE_USER" }
        }

        @Test
        @DisplayName("사용자 미존재 - 존재하지 않는 사번 → UsernameNotFoundException")
        fun loadUser_notFound() {
            whenever(userRepository.findByEmployeeId("99999999")).thenReturn(Optional.empty())

            assertThatThrownBy { adminUserDetailsService.loadUserByUsername("99999999") }
                .isInstanceOf(UsernameNotFoundException::class.java)
        }

        @Test
        @DisplayName("퇴직자 - status != 재직 → DisabledException")
        fun loadUser_notActive() {
            val user = createUser(status = "퇴직")
            whenever(userRepository.findByEmployeeId("20030117")).thenReturn(Optional.of(user))

            assertThatThrownBy { adminUserDetailsService.loadUserByUsername("20030117") }
                .isInstanceOf(DisabledException::class.java)
        }

        @Test
        @DisplayName("로그인 비활성 - appLoginActive = false → DisabledException")
        fun loadUser_loginDisabled() {
            val user = createUser(appLoginActive = false)
            whenever(userRepository.findByEmployeeId("20030117")).thenReturn(Optional.of(user))

            assertThatThrownBy { adminUserDetailsService.loadUserByUsername("20030117") }
                .isInstanceOf(DisabledException::class.java)
        }
    }

    private fun createUser(
        id: Long = 1L,
        employeeId: String = "20030117",
        name: String = "홍길동",
        status: String = "재직",
        appLoginActive: Boolean = true,
        appAuthority: String? = "조장",
        password: String = "\$2a\$10\$dummyHashedPassword"
    ): User = User(
        id = id,
        employeeId = employeeId,
        name = name,
        status = status,
        appLoginActive = appLoginActive,
        appAuthority = appAuthority,
        password = password
    )
}
