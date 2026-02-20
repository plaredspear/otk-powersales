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
import java.time.LocalTime

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

    private var testUserId: Long = 0

    @BeforeEach
    fun setUp() {
        scheduleRepository.deleteAll()
        testEntityManager.clear()

        // 테스트 사용자 생성
        val user = User(
            employeeId = "20030117",
            password = "encodedPassword",
            name = "최금주",
            orgName = "부산1지점"
        )
        testUserId = testEntityManager.persistAndFlush(user).id
        testEntityManager.clear()
    }

    @Test
    @DisplayName("findByUserIdAndScheduleDate - 해당 날짜 일정이 있으면 일정 목록을 반환한다")
    fun findByUserIdAndScheduleDate_withSchedules() {
        // Given
        val today = LocalDate.now()
        val schedule1 = Schedule(
            userId = testUserId,
            storeName = "이마트 부산점",
            scheduleDate = today,
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(12, 0),
            type = "순회"
        )
        val schedule2 = Schedule(
            userId = testUserId,
            storeName = "홈플러스 해운대점",
            scheduleDate = today,
            startTime = LocalTime.of(14, 0),
            endTime = LocalTime.of(17, 0),
            type = "격고"
        )
        testEntityManager.persistAndFlush(schedule1)
        testEntityManager.persistAndFlush(schedule2)
        testEntityManager.clear()

        // When
        val result = scheduleRepository.findByUserIdAndScheduleDate(testUserId, today)

        // Then
        assertThat(result).hasSize(2)
        assertThat(result.map { it.storeName }).containsExactlyInAnyOrder("이마트 부산점", "홈플러스 해운대점")
        assertThat(result.map { it.type }).containsExactlyInAnyOrder("순회", "격고")
    }

    @Test
    @DisplayName("findByUserIdAndScheduleDate - 다른 날짜 일정만 있으면 빈 목록을 반환한다")
    fun findByUserIdAndScheduleDate_differentDate() {
        // Given
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val schedule = Schedule(
            userId = testUserId,
            storeName = "이마트 부산점",
            scheduleDate = tomorrow,
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(12, 0),
            type = "순회"
        )
        testEntityManager.persistAndFlush(schedule)
        testEntityManager.clear()

        // When
        val result = scheduleRepository.findByUserIdAndScheduleDate(testUserId, today)

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("findByUserIdAndScheduleDate - 다른 사용자의 일정은 조회되지 않는다")
    fun findByUserIdAndScheduleDate_differentUser() {
        // Given
        val today = LocalDate.now()
        val otherUser = User(
            employeeId = "20030118",
            password = "encodedPassword",
            name = "김영희",
            orgName = "서울1지점"
        )
        val otherUserId = testEntityManager.persistAndFlush(otherUser).id

        val schedule = Schedule(
            userId = otherUserId,
            storeName = "이마트 서울점",
            scheduleDate = today,
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(12, 0),
            type = "순회"
        )
        testEntityManager.persistAndFlush(schedule)
        testEntityManager.clear()

        // When
        val result = scheduleRepository.findByUserIdAndScheduleDate(testUserId, today)

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("findByUserIdAndScheduleDate - 일정이 전혀 없으면 빈 목록을 반환한다")
    fun findByUserIdAndScheduleDate_noSchedules() {
        // When
        val result = scheduleRepository.findByUserIdAndScheduleDate(testUserId, LocalDate.now())

        // Then
        assertThat(result).isEmpty()
    }
}
