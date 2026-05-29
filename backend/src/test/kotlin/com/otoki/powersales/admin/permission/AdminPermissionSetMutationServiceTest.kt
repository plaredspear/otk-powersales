package com.otoki.powersales.admin.permission

import com.otoki.powersales.admin.permission.dto.PermissionSetCreateRequest
import com.otoki.powersales.admin.permission.dto.PermissionSetUpdateFlagsRequest
import com.otoki.powersales.admin.permission.dto.PermissionSetUpdateMetaRequest
import com.otoki.powersales.admin.permission.exception.InvalidCustomPermissionKeyException
import com.otoki.powersales.admin.permission.exception.InvalidObjectPermissionKeyException
import com.otoki.powersales.admin.permission.exception.PermissionSetFlagsNotFoundException
import com.otoki.powersales.admin.permission.exception.PermissionSetNameAlreadyExistsException
import com.otoki.powersales.admin.permission.exception.PermissionSetNameInvalidException
import com.otoki.powersales.admin.permission.exception.PermissionSetNotFoundException
import com.otoki.powersales.admin.permission.exception.SfOriginDeleteBlockedException
import com.otoki.powersales.admin.security.AdminDataScopeCache
import com.otoki.powersales.auth.permission.AdminPermissionCache
import com.otoki.powersales.auth.permission.EntitySfNameRegistry
import com.otoki.powersales.auth.sharing.entity.PermissionSet
import com.otoki.powersales.auth.sharing.entity.PermissionSetAssignment
import com.otoki.powersales.auth.sharing.entity.PermissionSetChangeLog
import com.otoki.powersales.auth.sharing.entity.PermissionSetChangeLogEventType
import com.otoki.powersales.auth.sharing.entity.PermissionSetFlags
import com.otoki.powersales.auth.sharing.repository.PermissionSetAssignmentRepository
import com.otoki.powersales.auth.sharing.repository.PermissionSetChangeLogRepository
import com.otoki.powersales.auth.sharing.repository.PermissionSetFlagsRepository
import com.otoki.powersales.auth.sharing.repository.PermissionSetRepository
import com.otoki.powersales.user.repository.UserRepository
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import java.util.Optional

@DisplayName("AdminPermissionSetMutationService 테스트")
class AdminPermissionSetMutationServiceTest {

    private val permissionSetRepository: PermissionSetRepository = mockk()
    private val permissionSetFlagsRepository: PermissionSetFlagsRepository = mockk()
    private val permissionSetAssignmentRepository: PermissionSetAssignmentRepository = mockk()
    private val permissionSetChangeLogRepository: PermissionSetChangeLogRepository = mockk()
    private val entitySfNameRegistry: EntitySfNameRegistry = mockk()
    private val userRepository: UserRepository = mockk()
    private val adminPermissionCache: AdminPermissionCache = mockk(relaxed = true)
    private val adminDataScopeCache: AdminDataScopeCache = mockk(relaxed = true)
    private val objectMapper: ObjectMapper = ObjectMapper()

    private val service = AdminPermissionSetMutationService(
        permissionSetRepository,
        permissionSetFlagsRepository,
        permissionSetAssignmentRepository,
        permissionSetChangeLogRepository,
        entitySfNameRegistry,
        userRepository,
        adminPermissionCache,
        adminDataScopeCache,
        objectMapper,
    )

    private fun permissionSet(id: Long = 90, name: String = "New_Read_Permission", sfid: String? = null): PermissionSet =
        PermissionSet(id = id, sfid = sfid, name = name, label = null, description = null)

    private fun permissionSetFlags(
        id: Long = 90,
        permissionSetId: Long? = 90,
        isLocallyModified: Boolean = false,
    ): PermissionSetFlags = PermissionSetFlags(
        id = id,
        permissionSetSfid = null,
        permissionSetName = "New_Read_Permission",
        permissionsViewAllData = false,
        permissionsModifyAllData = false,
        objectPermissions = "{}",
        customPermissions = "{}",
        permissionSetId = permissionSetId,
        isLocallyModified = isLocallyModified,
    )

    // ── create ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create - 정상 생성 → PS + flags + change_log CREATE 1건")
    fun createSuccess() {
        every { permissionSetRepository.findByName("New_Read_Permission") } returns null
        every { permissionSetRepository.save(any()) } returns permissionSet()
        every { permissionSetFlagsRepository.save(any()) } returns permissionSetFlags()
        every { permissionSetChangeLogRepository.save(any()) } returnsArgument 0

        val response = service.create(
            PermissionSetCreateRequest(name = "New_Read_Permission", label = "신규 조회 권한", description = null),
            principalUserId = 100,
        )

        assertThat(response.permissionSetId).isEqualTo(90)
        assertThat(response.sfOrigin).isFalse
        assertThat(response.isLocallyModified).isFalse
        verify(exactly = 1) {
            permissionSetChangeLogRepository.save(match { it.eventType == PermissionSetChangeLogEventType.CREATE })
        }
        verify(exactly = 1) { adminPermissionCache.invalidateAll() }
    }

