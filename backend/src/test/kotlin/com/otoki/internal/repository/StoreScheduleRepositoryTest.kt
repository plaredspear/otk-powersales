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
