package com.otoki.internal.schedule.repository

import com.otoki.internal.schedule.entity.TeamMemberSchedule
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
 * TeamMemberScheduleRepository 테스트
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
class TeamMemberScheduleRepositoryTest {

    @Autowired
    private lateinit var teamMemberScheduleRepository: TeamMemberScheduleRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    private val testEmployeeNumber = "00000001"

    @BeforeEach
    fun setUp() {
        teamMemberScheduleRepository.deleteAll()
        testEntityManager.clear()
    }

    @Test
    @DisplayName("findByEmployeeNumberAndWorkingDate - 해당 날짜 일정이 있으면 일정 목록을 반환한다")
    fun findByEmployeeNumberAndWorkingDate_withSchedules() {
        // Given
        val today = LocalDate.now()
        val teamMemberSchedule1 = TeamMemberSchedule(
            employeeNumber = testEmployeeNumber,
            workingDate = today,
            workingType = "순회"
        )
        val teamMemberSchedule2 = TeamMemberSchedule(
            employeeNumber = testEmployeeNumber,
            workingDate = today,
            workingType = "격고"
        )
        testEntityManager.persistAndFlush(teamMemberSchedule1)
        testEntityManager.persistAndFlush(teamMemberSchedule2)
        testEntityManager.clear()

        // When
        val result = teamMemberScheduleRepository.findByEmployeeNumberAndWorkingDate(testEmployeeNumber, today)

        // Then
        assertThat(result).hasSize(2)
        assertThat(result.map { it.workingType }).containsExactlyInAnyOrder("순회", "격고")
    }

    @Test
    @DisplayName("findByEmployeeNumberAndWorkingDate - 다른 날짜 일정만 있으면 빈 목록을 반환한다")
    fun findByEmployeeNumberAndWorkingDate_differentDate() {
        // Given
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val teamMemberSchedule = TeamMemberSchedule(
            employeeNumber = testEmployeeNumber,
            workingDate = tomorrow,
            workingType = "순회"
        )
        testEntityManager.persistAndFlush(teamMemberSchedule)
        testEntityManager.clear()

        // When
        val result = teamMemberScheduleRepository.findByEmployeeNumberAndWorkingDate(testEmployeeNumber, today)

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("findByEmployeeNumberAndWorkingDate - 다른 사원의 일정은 조회되지 않는다")
    fun findByEmployeeNumberAndWorkingDate_differentEmployee() {
        // Given
        val today = LocalDate.now()
        val otherEmployeeNumber = "00000099"
        val teamMemberSchedule = TeamMemberSchedule(
            employeeNumber = otherEmployeeNumber,
            workingDate = today,
            workingType = "순회"
        )
        testEntityManager.persistAndFlush(teamMemberSchedule)
        testEntityManager.clear()

        // When
        val result = teamMemberScheduleRepository.findByEmployeeNumberAndWorkingDate(testEmployeeNumber, today)

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("findByEmployeeNumberAndWorkingDate - 일정이 전혀 없으면 빈 목록을 반환한다")
    fun findByEmployeeNumberAndWorkingDate_noSchedules() {
        // When
        val result = teamMemberScheduleRepository.findByEmployeeNumberAndWorkingDate(testEmployeeNumber, LocalDate.now())

        // Then
        assertThat(result).isEmpty()
    }

    @Nested
    @DisplayName("findByWorkingDateAndEmployeeNumberIn")
    inner class FindByWorkingDateAndEmployeeNumberIn {

        @Test
        @DisplayName("팀 스케줄 조회 - 3명의 사원 스케줄 반환")
        fun findByWorkingDateAndEmployeeNumberIn_threeEmployees() {
            // Given
            val today = LocalDate.now()
            val employeeNumber1 = "00000011"
            val employeeNumber2 = "00000022"
            val employeeNumber3 = "00000033"

            val teamMemberSchedule1 = TeamMemberSchedule(
                employeeNumber = employeeNumber1,
                workingDate = today,
                workingType = "순회"
            )
            val teamMemberSchedule2 = TeamMemberSchedule(
                employeeNumber = employeeNumber2,
                workingDate = today,
                workingType = "격고"
            )
            val teamMemberSchedule3 = TeamMemberSchedule(
                employeeNumber = employeeNumber3,
                workingDate = today,
                workingType = "휴가"
            )
            testEntityManager.persistAndFlush(teamMemberSchedule1)
            testEntityManager.persistAndFlush(teamMemberSchedule2)
            testEntityManager.persistAndFlush(teamMemberSchedule3)
            testEntityManager.clear()

            // When
            val result = teamMemberScheduleRepository.findByWorkingDateAndEmployeeNumberIn(
                today,
                listOf(employeeNumber1, employeeNumber2, employeeNumber3)
            )

            // Then
            assertThat(result).hasSize(3)
            assertThat(result.map { it.employeeNumber })
                .containsExactlyInAnyOrder(employeeNumber1, employeeNumber2, employeeNumber3)
        }

        @Test
        @DisplayName("빈 결과 - 일치하는 사원 없음")
        fun findByWorkingDateAndEmployeeNumberIn_noMatchingEmployees() {
            // Given
            val today = LocalDate.now()
            val teamMemberSchedule = TeamMemberSchedule(
                employeeNumber = "00000099",
                workingDate = today,
                workingType = "순회"
            )
            testEntityManager.persistAndFlush(teamMemberSchedule)
            testEntityManager.clear()

            // When
            val result = teamMemberScheduleRepository.findByWorkingDateAndEmployeeNumberIn(
                today,
                listOf("00000011", "00000022")
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
            val teamMemberSchedule = TeamMemberSchedule(
                sfid = "SF001",
                employeeNumber = testEmployeeNumber,
                workingDate = LocalDate.now(),
                workingType = "순회"
            )
            testEntityManager.persistAndFlush(teamMemberSchedule)
            testEntityManager.clear()

            // When
            teamMemberScheduleRepository.updateCommuteLogId("SF001", "OK")
            testEntityManager.clear()

            // Then
            val updated = teamMemberScheduleRepository.findById(teamMemberSchedule.id)
            assertThat(updated).isPresent
            assertThat(updated.get().commuteLogId).isEqualTo("OK")
        }

        @Test
        @DisplayName("존재하지 않는 sfid - 에러 없이 0건 업데이트")
        fun updateCommuteLogId_nonExistentSfid() {
            // When & Then (에러 없이 실행)
            teamMemberScheduleRepository.updateCommuteLogId("NONE", "OK")
        }
    }
}
