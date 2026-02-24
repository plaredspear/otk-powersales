package com.otoki.internal.common.service

import com.otoki.internal.common.dto.response.HomeResponse
import com.otoki.internal.auth.exception.UserNotFoundException
// import com.otoki.internal.repository.ExpiryProductRepository  // Phase2: PG 대응 테이블 없음
import com.otoki.internal.repository.NoticeRepository
import com.otoki.internal.repository.ScheduleRepository
import com.otoki.internal.common.repository.UserRepository
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

        // 오늘 일정 조회 (V1: employeeId(sfid) + workingDate로 조회)
        val userSfid = user.sfid ?: ""
        val todaySchedules = scheduleRepository
            .findByEmployeeIdAndWorkingDate(userSfid, today)
            .map { schedule ->
                HomeResponse.ScheduleInfo(
                    id = schedule.id,
                    storeName = "",  // V1에서 storeName 삭제됨
                    startTime = schedule.startTime?.toLocalTime()?.format(TIME_FORMATTER) ?: "",
                    endTime = schedule.completeTime?.toLocalTime()?.format(TIME_FORMATTER) ?: "",
                    type = schedule.workingType ?: ""
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
            .findRecentNotices(branch = user.orgName ?: "", since = since)
            .map { notice ->
                HomeResponse.NoticeInfo(
                    id = notice.id,
                    title = notice.name ?: "",
                    type = notice.category ?: "",
                    createdAt = notice.createdDate ?: LocalDateTime.MIN
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
