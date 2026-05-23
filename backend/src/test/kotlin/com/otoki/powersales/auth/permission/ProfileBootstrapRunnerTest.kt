package com.otoki.powersales.auth.permission

import com.otoki.powersales.auth.entity.Profile
import com.otoki.powersales.auth.repository.ProfileRepository
import com.otoki.powersales.auth.sharing.entity.ProfileFlags
import com.otoki.powersales.auth.sharing.repository.ProfileFlagsRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.transaction.support.TransactionTemplate

@DisplayName("ProfileBootstrapRunner")
class ProfileBootstrapRunnerTest {

    private val profileRepository: ProfileRepository = mockk()
    private val profileFlagsRepository: ProfileFlagsRepository = mockk()
    private val transactionTemplate: TransactionTemplate = mockk()

    private val runner = ProfileBootstrapRunner(
        profileRepository,
        profileFlagsRepository,
        transactionTemplate,
    )

    init {
        // executeWithoutResult(Consumer<TransactionStatus>) 람다를 즉시 실행하도록 stub
        every { transactionTemplate.executeWithoutResult(any()) } answers {
            val action = arg<java.util.function.Consumer<org.springframework.transaction.TransactionStatus>>(0)
            action.accept(mockk(relaxed = true))
        }
    }

    @Test
    @DisplayName("부재 Profile 자동 생성 — 12종 INSERT")
    fun insertsMissingProfiles() {
        every { profileRepository.findByName(any()) } returns null
        every { profileRepository.save(any<Profile>()) } answers { firstArg<Profile>() }
        every { profileFlagsRepository.findByProfileId(any()) } returns null
        every { profileFlagsRepository.save(any<ProfileFlags>()) } answers { firstArg<ProfileFlags>() }

        runner.run(DefaultApplicationArguments())

        // 12종 INSERT 보장 — syncProfiles + syncSystemAdminFlags (재조회)
        verify(atLeast = 12) { profileRepository.save(any<Profile>()) }
    }

    @Test
    @DisplayName("시스템 관리자 ProfileFlags 5비트 모두 TRUE 보장")
    fun ensuresSystemAdminAllBitsTrue() {
        val sysadminProfile = Profile(id = 7, name = SystemAdminProfilePolicy.SYSTEM_ADMIN_PROFILE_NAME)
        every { profileRepository.findByName(any()) } returns sysadminProfile

        val flagsSlot = slot<ProfileFlags>()
        every { profileFlagsRepository.findByProfileId(7) } returns ProfileFlags(profileId = 7)
        every { profileFlagsRepository.save(capture(flagsSlot)) } answers { firstArg() }

        runner.run(DefaultApplicationArguments())

        // 가장 최근 save 가 시스템 관리자 flags — 5 비트 모두 TRUE
        assertThat(flagsSlot.captured.permissionsViewAllData).isTrue()
        assertThat(flagsSlot.captured.permissionsModifyAllData).isTrue()
        assertThat(flagsSlot.captured.permissionsViewAllUsers).isTrue()
        assertThat(flagsSlot.captured.permissionsManageUsers).isTrue()
        assertThat(flagsSlot.captured.permissionsApiEnabled).isTrue()
    }
}
