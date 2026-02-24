package com.otoki.internal.common.service

import com.otoki.internal.entity.*
import com.otoki.internal.common.entity.*
import com.otoki.internal.notice.entity.Notice
import com.otoki.internal.auth.exception.UserNotFoundException
import com.otoki.internal.notice.repository.NoticeRepository
import com.otoki.internal.repository.ScheduleRepository
import com.otoki.internal.common.repository.UserRepository
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
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("HomeService 테스트")
class HomeServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var scheduleRepository: ScheduleRepository

    @Mock
    private lateinit var noticeRepository: NoticeRepository

    @InjectMocks
    private lateinit var homeService: HomeService

    private val testUserSfid = "a0B000000012345"

    // ========== 정상 조회 Tests ==========

    @Test
    @DisplayName("홈 데이터 조회 성공 - 일정, 공지사항 있는 경우")
    fun getHomeData_allDataPresent() {
        // Given
        val userId = 1L
        val user = createTestUser(id = userId, sfid = testUserSfid)

        val schedules = listOf(
            Schedule(
                id = 1L,
                employeeId = testUserSfid,
                workingDate = LocalDate.now(),
                workingType = "순회",
                startTime = LocalDateTime.now().withHour(9).withMinute(0)
            ),
            Schedule(
                id = 2L,
                employeeId = testUserSfid,
                workingDate = LocalDate.now(),
                workingType = "격고",
                startTime = LocalDateTime.now().withHour(14).withMinute(0),
                completeTime = LocalDateTime.now().withHour(17).withMinute(0)
            )
        )

        val notices = listOf(
            Notice(
                id = 1L,
                name = "2월 영업 목표 달성 현황",
                category = "BRANCH",
                branch = "부산1지점",
                createdDate = LocalDateTime.now().minusDays(1)
            ),
            Notice(
                id = 2L,
                name = "신제품 출시 안내",
                category = "ALL",
                createdDate = LocalDateTime.now().minusDays(2)
            )
        )

        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
        whenever(scheduleRepository.findByEmployeeIdAndWorkingDate(eq(testUserSfid), any()))
            .thenReturn(schedules)
        whenever(noticeRepository.findRecentNotices(eq("부산1지점"), any(), any(), any()))
            .thenReturn(notices)

        // When
        val result = homeService.getHomeData(userId)

        // Then
        assertThat(result.todaySchedules).hasSize(2)
        assertThat(result.todaySchedules[0].type).isEqualTo("순회")
        assertThat(result.todaySchedules[1].type).isEqualTo("격고")

        // Phase2: expiryAlert는 비활성화됨
        assertThat(result.expiryAlert).isNull()

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
        val user = createTestUser(id = userId, sfid = testUserSfid)

        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
        whenever(scheduleRepository.findByEmployeeIdAndWorkingDate(eq(testUserSfid), any()))
            .thenReturn(emptyList())
        whenever(noticeRepository.findRecentNotices(eq("부산1지점"), any(), any(), any()))
            .thenReturn(emptyList())

        // When
        val result = homeService.getHomeData(userId)

        // Then
        assertThat(result.todaySchedules).isEmpty()
    }

    @Test
    @DisplayName("홈 데이터 조회 - 공지사항이 없는 경우 빈 배열 반환")
    fun getHomeData_noNotices() {
        // Given
        val userId = 1L
        val user = createTestUser(id = userId, sfid = testUserSfid)

        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
        whenever(scheduleRepository.findByEmployeeIdAndWorkingDate(eq(testUserSfid), any()))
            .thenReturn(emptyList())
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
        val user = createTestUser(id = userId, sfid = testUserSfid)

        val notices = listOf(
            Notice(id = 1L, name = "지점공지1", category = "BRANCH", branch = "부산1지점",
                createdDate = LocalDateTime.now().minusHours(1)),
            Notice(id = 2L, name = "지점공지2", category = "BRANCH", branch = "부산1지점",
                createdDate = LocalDateTime.now().minusHours(2)),
            Notice(id = 3L, name = "전체공지1", category = "ALL",
                createdDate = LocalDateTime.now().minusHours(3)),
            Notice(id = 4L, name = "전체공지2", category = "ALL",
                createdDate = LocalDateTime.now().minusDays(1)),
            Notice(id = 5L, name = "전체공지3", category = "ALL",
                createdDate = LocalDateTime.now().minusDays(2))
        )

        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
        whenever(scheduleRepository.findByEmployeeIdAndWorkingDate(eq(testUserSfid), any()))
            .thenReturn(emptyList())
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
        val user = createTestUser(id = userId, sfid = testUserSfid)

        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
        whenever(scheduleRepository.findByEmployeeIdAndWorkingDate(eq(testUserSfid), any()))
            .thenReturn(emptyList())
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
        orgName: String = "부산1지점",
        sfid: String? = null
    ): User {
        return User(
            id = id,
            employeeId = employeeId,
            password = "encoded_password",
            name = name,
            orgName = orgName,
            sfid = sfid
        )
    }
}
