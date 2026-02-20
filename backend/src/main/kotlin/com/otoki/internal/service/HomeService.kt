package com.otoki.internal.service

import com.otoki.internal.dto.response.HomeResponse
import com.otoki.internal.exception.UserNotFoundException
// import com.otoki.internal.repository.ExpiryProductRepository  // Phase2: PG 대응 테이블 없음
import com.otoki.internal.repository.NoticeRepository
import com.otoki.internal.repository.ScheduleRepository
import com.otoki.internal.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 홈 화면 비즈니스 로직 Service
 */
@Service
@Transactional(readOnly = true)
class HomeService(
    private val userRepository: UserRepository,
    private val scheduleRepository: ScheduleRepository,
    // private val expiryProductRepository: ExpiryProductRepository,  // Phase2: PG 대응 테이블 없음
    private val noticeRepository: NoticeRepository
) {

    companion object {
        private const val EXPIRY_ALERT_DAYS = 7L
        private const val NOTICE_DAYS = 7L
        private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    /**
     * 홈 화면 데이터 통합 조회
     *
     * 1. 사용자 존재 확인
     * 2. 오늘 일정 조회
     * 3. 유통기한 임박제품 건수 조회
     * 4. 최근 1주일 공지사항 조회
     */
    fun getHomeData(userId: Long): HomeResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException() }

        val today = LocalDate.now()

        // 오늘 일정 조회
        val todaySchedules = scheduleRepository
            .findByUserIdAndScheduleDate(userId, today)
            .map { schedule ->
                HomeResponse.ScheduleInfo(
                    id = schedule.id,
                    storeName = schedule.storeName,
                    startTime = schedule.startTime.format(TIME_FORMATTER),
                    endTime = schedule.endTime.format(TIME_FORMATTER),
                    type = schedule.type
                )
            }

        // Phase2: ExpiryProduct PG 대응 테이블 없음 - 주석 처리
        // val expiryCount = expiryProductRepository
        //     .countByUserIdAndExpiryDateBetween(userId, today, today.plusDays(EXPIRY_ALERT_DAYS))
        //
        // val expiryAlert = if (expiryCount > 0) {
        //     HomeResponse.ExpiryAlertInfo(
        //         branchName = user.branchName,
        //         employeeName = user.name,
        //         employeeId = user.employeeId,
        //         expiryCount = expiryCount.toInt()
        //     )
        // } else {
        //     null
        // }
        val expiryAlert: HomeResponse.ExpiryAlertInfo? = null

        // 최근 1주일 공지사항 조회
        val since = LocalDateTime.of(today.minusDays(NOTICE_DAYS), LocalTime.MIN)
        val notices = noticeRepository
            .findRecentNotices(branchName = user.orgName ?: "", since = since)
            .map { notice ->
                HomeResponse.NoticeInfo(
                    id = notice.id,
                    title = notice.title,
                    type = notice.type.name,
                    createdAt = notice.createdAt
                )
            }

        return HomeResponse(
            todaySchedules = todaySchedules,
            expiryAlert = expiryAlert,
            notices = notices,
            currentDate = today.format(DATE_FORMATTER)
        )
    }
}
