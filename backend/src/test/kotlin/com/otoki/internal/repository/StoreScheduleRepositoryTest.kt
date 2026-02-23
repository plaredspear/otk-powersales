package com.otoki.internal.repository

import com.otoki.internal.entity.StoreSchedule
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

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class StoreScheduleRepositoryTest {

    @Autowired
    private lateinit var storeScheduleRepository: StoreScheduleRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    private val testFullName = "a0B000000012345"
    private val today = LocalDate.now()

    @BeforeEach
    fun setUp() {
        storeScheduleRepository.deleteAll()
        testEntityManager.clear()
    }

    @Test
    @DisplayName("findByFullNameAndStartDate - 해당 날짜 스케줄이 있으면 목록 반환")
    fun findByFullNameAndStartDate_withSchedules() {
        // Given
        val schedule1 = createStoreSchedule(account = "ACC001")
        val schedule2 = createStoreSchedule(account = "ACC002")
        testEntityManager.persistAndFlush(schedule1)
        testEntityManager.persistAndFlush(schedule2)
        testEntityManager.clear()

        // When
        val result = storeScheduleRepository.findByFullNameAndStartDate(testFullName, today)

        // Then
        assertThat(result).hasSize(2)
        assertThat(result.map { it.account }).containsExactlyInAnyOrder("ACC001", "ACC002")
    }

    @Test
    @DisplayName("findByFullNameAndStartDate - 다른 날짜 스케줄만 있으면 빈 목록 반환")
    fun findByFullNameAndStartDate_differentDate() {
        // Given
        val schedule = createStoreSchedule(account = "ACC001", startDate = today.plusDays(1))
        testEntityManager.persistAndFlush(schedule)
        testEntityManager.clear()

        // When
        val result = storeScheduleRepository.findByFullNameAndStartDate(testFullName, today)

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("existsByFullNameAndAccountAndStartDate - 스케줄 존재 시 true")
    fun existsByFullNameAndAccountAndStartDate_exists() {
        // Given
        val schedule = createStoreSchedule(account = "ACC001")
        testEntityManager.persistAndFlush(schedule)
        testEntityManager.clear()

        // When
        val result = storeScheduleRepository.existsByFullNameAndAccountAndStartDate(testFullName, "ACC001", today)

        // Then
        assertThat(result).isTrue()
    }

    @Test
    @DisplayName("existsByFullNameAndAccountAndStartDate - 스케줄 미존재 시 false")
    fun existsByFullNameAndAccountAndStartDate_notExists() {
        // When
        val result = storeScheduleRepository.existsByFullNameAndAccountAndStartDate(testFullName, "ACC999", today)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    @DisplayName("findByFullNameAndAccountAndStartDate - 스케줄 조회 성공")
    fun findByFullNameAndAccountAndStartDate_found() {
        // Given
        val schedule = createStoreSchedule(account = "ACC001", typeOfWork1 = "진열")
        testEntityManager.persistAndFlush(schedule)
        testEntityManager.clear()

        // When
        val result = storeScheduleRepository.findByFullNameAndAccountAndStartDate(testFullName, "ACC001", today)

        // Then
        assertThat(result).isNotNull
        assertThat(result!!.typeOfWork1).isEqualTo("진열")
    }

    @Test
    @DisplayName("findByFullNameAndAccountAndStartDate - 스케줄 미존재 시 null")
    fun findByFullNameAndAccountAndStartDate_notFound() {
        // When
        val result = storeScheduleRepository.findByFullNameAndAccountAndStartDate(testFullName, "ACC999", today)

        // Then
        assertThat(result).isNull()
    }

    @Test
    @DisplayName("findByFullNameAndStartDateBetween - 기간 내 스케줄 조회")
    fun findByFullNameAndStartDateBetween_withinRange() {
        // Given
        val startDate = today
        val endDate = today.plusDays(6)

        val schedule1 = createStoreSchedule(account = "ACC001", startDate = today)
        val schedule2 = createStoreSchedule(account = "ACC002", startDate = today.plusDays(3))
        val schedule3 = createStoreSchedule(account = "ACC003", startDate = today.plusDays(6))
        testEntityManager.persistAndFlush(schedule1)
        testEntityManager.persistAndFlush(schedule2)
        testEntityManager.persistAndFlush(schedule3)
        testEntityManager.clear()

        // When
        val result = storeScheduleRepository.findByFullNameAndStartDateBetween(testFullName, startDate, endDate)

        // Then
        assertThat(result).hasSize(3)
        assertThat(result.map { it.account }).containsExactlyInAnyOrder("ACC001", "ACC002", "ACC003")
    }

    @Test
    @DisplayName("findByFullNameAndStartDateBetween - 기간 외 스케줄은 제외")
    fun findByFullNameAndStartDateBetween_outsideRange() {
        // Given
        val startDate = today.plusDays(1)
        val endDate = today.plusDays(3)

        val schedule1 = createStoreSchedule(account = "ACC001", startDate = today)
        val schedule2 = createStoreSchedule(account = "ACC002", startDate = today.plusDays(2))
        val schedule3 = createStoreSchedule(account = "ACC003", startDate = today.plusDays(5))
        testEntityManager.persistAndFlush(schedule1)
        testEntityManager.persistAndFlush(schedule2)
        testEntityManager.persistAndFlush(schedule3)
        testEntityManager.clear()

        // When
        val result = storeScheduleRepository.findByFullNameAndStartDateBetween(testFullName, startDate, endDate)

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].account).isEqualTo("ACC002")
    }

    @Test
    @DisplayName("findDistinctAccountsByFullNameAndStartDateBetween - 월별 중복 제거 거래처 account 조회")
    fun findDistinctAccounts_removeDuplicates() {
        // Given
        val startDate = today
        val endDate = today.plusDays(10)

        // 같은 거래처에 여러 날짜 스케줄
        testEntityManager.persistAndFlush(createStoreSchedule(account = "ACC001", startDate = today))
        testEntityManager.persistAndFlush(createStoreSchedule(account = "ACC001", startDate = today.plusDays(3)))
        testEntityManager.persistAndFlush(createStoreSchedule(account = "ACC001", startDate = today.plusDays(7)))
        testEntityManager.persistAndFlush(createStoreSchedule(account = "ACC002", startDate = today.plusDays(2)))
        testEntityManager.persistAndFlush(createStoreSchedule(account = "ACC002", startDate = today.plusDays(5)))
        testEntityManager.persistAndFlush(createStoreSchedule(account = "ACC003", startDate = today.plusDays(9)))
        testEntityManager.clear()

        // When
        val result = storeScheduleRepository.findDistinctAccountsByFullNameAndStartDateBetween(testFullName, startDate, endDate)

        // Then
        assertThat(result).hasSize(3)
        assertThat(result).containsExactlyInAnyOrder("ACC001", "ACC002", "ACC003")
    }

    @Test
    @DisplayName("findDistinctAccountsByFullNameAndStartDateBetween - 스케줄 없는 사용자는 빈 리스트 반환")
    fun findDistinctAccounts_noSchedules() {
        // Given
        val startDate = today
        val endDate = today.plusDays(10)
        val otherFullName = "a0B000000099999"

        val schedule = createStoreSchedule(account = "ACC001", startDate = today)
        testEntityManager.persistAndFlush(schedule)
        testEntityManager.clear()

        // When
        val result = storeScheduleRepository.findDistinctAccountsByFullNameAndStartDateBetween(otherFullName, startDate, endDate)

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("findByFullNameAndStartDateBetween - 스케줄 없는 기간 조회 시 빈 리스트")
    fun findByFullNameAndStartDateBetween_noSchedules() {
        // Given
        val startDate = today.plusMonths(1)
        val endDate = today.plusMonths(1).plusDays(10)

        val schedule = createStoreSchedule(account = "ACC001", startDate = today)
        testEntityManager.persistAndFlush(schedule)
        testEntityManager.clear()

        // When
        val result = storeScheduleRepository.findByFullNameAndStartDateBetween(testFullName, startDate, endDate)

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("findDistinctStartDatesByFullNameAndDateBetween - 기간 내 일정이 있는 날짜 목록 조회")
    fun findDistinctStartDates_success() {
        // Given
        val startDate = today
        val endDate = today.plusDays(10)

        testEntityManager.persistAndFlush(createStoreSchedule(account = "ACC001", startDate = today))
        testEntityManager.persistAndFlush(createStoreSchedule(account = "ACC002", startDate = today))
        testEntityManager.persistAndFlush(createStoreSchedule(account = "ACC003", startDate = today.plusDays(3)))
        testEntityManager.persistAndFlush(createStoreSchedule(account = "ACC001", startDate = today.plusDays(7)))
        testEntityManager.clear()

        // When
        val result = storeScheduleRepository.findDistinctStartDatesByFullNameAndDateBetween(testFullName, startDate, endDate)

        // Then
        assertThat(result).hasSize(3)
        assertThat(result).containsExactly(
            today,
            today.plusDays(3),
            today.plusDays(7)
        )
    }

    @Test
    @DisplayName("findDistinctStartDatesByFullNameAndDateBetween - 중복 날짜 제거 확인")
    fun findDistinctStartDates_removeDuplicates() {
        // Given
        val startDate = today
        val endDate = today.plusDays(1)

        testEntityManager.persistAndFlush(createStoreSchedule(account = "ACC001", startDate = today))
        testEntityManager.persistAndFlush(createStoreSchedule(account = "ACC002", startDate = today))
        testEntityManager.clear()

        // When
        val result = storeScheduleRepository.findDistinctStartDatesByFullNameAndDateBetween(testFullName, startDate, endDate)

        // Then
        assertThat(result).hasSize(1)
        assertThat(result).containsExactly(today)
    }

    @Test
    @DisplayName("findDistinctStartDatesByFullNameAndDateBetween - 날짜순 정렬 확인")
    fun findDistinctStartDates_orderedByDate() {
        // Given
        val startDate = today
        val endDate = today.plusDays(10)

        testEntityManager.persistAndFlush(createStoreSchedule(account = "ACC001", startDate = today.plusDays(7)))
        testEntityManager.persistAndFlush(createStoreSchedule(account = "ACC002", startDate = today))
        testEntityManager.persistAndFlush(createStoreSchedule(account = "ACC003", startDate = today.plusDays(3)))
        testEntityManager.clear()

        // When
        val result = storeScheduleRepository.findDistinctStartDatesByFullNameAndDateBetween(testFullName, startDate, endDate)

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
    @DisplayName("findDistinctStartDatesByFullNameAndDateBetween - 스케줄 없으면 빈 리스트 반환")
    fun findDistinctStartDates_noSchedules() {
        // Given
        val startDate = today.plusMonths(1)
        val endDate = today.plusMonths(1).plusDays(10)

        // When
        val result = storeScheduleRepository.findDistinctStartDatesByFullNameAndDateBetween(testFullName, startDate, endDate)

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("findDistinctStartDatesByFullNameAndDateBetween - 다른 사용자의 일정은 제외")
    fun findDistinctStartDates_filterByFullName() {
        // Given
        val otherFullName = "a0B000000099999"
        val startDate = today
        val endDate = today.plusDays(10)

        val schedule = createStoreSchedule(account = "ACC001", startDate = today)
        testEntityManager.persistAndFlush(schedule)
        testEntityManager.clear()

        // When
        val result = storeScheduleRepository.findDistinctStartDatesByFullNameAndDateBetween(otherFullName, startDate, endDate)

        // Then
        assertThat(result).isEmpty()
    }

    // ========== Helpers ==========

    private fun createStoreSchedule(
        account: String = "ACC001",
        typeOfWork1: String = "진열",
        startDate: LocalDate = today
    ): StoreSchedule {
        return StoreSchedule(
            fullName = testFullName,
            account = account,
            typeOfWork1 = typeOfWork1,
            startDate = startDate
        )
    }
}