    @Test
    @DisplayName("create - name 빈 값 → INVALID_NAME")
    fun createBlankName() {
        assertThatThrownBy {
            service.create(PermissionSetCreateRequest(name = " ", label = null), principalUserId = 100)
        }.isInstanceOf(PermissionSetNameInvalidException::class.java)
    }

    @Test
    @DisplayName("create - name 80자 초과 → INVALID_NAME")
    fun createNameTooLong() {
        assertThatThrownBy {
            service.create(PermissionSetCreateRequest(name = "a".repeat(81), label = null), principalUserId = 100)
        }.isInstanceOf(PermissionSetNameInvalidException::class.java)
    }

    @Test
    @DisplayName("create - name 한글/특수문자 → INVALID_NAME")
    fun createNameInvalidChar() {
        assertThatThrownBy {
            service.create(PermissionSetCreateRequest(name = "한글이름", label = null), principalUserId = 100)
        }.isInstanceOf(PermissionSetNameInvalidException::class.java)
    }

    @Test
    @DisplayName("create - name 중복 → NAME_ALREADY_EXISTS")
    fun createNameConflict() {
        every { permissionSetRepository.findByName("AccountManagement") } returns permissionSet()

        assertThatThrownBy {
            service.create(PermissionSetCreateRequest(name = "AccountManagement"), principalUserId = 100)
        }.isInstanceOf(PermissionSetNameAlreadyExistsException::class.java)
    }

    // ── updateMeta ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateMeta - SF 출처 PS 메타 수정 → dirty 플래그 set")
    fun updateMetaSetsLocallyModifiedForSfOrigin() {
        val ps = permissionSet(sfid = "0PS001")
        val flags = permissionSetFlags(isLocallyModified = false)
        every { permissionSetRepository.findById(90) } returns Optional.of(ps)
        every { permissionSetFlagsRepository.findByPermissionSetId(90) } returns flags
        every { permissionSetRepository.save(any()) } returnsArgument 0
        every { permissionSetFlagsRepository.save(any()) } returnsArgument 0
        every { permissionSetChangeLogRepository.save(any()) } returnsArgument 0

        val response = service.updateMeta(
            permissionSetId = 90,
            request = PermissionSetUpdateMetaRequest(label = "신규 라벨", description = "수정됨"),
            principalUserId = 100,
        )

        assertThat(response.isLocallyModified).isTrue
        assertThat(response.sfOrigin).isTrue
        verify { permissionSetFlagsRepository.save(match { it.isLocallyModified }) }
        verify(exactly = 1) {
            permissionSetChangeLogRepository.save(match { it.eventType == PermissionSetChangeLogEventType.UPDATE_META })
        }
    }

    @Test
    @DisplayName("updateMeta - 신규 자체 PS 메타 수정 → dirty 플래그 set 안 함")
    fun updateMetaDoesNotSetDirtyForNewPs() {
        val ps = permissionSet(sfid = null)
        val flags = permissionSetFlags(isLocallyModified = false)
        every { permissionSetRepository.findById(90) } returns Optional.of(ps)
        every { permissionSetFlagsRepository.findByPermissionSetId(90) } returns flags
        every { permissionSetRepository.save(any()) } returnsArgument 0
        every { permissionSetChangeLogRepository.save(any()) } returnsArgument 0

        service.updateMeta(
            permissionSetId = 90,
            request = PermissionSetUpdateMetaRequest(label = "수정"),
            principalUserId = 100,
        )

        verify(exactly = 0) { permissionSetFlagsRepository.save(any()) }
    }

    @Test
    @DisplayName("updateMeta - 미존재 PS → PERMISSION_SET_NOT_FOUND")
    fun updateMetaNotFound() {
        every { permissionSetRepository.findById(99) } returns Optional.empty()
        assertThatThrownBy {
            service.updateMeta(99, PermissionSetUpdateMetaRequest(label = "x"), 100)
        }.isInstanceOf(PermissionSetNotFoundException::class.java)
    }

