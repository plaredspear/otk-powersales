package com.otoki.internal.service

import com.otoki.internal.dto.response.*
import com.otoki.internal.exception.UserNotFoundException
// import com.otoki.internal.repository.AttendanceRepository  // Phase2: PG 대응 테이블 없음
import com.otoki.internal.repository.StoreScheduleRepository
import com.otoki.internal.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@Service
class MyScheduleService(
    private val userRepository: UserRepository,
    private val storeScheduleRepository: StoreScheduleRepository
    // private val attendanceRepository: AttendanceRepository  // Phase2: PG 대응 테이블 없음
) {

    companion object {
        private val DAY_OF_WEEK_KR = mapOf(
            DayOfWeek.MONDAY to "월",
            DayOfWeek.TUESDAY to "화",
            DayOfWeek.WEDNESDAY to "수",
            DayOfWeek.THURSDAY to "목",
            DayOfWeek.FRIDAY to "금",
            DayOfWeek.SATURDAY to "토",
            DayOfWeek.SUNDAY to "일"
        )
    }

    /**
     * 월간 일정 조회
     * 특정 연/월의 근무일 여부를 날짜별로 반환
     */
    @Transactional(readOnly = true)
    fun getMonthlySchedule(userId: Long, year: Int, month: Int): MonthlyScheduleResponse {
        // 사용자 sfid 조회 (V1 StoreSchedule은 fullName(sfid)로 조회)
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException() }
        val userSfid = user.sfid ?: ""

        // YearMonth로 해당 월의 시작일/종료일 계산
        val yearMonth = YearMonth.of(year, month)
        val startDate = yearMonth.atDay(1)
        val endDate = yearMonth.atEndOfMonth()

        // 해당 기간 내 일정이 있는 날짜 목록 조회
        val workDates = storeScheduleRepository
            .findDistinctStartDatesByFullNameAndDateBetween(userSfid, startDate, endDate)
            .toSet()

        // 해당 월의 모든 날짜에 대해 근무 여부 판정
        val workDays = mutableListOf<WorkDayDto>()
        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            workDays.add(
                WorkDayDto(
                    date = currentDate.toString(),
                    hasWork = workDates.contains(currentDate)
                )
            )
            currentDate = currentDate.plusDays(1)
        }

        return MonthlyScheduleResponse(
            year = year,
            month = month,
            workDays = workDays
        )
    }

    /**
     * 일간 일정 상세 조회
     * 특정 날짜의 거래처별 근무 정보 및 등록 현황 반환
     */
    @Transactional(readOnly = true)
    fun getDailySchedule(userId: Long, date: LocalDate): DailyScheduleResponse {
        // 사용자 정보 조회
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException() }
        val userSfid = user.sfid ?: ""

        // 해당 날짜의 거래처 일정 목록 조회
        val schedules = storeScheduleRepository.findByFullNameAndStartDate(userSfid, date)

        // Phase2: Attendance PG 대응 테이블 없음 - 주석 처리
        // val attendances = attendanceRepository.findByUserIdAndAttendanceDate(userId, date)
        // val attendanceStoreIds = attendances.map { it.storeId }.toSet()

        // 거래처 목록 매핑 (등록 여부 - Attendance 비활성화로 항상 false)
        // Note: V1 리매핑으로 storeId(Long)→account(String sfid) 변경, DTO는 Long 유지 → 0L 대체
        val storeItems = schedules.map { schedule ->
            StoreScheduleItemDto(
                storeId = 0L,  // V1: account(String sfid)로 변경됨, DTO Long 타입 유지
                storeName = "",  // V1에서 storeName 삭제됨
                workType1 = schedule.typeOfWork1 ?: "",
                workType2 = "",
                workType3 = "",
                isRegistered = false  // Phase2: attendance 비활성화
            )
        }

        // 보고 진행 상황 계산
        val completed = storeItems.count { it.isRegistered }
        val total = storeItems.size
        val workType = schedules.firstOrNull()?.typeOfWork1 ?: ""

        // 요일 계산
        val dayOfWeek = DAY_OF_WEEK_KR[date.dayOfWeek] ?: ""

        return DailyScheduleResponse(
            date = date.toString(),
            dayOfWeek = dayOfWeek,
            memberName = user.name,
            employeeNumber = user.employeeId,
            reportProgress = ReportProgressDto(
                completed = completed,
                total = total,
                workType = workType
            ),
            stores = storeItems
        )
    }
}
