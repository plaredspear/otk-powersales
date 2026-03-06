package com.otoki.internal.schedule.repository

import com.otoki.internal.schedule.entity.Schedule
import com.otoki.internal.sap.entity.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import com.otoki.internal.common.config.QueryDslConfig
import java.time.LocalDate

/**
 * ScheduleRepository 테스트
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
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

    @Nested
    @DisplayName("findByWorkingDateAndEmployeeIdIn")
    inner class FindByWorkingDateAndEmployeeIdIn {

        @Test
        @DisplayName("팀 스케줄 조회 - 3명의 사원 스케줄 반환")
        fun findByWorkingDateAndEmployeeIdIn_threeEmployees() {
            // Given
            val today = LocalDate.now()
            val employeeId1 = "a0B000000011111"
            val employeeId2 = "a0B000000022222"
            val employeeId3 = "a0B000000033333"

            val schedule1 = Schedule(
                employeeId = employeeId1,
                workingDate = today,
                workingType = "순회"
            )
            val schedule2 = Schedule(
                employeeId = employeeId2,
                workingDate = today,
                workingType = "격고"
            )
            val schedule3 = Schedule(
                employeeId = employeeId3,
                workingDate = today,
                workingType = "휴가"
            )
            testEntityManager.persistAndFlush(schedule1)
            testEntityManager.persistAndFlush(schedule2)
            testEntityManager.persistAndFlush(schedule3)
            testEntityManager.clear()

            // When
            val result = scheduleRepository.findByWorkingDateAndEmployeeIdIn(
                today,
                listOf(employeeId1, employeeId2, employeeId3)
            )

            // Then
            assertThat(result).hasSize(3)
            assertThat(result.map { it.employeeId })
                .containsExactlyInAnyOrder(employeeId1, employeeId2, employeeId3)
        }

        @Test
        @DisplayName("빈 결과 - 일치하는 사원 없음")
        fun findByWorkingDateAndEmployeeIdIn_noMatchingEmployees() {
            // Given
            val today = LocalDate.now()
            val schedule = Schedule(
                employeeId = "a0B000000099999",
                workingDate = today,
                workingType = "순회"
            )
            testEntityManager.persistAndFlush(schedule)
            testEntityManager.clear()

            // When
            val result = scheduleRepository.findByWorkingDateAndEmployeeIdIn(
                today,
                listOf("a0B000000011111", "a0B000000022222")
            )

            // Then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("updateCommuteLogId - commuteLogId 업데이트")
    inner class UpdateCommuteLogIdTests {

        @Test
        @DisplayName("정상 업데이트 - sfid 일치 시 commuteLogId 변경")
        fun updateCommuteLogId_success() {
            // Given
            val schedule = Schedule(
                sfid = "SF001",
                employeeId = testEmployeeId,
                workingDate = LocalDate.now(),
                workingType = "순회"
            )
            testEntityManager.persistAndFlush(schedule)
            testEntityManager.clear()

            // When
            scheduleRepository.updateCommuteLogId("SF001", "OK")
            testEntityManager.clear()

            // Then
            val updated = scheduleRepository.findBySfid("SF001")
            assertThat(updated).isNotNull
            assertThat(updated!!.commuteLogId).isEqualTo("OK")
        }

        @Test
        @DisplayName("존재하지 않는 sfid - 에러 없이 0건 업데이트")
        fun updateCommuteLogId_nonExistentSfid() {
            // When & Then (에러 없이 실행)
            scheduleRepository.updateCommuteLogId("NONE", "OK")
        }
    }
}
