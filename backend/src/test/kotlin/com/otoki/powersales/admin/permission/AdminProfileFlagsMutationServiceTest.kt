package com.otoki.powersales.admin.permission

import com.otoki.powersales.admin.permission.dto.ProfileUpdateFlagsRequest
import com.otoki.powersales.admin.permission.exception.InvalidCustomPermissionKeyException
import com.otoki.powersales.admin.permission.exception.InvalidObjectPermissionKeyException
import com.otoki.powersales.admin.permission.exception.ProfileFlagsNotFoundException
import com.otoki.powersales.admin.permission.exception.ProfileNotFoundException
import com.otoki.powersales.admin.security.AdminDataScopeCache
import com.otoki.powersales.auth.entity.Profile
import com.otoki.powersales.auth.permission.AdminPermissionCache
import com.otoki.powersales.auth.permission.EntitySfNameRegistry
import com.otoki.powersales.auth.repository.ProfileRepository
import com.otoki.powersales.auth.sharing.entity.ProfileFlags
import com.otoki.powersales.auth.sharing.repository.ProfileFlagsRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.cache.CacheManager
import java.util.Optional
import tools.jackson.databind.ObjectMapper

/**
 * AdminProfileFlagsMutationService 단위 테스트 — Profile 권한 비트 편집 (SF 정합 객체권한 복원).
 */
class AdminProfileFlagsMutationServiceTest {

    private val profileRepository: ProfileRepository = mockk(relaxed = true)
    private val profileFlagsRepository: ProfileFlagsRepository = mockk(relaxed = true)
    // save 는 relaxed 가 Object 를 반환해 ProfileFlags 캐스팅 실패 → 명시 stub (returnsArgument 0).
    private val entitySfNameRegistry: EntitySfNameRegistry = mockk(relaxed = true)
    private val adminPermissionCache: AdminPermissionCache = mockk(relaxed = true)
    private val adminDataScopeCache: AdminDataScopeCache = mockk(relaxed = true)
    private val cacheManager: CacheManager = mockk(relaxed = true)
    private val objectMapper = ObjectMapper()

    private val service = AdminProfileFlagsMutationService(
        profileRepository,
        profileFlagsRepository,
        entitySfNameRegistry,
        adminPermissionCache,
        adminDataScopeCache,
        cacheManager,
        objectMapper,
    )

    private fun profile(id: Long = 10L, name: String = "6.조장") =
        Profile(sfid = "PROF1", name = name)

    private fun profileFlags(profileId: Long = 10L) = ProfileFlags(profileId = profileId)

    @Test
    fun `updateFlags — object_permissions 키 검증 통과 시 flags 갱신 + dirty set`() {
        val flags = profileFlags()
        every { profileRepository.findById(10L) } returns Optional.of(profile())
        every { profileFlagsRepository.findByProfileId(10L) } returns flags
        every { profileFlagsRepository.save(any()) } returnsArgument 0
        every { entitySfNameRegistry.snapshot() } returns mapOf("monthly_sales_history" to "MonthlySalesHistory__c")

        val request = ProfileUpdateFlagsRequest(
            objectPermissions = mapOf("MonthlySalesHistory__c" to mapOf("allowRead" to true)),
        )
        val response = service.updateFlags(10L, request, principalUserId = 1L)

        assertThat(response.objectPermissions).containsKey("MonthlySalesHistory__c")
        assertThat(response.isLocallyModified).isTrue()
        assertThat(flags.isLocallyModified).isTrue()
    }

    @Test
    fun `updateFlags — system 비트 5종이 모두 반영된다`() {
        val flags = profileFlags()
        every { profileRepository.findById(10L) } returns Optional.of(profile())
        every { profileFlagsRepository.findByProfileId(10L) } returns flags
        every { profileFlagsRepository.save(any()) } returnsArgument 0

        val request = ProfileUpdateFlagsRequest(
            viewAllData = true,
            modifyAllData = true,
            viewAllUsers = true,
            manageUsers = true,
            apiEnabled = true,
        )
        val response = service.updateFlags(10L, request, 1L)

        assertThat(response.viewAllData).isTrue()
        assertThat(response.modifyAllData).isTrue()
        assertThat(response.viewAllUsers).isTrue()
        assertThat(response.manageUsers).isTrue()
        assertThat(response.apiEnabled).isTrue()
    }

    @Test
    fun `updateFlags — 존재하지 않는 Profile 이면 ProfileNotFoundException`() {
        every { profileRepository.findById(99L) } returns Optional.empty()

        assertThatThrownBy { service.updateFlags(99L, ProfileUpdateFlagsRequest(), 1L) }
            .isInstanceOf(ProfileNotFoundException::class.java)
    }

    @Test
    fun `updateFlags — flags 행이 없으면 ProfileFlagsNotFoundException`() {
        every { profileRepository.findById(10L) } returns Optional.of(profile())
        every { profileFlagsRepository.findByProfileId(10L) } returns null

        assertThatThrownBy { service.updateFlags(10L, ProfileUpdateFlagsRequest(), 1L) }
            .isInstanceOf(ProfileFlagsNotFoundException::class.java)
    }

    @Test
    fun `updateFlags — 미등록 object 키면 InvalidObjectPermissionKeyException`() {
        every { profileRepository.findById(10L) } returns Optional.of(profile())
        every { profileFlagsRepository.findByProfileId(10L) } returns profileFlags()
        every { entitySfNameRegistry.snapshot() } returns mapOf("account" to "Account")

        val request = ProfileUpdateFlagsRequest(
            objectPermissions = mapOf("UnknownObj" to mapOf("allowRead" to true)),
        )
        assertThatThrownBy { service.updateFlags(10L, request, 1L) }
            .isInstanceOf(InvalidObjectPermissionKeyException::class.java)
    }

    @Test
    fun `updateFlags — 미등록 custom 키면 InvalidCustomPermissionKeyException`() {
        every { profileRepository.findById(10L) } returns Optional.of(profile())
        every { profileFlagsRepository.findByProfileId(10L) } returns profileFlags()
        every { entitySfNameRegistry.snapshot() } returns mapOf("account" to "Account")
        every { entitySfNameRegistry.allResources() } returns setOf("account", "dashboard")

        val request = ProfileUpdateFlagsRequest(
            customPermissions = mapOf("unknown_resource" to mapOf("allowRead" to true)),
        )
        assertThatThrownBy { service.updateFlags(10L, request, 1L) }
            .isInstanceOf(InvalidCustomPermissionKeyException::class.java)
    }
}
