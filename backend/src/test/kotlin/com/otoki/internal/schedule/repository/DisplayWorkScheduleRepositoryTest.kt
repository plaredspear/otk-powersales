package com.otoki.internal.schedule.repository

import com.otoki.internal.schedule.entity.DisplayWorkSchedule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import com.otoki.internal.common.config.QueryDslConfig
import java.time.LocalDate

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
class DisplayWorkScheduleRepositoryTest {

    @Autowired
    private lateinit var displayWorkScheduleRepository: DisplayWorkScheduleRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    private val testEmployeeId = 12345L
    private val today = LocalDate.now()

    @BeforeEach
    fun setUp() {
        displayWorkScheduleRepository.deleteAll()
        testEntityManager.clear()
    }

    @Test
    @DisplayName("findByEmployeeIdAndStartDate - 해당 날짜 스케줄이 있으면 목록 반환")
    fun findByEmployeeIdAndStartDate_withSchedules() {
        // Given
        val schedule1 = createDisplayWorkSchedule(accountId = 1001)
        val schedule2 = createDisplayWorkSchedule(accountId = 1002)
        testEntityManager.persistAndFlush(schedule1)
        testEntityManager.persistAndFlush(schedule2)
        testEntityManager.clear()

        // When
        val result = displayWorkScheduleRepository.findByEmployeeIdAndStartDate(testEmployeeId, today)

        // Then
        assertThat(result).hasSize(2)
        assertThat(result.map { it.accountId }).containsExactlyInAnyOrder(1001, 1002)
    }

    @Test
    @DisplayName("findByEmployeeIdAndStartDate - 다른 날짜 스케줄만 있으면 빈 목록 반환")
    fun findByEmployeeIdAndStartDate_differentDate() {
        // Given
        val schedule = createDisplayWorkSchedule(accountId = 1001, startDate = today.plusDays(1))
        testEntityManager.persistAndFlush(schedule)
        testEntityManager.clear()

        // When
        val result = displayWorkScheduleRepository.findByEmployeeIdAndStartDate(testEmployeeId, today)

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("existsByEmployeeIdAndAccountIdAndStartDate - 스케줄 존재 시 true")
    fun existsByEmployeeIdAndAccountIdAndStartDate_exists() {
        // Given
        val schedule = createDisplayWorkSchedule(accountId = 1001)
        testEntityManager.persistAndFlush(schedule)
        testEntityManager.clear()

        // When
        val result = displayWorkScheduleRepository.existsByEmployeeIdAndAccountIdAndStartDate(testEmployeeId, 1001, today)

        // Then
        assertThat(result).isTrue()
    }

    @Test
    @DisplayName("existsByEmployeeIdAndAccountIdAndStartDate - 스케줄 미존재 시 false")
    fun existsByEmployeeIdAndAccountIdAndStartDate_notExists() {
        // When
        val result = displayWorkScheduleRepository.existsByEmployeeIdAndAccountIdAndStartDate(testEmployeeId, 9999, today)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    @DisplayName("findByEmployeeIdAndAccountIdAndStartDate - 스케줄 조회 성공")
    fun findByEmployeeIdAndAccountIdAndStartDate_found() {
        // Given
        val schedule = createDisplayWorkSchedule(accountId = 1001, typeOfWork1 = "진열")
        testEntityManager.persistAndFlush(schedule)
        testEntityManager.clear()

        // When
        val result = displayWorkScheduleRepository.findByEmployeeIdAndAccountIdAndStartDate(testEmployeeId, 1001, today)

        // Then
        assertThat(result).isNotNull
        assertThat(result!!.typeOfWork1).isEqualTo("진열")
    }

    @Test
    @DisplayName("findByEmployeeIdAndAccountIdAndStartDate - 스케줄 미존재 시 null")
    fun findByEmployeeIdAndAccountIdAndStartDate_notFound() {
        // When
        val result = displayWorkScheduleRepository.findByEmployeeIdAndAccountIdAndStartDate(testEmployeeId, 9999, today)

        // Then
        assertThat(result).isNull()
    }

    @Test
    @DisplayName("findByEmployeeIdAndStartDateBetween - 기간 내 스케줄 조회")
    fun findByEmployeeIdAndStartDateBetween_withinRange() {
        // Given
        val startDate = today
        val endDate = today.plusDays(6)

        val schedule1 = createDisplayWorkSchedule(accountId = 1001, startDate = today)
        val schedule2 = createDisplayWorkSchedule(accountId = 1002, startDate = today.plusDays(3))
        val schedule3 = createDisplayWorkSchedule(accountId = 1003, startDate = today.plusDays(6))
        testEntityManager.persistAndFlush(schedule1)
        testEntityManager.persistAndFlush(schedule2)
        testEntityManager.persistAndFlush(schedule3)
        testEntityManager.clear()

        // When
        val result = displayWorkScheduleRepository.findByEmployeeIdAndStartDateBetween(testEmployeeId, startDate, endDate)

        // Then
        assertThat(result).hasSize(3)
        assertThat(result.map { it.accountId }).containsExactlyInAnyOrder(1001, 1002, 1003)
    }

    @Test
    @DisplayName("findByEmployeeIdAndStartDateBetween - 기간 외 스케줄은 제외")
    fun findByEmployeeIdAndStartDateBetween_outsideRange() {
        // Given
        val startDate = today.plusDays(1)
        val endDate = today.plusDays(3)

        val schedule1 = createDisplayWorkSchedule(accountId = 1001, startDate = today)
        val schedule2 = createDisplayWorkSchedule(accountId = 1002, startDate = today.plusDays(2))
        val schedule3 = createDisplayWorkSchedule(accountId = 1003, startDate = today.plusDays(5))
        testEntityManager.persistAndFlush(schedule1)
        testEntityManager.persistAndFlush(schedule2)
        testEntityManager.persistAndFlush(schedule3)
        testEntityManager.clear()

        // When
        val result = displayWorkScheduleRepository.findByEmployeeIdAndStartDateBetween(testEmployeeId, startDate, endDate)

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].accountId).isEqualTo(1002)
    }

