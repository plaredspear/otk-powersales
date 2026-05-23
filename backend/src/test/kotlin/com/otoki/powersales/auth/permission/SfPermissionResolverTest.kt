package com.otoki.powersales.auth.permission

import com.otoki.powersales.auth.sharing.entity.PermissionSetAssignment
import com.otoki.powersales.auth.sharing.entity.PermissionSetFlags
import com.otoki.powersales.auth.sharing.entity.ProfileFlags
import com.otoki.powersales.auth.sharing.repository.PermissionSetAssignmentRepository
import com.otoki.powersales.auth.sharing.repository.PermissionSetFlagsRepository
import com.otoki.powersales.auth.sharing.repository.ProfileFlagsRepository
import com.otoki.powersales.user.entity.User
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import java.util.Optional

@DisplayName("SfPermissionResolver")
class SfPermissionResolverTest {

    private val profileFlagsRepository: ProfileFlagsRepository = mockk()
    private val assignmentRepository: PermissionSetAssignmentRepository = mockk()
    private val permissionSetFlagsRepository: PermissionSetFlagsRepository = mockk()
    private val entitySfNameRegistry: EntitySfNameRegistry = mockk()
    private val objectMapper = JsonMapper.builder().build()

    private val resolver = SfPermissionResolver(
        profileFlagsRepository = profileFlagsRepository,
        permissionSetAssignmentRepository = assignmentRepository,
        permissionSetFlagsRepository = permissionSetFlagsRepository,
        entitySfNameRegistry = entitySfNameRegistry,
        objectMapper = objectMapper,
    )

    @Test
    @DisplayName("custom_permissions JSON — 가상 자원 권한 키 산출 (spec #808)")
    fun customPermissionsParsed() {
        val user = userWithProfile(profileId = null)
        every { profileFlagsRepository.findByProfileId(any()) } returns null

        val flags = permissionSetFlagsOf(
            id = 100,
            customPermissions = """{"dashboard": {"allowRead": true, "allowEdit": false}}""",
        )
        every { assignmentRepository.findAllByAssigneeUserIdAndIsActiveTrue(99L) } returns listOf(
            assignmentOf(permissionSetFlagsId = 100),
        )
        every { permissionSetFlagsRepository.findById(100L) } returns Optional.of(flags)
        every { entitySfNameRegistry.allResources() } returns emptySet()

        val result = resolver.resolveForUser(user)

        assertThat(result).contains("dashboard:R")
        assertThat(result).doesNotContain("dashboard:E")
    }

    @Test
    @DisplayName("MODIFY_ALL_DATA 비트 — 모든 자원 (entity + custom) 으로 펼침 (spec #808)")
    fun modifyAllDataExpandsAcrossAllResources() {
        val user = userWithProfile(profileId = 1L)
        val profileFlags = profileFlagsOf(profileId = 1L, modifyAllData = true)
        every { profileFlagsRepository.findByProfileId(1L) } returns profileFlags
        every { assignmentRepository.findAllByAssigneeUserIdAndIsActiveTrue(99L) } returns emptyList()
        every { entitySfNameRegistry.allResources() } returns setOf("employee", "education_post", "dashboard")

        val result = resolver.resolveForUser(user)

        assertThat(result).contains("SYSTEM:MODIFY_ALL_DATA")
        assertThat(result).contains("employee:R", "employee:C", "employee:E", "employee:D")
        assertThat(result).contains("education_post:R", "education_post:C", "education_post:E", "education_post:D")
        assertThat(result).contains("dashboard:R", "dashboard:C", "dashboard:E", "dashboard:D")
    }

    @Test
    @DisplayName("VIEW_ALL_DATA 비트 — 모든 자원의 READ 키만 펼침")
    fun viewAllDataExpandsReadOnly() {
        val user = userWithProfile(profileId = 1L)
        val profileFlags = profileFlagsOf(profileId = 1L, viewAllData = true)
        every { profileFlagsRepository.findByProfileId(1L) } returns profileFlags
        every { assignmentRepository.findAllByAssigneeUserIdAndIsActiveTrue(99L) } returns emptyList()
        every { entitySfNameRegistry.allResources() } returns setOf("employee", "dashboard")

        val result = resolver.resolveForUser(user)

        assertThat(result).contains("SYSTEM:VIEW_ALL_DATA")
        assertThat(result).contains("employee:R", "dashboard:R")
        assertThat(result).doesNotContain("employee:E", "dashboard:D")
    }

    @Test
    @DisplayName("custom_permissions JSON 깨짐 — 무시 + 다른 권한 산출 정상")
    fun corruptedCustomPermissionsJsonIgnored() {
        val user = userWithProfile(profileId = null)
        every { profileFlagsRepository.findByProfileId(any()) } returns null

        val flags = permissionSetFlagsOf(
            id = 200,
            customPermissions = "not-a-valid-json{{",
            objectPermissions = """{"Account": {"allowRead": true}}""",
        )
        every { assignmentRepository.findAllByAssigneeUserIdAndIsActiveTrue(99L) } returns listOf(
            assignmentOf(permissionSetFlagsId = 200),
        )
        every { permissionSetFlagsRepository.findById(200L) } returns Optional.of(flags)
        every { entitySfNameRegistry.toEntityTableName("Account") } returns "account"
        every { entitySfNameRegistry.allResources() } returns emptySet()

        val result = resolver.resolveForUser(user)

        assertThat(result).contains("account:R")
    }

    private fun userWithProfile(profileId: Long?): User {
        val mock = mockk<User>()
        every { mock.id } returns 99L
        every { mock.profileId } returns profileId
        return mock
    }

    private fun profileFlagsOf(
        profileId: Long,
        viewAllData: Boolean = false,
        modifyAllData: Boolean = false,
    ): ProfileFlags {
        return ProfileFlags(
            profileId = profileId,
            permissionsViewAllData = viewAllData,
            permissionsModifyAllData = modifyAllData,
        )
    }

    private fun permissionSetFlagsOf(
        id: Long,
        objectPermissions: String? = null,
        customPermissions: String? = null,
    ): PermissionSetFlags {
        return PermissionSetFlags(
            id = id,
            permissionSetSfid = "0PS3z00000A${id}",
            permissionSetName = "PS_$id",
            objectPermissions = objectPermissions,
            customPermissions = customPermissions,
        )
    }

    private fun assignmentOf(permissionSetFlagsId: Long): PermissionSetAssignment {
        return PermissionSetAssignment(
            id = 1L,
            sfid = null,
            assigneeUserSfid = "005000000000001",
            assigneeUserId = 99L,
            permissionSetSfid = "0PS3z00000A${permissionSetFlagsId}",
            permissionSetFlagsId = permissionSetFlagsId,
            isActive = true,
        )
    }
}
