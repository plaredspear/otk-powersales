package com.otoki.internal.repository

import com.otoki.internal.entity.Attendance
import com.otoki.internal.entity.AttendanceWorkType
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
class AttendanceRepositoryTest {

    @Autowired
    private lateinit var attendanceRepository: AttendanceRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    private var testUserId: Long = 0
    private val today = LocalDate.now()

    @BeforeEach
    fun setUp() {
        attendanceRepository.deleteAll()
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
    @DisplayName("findByUserIdAndAttendanceDate - 등록 건이 있으면 목록 반환")
    fun findByUserIdAndAttendanceDate_withAttendances() {
        // Given
        val att1 = Attendance(
            userId = testUserId, storeId = 101,
            workType = AttendanceWorkType.ROOM_TEMP, attendanceDate = today
        )
        val att2 = Attendance(
            userId = testUserId, storeId = 102,
            workType = AttendanceWorkType.REFRIGERATED, attendanceDate = today
        )
        testEntityManager.persistAndFlush(att1)
        testEntityManager.persistAndFlush(att2)
        testEntityManager.clear()

        // When
        val result = attendanceRepository.findByUserIdAndAttendanceDate(testUserId, today)

        // Then
        assertThat(result).hasSize(2)
        assertThat(result.map { it.storeId }).containsExactlyInAnyOrder(101, 102)
    }

    @Test
    @DisplayName("findByUserIdAndAttendanceDate - 다른 날짜 등록 건은 조회되지 않는다")
    fun findByUserIdAndAttendanceDate_differentDate() {
        // Given
        val att = Attendance(
            userId = testUserId, storeId = 101,
            workType = AttendanceWorkType.ROOM_TEMP, attendanceDate = today.plusDays(1)
        )
        testEntityManager.persistAndFlush(att)
        testEntityManager.clear()

        // When
        val result = attendanceRepository.findByUserIdAndAttendanceDate(testUserId, today)

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("countByUserIdAndAttendanceDate - 등록 건수 반환")
    fun countByUserIdAndAttendanceDate_returnsCount() {
        // Given
        val att1 = Attendance(
            userId = testUserId, storeId = 101,
            workType = AttendanceWorkType.ROOM_TEMP, attendanceDate = today
        )
        val att2 = Attendance(
            userId = testUserId, storeId = 102,
            workType = AttendanceWorkType.REFRIGERATED, attendanceDate = today
        )
        testEntityManager.persistAndFlush(att1)
        testEntityManager.persistAndFlush(att2)
        testEntityManager.clear()

        // When
        val count = attendanceRepository.countByUserIdAndAttendanceDate(testUserId, today)

        // Then
        assertThat(count).isEqualTo(2)
    }

    @Test
    @DisplayName("countByUserIdAndAttendanceDate - 등록 건이 없으면 0 반환")
    fun countByUserIdAndAttendanceDate_zero() {
        // When
        val count = attendanceRepository.countByUserIdAndAttendanceDate(testUserId, today)

        // Then
        assertThat(count).isEqualTo(0)
    }

    @Test
    @DisplayName("existsByUserIdAndStoreIdAndAttendanceDate - 등록됨")
    fun existsByUserIdAndStoreIdAndAttendanceDate_exists() {
        // Given
        val att = Attendance(
            userId = testUserId, storeId = 101,
            workType = AttendanceWorkType.ROOM_TEMP, attendanceDate = today
        )
        testEntityManager.persistAndFlush(att)
        testEntityManager.clear()

        // When
        val result = attendanceRepository.existsByUserIdAndStoreIdAndAttendanceDate(testUserId, 101, today)

        // Then
        assertThat(result).isTrue()
    }

    @Test
    @DisplayName("existsByUserIdAndStoreIdAndAttendanceDate - 미등록")
    fun existsByUserIdAndStoreIdAndAttendanceDate_notExists() {
        // When
        val result = attendanceRepository.existsByUserIdAndStoreIdAndAttendanceDate(testUserId, 999, today)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    @DisplayName("다른 사용자의 등록 건은 조회되지 않는다")
    fun findByUserIdAndAttendanceDate_differentUser() {
        // Given
        val otherUser = User(
            employeeId = "20030118",
            password = "encodedPassword",
            name = "다른 사용자",
            department = "영업2팀",
            branchName = "서울1지점",
            role = UserRole.USER,
            workerType = WorkerType.PATROL
        )
        val otherUserId = testEntityManager.persistAndFlush(otherUser).id

        val att = Attendance(
            userId = otherUserId, storeId = 101,
            workType = AttendanceWorkType.ROOM_TEMP, attendanceDate = today
        )
        testEntityManager.persistAndFlush(att)
        testEntityManager.clear()

        // When
        val result = attendanceRepository.findByUserIdAndAttendanceDate(testUserId, today)

        // Then
        assertThat(result).isEmpty()
    }
}