    @Test
    @DisplayName("findDistinctAccountIdsByEmployeeIdAndStartDateBetween - 월별 중복 제거 거래처 account 조회")
    fun findDistinctAccounts_removeDuplicates() {
        // Given
        val startDate = today
        val endDate = today.plusDays(10)

        // 같은 거래처에 여러 날짜 스케줄
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(accountId = 1001, startDate = today))
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(accountId = 1001, startDate = today.plusDays(3)))
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(accountId = 1001, startDate = today.plusDays(7)))
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(accountId = 1002, startDate = today.plusDays(2)))
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(accountId = 1002, startDate = today.plusDays(5)))
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(accountId = 1003, startDate = today.plusDays(9)))
        testEntityManager.clear()

        // When
        val result = displayWorkScheduleRepository.findDistinctAccountIdsByEmployeeIdAndStartDateBetween(testEmployeeId, startDate, endDate)

        // Then
        assertThat(result).hasSize(3)
        assertThat(result).containsExactlyInAnyOrder(1001, 1002, 1003)
    }

    @Test
    @DisplayName("findDistinctAccountIdsByEmployeeIdAndStartDateBetween - 스케줄 없는 사용자는 빈 리스트 반환")
    fun findDistinctAccounts_noSchedules() {
        // Given
        val startDate = today
        val endDate = today.plusDays(10)
        val otherEmployeeId = 99999L

        val schedule = createDisplayWorkSchedule(accountId = 1001, startDate = today)
        testEntityManager.persistAndFlush(schedule)
        testEntityManager.clear()

        // When
        val result = displayWorkScheduleRepository.findDistinctAccountIdsByEmployeeIdAndStartDateBetween(otherEmployeeId, startDate, endDate)

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("findByEmployeeIdAndStartDateBetween - 스케줄 없는 기간 조회 시 빈 리스트")
    fun findByEmployeeIdAndStartDateBetween_noSchedules() {
        // Given
        val startDate = today.plusMonths(1)
        val endDate = today.plusMonths(1).plusDays(10)

        val schedule = createDisplayWorkSchedule(accountId = 1001, startDate = today)
        testEntityManager.persistAndFlush(schedule)
        testEntityManager.clear()

        // When
        val result = displayWorkScheduleRepository.findByEmployeeIdAndStartDateBetween(testEmployeeId, startDate, endDate)

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("findDistinctStartDatesByEmployeeIdAndDateBetween - 기간 내 일정이 있는 날짜 목록 조회")
    fun findDistinctStartDates_success() {
        // Given
        val startDate = today
        val endDate = today.plusDays(10)

        testEntityManager.persistAndFlush(createDisplayWorkSchedule(accountId = 1001, startDate = today))
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(accountId = 1002, startDate = today))
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(accountId = 1003, startDate = today.plusDays(3)))
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(accountId = 1001, startDate = today.plusDays(7)))
        testEntityManager.clear()

        // When
        val result = displayWorkScheduleRepository.findDistinctStartDatesByEmployeeIdAndDateBetween(testEmployeeId, startDate, endDate)

        // Then
        assertThat(result).hasSize(3)
        assertThat(result).containsExactly(
            today,
            today.plusDays(3),
            today.plusDays(7)
        )
    }

    @Test
    @DisplayName("findDistinctStartDatesByEmployeeIdAndDateBetween - 중복 날짜 제거 확인")
    fun findDistinctStartDates_removeDuplicates() {
        // Given
        val startDate = today
        val endDate = today.plusDays(1)

        testEntityManager.persistAndFlush(createDisplayWorkSchedule(accountId = 1001, startDate = today))
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(accountId = 1002, startDate = today))
        testEntityManager.clear()

        // When
        val result = displayWorkScheduleRepository.findDistinctStartDatesByEmployeeIdAndDateBetween(testEmployeeId, startDate, endDate)

        // Then
        assertThat(result).hasSize(1)
        assertThat(result).containsExactly(today)
    }

    @Test
    @DisplayName("findDistinctStartDatesByEmployeeIdAndDateBetween - 날짜순 정렬 확인")
    fun findDistinctStartDates_orderedByDate() {
        // Given
        val startDate = today
        val endDate = today.plusDays(10)

        testEntityManager.persistAndFlush(createDisplayWorkSchedule(accountId = 1001, startDate = today.plusDays(7)))
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(accountId = 1002, startDate = today))
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(accountId = 1003, startDate = today.plusDays(3)))
        testEntityManager.clear()

        // When
        val result = displayWorkScheduleRepository.findDistinctStartDatesByEmployeeIdAndDateBetween(testEmployeeId, startDate, endDate)

        // Then
        assertThat(result).hasSize(3)
        assertThat(result).isSorted()
        assertThat(result).containsExactly(
            today,
            today.plusDays(3),
            today.plusDays(7)
        )
    }

    @Test
    @DisplayName("findDistinctStartDatesByEmployeeIdAndDateBetween - 스케줄 없으면 빈 리스트 반환")
    fun findDistinctStartDates_noSchedules() {
        // Given
        val startDate = today.plusMonths(1)
        val endDate = today.plusMonths(1).plusDays(10)

        // When
        val result = displayWorkScheduleRepository.findDistinctStartDatesByEmployeeIdAndDateBetween(testEmployeeId, startDate, endDate)

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("findDistinctStartDatesByEmployeeIdAndDateBetween - 다른 사용자의 일정은 제외")
    fun findDistinctStartDates_filterByEmployeeId() {
        // Given
        val otherEmployeeId = 99999L
        val startDate = today
        val endDate = today.plusDays(10)

        val schedule = createDisplayWorkSchedule(accountId = 1001, startDate = today)
        testEntityManager.persistAndFlush(schedule)
        testEntityManager.clear()

        // When
        val result = displayWorkScheduleRepository.findDistinctStartDatesByEmployeeIdAndDateBetween(otherEmployeeId, startDate, endDate)

        // Then
        assertThat(result).isEmpty()
    }

    // ========== Helpers ==========

    private fun createDisplayWorkSchedule(
        accountId: Int = 1001,
        typeOfWork1: String = "진열",
        startDate: LocalDate = today
    ): DisplayWorkSchedule {
        return DisplayWorkSchedule(
            employeeId = testEmployeeId,
            accountId = accountId,
            typeOfWork1 = typeOfWork1,
            startDate = startDate
        )
    }
}
