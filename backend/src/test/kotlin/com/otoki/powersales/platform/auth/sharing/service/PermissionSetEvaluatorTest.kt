package com.otoki.powersales.platform.auth.sharing.service

import com.otoki.powersales.platform.auth.sharing.entity.PermissionSetAssignment
import com.otoki.powersales.platform.auth.sharing.entity.PermissionSetFlags
import com.otoki.powersales.platform.auth.sharing.repository.PermissionSetAssignmentRepository
import com.otoki.powersales.platform.auth.sharing.repository.PermissionSetFlagsRepository
import com.otoki.powersales.platform.auth.sharing.service.PermissionSetEvaluator
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

@DisplayName("PermissionSetEvaluator")
class PermissionSetEvaluatorTest {

    private val assignmentRepository: PermissionSetAssignmentRepository = mockk()
    private val flagsRepository: PermissionSetFlagsRepository = mockk()
    private val objectMapper = JsonMapper.builder().build()
    private val evaluator = PermissionSetEvaluator(assignmentRepository, flagsRepository, objectMapper)

    @Test
    @DisplayName("PermissionSetFlags 일람을 findAllById 1회로 일괄 조회 — forEach N+1 회피")
    fun flagsLoadedInSingleBatch() {
        every { assignmentRepository.findAllByAssigneeUserIdAndIsActiveTrue(1L) } returns listOf(
            assignmentOf(flagsId = 100),
            assignmentOf(flagsId = 200),
            assignmentOf(flagsId = 300),
        )
        val flags100 = flagsOf(id = 100, permissionSetId = 1100)
        val flags200 = flagsOf(id = 200, permissionSetId = 1200, viewAll = true)
        val flags300 = flagsOf(id = 300, permissionSetId = 1300, objectJson = """{"Account": {"viewAllRecords": true}}""")
        every { flagsRepository.findAllById(listOf(100L, 200L, 300L)) } returns listOf(flags100, flags200, flags300)

        val snapshot = evaluator.getPermissionSetSnapshot(1L)

        assertThat(snapshot.viewAllDataSystem).isTrue()
        assertThat(snapshot.permissionSetIds).containsExactlyInAnyOrder(1100L, 1200L, 1300L)
        assertThat(snapshot.viewAllRecordsBySObject).containsEntry("Account", true)
        verify(exactly = 1) { flagsRepository.findAllById(listOf(100L, 200L, 300L)) }
        verify(exactly = 0) { flagsRepository.findById(any()) }
    }

    @Test
    @DisplayName("assignment 일람 비어있음 — flags 조회 skip + NONE 반환")
    fun emptyAssignmentsReturnsNone() {
        every { assignmentRepository.findAllByAssigneeUserIdAndIsActiveTrue(1L) } returns emptyList()

        val snapshot = evaluator.getPermissionSetSnapshot(1L)

        assertThat(snapshot.permissionSetIds).isEmpty()
        assertThat(snapshot.viewAllDataSystem).isFalse()
        verify(exactly = 0) { flagsRepository.findAllById(any<Iterable<Long>>()) }
    }

    @Test
    @DisplayName("assignment 전부 permissionSetFlagsId NULL — flags 조회 skip + NONE 반환 (spec #798 Stage1 직후 상태)")
    fun nullFlagsIdReturnsNone() {
        every { assignmentRepository.findAllByAssigneeUserIdAndIsActiveTrue(1L) } returns listOf(
            assignmentOf(flagsId = null),
            assignmentOf(flagsId = null),
        )

        val snapshot = evaluator.getPermissionSetSnapshot(1L)

        assertThat(snapshot.permissionSetIds).isEmpty()
        verify(exactly = 0) { flagsRepository.findAllById(any<Iterable<Long>>()) }
    }

    private fun assignmentOf(flagsId: Long?): PermissionSetAssignment {
        return PermissionSetAssignment(
            id = 0L,
            sfid = null,
            assigneeUserSfid = "005000000000001",
            assigneeUserId = 1L,
            permissionSetSfid = "0PS3z00000A${flagsId ?: 0}",
            permissionSetFlagsId = flagsId,
            isActive = true,
        )
    }

    private fun flagsOf(
        id: Long,
        permissionSetId: Long? = null,
        viewAll: Boolean = false,
        modifyAll: Boolean = false,
        objectJson: String? = null,
    ): PermissionSetFlags {
        return PermissionSetFlags(
            id = id,
            permissionSetSfid = "0PS3z00000A${id}",
            permissionSetName = "PS_$id",
            permissionSetId = permissionSetId,
            permissionsViewAllData = viewAll,
            permissionsModifyAllData = modifyAll,
            objectPermissions = objectJson,
        )
    }
}
