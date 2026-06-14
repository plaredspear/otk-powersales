package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.admin.exception.EmployeeNotFoundException
import com.otoki.powersales.domain.activity.schedule.service.EmployeeWorkHistoryService
import com.otoki.powersales.platform.common.enums.WorkingType
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.activity.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Pageable
import java.time.LocalDate
import java.util.Optional

@DisplayName("EmployeeWorkHistoryService")
class EmployeeWorkHistoryServiceTest {

    private lateinit var employeeRepository: EmployeeRepository
    private lateinit var teamMemberScheduleRepository: TeamMemberScheduleRepository
    private lateinit var service: EmployeeWorkHistoryService

    @BeforeEach
    fun setUp() {
        employeeRepository = mockk()
        teamMemberScheduleRepository = mockk()
        service = EmployeeWorkHistoryService(employeeRepository, teamMemberScheduleRepository)
    }

    @Test
    @DisplayName("정상 — TeamMemberSchedule 을 DTO 로 변환해 반환")
    fun getRecentHistory_returnsItems() {
        val employee = Employee(id = 1L, employeeCode = "EMP001", name = "테스트사원")
        val s1 = TeamMemberSchedule(
            id = 10L,
            employee = employee,
            workingDate = LocalDate.of(2026, 5, 20),
            workingType = WorkingType.WORK,
        )
        val s2 = TeamMemberSchedule(
            id = 11L,
            employee = employee,
            workingDate = LocalDate.of(2026, 5, 19),
            workingType = WorkingType.ANNUAL_LEAVE,
        )
        every { employeeRepository.findById(1L) } returns Optional.of(employee)
        every {
            teamMemberScheduleRepository.findByEmployeeOrderByWorkingDateDescCreatedAtDesc(
                employee,
                any<Pageable>(),
            )
        } returns listOf(s1, s2)

        val response = service.getRecentHistory(1L, 10)

        assertThat(response.items).hasSize(2)
        assertThat(response.items[0].id).isEqualTo(10L)
        assertThat(response.items[0].workingDate).isEqualTo(LocalDate.of(2026, 5, 20))
        assertThat(response.items[0].workingType).isEqualTo(WorkingType.WORK.displayName)
        assertThat(response.items[1].id).isEqualTo(11L)
    }

    @Test
    @DisplayName("limit 이 Pageable 의 page size 로 전달된다")
    fun limit_propagatedAsPageable() {
        val employee = Employee(id = 1L, employeeCode = "EMP001", name = "테스트사원")
        every { employeeRepository.findById(1L) } returns Optional.of(employee)
        every {
            teamMemberScheduleRepository.findByEmployeeOrderByWorkingDateDescCreatedAtDesc(
                employee,
                any<Pageable>(),
            )
        } returns emptyList()

        service.getRecentHistory(1L, 5)

        verify {
            teamMemberScheduleRepository.findByEmployeeOrderByWorkingDateDescCreatedAtDesc(
                employee,
                match<Pageable> { it.pageSize == 5 && it.pageNumber == 0 },
            )
        }
    }

    @Test
    @DisplayName("존재하지 않는 employee — EmployeeNotFoundException")
    fun missingEmployee_throws() {
        every { employeeRepository.findById(999L) } returns Optional.empty()

        assertThatThrownBy { service.getRecentHistory(999L, 10) }
            .isInstanceOf(EmployeeNotFoundException::class.java)
    }

    @Test
    @DisplayName("빈 리스트 — items 비어있는 응답")
    fun emptyResult() {
        val employee = Employee(id = 1L, employeeCode = "EMP001", name = "테스트사원")
        every { employeeRepository.findById(1L) } returns Optional.of(employee)
        every {
            teamMemberScheduleRepository.findByEmployeeOrderByWorkingDateDescCreatedAtDesc(
                employee,
                any<Pageable>(),
            )
        } returns emptyList()

        val response = service.getRecentHistory(1L, 10)

        assertThat(response.items).isEmpty()
    }
}
