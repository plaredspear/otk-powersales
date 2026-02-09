package com.otoki.internal.service

import com.otoki.internal.entity.*
import com.otoki.internal.exception.UserNotFoundException
import com.otoki.internal.repository.ExpiryProductRepository
import com.otoki.internal.repository.NoticeRepository
import com.otoki.internal.repository.ScheduleRepository
import com.otoki.internal.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("HomeService 테스트")
class HomeServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var scheduleRepository: ScheduleRepository

    @Mock
    private lateinit var expiryProductRepository: ExpiryProductRepository

    @Mock
    private lateinit var noticeRepository: NoticeRepository

    @InjectMocks
    private lateinit var homeService: HomeService

    // ========== 정상 조회 Tests ==========

    @Test
    @DisplayName("홈 데이터 조회 성공 - 일정, 유통기한 알림, 공지사항 모두 있는 경우")
    fun getHomeData_allDataPresent() {
        // Given
        val userId = 1L
        val user = createTestUser(id = userId)

        val schedules = listOf(
            Schedule(
                id = 1L,
                userId = userId,
                storeName = "이마트 부산점",
                scheduleDate = LocalDate.now(),
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(12, 0),
                type = "순회"
            ),
            Schedule(
                id = 2L,
                userId = userId,
                storeName = "홈플러스 해운대점",
                scheduleDate = LocalDate.now(),
                startTime = LocalTime.of(14, 0),
                endTime = LocalTime.of(17, 0),
                type = "격고"
            )
        )

        val notices = listOf(
            Notice(
                id = 1L,
                title = "2월 영업 목표 달성 현황",
                type = NoticeType.BRANCH,
                branchName = "부산1지점",
                createdAt = LocalDateTime.now().minusDays(1)
            ),
            Notice(
                id = 2L,
                title = "신제품 출시 안내",
                type = NoticeType.ALL,
                createdAt = LocalDateTime.now().minusDays(2)
            )
        )

        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
        whenever(scheduleRepository.findByUserIdAndScheduleDate(eq(userId), any()))
            .thenReturn(schedules)
        whenever(expiryProductRepository.countByUserIdAndExpiryDateBetween(eq(userId), any(), any()))
            .thenReturn(3L)
        whenever(noticeRepository.findRecentNotices(eq("부산1지점"), any(), any(), any()))
            .thenReturn(notices)

        // When
        val result = homeService.getHomeData(userId)

        // Then
        assertThat(result.todaySchedules).hasSize(2)
        assertThat(result.todaySchedules[0].storeName).isEqualTo("이마트 부산점")
        assertThat(result.todaySchedules[0].startTime).isEqualTo("09:00")
        assertThat(result.todaySchedules[0].endTime).isEqualTo("12:00")
        assertThat(result.todaySchedules[0].type).isEqualTo("순회")
        assertThat(result.todaySchedules[1].storeName).isEqualTo("홈플러스 해운대점")

        assertThat(result.expiryAlert).isNotNull
        assertThat(result.expiryAlert!!.branchName).isEqualTo("부산1지점")
        assertThat(result.expiryAlert!!.employeeName).isEqualTo("최금주")
        assertThat(result.expiryAlert!!.employeeId).isEqualTo("20030117")
        assertThat(result.expiryAlert!!.expiryCount).isEqualTo(3)

        assertThat(result.notices).hasSize(2)
        assertThat(result.notices[0].title).isEqualTo("2월 영업 목표 달성 현황")
        assertThat(result.notices[0].type).isEqualTo("BRANCH")
        assertThat(result.notices[1].title).isEqualTo("신제품 출시 안내")
        assertThat(result.notices[1].type).isEqualTo("ALL")

        assertThat(result.currentDate).isEqualTo(LocalDate.now().toString())
    }

    @Test
    @DisplayName("홈 데이터 조회 - 오늘 일정이 없는 경우 빈 배열 반환")
    fun getHomeData_noSchedules() {
        // Given
        val userId = 1L
        val user = createTestUser(id = userId)

        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
        whenever(scheduleRepository.findByUserIdAndScheduleDate(eq(userId), any()))
            .thenReturn(emptyList())
        whenever(expiryProductRepository.countByUserIdAndExpiryDateBetween(eq(userId), any(), any()))
            .thenReturn(0L)
        whenever(noticeRepository.findRecentNotices(eq("부산1지점"), any(), any(), any()))
            .thenReturn(emptyList())

        // When
        val result = homeService.getHomeData(userId)

        // Then
        assertThat(result.todaySchedules).isEmpty()
    }

    @Test
    @DisplayName("홈 데이터 조회 - 유통기한 임박제품이 없는 경우 expiryAlert가 null")
    fun getHomeData_noExpiryProducts() {
        // Given
        val userId = 1L
        val user = createTestUser(id = userId)

        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
        whenever(scheduleRepository.findByUserIdAndScheduleDate(eq(userId), any()))
            .thenReturn(emptyList())
        whenever(expiryProductRepository.countByUserIdAndExpiryDateBetween(eq(userId), any(), any()))
            .thenReturn(0L)
        whenever(noticeRepository.findRecentNotices(eq("부산1지점"), any(), any(), any()))
            .thenReturn(emptyList())

        // When
        val result = homeService.getHomeData(userId)

        // Then
        assertThat(result.expiryAlert).isNull()
    }

    @Test
    @DisplayName("홈 데이터 조회 - 유통기한 임박제품이 있는 경우 사용자 정보와 함께 알림 반환")
    fun getHomeData_withExpiryProducts() {
        // Given
        val userId = 1L
        val user = createTestUser(id = userId)

        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
        whenever(scheduleRepository.findByUserIdAndScheduleDate(eq(userId), any()))
            .thenReturn(emptyList())
        whenever(expiryProductRepository.countByUserIdAndExpiryDateBetween(eq(userId), any(), any()))
            .thenReturn(5L)
        whenever(noticeRepository.findRecentNotices(eq("부산1지점"), any(), any(), any()))
            .thenReturn(emptyList())

        // When
        val result = homeService.getHomeData(userId)

        // Then
        assertThat(result.expiryAlert).isNotNull
        assertThat(result.expiryAlert!!.expiryCount).isEqualTo(5)
        assertThat(result.expiryAlert!!.branchName).isEqualTo("부산1지점")
        assertThat(result.expiryAlert!!.employeeName).isEqualTo("최금주")
        assertThat(result.expiryAlert!!.employeeId).isEqualTo("20030117")
    }

    @Test
    @DisplayName("홈 데이터 조회 - 공지사항이 없는 경우 빈 배열 반환")
    fun getHomeData_noNotices() {
        // Given
        val userId = 1L
        val user = createTestUser(id = userId)

        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
        whenever(scheduleRepository.findByUserIdAndScheduleDate(eq(userId), any()))
            .thenReturn(emptyList())
        whenever(expiryProductRepository.countByUserIdAndExpiryDateBetween(eq(userId), any(), any()))
            .thenReturn(0L)
        whenever(noticeRepository.findRecentNotices(eq("부산1지점"), any(), any(), any()))
            .thenReturn(emptyList())

        // When
        val result = homeService.getHomeData(userId)

        // Then
        assertThat(result.notices).isEmpty()
    }

    @Test
    @DisplayName("홈 데이터 조회 - 지점공지 + 전체공지 혼합 조회")
    fun getHomeData_mixedNotices() {
        // Given
        val userId = 1L
        val user = createTestUser(id = userId)

        val notices = listOf(
            Notice(id = 1L, title = "지점공지1", type = NoticeType.BRANCH, branchName = "부산1지점",
                createdAt = LocalDateTime.now().minusHours(1)),
            Notice(id = 2L, title = "지점공지2", type = NoticeType.BRANCH, branchName = "부산1지점",
                createdAt = LocalDateTime.now().minusHours(2)),
            Notice(id = 3L, title = "전체공지1", type = NoticeType.ALL,
                createdAt = LocalDateTime.now().minusHours(3)),
            Notice(id = 4L, title = "전체공지2", type = NoticeType.ALL,
                createdAt = LocalDateTime.now().minusDays(1)),
            Notice(id = 5L, title = "전체공지3", type = NoticeType.ALL,
                createdAt = LocalDateTime.now().minusDays(2))
        )

        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
        whenever(scheduleRepository.findByUserIdAndScheduleDate(eq(userId), any()))
            .thenReturn(emptyList())
        whenever(expiryProductRepository.countByUserIdAndExpiryDateBetween(eq(userId), any(), any()))
            .thenReturn(0L)
        whenever(noticeRepository.findRecentNotices(eq("부산1지점"), any(), any(), any()))
            .thenReturn(notices)

        // When
        val result = homeService.getHomeData(userId)

        // Then
        assertThat(result.notices).hasSize(5)
        assertThat(result.notices[0].type).isEqualTo("BRANCH")
        assertThat(result.notices[2].type).isEqualTo("ALL")
    }

    // ========== 에러 케이스 Tests ==========

    @Test
    @DisplayName("홈 데이터 조회 실패 - 존재하지 않는 사용자 ID로 조회 시 UserNotFoundException 발생")
    fun getHomeData_userNotFound() {
        // Given
        whenever(userRepository.findById(999L)).thenReturn(Optional.empty())

        // When & Then
        assertThatThrownBy { homeService.getHomeData(999L) }
            .isInstanceOf(UserNotFoundException::class.java)
    }

    @Test
    @DisplayName("홈 데이터 조회 - currentDate가 서버 기준 현재 날짜를 반환")
    fun getHomeData_currentDateIsToday() {
        // Given
        val userId = 1L
        val user = createTestUser(id = userId)

        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
        whenever(scheduleRepository.findByUserIdAndScheduleDate(eq(userId), any()))
            .thenReturn(emptyList())
        whenever(expiryProductRepository.countByUserIdAndExpiryDateBetween(eq(userId), any(), any()))
            .thenReturn(0L)
        whenever(noticeRepository.findRecentNotices(eq("부산1지점"), any(), any(), any()))
            .thenReturn(emptyList())

        // When
        val result = homeService.getHomeData(userId)

        // Then
        assertThat(result.currentDate).isEqualTo(LocalDate.now().toString())
    }

    // ========== Helper ==========

    private fun createTestUser(
        id: Long = 1L,
        employeeId: String = "20030117",
        name: String = "최금주",
        department: String = "영업1팀",
        branchName: String = "부산1지점",
        role: UserRole = UserRole.USER
    ): User {
        return User(
            id = id,
            employeeId = employeeId,
            password = "encoded_password",
            name = name,
            department = department,
            branchName = branchName,
            role = role
        )
    }
}
