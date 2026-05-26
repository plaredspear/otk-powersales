package com.otoki.powersales.auth.sharing.service

import com.otoki.powersales.employee.entity.Group
import com.otoki.powersales.employee.repository.GroupRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("GroupMembershipEvaluator")
class GroupMembershipEvaluatorTest {

    private val groupRepository: GroupRepository = mockk()
    private val userRoleHierarchyTraversal: UserRoleHierarchyTraversal = mockk()
    private val evaluator = GroupMembershipEvaluator(groupRepository, userRoleHierarchyTraversal)

    @Test
    @DisplayName("ancestor path 일람을 IN 절 1회로 조회 — forEach N+1 회피")
    fun ancestorPathLoadedAsSingleInQuery() {
        val ancestorPath = listOf(100L, 200L, 300L, 400L, 500L)
        every { userRoleHierarchyTraversal.getAncestorPath(100L) } returns ancestorPath
        every { groupRepository.findAllByRelatedUserId(1L) } returns emptyList()
        every { groupRepository.findAllByRelatedUserRoleIdIn(ancestorPath) } returns listOf(
            groupWithId(10L), groupWithId(20L),
        )

        val result = evaluator.getMemberGroupIds(userId = 1L, userRoleId = 100L)

        assertThat(result).containsExactlyInAnyOrder(10L, 20L)
        verify(exactly = 1) { groupRepository.findAllByRelatedUserRoleIdIn(ancestorPath) }
        verify(exactly = 0) { groupRepository.findAllByRelatedUserRoleId(any()) }
    }

    @Test
    @DisplayName("userRoleId null — User 직접 매칭만 조회 (ancestor 단계 skip)")
    fun userRoleNullSkipsAncestorLookup() {
        every { groupRepository.findAllByRelatedUserId(1L) } returns listOf(groupWithId(5L))

        val result = evaluator.getMemberGroupIds(userId = 1L, userRoleId = null)

        assertThat(result).containsExactly(5L)
        verify(exactly = 0) { groupRepository.findAllByRelatedUserRoleIdIn(any()) }
    }

    @Test
    @DisplayName("ancestor path 빈 일람 — IN 절 쿼리 skip")
    fun emptyAncestorPathSkipsQuery() {
        every { userRoleHierarchyTraversal.getAncestorPath(100L) } returns emptyList()
        every { groupRepository.findAllByRelatedUserId(1L) } returns emptyList()

        val result = evaluator.getMemberGroupIds(userId = 1L, userRoleId = 100L)

        assertThat(result).isEmpty()
        verify(exactly = 0) { groupRepository.findAllByRelatedUserRoleIdIn(any()) }
    }

    private fun groupWithId(groupId: Long): Group {
        val mock = mockk<Group>()
        every { mock.id } returns groupId
        return mock
    }
}
