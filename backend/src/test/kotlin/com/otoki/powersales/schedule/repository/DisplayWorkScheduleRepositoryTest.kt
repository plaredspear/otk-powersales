package com.otoki.powersales.schedule.repository

import com.otoki.powersales.sap.entity.Account
import com.otoki.powersales.sap.entity.Employee
import com.otoki.powersales.schedule.entity.DisplayWorkSchedule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import com.otoki.powersales.common.config.QueryDslConfig
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

    private lateinit var testEmployee: Employee
    private lateinit var testAccount1: Account
    private lateinit var testAccount2: Account
    private lateinit var testAccount3: Account
    private val today = LocalDate.now()

    @BeforeEach
    fun setUp() {
        displayWorkScheduleRepository.deleteAll()
        testEntityManager.clear()

        testEmployee = testEntityManager.persistAndFlush(Employee(employeeCode = "20030117", name = "테스트사원"))
        testAccount1 = testEntityManager.persistAndFlush(Account(externalKey = "ACC001", name = "거래처1"))
        testAccount2 = testEntityManager.persistAndFlush(Account(externalKey = "ACC002", name = "거래처2"))
        testAccount3 = testEntityManager.persistAndFlush(Account(externalKey = "ACC003", name = "거래처3"))
        testEntityManager.clear()
    }

    @Test
    @DisplayName("findByEmployeeAndStartDate - 해당 날짜 스케줄이 있으면 목록 반환")
    fun findByEmployeeAndStartDate_withSchedules() {
        // Given
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount1))
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount2))
        testEntityManager.clear()

        // When
        val result = displayWorkScheduleRepository.findByEmployeeAndStartDate(testEmployee.id, today)

        // Then
        assertThat(result).hasSize(2)
        assertThat(result.map { it.account?.id }).containsExactlyInAnyOrder(testAccount1.id, testAccount2.id)
    }

    @Test
    @DisplayName("findByEmployeeAndStartDate - 다른 날짜 스케줄만 있으면 빈 목록 반환")
    fun findByEmployeeAndStartDate_differentDate() {
        // Given
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount1, startDate = today.plusDays(1)))
        testEntityManager.clear()

        // When
        val result = displayWorkScheduleRepository.findByEmployeeAndStartDate(testEmployee.id, today)

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("findByEmployeeAndAccountAndStartDate - 스케줄 조회 성공")
    fun findByEmployeeAndAccountAndStartDate_found() {
        // Given
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount1, typeOfWork1 = "진열"))
        testEntityManager.clear()

        // When
        val result = displayWorkScheduleRepository.findByEmployeeAndAccountAndStartDate(testEmployee.id, testAccount1.id, today)

        // Then
        assertThat(result).isNotNull
        assertThat(result!!.typeOfWork1).isEqualTo("진열")
    }

    @Test
    @DisplayName("findByEmployeeAndAccountAndStartDate - 스케줄 미존재 시 null")
    fun findByEmployeeAndAccountAndStartDate_notFound() {
        // When
        val result = displayWorkScheduleRepository.findByEmployeeAndAccountAndStartDate(testEmployee.id, 9999, today)

        // Then
        assertThat(result).isNull()
    }

    @Test
    @DisplayName("findByEmployeeAndStartDateBetween - 기간 내 스케줄 조회")
    fun findByEmployeeAndStartDateBetween_withinRange() {
        // Given
        val startDate = today
        val endDate = today.plusDays(6)

        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount1, startDate = today))
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount2, startDate = today.plusDays(3)))
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount3, startDate = today.plusDays(6)))
        testEntityManager.clear()

        // When
        val result = displayWorkScheduleRepository.findByEmployeeAndStartDateBetween(testEmployee.id, startDate, endDate)

        // Then
        assertThat(result).hasSize(3)
        assertThat(result.map { it.account?.id }).containsExactlyInAnyOrder(testAccount1.id, testAccount2.id, testAccount3.id)
    }

    @Test
    @DisplayName("findByEmployeeAndStartDateBetween - 기간 외 스케줄은 제외")
    fun findByEmployeeAndStartDateBetween_outsideRange() {
        // Given
        val startDate = today.plusDays(1)
        val endDate = today.plusDays(3)

        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount1, startDate = today))
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount2, startDate = today.plusDays(2)))
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount3, startDate = today.plusDays(5)))
        testEntityManager.clear()

        // When
        val result = displayWorkScheduleRepository.findByEmployeeAndStartDateBetween(testEmployee.id, startDate, endDate)

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].account?.id).isEqualTo(testAccount2.id)
    }

    @Test
    @DisplayName("findDistinctAccountIdsByEmployeeIdAndStartDateBetween - 월별 중복 제거 거래처 조회")
    fun findDistinctAccounts_removeDuplicates() {
        // Given
        val startDate = today
        val endDate = today.plusDays(10)

        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount1, startDate = today))
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount1, startDate = today.plusDays(3)))
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount1, startDate = today.plusDays(7)))
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount2, startDate = today.plusDays(2)))
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount2, startDate = today.plusDays(5)))
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount3, startDate = today.plusDays(9)))
        testEntityManager.clear()

        // When
        val result = displayWorkScheduleRepository.findDistinctAccountIdsByEmployeeIdAndStartDateBetween(testEmployee.id, startDate, endDate)

        // Then
        assertThat(result).hasSize(3)
        assertThat(result).containsExactlyInAnyOrder(testAccount1.id, testAccount2.id, testAccount3.id)
    }

    @Test
    @DisplayName("findDistinctAccountIdsByEmployeeIdAndStartDateBetween - 스케줄 없는 사용자는 빈 리스트 반환")
    fun findDistinctAccounts_noSchedules() {
        // Given
        val startDate = today
        val endDate = today.plusDays(10)
        val otherEmployeeId = 99999L

        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount1, startDate = today))
        testEntityManager.clear()

        // When
        val result = displayWorkScheduleRepository.findDistinctAccountIdsByEmployeeIdAndStartDateBetween(otherEmployeeId, startDate, endDate)

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("findByEmployeeAndStartDateBetween - 스케줄 없는 기간 조회 시 빈 리스트")
    fun findByEmployeeAndStartDateBetween_noSchedules() {
        // Given
        val startDate = today.plusMonths(1)
        val endDate = today.plusMonths(1).plusDays(10)

        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount1, startDate = today))
        testEntityManager.clear()

        // When
        val result = displayWorkScheduleRepository.findByEmployeeAndStartDateBetween(testEmployee.id, startDate, endDate)

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("findDistinctStartDatesByEmployeeIdAndDateBetween - 기간 내 일정이 있는 날짜 목록 조회")
    fun findDistinctStartDates_success() {
        // Given
        val startDate = today
        val endDate = today.plusDays(10)

        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount1, startDate = today))
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount2, startDate = today))
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount3, startDate = today.plusDays(3)))
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount1, startDate = today.plusDays(7)))
        testEntityManager.clear()

        // When
        val result = displayWorkScheduleRepository.findDistinctStartDatesByEmployeeIdAndDateBetween(testEmployee.id, startDate, endDate)

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

        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount1, startDate = today))
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount2, startDate = today))
        testEntityManager.clear()

        // When
        val result = displayWorkScheduleRepository.findDistinctStartDatesByEmployeeIdAndDateBetween(testEmployee.id, startDate, endDate)

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

        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount1, startDate = today.plusDays(7)))
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount2, startDate = today))
        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount3, startDate = today.plusDays(3)))
        testEntityManager.clear()

        // When
        val result = displayWorkScheduleRepository.findDistinctStartDatesByEmployeeIdAndDateBetween(testEmployee.id, startDate, endDate)

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
        val result = displayWorkScheduleRepository.findDistinctStartDatesByEmployeeIdAndDateBetween(testEmployee.id, startDate, endDate)

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

        testEntityManager.persistAndFlush(createDisplayWorkSchedule(account = testAccount1, startDate = today))
        testEntityManager.clear()

        // When
        val result = displayWorkScheduleRepository.findDistinctStartDatesByEmployeeIdAndDateBetween(otherEmployeeId, startDate, endDate)

        // Then
        assertThat(result).isEmpty()
    }

    // ========== Helpers ==========

    private fun createDisplayWorkSchedule(
        account: Account = testAccount1,
        typeOfWork1: String = "진열",
        startDate: LocalDate = today
    ): DisplayWorkSchedule {
        return DisplayWorkSchedule(
            employee = testEmployee,
            account = account,
            typeOfWork1 = typeOfWork1,
            startDate = startDate
        )
    }
}
