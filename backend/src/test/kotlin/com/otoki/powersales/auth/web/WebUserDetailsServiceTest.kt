package com.otoki.powersales.auth.web

import com.otoki.powersales.user.entity.ProfileType
import com.otoki.powersales.user.entity.User
import com.otoki.powersales.user.repository.UserRepository
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
import org.springframework.security.core.userdetails.UsernameNotFoundException

@ExtendWith(MockitoExtension::class)
@DisplayName("WebUserDetailsService 테스트")
class WebUserDetailsServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @InjectMocks
    private lateinit var service: WebUserDetailsService

    @Nested
    @DisplayName("loadUserByUsername - User 조회 + 권한 산출")
    inner class LoadUserByUsername {

        @Test
        @DisplayName("성공 - 활성 사용자 + STAFF profile → ROLE_STAFF 부여")
        fun success_staff() {
            val user = createUser(profileType = ProfileType.STAFF, isSalesSupport = false)
            whenever(userRepository.findByUsername("u@otokims.co.kr")).thenReturn(user)

            val principal = service.loadUserByUsername("u@otokims.co.kr")

            assertThat(principal.usernameValue).isEqualTo("u@otokims.co.kr")
            assertThat(principal.userId).isEqualTo(1L)
            assertThat(principal.authorities.map { it.authority }).containsExactly("ROLE_STAFF")
            assertThat(principal.isEnabled).isTrue()
        }

        @Test
        @DisplayName("성공 - SYSTEM_ADMIN + is_sales_support=true → ROLE_ADMIN + ROLE_SALES_SUPPORT")
        fun success_admin_with_sales_support() {
            val user = createUser(profileType = ProfileType.SYSTEM_ADMIN, isSalesSupport = true)
            whenever(userRepository.findByUsername("admin@otokims.co.kr")).thenReturn(user)

            val principal = service.loadUserByUsername("admin@otokims.co.kr")

            assertThat(principal.authorities.map { it.authority })
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_SALES_SUPPORT")
        }

        @Test
        @DisplayName("성공 - BRANCH_MANAGER → ROLE_MANAGER 매핑 (§2.3 매핑 표)")
        fun success_branch_manager_maps_to_manager() {
            val user = createUser(profileType = ProfileType.BRANCH_MANAGER, isSalesSupport = false)
            whenever(userRepository.findByUsername("bm@otokims.co.kr")).thenReturn(user)

            val principal = service.loadUserByUsername("bm@otokims.co.kr")

            assertThat(principal.authorities.map { it.authority }).containsExactly("ROLE_MANAGER")
        }

        @Test
        @DisplayName("비활성 사용자 - is_active=false → isEnabled=false")
        fun inactive_user() {
            val user = createUser(profileType = ProfileType.STAFF, isSalesSupport = false, isActive = false)
            whenever(userRepository.findByUsername("inactive@otokims.co.kr")).thenReturn(user)

            val principal = service.loadUserByUsername("inactive@otokims.co.kr")

            assertThat(principal.isEnabled).isFalse()
        }

        @Test
        @DisplayName("실패 - User 미존재 → UsernameNotFoundException")
        fun user_not_found() {
            whenever(userRepository.findByUsername("missing@otokims.co.kr")).thenReturn(null)

            assertThatThrownBy { service.loadUserByUsername("missing@otokims.co.kr") }
                .isInstanceOf(UsernameNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("resolveAuthorities - ProfileType → GrantedAuthority 매핑 (§2.3 표)")
    inner class ResolveAuthorities {

        @Test
        @DisplayName("BUSINESS_DIRECTOR / DIVISION_HEAD 둘 다 ROLE_DIRECTOR")
        fun director_mapping() {
            val a1 = WebUserDetailsService.resolveAuthorities(ProfileType.BUSINESS_DIRECTOR, false)
            val a2 = WebUserDetailsService.resolveAuthorities(ProfileType.DIVISION_HEAD, false)
            assertThat(a1.map { it.authority }).containsExactly("ROLE_DIRECTOR")
            assertThat(a2.map { it.authority }).containsExactly("ROLE_DIRECTOR")
        }

        @Test
        @DisplayName("SALES_MANAGER 도 ROLE_MANAGER 매핑")
        fun sales_manager_mapping() {
            val authorities = WebUserDetailsService.resolveAuthorities(ProfileType.SALES_MANAGER, false)
            assertThat(authorities.map { it.authority }).containsExactly("ROLE_MANAGER")
        }

        @Test
        @DisplayName("TEAM_LEADER → ROLE_LEADER")
        fun team_leader_mapping() {
            val authorities = WebUserDetailsService.resolveAuthorities(ProfileType.TEAM_LEADER, false)
            assertThat(authorities.map { it.authority }).containsExactly("ROLE_LEADER")
        }

        @Test
        @DisplayName("SALES_REP → ROLE_SALES_REP")
        fun sales_rep_mapping() {
            val authorities = WebUserDetailsService.resolveAuthorities(ProfileType.SALES_REP, false)
            assertThat(authorities.map { it.authority }).containsExactly("ROLE_SALES_REP")
        }

        @Test
        @DisplayName("MARKETING → ROLE_MARKETING")
        fun marketing_mapping() {
            val authorities = WebUserDetailsService.resolveAuthorities(ProfileType.MARKETING, false)
            assertThat(authorities.map { it.authority }).containsExactly("ROLE_MARKETING")
        }
    }

    private fun createUser(
        id: Long = 1L,
        username: String = "u@otokims.co.kr",
        employeeCode: String = "S001",
        profileType: ProfileType,
        isSalesSupport: Boolean,
        isActive: Boolean = true,
        password: String = "\$2a\$10\$encodedHash"
    ): User = User(
        id = id,
        username = username,
        isActive = isActive,
        employeeCode = employeeCode,
        profileType = profileType,
        isSalesSupport = isSalesSupport,
        password = password,
        passwordChangeRequired = false
    )
}
