package com.otoki.internal.schedule.service

import com.otoki.internal.common.dto.response.AccountInfo
import com.otoki.internal.common.dto.response.AccountListResponse
import com.otoki.internal.common.util.GeoUtils
import com.otoki.internal.sap.entity.Account
import com.otoki.internal.auth.exception.EmployeeNotFoundException
import com.otoki.internal.safetycheck.repository.SafetyCheckSubmissionRepository
import com.otoki.internal.schedule.dto.response.AttendanceRegisterResponse
import com.otoki.internal.schedule.dto.response.AttendanceStatusItem
import com.otoki.internal.schedule.dto.response.AttendanceStatusResponse
import com.otoki.internal.schedule.entity.TeamMemberSchedule
import com.otoki.internal.schedule.exception.AlreadyRegisteredException
import com.otoki.internal.schedule.exception.DistanceExceededException
import com.otoki.internal.schedule.exception.SafetyCheckRequiredException
import com.otoki.internal.schedule.exception.TeamMemberScheduleNotFoundException
import com.otoki.internal.schedule.integration.OroraApiService
import com.otoki.internal.schedule.integration.OroraWorkReportRequest
import com.otoki.internal.schedule.repository.TeamMemberScheduleRepository
import com.otoki.internal.sap.repository.EmployeeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
@Transactional(readOnly = true)
class AttendanceService(
    private val employeeRepository: EmployeeRepository,
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
    private val safetyCheckSubmissionRepository: SafetyCheckSubmissionRepository,
    private val ororaApiService: OroraApiService
) {

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private const val DEFAULT_ALLOWED_DISTANCE_KM = 0.5

        /** 대리점 유형코드 면제 목록 — GPS 거리 검증 생략 */
        private val EXEMPT_ACCOUNT_TYPE_CODES = setOf(
            "1110", "1120", "1130", "1140",
            "1210", "1220",
            "1510", "1530",
            "1810", "1900"
        )
    }

    /**
     * 오늘 출근 거래처 목록 조회
     */
    fun getAccountList(userId: Long, keyword: String?): AccountListResponse {
        val employee = employeeRepository.findById(userId)
            .orElseThrow { EmployeeNotFoundException() }

        val today = LocalDate.now()

        // 안전점검 완료 여부 확인
        val safetyCheckCompleted = safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(employee.id, today)

        // 오늘 스케줄 조회 (account fetch join 포함)
        val teamMemberSchedules = teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(employee.id, today)

        // DTO 변환 + 키워드 필터링
        val accountInfos = teamMemberSchedules.mapNotNull { teamMemberSchedule ->
            val account = teamMemberSchedule.account
            val accountName = account?.name ?: ""

            // 키워드 필터링 (거래처명, 주소, 거래처코드)
            if (!keyword.isNullOrBlank()) {
                val lowerKeyword = keyword.lowercase()
                val address = account?.address1 ?: ""
                val accountTypeCode = account?.abcTypeCode ?: ""
                val matches = accountName.lowercase().contains(lowerKeyword) ||
                    address.lowercase().contains(lowerKeyword) ||
                    accountTypeCode.lowercase().contains(lowerKeyword)
                if (!matches) return@mapNotNull null
            }

            AccountInfo(
                scheduleId = teamMemberSchedule.id,
                accountId = teamMemberSchedule.account?.id,
                accountName = accountName,
                accountTypeCode = account?.abcTypeCode,
                workCategory = teamMemberSchedule.workingCategory1 ?: "",
                workCategory3 = teamMemberSchedule.workingCategory3,
                address = account?.address1,
                latitude = account?.latitude?.toDoubleOrNull(),
                longitude = account?.longitude?.toDoubleOrNull(),
                isRegistered = teamMemberSchedule.commuteLogId != null
            )
        }

        val registeredCount = accountInfos.count { it.isRegistered }

        return AccountListResponse(
            safetyCheckCompleted = safetyCheckCompleted,
            accounts = accountInfos,
            totalCount = accountInfos.size,
            registeredCount = registeredCount,
            currentDate = today.format(DATE_FORMATTER)
        )
    }

    /**
     * 출근 등록
     *
     * 1. 안전점검 완료 여부 검증
     * 2. 스케줄 조회 + 중복 검증
     * 3. GPS 거리 검증 (면제 코드 확인)
     * 4. Orora WorkReport 전송 (Mock)
     * 5. 응답 반환
     */
    @Transactional
    fun register(userId: Long, scheduleId: Long, latitude: Double, longitude: Double, workType: String?): AttendanceRegisterResponse {
        val employee = employeeRepository.findById(userId)
            .orElseThrow { EmployeeNotFoundException() }

        // 1. 안전점검 완료 여부 검증
        val today = LocalDate.now()
        if (!safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(employee.id, today)) {
            throw SafetyCheckRequiredException()
        }

        // 2. 스케줄 조회
        val teamMemberSchedule = teamMemberScheduleRepository.findById(scheduleId)
            .orElseThrow { TeamMemberScheduleNotFoundException() }

        // 3. 중복 등록 검증
        if (teamMemberSchedule.commuteLogId != null) {
            throw AlreadyRegisteredException()
        }

        // 4. 거래처 정보 조회 + GPS 거리 검증
        val account = teamMemberSchedule.account
        val distanceKm = validateDistance(latitude, longitude, account)

        // 5. SafetyCheckSubmission 조회 + OroraWorkReportRequest 구성 + 전송
        val safetyCheckSubmission = safetyCheckSubmissionRepository
            .findByEmployeeIdAndWorkingDate(employee.id, today)
            .orElse(null)

        val request = OroraWorkReportRequest(
            scheduleId = teamMemberSchedule.id,
            equipment1 = safetyCheckSubmission?.equipment1,
            equipment2 = safetyCheckSubmission?.equipment2,
            equipment3 = safetyCheckSubmission?.equipment3,
            equipment4 = safetyCheckSubmission?.equipment4,
            equipment5 = safetyCheckSubmission?.equipment5,
            equipment6 = safetyCheckSubmission?.equipment6,
            equipment7 = safetyCheckSubmission?.equipment7,
            equipment8 = safetyCheckSubmission?.equipment8,
            equipment9 = safetyCheckSubmission?.equipment9,
            yesCount = safetyCheckSubmission?.yesCheckCount,
            noCount = safetyCheckSubmission?.noCheckCount,
            startTime = safetyCheckSubmission?.startTime?.toString(),
            completeTime = safetyCheckSubmission?.completeTime?.toString(),
            precaution = safetyCheckSubmission?.precaution,
            precautionCount = safetyCheckSubmission?.precautionCheckCount,
            traversalFlag = safetyCheckSubmission?.traversalFlag
        )
        ororaApiService.sendWorkReport(request)

        // 6. 후속 처리: SafetyCheckSubmission.completeWorkYn = 'Y' + TMS 안전점검 데이터 반영
        if (safetyCheckSubmission != null) {
            safetyCheckSubmission.completeWorkYn = "Y"
            safetyCheckSubmissionRepository.save(safetyCheckSubmission)

            teamMemberScheduleRepository.updateSafetyCheckData(
                id = teamMemberSchedule.id,
                equipment1 = safetyCheckSubmission.equipment1,
                equipment2 = safetyCheckSubmission.equipment2,
                equipment3 = safetyCheckSubmission.equipment3,
                equipment4 = safetyCheckSubmission.equipment4,
                equipment5 = safetyCheckSubmission.equipment5,
                equipment6 = safetyCheckSubmission.equipment6,
                equipment7 = safetyCheckSubmission.equipment7,
                equipment8 = safetyCheckSubmission.equipment8,
                equipment9 = safetyCheckSubmission.equipment9,
                yesChkCnt = safetyCheckSubmission.yesCheckCount?.toDouble(),
                noChkCnt = safetyCheckSubmission.noCheckCount?.toDouble(),
                startTime = safetyCheckSubmission.startTime,
                completeTime = safetyCheckSubmission.completeTime,
                precaution = safetyCheckSubmission.precaution,
                precautionChk = safetyCheckSubmission.precautionCheckCount?.toDouble(),
                traversalFlag = safetyCheckSubmission.traversalFlag
            )
        }

        // 7. 출근 현황 집계 (commuteLogId 업데이트 후)
        val todayTeamMemberSchedules = teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(employee.id, today)
        val totalCount = todayTeamMemberSchedules.size
        val registeredCount = todayTeamMemberSchedules.count { it.commuteLogId != null || it.id == scheduleId }

        return AttendanceRegisterResponse(
            scheduleId = scheduleId,
            accountName = account?.name ?: "",
            workType = workType ?: teamMemberSchedule.workingType,
            distanceKm = distanceKm,
            totalCount = totalCount,
            registeredCount = registeredCount
        )
    }

    /**
     * 출근 현황 조회
     */
    fun getStatus(userId: Long): AttendanceStatusResponse {
        val employee = employeeRepository.findById(userId)
            .orElseThrow { EmployeeNotFoundException() }

        val today = LocalDate.now()

        val teamMemberSchedules = teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(employee.id, today)

        val statusList = teamMemberSchedules.map { teamMemberSchedule ->
            AttendanceStatusItem(
                scheduleId = teamMemberSchedule.id,
                accountName = teamMemberSchedule.account?.name ?: "",
                workCategory = teamMemberSchedule.workingCategory1 ?: "",
                status = if (teamMemberSchedule.commuteLogId != null) "REGISTERED" else "PENDING"
            )
        }

        val registeredCount = statusList.count { it.status == "REGISTERED" }

        return AttendanceStatusResponse(
            totalCount = statusList.size,
            registeredCount = registeredCount,
            statusList = statusList,
            currentDate = today.format(DATE_FORMATTER)
        )
    }

    /**
     * GPS 거리 검증
     * @return 계산된 거리 (km). 면제 시 0.0
     */
    private fun validateDistance(userLat: Double, userLon: Double, account: Account?): Double {
        // 면제 코드 확인
        val accountTypeCode = account?.abcTypeCode
        if (accountTypeCode != null && accountTypeCode in EXEMPT_ACCOUNT_TYPE_CODES) {
            return 0.0
        }

        // 거래처 위경도 확인
        val accountLat = account?.latitude?.toDoubleOrNull() ?: return 0.0
        val accountLon = account.longitude?.toDoubleOrNull() ?: return 0.0

        // Haversine 거리 계산
        val distance = GeoUtils.calculateDistance(userLat, userLon, accountLat, accountLon)

        // 허용 거리 비교 (commute_distance 테이블 미구현 → 기본값 사용)
        val allowedDistance = DEFAULT_ALLOWED_DISTANCE_KM

        if (distance > allowedDistance) {
            throw DistanceExceededException(distance)
        }

        return distance
    }
}
