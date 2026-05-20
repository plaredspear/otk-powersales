package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.entity.RolePermission
import com.otoki.powersales.admin.repository.RolePermissionRepository
import com.otoki.powersales.admin.security.RolePermissionMatrix
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import java.util.function.Consumer

@DisplayName("RolePermissionSyncRunner 테스트")
class RolePermissionSyncRunnerTest {

    private val rolePermissionRepository: RolePermissionRepository = mockk()
    private val transactionTemplate: TransactionTemplate = mockk()
    private val runner = RolePermissionSyncRunner(rolePermissionRepository, transactionTemplate)

    private fun stubInlineTransaction() {
        val status = mockk<TransactionStatus>()
        every { transactionTemplate.executeWithoutResult(any()) } answers {
            firstArg<Consumer<TransactionStatus>>().accept(status)
        }
    }

    @Test
    @DisplayName("빈 DB - 매트릭스 전체 INSERT")
    fun emptyDb_insertsAllMatrixPairs() {
        // Given
        stubInlineTransaction()
        every { rolePermissionRepository.findAll() } returns emptyList()
        val savedSlot = slot<List<RolePermission>>()
        every { rolePermissionRepository.saveAll(capture(savedSlot)) } answers { firstArg<List<RolePermission>>() }

        // When
        runner.run(DefaultApplicationArguments())

        // Then
        verify { rolePermissionRepository.saveAll(any<List<RolePermission>>()) }
        val saved = savedSlot.captured.map { it.role to it.permission }.toSet()
        val expected = RolePermissionMatrix.asPairs()
            .map { (role, perm) -> role.name to perm.name }
            .toSet()
        assertThat(saved).isEqualTo(expected)
    }

    @Test
    @DisplayName("부분 DB - 누락된 row 만 INSERT")
    fun partialDb_insertsOnlyMissing() {
        // Given
        stubInlineTransaction()
        val firstTwoExisting = RolePermissionMatrix.asPairs()
            .take(2)
            .map { (role, perm) -> RolePermission(role = role.name, permission = perm.name) }
        every { rolePermissionRepository.findAll() } returns firstTwoExisting
        val savedSlot = slot<List<RolePermission>>()
        every { rolePermissionRepository.saveAll(capture(savedSlot)) } answers { firstArg<List<RolePermission>>() }

        // When
        runner.run(DefaultApplicationArguments())

        // Then
        verify { rolePermissionRepository.saveAll(any<List<RolePermission>>()) }
        val saved = savedSlot.captured.map { it.role to it.permission }.toSet()
        val allPairs = RolePermissionMatrix.asPairs()
            .map { (role, perm) -> role.name to perm.name }
            .toSet()
        val existing = firstTwoExisting.map { it.role to it.permission }.toSet()
        assertThat(saved).isEqualTo(allPairs - existing)
    }

    @Test
    @DisplayName("DB-only row - 보존 (INSERT only)")
    fun dbOnlyRows_arePreserved() {
        // Given — 매트릭스에 없는 fictitious 권한 1건이 DB 에 있다
        stubInlineTransaction()
        val matrixRows = RolePermissionMatrix.asPairs()
            .map { (role, perm) -> RolePermission(role = role.name, permission = perm.name) }
        val dbOnly = RolePermission(role = "WOMAN", permission = "CUSTOM_RUNTIME_GRANTED")
        every { rolePermissionRepository.findAll() } returns (matrixRows + dbOnly)

        // When
        runner.run(DefaultApplicationArguments())

        // Then — saveAll 호출 없음 (모든 SoT row 이미 존재)
        verify(exactly = 0) { rolePermissionRepository.saveAll(any<List<RolePermission>>()) }
    }

    @Test
    @DisplayName("매트릭스 전부 일치 - INSERT 호출 없음")
    fun fullySynced_noInsert() {
        // Given
        stubInlineTransaction()
        val matrixRows = RolePermissionMatrix.asPairs()
            .map { (role, perm) -> RolePermission(role = role.name, permission = perm.name) }
        every { rolePermissionRepository.findAll() } returns matrixRows

        // When
        runner.run(DefaultApplicationArguments())

        // Then
        verify(exactly = 0) { rolePermissionRepository.saveAll(any<List<RolePermission>>()) }
    }
}
