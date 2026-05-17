package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.entity.RolePermission
import com.otoki.powersales.admin.repository.RolePermissionRepository
import com.otoki.powersales.admin.security.RolePermissionMatrix
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.util.function.Consumer
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionTemplate

@ExtendWith(MockitoExtension::class)
@DisplayName("RolePermissionSyncRunner 테스트")
class RolePermissionSyncRunnerTest {

    @Mock
    private lateinit var rolePermissionRepository: RolePermissionRepository

    @Mock
    private lateinit var transactionTemplate: TransactionTemplate

    @InjectMocks
    private lateinit var runner: RolePermissionSyncRunner

    @Captor
    private lateinit var savedCaptor: ArgumentCaptor<List<RolePermission>>

    private fun stubInlineTransaction() {
        val status = mock<TransactionStatus>()
        whenever(transactionTemplate.executeWithoutResult(any<Consumer<TransactionStatus>>())).thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            (invocation.arguments[0] as Consumer<TransactionStatus>).accept(status)
            null
        }
    }

    @Test
    @DisplayName("빈 DB - 매트릭스 전체 INSERT")
    fun emptyDb_insertsAllMatrixPairs() {
        // Given
        stubInlineTransaction()
        whenever(rolePermissionRepository.findAll()).thenReturn(emptyList())

        // When
        runner.run(DefaultApplicationArguments())

        // Then
        verify(rolePermissionRepository).saveAll(savedCaptor.capture())
        val saved = savedCaptor.value.map { it.role to it.permission }.toSet()
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
        whenever(rolePermissionRepository.findAll()).thenReturn(firstTwoExisting)

        // When
        runner.run(DefaultApplicationArguments())

        // Then
        verify(rolePermissionRepository).saveAll(savedCaptor.capture())
        val saved = savedCaptor.value.map { it.role to it.permission }.toSet()
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
        whenever(rolePermissionRepository.findAll()).thenReturn(matrixRows + dbOnly)

        // When
        runner.run(DefaultApplicationArguments())

        // Then — saveAll 호출 없음 (모든 SoT row 이미 존재)
        verify(rolePermissionRepository, never()).saveAll(any<List<RolePermission>>())
    }

    @Test
    @DisplayName("매트릭스 전부 일치 - INSERT 호출 없음")
    fun fullySynced_noInsert() {
        // Given
        stubInlineTransaction()
        val matrixRows = RolePermissionMatrix.asPairs()
            .map { (role, perm) -> RolePermission(role = role.name, permission = perm.name) }
        whenever(rolePermissionRepository.findAll()).thenReturn(matrixRows)

        // When
        runner.run(DefaultApplicationArguments())

        // Then
        verify(rolePermissionRepository, never()).saveAll(any<List<RolePermission>>())
    }
}
