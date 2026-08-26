package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.exception.AdminProfileNotFoundException
import com.otoki.powersales.admin.exception.AdminUserNotFoundException
import com.otoki.powersales.admin.exception.CannotChangeOwnProfileException
import com.otoki.powersales.admin.exception.ProfileChangeForbiddenException
import com.otoki.powersales.admin.security.AdminDataScopeCache
import com.otoki.powersales.domain.org.organization.repository.OrganizationRepository
import com.otoki.powersales.platform.auth.entity.Profile
import com.otoki.powersales.platform.auth.permission.AdminPermissionCache
import com.otoki.powersales.platform.auth.repository.ProfileRepository
import com.otoki.powersales.user.entity.User
import com.otoki.powersales.user.repository.UserRepository
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Optional

/**
 * [AdminUserService.updateProfile] 전용 테스트 — 프로파일 수동 변경의 가드와 부수효과.
 *
 * 검증 축:
 * - 시스템 관리자 판정 (비관리자 차단)
 * - 자기 자신 대상 차단 (권한 자가 상승 / 자기 권한 상실 양방향)
 * - 존재 검증 (User / Profile)
 * - 권한·데이터스코프 캐시 무효화 — 누락 시 "바꿨는데 안 바뀐다" 로 관측되는 회귀
 */
@DisplayName("AdminUserService 프로파일 수동 변경 테스트")
class AdminUserServiceProfileTest {

    private val userRepository: UserRepository = mockk()
    private val profileRepository: ProfileRepository = mockk()
    private val organizationRepository: OrganizationRepository = mockk()
    private val passwordEncoder: PasswordEncoder = mockk()
    private val adminPermissionCache: AdminPermissionCache = mockk(relaxed = true)
    private val adminDataScopeCache: AdminDataScopeCache = mockk(relaxed = true)

    private val service = AdminUserService(
        userRepository,
        profileRepository,
        organizationRepository,
        passwordEncoder,
        adminPermissionCache,
        adminDataScopeCache,
    )

    private val systemAdmin = "시스템 관리자"

    private fun user(id: Long, profileId: Long?): User = User(
        id = id,
        username = "user$id@otoki.com",
        employeeCode = "1000000$id",
        isSalesSupport = false,
        password = "encoded-not-used",
    ).apply { this.profileId = profileId }

    private fun profile(id: Long, name: String): Profile = Profile(id = id, name = name)

    @Test
    @DisplayName("성공 - 프로파일이 바뀌고 권한 캐시가 무효화된다")
    fun updateProfile_success() {
        val target = user(5L, profileId = 30L)
        every { userRepository.findById(5L) } returns Optional.of(target)
        every { profileRepository.findById(25L) } returns Optional.of(profile(25L, "6.조장"))

        service.updateProfile(
            targetUserId = 5L,
            requesterUserId = 1L,
            requesterProfileName = systemAdmin,
            profileId = 25L,
        )

        assertThat(target.profileId).isEqualTo(25L)
        // 캐시를 버리지 않으면 대상자는 재로그인 전까지 이전 권한으로 동작한다.
        verify(exactly = 1) { adminPermissionCache.invalidate(5L) }
        verify(exactly = 1) { adminDataScopeCache.invalidate(5L) }
    }

    @Test
    @DisplayName("동일 프로파일 재지정 - 변경 없이 조기 반환한다")
    fun updateProfile_noop_whenSameProfile() {
        val target = user(5L, profileId = 25L)
        every { userRepository.findById(5L) } returns Optional.of(target)
        every { profileRepository.findById(25L) } returns Optional.of(profile(25L, "6.조장"))

        service.updateProfile(
            targetUserId = 5L,
            requesterUserId = 1L,
            requesterProfileName = systemAdmin,
            profileId = 25L,
        )

        assertThat(target.profileId).isEqualTo(25L)
        // 실제 변경이 없으므로 캐시를 버릴 이유도 없다 (불필요한 권한 재산출 회피).
        verify(exactly = 0) { adminPermissionCache.invalidate(any()) }
        verify(exactly = 0) { adminDataScopeCache.invalidate(any()) }
    }

    @Test
    @DisplayName("실패 - 시스템 관리자가 아니면 403")
    fun updateProfile_rejectsNonSystemAdmin() {
        assertThatThrownBy {
            service.updateProfile(
                targetUserId = 5L,
                requesterUserId = 1L,
                requesterProfileName = "4.지점장",
                profileId = 25L,
            )
        }.isInstanceOf(ProfileChangeForbiddenException::class.java)

        // 권한 판정이 조회보다 먼저 — 비관리자에게 사용자 존재 여부를 흘리지 않는다.
        verify(exactly = 0) { userRepository.findById(any()) }
    }

    @Test
    @DisplayName("실패 - profileName 이 null 이면 403")
    fun updateProfile_rejectsNullProfileName() {
        assertThatThrownBy {
            service.updateProfile(
                targetUserId = 5L,
                requesterUserId = 1L,
                requesterProfileName = null,
                profileId = 25L,
            )
        }.isInstanceOf(ProfileChangeForbiddenException::class.java)
    }

    @Test
    @DisplayName("실패 - 자기 자신 대상이면 400")
    fun updateProfile_rejectsSelf() {
        assertThatThrownBy {
            service.updateProfile(
                targetUserId = 7L,
                requesterUserId = 7L,
                requesterProfileName = systemAdmin,
                profileId = 25L,
            )
        }.isInstanceOf(CannotChangeOwnProfileException::class.java)

        verify(exactly = 0) { userRepository.findById(any()) }
    }

    @Test
    @DisplayName("실패 - 미존재 사용자면 404")
    fun updateProfile_userNotFound() {
        every { userRepository.findById(99999L) } returns Optional.empty()

        assertThatThrownBy {
            service.updateProfile(
                targetUserId = 99999L,
                requesterUserId = 1L,
                requesterProfileName = systemAdmin,
                profileId = 25L,
            )
        }.isInstanceOf(AdminUserNotFoundException::class.java)
    }

    @Test
    @DisplayName("실패 - 미존재 프로파일이면 404이고 사용자는 그대로다")
    fun updateProfile_profileNotFound() {
        val target = user(5L, profileId = 30L)
        every { userRepository.findById(5L) } returns Optional.of(target)
        every { profileRepository.findById(99999L) } returns Optional.empty()

        assertThatThrownBy {
            service.updateProfile(
                targetUserId = 5L,
                requesterUserId = 1L,
                requesterProfileName = systemAdmin,
                profileId = 99999L,
            )
        }.isInstanceOf(AdminProfileNotFoundException::class.java)

        // 존재 검증 실패 시 dangling profileId 가 남지 않아야 한다.
        assertThat(target.profileId).isEqualTo(30L)
    }

    @Test
    @DisplayName("프로파일이 null 이던 사용자도 지정할 수 있다")
    fun updateProfile_fromNull() {
        val target = user(5L, profileId = null)
        every { userRepository.findById(5L) } returns Optional.of(target)
        every { profileRepository.findById(25L) } returns Optional.of(profile(25L, "6.조장"))

        service.updateProfile(
            targetUserId = 5L,
            requesterUserId = 1L,
            requesterProfileName = systemAdmin,
            profileId = 25L,
        )

        assertThat(target.profileId).isEqualTo(25L)
        verify(exactly = 1) { adminPermissionCache.invalidate(5L) }
    }
}
