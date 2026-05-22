package com.otoki.powersales.auth.sharing.service

import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.auth.repository.UserRoleEntityRepository
import com.otoki.powersales.auth.sharing.repository.UserRoleHierarchySnapshotRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.util.Optional

/**
 * UserRoleHierarchyTraversal 단위 테스트 (spec #782 P2-B).
 *
 * EntityManager / native query 는 mock 처리 — application-level 재귀 (computeAncestorPath) 만 검증.
 * DB recursive CTE 동작은 통합 테스트 단계에서 별도 검증 (testcontainers + Flyway V173/V174 도입 시).
 */
class UserRoleHierarchyTraversalTest {

    private val snapshotRepository = mockk<UserRoleHierarchySnapshotRepository>(relaxed = true)
    private val userRoleRepository = mockk<UserRoleEntityRepository>(relaxed = true)
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    private lateinit var traversal: UserRoleHierarchyTraversal

    @BeforeEach
    fun setUp() {
        traversal = UserRoleHierarchyTraversal(snapshotRepository, userRoleRepository, objectMapper)
    }

    private fun stubUserRoleChain(chain: Map<Long, Long?>) {
        chain.forEach { (id, parentId) ->
            val role = mockk<UserRole>()
            every { role.parentUserRoleId } returns parentId
            every { userRoleRepository.findById(id) } returns Optional.of(role)
        }
    }

    @Test
    @DisplayName("ancestor path — root 까지 정상 추적")
    fun ancestorPathNormal() {
        // 10 (root) ← 20 ← 30 — 본 user 의 path = [30, 20, 10]
        stubUserRoleChain(
            mapOf(
                30L to 20L,
                20L to 10L,
                10L to null,
            ),
        )
        every { snapshotRepository.findById(30L) } returns Optional.empty()

        val path = traversal.getAncestorPath(30L)
        assertThat(path).containsExactly(30L, 20L, 10L)
    }

    @Test
    @DisplayName("cycle 감지 — IllegalStateException")
    fun ancestorPathCycle() {
        // 30 ← 20 ← 30 (cycle)
        stubUserRoleChain(
            mapOf(
                30L to 20L,
                20L to 30L,
            ),
        )
        every { snapshotRepository.findById(30L) } returns Optional.empty()

        assertThatThrownBy { traversal.getAncestorPath(30L) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("UserRole cycle detected")
    }

    @Test
    @DisplayName("자기참조 (parent_user_role_id = self) 감지")
    fun ancestorPathSelfReference() {
        stubUserRoleChain(mapOf(30L to 30L))
        every { snapshotRepository.findById(30L) } returns Optional.empty()

        assertThatThrownBy { traversal.getAncestorPath(30L) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("UserRole self-reference")
    }

    @Test
    @DisplayName("root UserRole — path = 자기 자신만")
    fun ancestorPathRoot() {
        stubUserRoleChain(mapOf(10L to null))
        every { snapshotRepository.findById(10L) } returns Optional.empty()

        val path = traversal.getAncestorPath(10L)
        assertThat(path).containsExactly(10L)
    }
}
