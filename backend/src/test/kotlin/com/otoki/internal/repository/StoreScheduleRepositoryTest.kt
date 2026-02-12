package com.otoki.internal.repository

import com.otoki.internal.entity.StoreSchedule
import com.otoki.internal.entity.User
import com.otoki.internal.entity.UserRole
import com.otoki.internal.entity.WorkerType
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

    private var testUserId: Long = 0
    private val today = LocalDate.now()

    @BeforeEach
    fun setUp() {
        storeScheduleRepository.deleteAll()
        testEntityManager.clear()

        val user = User(
            employeeId = "20030117",
            password = "encodedPassword",
            name = "테스트 사용자",
            department = "영업1팀",
            branchName = "부산1지점",
            role = UserRole.USER,
            workerType = WorkerType.PATROL
        )
        testUserId = testEntityManager.persistAndFlush(user).id
        testEntityManager.clear()
    }

    @Test
    @DisplayName("findByUserIdAndScheduleDate - 해당 날짜 스케줄이 있으면 목록 반환")
    fun findByUserIdAndScheduleDate_withSchedules() {
        // Given
        val schedule1 = createStoreSchedule(storeId = 101, storeName = "이마트 부산점")
        val schedule2 = createStoreSchedule(storeId = 102, storeName = "홈플러스 서면점")
        testEntityManager.persistAndFlush(schedule1)
        testEntityManager.persistAndFlush(schedule2)
        testEntityManager.clear()

        // When
        val result = storeScheduleRepository.findByUserIdAndScheduleDate(testUserId, today)

        // Then
        assertThat(result).hasSize(2)
        assertThat(result.map { it.storeName }).containsExactlyInAnyOrder("이마트 부산점", "홈플러스 서면점")
    }

    @Test
    @DisplayName("findByUserIdAndScheduleDate - 다른 날짜 스케줄만 있으면 빈 목록 반환")
    fun findByUserIdAndScheduleDate_differentDate() {
        // Given
        val schedule = createStoreSchedule(
            storeId = 101, storeName = "이마트 부산점",
            scheduleDate = today.plusDays(1)
        )
        testEntityManager.persistAndFlush(schedule)
        testEntityManager.clear()

        // When
        val result = storeScheduleRepository.findByUserIdAndScheduleDate(testUserId, today)

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("existsByUserIdAndStoreIdAndScheduleDate - 스케줄 존재 시 true")
    fun existsByUserIdAndStoreIdAndScheduleDate_exists() {
        // Given
        val schedule = createStoreSchedule(storeId = 101, storeName = "이마트 부산점")
        testEntityManager.persistAndFlush(schedule)
        testEntityManager.clear()

        // When
        val result = storeScheduleRepository.existsByUserIdAndStoreIdAndScheduleDate(testUserId, 101, today)

        // Then
        assertThat(result).isTrue()
    }

    @Test
    @DisplayName("existsByUserIdAndStoreIdAndScheduleDate - 스케줄 미존재 시 false")
    fun existsByUserIdAndStoreIdAndScheduleDate_notExists() {
        // When
        val result = storeScheduleRepository.existsByUserIdAndStoreIdAndScheduleDate(testUserId, 999, today)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    @DisplayName("findByUserIdAndStoreIdAndScheduleDate - 스케줄 조회 성공")
    fun findByUserIdAndStoreIdAndScheduleDate_found() {
        // Given
        val schedule = createStoreSchedule(storeId = 101, storeName = "이마트 부산점")
        testEntityManager.persistAndFlush(schedule)
        testEntityManager.clear()

        // When
        val result = storeScheduleRepository.findByUserIdAndStoreIdAndScheduleDate(testUserId, 101, today)

        // Then
        assertThat(result).isNotNull
        assertThat(result!!.storeName).isEqualTo("이마트 부산점")
    }

    @Test
    @DisplayName("findByUserIdAndStoreIdAndScheduleDate - 스케줄 미존재 시 null")
    fun findByUserIdAndStoreIdAndScheduleDate_notFound() {
        // When
        val result = storeScheduleRepository.findByUserIdAndStoreIdAndScheduleDate(testUserId, 999, today)

        // Then
        assertThat(result).isNull()
    }

    @Test
    @DisplayName("findByUserIdAndScheduleDateAndKeyword - 거래처명으로 검색")
    fun findByKeyword_storeName() {
        // Given
        val schedule1 = createStoreSchedule(storeId = 101, storeName = "이마트 부산점")
        val schedule2 = createStoreSchedule(storeId = 102, storeName = "홈플러스 서면점")
        testEntityManager.persistAndFlush(schedule1)
        testEntityManager.persistAndFlush(schedule2)
        testEntityManager.clear()

        // When
        val result = storeScheduleRepository.findByUserIdAndScheduleDateAndKeyword(testUserId, today, "이마트")

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].storeName).isEqualTo("이마트 부산점")
    }

    @Test
    @DisplayName("findByUserIdAndScheduleDateAndKeyword - 주소로 검색")
    fun findByKeyword_address() {
        // Given
        val schedule = createStoreSchedule(storeId = 101, storeName = "이마트 부산점", address = "부산시 해운대구")
        testEntityManager.persistAndFlush(schedule)
        testEntityManager.clear()

        // When
        val result = storeScheduleRepository.findByUserIdAndScheduleDateAndKeyword(testUserId, today, "해운대")

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].address).isEqualTo("부산시 해운대구")
    }

    @Test
    @DisplayName("findByUserIdAndScheduleDateAndKeyword - 거래처코드로 검색")
    fun findByKeyword_storeCode() {
        // Given
        val schedule = createStoreSchedule(storeId = 101, storeName = "이마트 부산점", storeCode = "ST-00101")
        testEntityManager.persistAndFlush(schedule)
        testEntityManager.clear()

        // When
        val result = storeScheduleRepository.findByUserIdAndScheduleDateAndKeyword(testUserId, today, "ST-001")

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].storeCode).isEqualTo("ST-00101")
    }

    @Test
    @DisplayName("findByUserIdAndScheduleDateAndKeyword - 매칭 없으면 빈 목록")
    fun findByKeyword_noMatch() {
        // Given
        val schedule = createStoreSchedule(storeId = 101, storeName = "이마트 부산점")
        testEntityManager.persistAndFlush(schedule)
        testEntityManager.clear()

        // When
        val result = storeScheduleRepository.findByUserIdAndScheduleDateAndKeyword(testUserId, today, "롯데마트")

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("findByUserIdAndScheduleDateBetween - 기간 내 스케줄 조회")
    fun findByScheduleDateBetween_withinRange() {
        // Given
        val startDate = today
        val endDate = today.plusDays(6)

        val schedule1 = createStoreSchedule(storeId = 101, storeName = "이마트 부산점", scheduleDate = today)
        val schedule2 = createStoreSchedule(storeId = 102, storeName = "홈플러스 서면점", scheduleDate = today.plusDays(3))
        val schedule3 = createStoreSchedule(storeId = 103, storeName = "롯데마트 해운대점", scheduleDate = today.plusDays(6))
        testEntityManager.persistAndFlush(schedule1)
        testEntityManager.persistAndFlush(schedule2)
        testEntityManager.persistAndFlush(schedule3)
        testEntityManager.clear()

        // When
        val result = storeScheduleRepository.findByUserIdAndScheduleDateBetween(testUserId, startDate, endDate)

        // Then
        assertThat(result).hasSize(3)
        assertThat(result.map { it.storeName }).containsExactlyInAnyOrder(
            "이마트 부산점",
            "홈플러스 서면점",
            "롯데마트 해운대점"
        )
    }

    @Test
    @DisplayName("findByUserIdAndScheduleDateBetween - 기간 외 스케줄은 제외")
    fun findByScheduleDateBetween_outsideRange() {
        // Given
        val startDate = today.plusDays(1)
        val endDate = today.plusDays(3)

        val schedule1 = createStoreSchedule(storeId = 101, storeName = "이마트 부산점", scheduleDate = today)
        val schedule2 = createStoreSchedule(storeId = 102, storeName = "홈플러스 서면점", scheduleDate = today.plusDays(2))
        val schedule3 = createStoreSchedule(storeId = 103, storeName = "롯데마트 해운대점", scheduleDate = today.plusDays(5))
        testEntityManager.persistAndFlush(schedule1)
        testEntityManager.persistAndFlush(schedule2)
        testEntityManager.persistAndFlush(schedule3)
        testEntityManager.clear()

        // When
        val result = storeScheduleRepository.findByUserIdAndScheduleDateBetween(testUserId, startDate, endDate)

        // Then
        assertThat(result).hasSize(1)
        assertThat(result[0].storeName).isEqualTo("홈플러스 서면점")
    }

    @Test
    @DisplayName("findDistinctStoreIdsByUserIdAndScheduleDateBetween - 월별 중복 제거 거래처 ID 조회")
    fun findDistinctStoreIds_removeDuplicates() {
        // Given
        val startDate = today
        val endDate = today.plusDays(10)

        // 같은 거래처에 여러 날짜 스케줄
        val schedule1 = createStoreSchedule(storeId = 101, storeName = "이마트 부산점", scheduleDate = today)
        val schedule2 = createStoreSchedule(storeId = 101, storeName = "이마트 부산점", scheduleDate = today.plusDays(3))
        val schedule3 = createStoreSchedule(storeId = 101, storeName = "이마트 부산점", scheduleDate = today.plusDays(7))
        val schedule4 = createStoreSchedule(storeId = 102, storeName = "홈플러스 서면점", scheduleDate = today.plusDays(2))
        val schedule5 = createStoreSchedule(storeId = 102, storeName = "홈플러스 서면점", scheduleDate = today.plusDays(5))
        val schedule6 = createStoreSchedule(storeId = 103, storeName = "롯데마트 해운대점", scheduleDate = today.plusDays(9))

        testEntityManager.persistAndFlush(schedule1)
        testEntityManager.persistAndFlush(schedule2)
        testEntityManager.persistAndFlush(schedule3)
        testEntityManager.persistAndFlush(schedule4)
        testEntityManager.persistAndFlush(schedule5)
        testEntityManager.persistAndFlush(schedule6)
        testEntityManager.clear()

        // When
        val result = storeScheduleRepository.findDistinctStoreIdsByUserIdAndScheduleDateBetween(testUserId, startDate, endDate)

        // Then
        assertThat(result).hasSize(3)
        assertThat(result).containsExactlyInAnyOrder(101L, 102L, 103L)
    }

    @Test
    @DisplayName("findDistinctStoreIdsByUserIdAndScheduleDateBetween - 스케줄 없는 사용자는 빈 리스트 반환")
    fun findDistinctStoreIds_noSchedules() {
        // Given
        val startDate = today
        val endDate = today.plusDays(10)
        val otherUserId = 999L

        val schedule = createStoreSchedule(storeId = 101, storeName = "이마트 부산점", scheduleDate = today)
        testEntityManager.persistAndFlush(schedule)
        testEntityManager.clear()

        // When
        val result = storeScheduleRepository.findDistinctStoreIdsByUserIdAndScheduleDateBetween(otherUserId, startDate, endDate)

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("findByUserIdAndScheduleDateBetween - 스케줄 없는 기간 조회 시 빈 리스트")
    fun findByScheduleDateBetween_noSchedules() {
        // Given
        val startDate = today.plusMonths(1)
        val endDate = today.plusMonths(1).plusDays(10)

        val schedule = createStoreSchedule(storeId = 101, storeName = "이마트 부산점", scheduleDate = today)
        testEntityManager.persistAndFlush(schedule)
        testEntityManager.clear()

        // When
        val result = storeScheduleRepository.findByUserIdAndScheduleDateBetween(testUserId, startDate, endDate)

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("findDistinctScheduleDatesByUserIdAndDateBetween - 기간 내 일정이 있는 날짜 목록 조회")
    fun findDistinctScheduleDates_success() {
        // Given
        val startDate = today
        val endDate = today.plusDays(10)

        // 같은 날짜에 여러 거래처 스케줄
        val schedule1 = createStoreSchedule(storeId = 101, storeName = "이마트 부산점", scheduleDate = today)
        val schedule2 = createStoreSchedule(storeId = 102, storeName = "홈플러스 서면점", scheduleDate = today)
        val schedule3 = createStoreSchedule(storeId = 103, storeName = "롯데마트 해운대점", scheduleDate = today.plusDays(3))
        val schedule4 = createStoreSchedule(storeId = 101, storeName = "이마트 부산점", scheduleDate = today.plusDays(7))

        testEntityManager.persistAndFlush(schedule1)
        testEntityManager.persistAndFlush(schedule2)
        testEntityManager.persistAndFlush(schedule3)
        testEntityManager.persistAndFlush(schedule4)
        testEntityManager.clear()

        // When
        val result = storeScheduleRepository.findDistinctScheduleDatesByUserIdAndDateBetween(testUserId, startDate, endDate)

        // Then
        assertThat(result).hasSize(3)
        assertThat(result).containsExactly(
            today,
            today.plusDays(3),
            today.plusDays(7)
        )
    }

    @Test
    @DisplayName("findDistinctScheduleDatesByUserIdAndDateBetween - 중복 날짜 제거 확인")
    fun findDistinctScheduleDates_removeDuplicates() {
        // Given
        val startDate = today
        val endDate = today.plusDays(1)

        // 같은 날짜에 2개 거래처 스케줄
        val schedule1 = createStoreSchedule(storeId = 101, storeName = "이마트 부산점", scheduleDate = today)
        val schedule2 = createStoreSchedule(storeId = 102, storeName = "홈플러스 서면점", scheduleDate = today)

        testEntityManager.persistAndFlush(schedule1)
        testEntityManager.persistAndFlush(schedule2)
        testEntityManager.clear()

        // When
        val result = storeScheduleRepository.findDistinctScheduleDatesByUserIdAndDateBetween(testUserId, startDate, endDate)

        // Then
        assertThat(result).hasSize(1)
        assertThat(result).containsExactly(today)
    }

    @Test
    @DisplayName("findDistinctScheduleDatesByUserIdAndDateBetween - 날짜순 정렬 확인")
    fun findDistinctScheduleDates_orderedByDate() {
        // Given
        val startDate = today
        val endDate = today.plusDays(10)

        // 순서와 상관없이 삽입
        val schedule1 = createStoreSchedule(storeId = 101, storeName = "이마트 부산점", scheduleDate = today.plusDays(7))
        val schedule2 = createStoreSchedule(storeId = 102, storeName = "홈플러스 서면점", scheduleDate = today)
        val schedule3 = createStoreSchedule(storeId = 103, storeName = "롯데마트 해운대점", scheduleDate = today.plusDays(3))

        testEntityManager.persistAndFlush(schedule1)
        testEntityManager.persistAndFlush(schedule2)
        testEntityManager.persistAndFlush(schedule3)
        testEntityManager.clear()

        // When
        val result = storeScheduleRepository.findDistinctScheduleDatesByUserIdAndDateBetween(testUserId, startDate, endDate)

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
    @DisplayName("findDistinctScheduleDatesByUserIdAndDateBetween - 스케줄 없으면 빈 리스트 반환")
    fun findDistinctScheduleDates_noSchedules() {
        // Given
        val startDate = today.plusMonths(1)
        val endDate = today.plusMonths(1).plusDays(10)

        // When
        val result = storeScheduleRepository.findDistinctScheduleDatesByUserIdAndDateBetween(testUserId, startDate, endDate)

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("findDistinctScheduleDatesByUserIdAndDateBetween - 다른 사용자의 일정은 제외")
    fun findDistinctScheduleDates_filterByUserId() {
        // Given
        val otherUserId = 999L
        val startDate = today
        val endDate = today.plusDays(10)

        val schedule = createStoreSchedule(storeId = 101, storeName = "이마트 부산점", scheduleDate = today)
        testEntityManager.persistAndFlush(schedule)
        testEntityManager.clear()

        // When
        val result = storeScheduleRepository.findDistinctScheduleDatesByUserIdAndDateBetween(otherUserId, startDate, endDate)

        // Then
        assertThat(result).isEmpty()
    }

    // ========== Helpers ==========

    private fun createStoreSchedule(
        storeId: Long = 101,
        storeName: String = "테스트 거래처",
        storeCode: String = "ST-${String.format("%05d", storeId)}",
        address: String = "부산시 테스트구",
        scheduleDate: LocalDate = today
    ): StoreSchedule {
        return StoreSchedule(
            userId = testUserId,
            storeId = storeId,
            storeName = storeName,
            storeCode = storeCode,
            workCategory = "진열",
            address = address,
            scheduleDate = scheduleDate
        )
    }
}
