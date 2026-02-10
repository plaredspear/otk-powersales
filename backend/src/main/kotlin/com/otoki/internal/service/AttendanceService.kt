package com.otoki.internal.service

import com.otoki.internal.dto.response.*
import com.otoki.internal.entity.Attendance
import com.otoki.internal.entity.AttendanceWorkType
import com.otoki.internal.entity.StoreSchedule
import com.otoki.internal.entity.WorkerType
import com.otoki.internal.exception.*
import com.otoki.internal.repository.AttendanceRepository
import com.otoki.internal.repository.StoreScheduleRepository
import com.otoki.internal.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class AttendanceService(
    private val userRepository: UserRepository,
    private val storeScheduleRepository: StoreScheduleRepository,
    private val attendanceRepository: AttendanceRepository
) {

    companion object {
        private const val IRREGULAR_MAX_REGISTRATIONS = 2
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    /**
     * 오늘 출근 거래처 목록 조회
     */
    @Transactional(readOnly = true)
    fun getStoreList(userId: Long, keyword: String?): StoreListResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException() }

        val today = LocalDate.now()

        // 키워드가 있으면 키워드 검색, 없으면 전체 조회
        val schedules = if (!keyword.isNullOrBlank()) {
            storeScheduleRepository.findByUserIdAndScheduleDateAndKeyword(userId, today, keyword)
        } else {
            storeScheduleRepository.findByUserIdAndScheduleDate(userId, today)
        }

        // 오늘 출근등록 목록 조회
        val attendances = attendanceRepository.findByUserIdAndAttendanceDate(userId, today)
        val attendanceMap = attendances.associateBy { it.storeId }

        // 거래처별 등록 여부 매핑
        val storeInfos = schedules.map { schedule ->
            val attendance = attendanceMap[schedule.storeId]
            StoreInfo(
                storeId = schedule.storeId,
                storeName = schedule.storeName,
                storeCode = schedule.storeCode,
                workCategory = schedule.workCategory,
                address = schedule.address,
                isRegistered = attendance != null,
                registeredWorkType = attendance?.workType?.name
            )
        }

        val registeredCount = storeInfos.count { it.isRegistered }

        return StoreListResponse(
            workerType = user.workerType.name,
            stores = storeInfos,
            totalCount = storeInfos.size,
            registeredCount = registeredCount,
            currentDate = today.format(DATE_FORMATTER)
        )
    }

    /**
     * 출근등록
     */
    @Transactional
    fun registerAttendance(userId: Long, storeId: Long, workTypeStr: String): AttendanceResponse {
        // 1. 사용자 확인
        val user = userRepository.findById(userId)
            .orElseThrow { UserNotFoundException() }

        // 2. workType 유효성 검증
        val workType = try {
            AttendanceWorkType.valueOf(workTypeStr)
        } catch (e: IllegalArgumentException) {
            throw InvalidWorkTypeException()
        }

        val today = LocalDate.now()

        // 3. 거래처가 오늘 스케줄에 존재하는지 확인
        val schedule = storeScheduleRepository.findByUserIdAndStoreIdAndScheduleDate(userId, storeId, today)
            ?: throw StoreNotFoundException()

        // 4. 중복 등록 확인
        if (attendanceRepository.existsByUserIdAndStoreIdAndAttendanceDate(userId, storeId, today)) {
            throw AlreadyRegisteredException()
        }

        // 5. 격고 근무자 등록 한도 확인
        if (user.workerType == WorkerType.IRREGULAR) {
            val currentCount = attendanceRepository.countByUserIdAndAttendanceDate(userId, today)
            if (currentCount >= IRREGULAR_MAX_REGISTRATIONS) {
                throw RegistrationLimitExceededException()
            }
        }

        // 6. 출근등록 생성
        val attendance = Attendance(
            userId = userId,
            storeId = storeId,
            workType = workType,
            attendanceDate = today
        )
        val savedAttendance = attendanceRepository.save(attendance)

        // 7. 카운트 집계
        val totalCount = storeScheduleRepository.findByUserIdAndScheduleDate(userId, today).size
        val registeredCount = attendanceRepository.countByUserIdAndAttendanceDate(userId, today).toInt()

        return AttendanceResponse(
            attendanceId = savedAttendance.id,
            storeId = schedule.storeId,
            storeName = schedule.storeName,
            workType = workType.name,
            registeredAt = savedAttendance.registeredAt,
            totalCount = totalCount,
            registeredCount = registeredCount
        )
    }

    /**
     * 출근등록 현황 조회
     */
    @Transactional(readOnly = true)
    fun getAttendanceStatus(userId: Long): AttendanceStatusResponse {
        userRepository.findById(userId)
            .orElseThrow { UserNotFoundException() }

        val today = LocalDate.now()

        // 오늘 스케줄 전체 조회
        val schedules = storeScheduleRepository.findByUserIdAndScheduleDate(userId, today)

        // 오늘 출근등록 목록 조회
        val attendances = attendanceRepository.findByUserIdAndAttendanceDate(userId, today)
        val attendanceMap = attendances.associateBy { it.storeId }

        // 거래처별 상태 매핑
        val statusList = schedules.map { schedule ->
            val attendance = attendanceMap[schedule.storeId]
            AttendanceStatusInfo(
                storeId = schedule.storeId,
                storeName = schedule.storeName,
                status = if (attendance != null) "COMPLETED" else "PENDING",
                workType = attendance?.workType?.name,
                registeredAt = attendance?.registeredAt
            )
        }

        val registeredCount = statusList.count { it.status == "COMPLETED" }

        return AttendanceStatusResponse(
            totalCount = statusList.size,
            registeredCount = registeredCount,
            statusList = statusList,
            currentDate = today.format(DATE_FORMATTER)
        )
    }
}