    // ── updateFlags ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateFlags - SF 출처 PS 비트 수정 → dirty 플래그 set + change_log UPDATE_FLAGS")
    fun updateFlagsSetsDirty() {
        val ps = permissionSet(sfid = "0PS001")
        val flags = permissionSetFlags()
        every { permissionSetRepository.findById(90) } returns Optional.of(ps)
        every { permissionSetFlagsRepository.findByPermissionSetId(90) } returns flags
        every { entitySfNameRegistry.snapshot() } returns mapOf("account" to "Account")
        every { entitySfNameRegistry.allResources() } returns setOf("account", "dashboard")
        every { permissionSetFlagsRepository.save(any()) } returnsArgument 0
        every { permissionSetChangeLogRepository.save(any()) } returnsArgument 0

        val response = service.updateFlags(
            permissionSetId = 90,
            request = PermissionSetUpdateFlagsRequest(
                viewAllData = true,
                modifyAllData = false,
                objectPermissions = mapOf("Account" to mapOf("allowRead" to true)),
                customPermissions = mapOf("dashboard" to mapOf("allowRead" to true)),
            ),
            principalUserId = 100,
        )

        assertThat(response.isLocallyModified).isTrue
        assertThat(response.viewAllData).isTrue
        verify(exactly = 1) {
            permissionSetChangeLogRepository.save(match { it.eventType == PermissionSetChangeLogEventType.UPDATE_FLAGS })
        }
    }

    @Test
    @DisplayName("updateFlags - 미등록 SObject 키 → INVALID_OBJECT_PERMISSION_KEY")
    fun updateFlagsInvalidSObjectKey() {
        val ps = permissionSet(sfid = null)
        val flags = permissionSetFlags()
        every { permissionSetRepository.findById(90) } returns Optional.of(ps)
        every { permissionSetFlagsRepository.findByPermissionSetId(90) } returns flags
        every { entitySfNameRegistry.snapshot() } returns mapOf("account" to "Account")
        every { entitySfNameRegistry.allResources() } returns setOf("account")

        assertThatThrownBy {
            service.updateFlags(
                permissionSetId = 90,
                request = PermissionSetUpdateFlagsRequest(
                    objectPermissions = mapOf("Foo__c" to mapOf("allowRead" to true)),
                ),
                principalUserId = 100,
            )
        }.isInstanceOf(InvalidObjectPermissionKeyException::class.java)
    }

    @Test
    @DisplayName("updateFlags - 미등록 custom 자원 키 → INVALID_CUSTOM_PERMISSION_KEY")
    fun updateFlagsInvalidCustomKey() {
        val ps = permissionSet(sfid = null)
        val flags = permissionSetFlags()
        every { permissionSetRepository.findById(90) } returns Optional.of(ps)
        every { permissionSetFlagsRepository.findByPermissionSetId(90) } returns flags
        every { entitySfNameRegistry.snapshot() } returns mapOf("account" to "Account")
        every { entitySfNameRegistry.allResources() } returns setOf("account", "dashboard")

        assertThatThrownBy {
            service.updateFlags(
                permissionSetId = 90,
                request = PermissionSetUpdateFlagsRequest(
                    customPermissions = mapOf("invalidResource" to mapOf("allowRead" to true)),
                ),
                principalUserId = 100,
            )
        }.isInstanceOf(InvalidCustomPermissionKeyException::class.java)
    }

    // ── delete ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete - SF 출처 PS 삭제 시도 → SF_ORIGIN_DELETE_BLOCKED (409)")
    fun deleteSfOriginBlocked() {
        every { permissionSetRepository.findById(90) } returns Optional.of(permissionSet(sfid = "0PS001"))

        assertThatThrownBy {
            service.delete(90, 100)
        }.isInstanceOf(SfOriginDeleteBlockedException::class.java)
    }

