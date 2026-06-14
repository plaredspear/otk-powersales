package com.otoki.powersales.platform.auth.web

import com.otoki.powersales.platform.auth.entity.Profile
import com.otoki.powersales.platform.auth.permission.SfPermissionResolver
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.platform.auth.repository.ProfileRepository
import com.otoki.powersales.user.entity.User
import com.otoki.powersales.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.core.userdetails.UsernameNotFoundException
import java.util.Optional

@DisplayName("WebUserDetailsService 테스트")
class WebUserDetailsServiceTest {

    private val userRepository: UserRepository = mockk()
    private val employeeRepository: EmployeeRepository = mockk()
    private val sfPermissionResolver: SfPermissionResolver = mockk()
    private val profileRepository: ProfileRepository = mockk()

    private val service = WebUserDetailsService(
        userRepository,
        employeeRepository,
        sfPermissionResolver,
        profileRepository,
    )

    @BeforeEach
    fun stubEmployeeLookup() {
        every { employeeRepository.findByEmployeeCode(any()) } returns Optional.empty()
        every { sfPermissionResolver.resolveForUser(any()) } returns emptySet()
        every { profileRepository.findById(any()) } returns Optional.empty()
    }

    @Nested
    @DisplayName("loadUserByUsername - User 조회 + 권한 산출")
    inner class LoadUserByUsername {

        @Test
        @DisplayName("성공 - 활성 사용자 + Staff profile → ROLE_STAFF 부여")
        fun success_staff() {
            val user = createUser(profileId = 1L, isSalesSupport = false)
            stubProfile(1L, "9. Staff")
            every { userRepository.findByUsername("u@otokims.co.kr") } returns user

            val principal = service.loadUserByUsername("u@otokims.co.kr")

            assertThat(principal.usernameValue).isEqualTo("u@otokims.co.kr")
            assertThat(principal.userId).isEqualTo(1L)
            assertThat(principal.authorities.map { it.authority }).containsExactly("ROLE_STAFF")
            assertThat(principal.isEnabled).isTrue()
        }

        @Test
        @DisplayName("성공 - 시스템 관리자 + is_sales_support=true → ROLE_ADMIN + ROLE_SALES_SUPPORT")
        fun success_admin_with_sales_support() {
            val user = createUser(profileId = 2L, isSalesSupport = true)
            stubProfile(2L, "시스템 관리자")
            every { userRepository.findByUsername("admin@otokims.co.kr") } returns user

            val principal = service.loadUserByUsername("admin@otokims.co.kr")

            assertThat(principal.authorities.map { it.authority })
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_SALES_SUPPORT")
        }

        @Test
        @DisplayName("성공 - 4.지점장 → ROLE_MANAGER 매핑")
        fun success_branch_manager_maps_to_manager() {
            val user = createUser(profileId = 3L, isSalesSupport = false)
            stubProfile(3L, "4.지점장")
            every { userRepository.findByUsername("bm@otokims.co.kr") } returns user

            val principal = service.loadUserByUsername("bm@otokims.co.kr")

            assertThat(principal.authorities.map { it.authority }).containsExactly("ROLE_MANAGER")
        }

        @Test
        @DisplayName("비활성 사용자 - is_active=false → isEnabled=false")
        fun inactive_user() {
            val user = createUser(profileId = 1L, isSalesSupport = false, isActive = false)
            stubProfile(1L, "9. Staff")
            every { userRepository.findByUsername("inactive@otokims.co.kr") } returns user

            val principal = service.loadUserByUsername("inactive@otokims.co.kr")

            assertThat(principal.isEnabled).isFalse()
        }

        @Test
        @DisplayName("실패 - User 미존재 → UsernameNotFoundException")
        fun user_not_found() {
            every { userRepository.findByUsername("missing@otokims.co.kr") } returns null

            assertThatThrownBy { service.loadUserByUsername("missing@otokims.co.kr") }
                .isInstanceOf(UsernameNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("resolveAuthoritiesByProfileName - Profile.name → GrantedAuthority 매핑")
    inner class ResolveAuthoritiesByProfileName {

        @Test
        @DisplayName("2.사업부장 / 1.본부장 둘 다 ROLE_DIRECTOR")
        fun director_mapping() {
            val a1 = WebUserDetailsService.resolveAuthoritiesByProfileName("2.사업부장", false)
            val a2 = WebUserDetailsService.resolveAuthoritiesByProfileName("1.본부장", false)
            assertThat(a1.map { it.authority }).containsExactly("ROLE_DIRECTOR")
            assertThat(a2.map { it.authority }).containsExactly("ROLE_DIRECTOR")
        }

        @Test
        @DisplayName("3.영업부장 / 4.지점장 도 ROLE_MANAGER 매핑")
        fun manager_mapping() {
            val a1 = WebUserDetailsService.resolveAuthoritiesByProfileName("3.영업부장", false)
            val a2 = WebUserDetailsService.resolveAuthoritiesByProfileName("4.지점장", false)
            assertThat(a1.map { it.authority }).containsExactly("ROLE_MANAGER")
            assertThat(a2.map { it.authority }).containsExactly("ROLE_MANAGER")
        }

        @Test
        @DisplayName("6.조장 / 7.영업사원 + 조장 → ROLE_LEADER")
        fun leader_mapping() {
            val a1 = WebUserDetailsService.resolveAuthoritiesByProfileName("6.조장", false)
            val a2 = WebUserDetailsService.resolveAuthoritiesByProfileName("7.영업사원 + 조장", false)
            assertThat(a1.map { it.authority }).containsExactly("ROLE_LEADER")
            assertThat(a2.map { it.authority }).containsExactly("ROLE_LEADER")
        }

        @Test
        @DisplayName("5.영업사원 → ROLE_SALES_REP")
        fun sales_rep_mapping() {
            val authorities = WebUserDetailsService.resolveAuthoritiesByProfileName("5.영업사원", false)
            assertThat(authorities.map { it.authority }).containsExactly("ROLE_SALES_REP")
        }

        @Test
        @DisplayName("8.마케팅 → ROLE_MARKETING")
        fun marketing_mapping() {
            val authorities = WebUserDetailsService.resolveAuthoritiesByProfileName("8.마케팅", false)
            assertThat(authorities.map { it.authority }).containsExactly("ROLE_MARKETING")
        }

        @Test
        @DisplayName("공장관계자 → ROLE_FACTORY")
        fun factory_staff_mapping() {
            val authorities = WebUserDetailsService.resolveAuthoritiesByProfileName("공장관계자", false)
            assertThat(authorities.map { it.authority }).containsExactly("ROLE_FACTORY")
        }

        @Test
        @DisplayName("OLS → ROLE_OLS")
        fun ols_mapping() {
            val authorities = WebUserDetailsService.resolveAuthoritiesByProfileName("OLS", false)
            assertThat(authorities.map { it.authority }).containsExactly("ROLE_OLS")
        }

        @Test
        @DisplayName("미등록 profileName / null → ROLE_STAFF fallback")
        fun unknown_fallback() {
            val a1 = WebUserDetailsService.resolveAuthoritiesByProfileName(null, false)
            val a2 = WebUserDetailsService.resolveAuthoritiesByProfileName("unknown-profile", false)
            assertThat(a1.map { it.authority }).containsExactly("ROLE_STAFF")
            assertThat(a2.map { it.authority }).containsExactly("ROLE_STAFF")
        }
    }

    private fun stubProfile(id: Long, name: String) {
        every { profileRepository.findById(id) } returns Optional.of(Profile(id = id, name = name))
    }

    private fun createUser(
        id: Long = 1L,
        username: String = "u@otokims.co.kr",
        employeeCode: String = "S001",
        profileId: Long?,
        isSalesSupport: Boolean,
        isActive: Boolean = true,
        password: String = "\$2a\$10\$encodedHash"
    ): User = User(
        id = id,
        username = username,
        isActive = isActive,
        employeeCode = employeeCode,
        profileId = profileId,
        isSalesSupport = isSalesSupport,
        password = password,
        passwordChangeRequired = false
    )
}
