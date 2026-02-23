package com.otoki.internal.repository

import com.otoki.internal.entity.Schedule
import com.otoki.internal.entity.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

/**
 * ScheduleRepository 테스트
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class ScheduleRepositoryTest {

    @Autowired
    private lateinit var scheduleRepository: ScheduleRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    private val testEmployeeId = "a0B000000012345"

    @BeforeEach
    fun setUp() {
        scheduleRepository.deleteAll()
        testEntityManager.clear()
    }

    @Test
    @DisplayName("findByEmployeeIdAndWorkingDate - 해당 날짜 일정이 있으면 일정 목록을 반환한다")
    fun findByEmployeeIdAndWorkingDate_withSchedules() {
        // Given
        val today = LocalDate.now()
        val schedule1 = Schedule(
            employeeId = testEmployeeId,
            workingDate = today,
            workingType = "순회"
        )
        val schedule2 = Schedule(
            employeeId = testEmployeeId,
            workingDate = today,
            workingType = "격고"
        )
        testEntityManager.persistAndFlush(schedule1)
        testEntityManager.persistAndFlush(schedule2)
        testEntityManager.clear()

        // When
        val result = scheduleRepository.findByEmployeeIdAndWorkingDate(testEmployeeId, today)

        // Then
        assertThat(result).hasSize(2)
        assertThat(result.map { it.workingType }).containsExactlyInAnyOrder("순회", "격고")
    }

    @Test
    @DisplayName("findByEmployeeIdAndWorkingDate - 다른 날짜 일정만 있으면 빈 목록을 반환한다")
    fun findByEmployeeIdAndWorkingDate_differentDate() {
        // Given
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val schedule = Schedule(
            employeeId = testEmployeeId,
            workingDate = tomorrow,
            workingType = "순회"
        )
        testEntityManager.persistAndFlush(schedule)
        testEntityManager.clear()

        // When
        val result = scheduleRepository.findByEmployeeIdAndWorkingDate(testEmployeeId, today)

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("findByEmployeeIdAndWorkingDate - 다른 사원의 일정은 조회되지 않는다")
    fun findByEmployeeIdAndWorkingDate_differentEmployee() {
        // Given
        val today = LocalDate.now()
        val otherEmployeeId = "a0B000000099999"
        val schedule = Schedule(
            employeeId = otherEmployeeId,
            workingDate = today,
            workingType = "순회"
        )
        testEntityManager.persistAndFlush(schedule)
        testEntityManager.clear()

        // When
        val result = scheduleRepository.findByEmployeeIdAndWorkingDate(testEmployeeId, today)

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("findByEmployeeIdAndWorkingDate - 일정이 전혀 없으면 빈 목록을 반환한다")
    fun findByEmployeeIdAndWorkingDate_noSchedules() {
        // When
        val result = scheduleRepository.findByEmployeeIdAndWorkingDate(testEmployeeId, LocalDate.now())

        // Then
        assertThat(result).isEmpty()
    }
}