    @Test
    @DisplayName("delete - 신규 자체 PS 삭제 → assignment cascade + flags + ps hard delete + change_log DELETE")
    fun deleteSuccessCascade() {
        val ps = permissionSet(sfid = null)
        val flags = permissionSetFlags()
        val assignments = listOf(
            PermissionSetAssignment(
                id = 5001, sfid = null, assigneeUserSfid = null, assigneeUserId = 10,
                permissionSetSfid = null, permissionSetFlagsId = 90, isActive = true,
            ),
            PermissionSetAssignment(
                id = 5002, sfid = null, assigneeUserSfid = null, assigneeUserId = 11,
                permissionSetSfid = null, permissionSetFlagsId = 90, isActive = false,
            ),
        )
        every { permissionSetRepository.findById(90) } returns Optional.of(ps)
        every { permissionSetFlagsRepository.findByPermissionSetId(90) } returns flags
        every { permissionSetAssignmentRepository.findAllByPermissionSetFlagsId(90) } returns assignments
        every { permissionSetAssignmentRepository.deleteAll(any<List<PermissionSetAssignment>>()) } just runs
        every { permissionSetFlagsRepository.delete(flags) } just runs
        every { permissionSetRepository.delete(ps) } just runs
        every { permissionSetChangeLogRepository.save(any()) } returnsArgument 0

        service.delete(90, 100)

        val logSlot = slot<PermissionSetChangeLog>()
        verify(exactly = 1) { permissionSetChangeLogRepository.save(capture(logSlot)) }
        assertThat(logSlot.captured.eventType).isEqualTo(PermissionSetChangeLogEventType.DELETE)
        assertThat(logSlot.captured.permissionSetId).isNull() // FK SET NULL 동기
        assertThat(logSlot.captured.beforeSnapshot).isNotNull
        verify(exactly = 1) { permissionSetAssignmentRepository.deleteAll(assignments) }
        verify(exactly = 1) { permissionSetFlagsRepository.delete(flags) }
        verify(exactly = 1) { permissionSetRepository.delete(ps) }
        verify { adminPermissionCache.invalidate(10) }
        verify { adminPermissionCache.invalidate(11) }
    }

    @Test
    @DisplayName("delete - 미존재 PS → PERMISSION_SET_NOT_FOUND")
    fun deleteNotFound() {
        every { permissionSetRepository.findById(99) } returns Optional.empty()
        assertThatThrownBy {
            service.delete(99, 100)
        }.isInstanceOf(PermissionSetNotFoundException::class.java)
    }

    @Test
    @DisplayName("updateFlags - 신규 자체 PS 비트 수정 → dirty 안 set")
    fun updateFlagsDoesNotSetDirtyForNewPs() {
        val ps = permissionSet(sfid = null)
        val flags = permissionSetFlags(isLocallyModified = false)
        every { permissionSetRepository.findById(90) } returns Optional.of(ps)
        every { permissionSetFlagsRepository.findByPermissionSetId(90) } returns flags
        every { entitySfNameRegistry.snapshot() } returns mapOf("account" to "Account")
        every { entitySfNameRegistry.allResources() } returns setOf("account")
        every { permissionSetFlagsRepository.save(any()) } returnsArgument 0
        every { permissionSetChangeLogRepository.save(any()) } returnsArgument 0

        val response = service.updateFlags(
            permissionSetId = 90,
            request = PermissionSetUpdateFlagsRequest(
                objectPermissions = mapOf("Account" to mapOf("allowRead" to true)),
            ),
            principalUserId = 100,
        )

        assertThat(response.isLocallyModified).isFalse
        assertThat(response.sfOrigin).isFalse
    }

    @Test
    @DisplayName("updateFlags - flags 누락 시 PERMISSION_SET_FLAGS_NOT_FOUND (500 노출 차단)")
    fun updateFlagsFlagsMissing() {
        val ps = permissionSet(sfid = null)
        every { permissionSetRepository.findById(90) } returns Optional.of(ps)
        every { permissionSetFlagsRepository.findByPermissionSetId(90) } returns null

        assertThatThrownBy {
            service.updateFlags(90, PermissionSetUpdateFlagsRequest(), 100)
        }.isInstanceOf(PermissionSetFlagsNotFoundException::class.java)
    }

    @Test
    @DisplayName("listChangeLog - changedAt desc 정렬 + changedByName 채움")
    fun listChangeLogReturnsResponses() {
        val log1 = com.otoki.powersales.auth.sharing.entity.PermissionSetChangeLog(
            id = 12,
            permissionSetId = 90,
            eventType = com.otoki.powersales.auth.sharing.entity.PermissionSetChangeLogEventType.UPDATE_FLAGS,
            beforeSnapshot = """{"viewAllData":false}""",
            afterSnapshot = """{"viewAllData":true}""",
            changedById = 100,
        )
        val pageResult = org.springframework.data.domain.PageImpl(
            listOf(log1),
            org.springframework.data.domain.PageRequest.of(0, 20, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "changedAt")),
            1,
        )
        every { permissionSetChangeLogRepository.findByPermissionSetId(eq(90), any()) } returns pageResult
        val mockUser: com.otoki.powersales.user.entity.User = io.mockk.mockk()
        every { mockUser.name } returns "관리자"
        every { userRepository.findById(100) } returns Optional.of(mockUser)

        val result = service.listChangeLog(90, 0, 20)

        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].changeLogId).isEqualTo(12)
        assertThat(result.content[0].eventType).isEqualTo("UPDATE_FLAGS")
        assertThat(result.content[0].changedByName).isEqualTo("관리자")
    }
}
