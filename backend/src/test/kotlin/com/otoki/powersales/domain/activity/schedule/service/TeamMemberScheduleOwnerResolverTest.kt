package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.activity.schedule.service.TeamMemberScheduleOwnerResolver
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.user.entity.User
import com.otoki.powersales.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("TeamMemberScheduleOwnerResolver — 여사원일정 owner = 소속 조장 User")
class TeamMemberScheduleOwnerResolverTest {

    private val employeeRepository: EmployeeRepository = mockk()
    private val userRepository: UserRepository = mockk()
    private val resolver = TeamMemberScheduleOwnerResolver(employeeRepository, userRepository)

    private fun employee(
        id: Long,
        employeeCode: String,
        costCenterCode: String?,
        role: String? = null,
    ): Employee = Employee(
        id = id,
        employeeCode = employeeCode,
        name = "사원$id",
        role = role,
        costCenterCode = costCenterCode,
    )

    private fun user(id: Long, employeeCode: String): User =
        User(id = id, username = "user$id", employeeCode = employeeCode, password = "")

    @Test
    @DisplayName("costCenterCode → 조장 Employee → User 해소 (단건)")
    fun resolveOwnerHappyPath() {
        val woman = employee(id = 1L, employeeCode = "W001", costCenterCode = "CC10")
        val leader = employee(id = 2L, employeeCode = "L001", costCenterCode = "CC10", role = AppAuthority.LEADER)
        val leaderUser = user(id = 100L, employeeCode = "L001")

        every {
            employeeRepository.findByCostCenterCodeInAndRoleAndAppLoginActiveTrue(listOf("CC10"), AppAuthority.LEADER)
        } returns listOf(leader)
        every { userRepository.findByEmployeeCodeIn(listOf("L001")) } returns listOf(leaderUser)

        val owner = resolver.resolveOwner(woman)

        assertThat(owner).isNotNull
        assertThat(owner!!.id).isEqualTo(100L)
        assertThat(owner.employeeCode).isEqualTo("L001")
    }

    @Test
    @DisplayName("costCenterCode 없으면 null (조회 호출 없이 단락)")
    fun resolveOwnerNoCostCenter() {
        val woman = employee(id = 1L, employeeCode = "W001", costCenterCode = null)

        val owner = resolver.resolveOwner(woman)

        assertThat(owner).isNull()
    }

    @Test
    @DisplayName("조장 부재 시 null (해당 costCenterCode 미포함)")
    fun resolveOwnerNoLeader() {
        val woman = employee(id = 1L, employeeCode = "W001", costCenterCode = "CC10")

        every {
            employeeRepository.findByCostCenterCodeInAndRoleAndAppLoginActiveTrue(listOf("CC10"), AppAuthority.LEADER)
        } returns emptyList()

        val owner = resolver.resolveOwner(woman)

        assertThat(owner).isNull()
    }

    @Test
    @DisplayName("조장은 있으나 매칭 User 부재 시 null")
    fun resolveOwnerLeaderWithoutUser() {
        val woman = employee(id = 1L, employeeCode = "W001", costCenterCode = "CC10")
        val leader = employee(id = 2L, employeeCode = "L001", costCenterCode = "CC10", role = AppAuthority.LEADER)

        every {
            employeeRepository.findByCostCenterCodeInAndRoleAndAppLoginActiveTrue(listOf("CC10"), AppAuthority.LEADER)
        } returns listOf(leader)
        every { userRepository.findByEmployeeCodeIn(listOf("L001")) } returns emptyList()

        val owner = resolver.resolveOwner(woman)

        assertThat(owner).isNull()
    }

    @Test
    @DisplayName("배치 — 여러 costCenterCode 일괄 해소 (해소 실패 costCenter 는 맵에서 제외)")
    fun resolveOwnersByCostCenterCodeBatch() {
        val womanA = employee(id = 1L, employeeCode = "W001", costCenterCode = "CC10")
        val womanB = employee(id = 2L, employeeCode = "W002", costCenterCode = "CC20") // 조장 없음
        val leaderA = employee(id = 3L, employeeCode = "L001", costCenterCode = "CC10", role = AppAuthority.LEADER)
        val leaderAUser = user(id = 100L, employeeCode = "L001")

        every {
            employeeRepository.findByCostCenterCodeInAndRoleAndAppLoginActiveTrue(
                listOf("CC10", "CC20"), AppAuthority.LEADER
            )
        } returns listOf(leaderA)
        every { userRepository.findByEmployeeCodeIn(listOf("L001")) } returns listOf(leaderAUser)

        val result = resolver.resolveOwnersByCostCenterCode(listOf(womanA, womanB))

        assertThat(result).containsOnlyKeys("CC10")
        assertThat(result["CC10"]!!.id).isEqualTo(100L)
        assertThat(result).doesNotContainKey("CC20")
    }
}
