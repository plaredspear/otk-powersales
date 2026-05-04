package com.otoki.powersales.schedule.service

import com.otoki.powersales.common.dto.response.AccountInfo
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.common.dto.response.AccountListResponse
import com.otoki.powersales.common.util.GeoUtils
import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.safetycheck.repository.SafetyCheckSubmissionRepository
import com.otoki.powersales.schedule.config.AttendanceProperties
import com.otoki.powersales.schedule.dto.response.AttendanceRegisterResponse
import com.otoki.powersales.schedule.dto.response.AttendanceStatusItem
import com.otoki.powersales.schedule.dto.response.AttendanceStatusResponse
import com.otoki.powersales.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.schedule.exception.*
import com.otoki.powersales.schedule.integration.OroraApiService
import com.otoki.powersales.schedule.integration.OroraWorkReportRequest
import com.otoki.powersales.schedule.policy.AbcExemptPolicy
import com.otoki.powersales.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.schedule.util.AccountCoordinateParser
import com.otoki.powersales.employee.repository.EmployeeRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
@Transactional(readOnly = true)
class AttendanceService(
    private val employeeRepository: EmployeeRepository,
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
    private val displayWorkScheduleRepository: DisplayWorkScheduleRepository,
    private val safetyCheckSubmissionRepository: SafetyCheckSubmissionRepository,
    private val ororaApiService: OroraApiService,
    private val adminMonthlyIntegrationService: AdminMonthlyIntegrationService,
    private val attendanceProperties: AttendanceProperties,
    private val clock: Clock
) {

    private val log = LoggerFactory.getLogger(AttendanceService::class.java)

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private val SEOUL_ZONE = ZoneId.of("Asia/Seoul")
        private val REGISTRATION_DEADLINE = LocalTime.of(17, 0)
        private const val LAT_MIN = -90.0
        private const val LAT_MAX = 90.0
        private const val LNG_MIN = -180.0
        private const val LNG_MAX = 180.0
    }

    /**
     * 오늘 출근 거래처 목록 조회
     * 기존 TeamMemberSchedule + 진열마스터 기반 거래처를 병합하여 반환
     */
    fun getAccountList(userId: Long, keyword: String?): AccountListResponse {
        val employee = employeeRepository.findById(userId)
            .orElseThrow { EmployeeNotFoundException() }

        val today = LocalDate.now()

        // 안전점검 완료 여부 확인
        val safetyCheckCompleted = safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(employee.id, today)

        // 오늘 스케줄 조회 (account fetch join 포함)
        val teamMemberSchedules = teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(employee.id, today)

        // 기존 TeamMemberSchedule → AccountInfo (source=schedule)
        val scheduleAccountInfos = teamMemberSchedules.mapNotNull { tms ->
            val account = tms.account
            val accountName = account?.name ?: ""

            if (!matchesKeyword(keyword, accountName, account)) return@mapNotNull null

            AccountInfo(
                scheduleId = tms.id,
                accountId = tms.account?.id,
                accountName = accountName,
                accountTypeCode = account?.abcTypeCode,
                workCategory = tms.workingCategory1 ?: "",
                workCategory2 = tms.workingCategory2,
                workCategory3 = tms.workingCategory3,
                address = account?.address1,
                latitude = account?.latitude?.toDoubleOrNull(),
                longitude = account?.longitude?.toDoubleOrNull(),
                isRegistered = tms.commuteLogId != null,
                source = "schedule"
            )
        }

        // 진열마스터 기반 거래처 조회 (confirmed=true, 오늘 유효, 미삭제)
        val validMasters = displayWorkScheduleRepository.findConfirmedValidByEmployeeAndDate(employee.id, today)

        // 이미 TeamMemberSchedule에 존재하는 거래처 제외
        val existingAccountIds = teamMemberSchedules.mapNotNull { it.account?.id }.toSet()

        val masterAccountInfos = validMasters.mapNotNull { master ->
            val account = master.account
            val accountId = account?.id ?: return@mapNotNull null
            if (accountId in existingAccountIds) return@mapNotNull null

            val accountName = account.name ?: ""
            if (!matchesKeyword(keyword, accountName, account)) return@mapNotNull null

            AccountInfo(
                displayWorkScheduleId = master.id,
                accountId = accountId,
                accountName = accountName,
                accountTypeCode = account.abcTypeCode,
                workCategory = "진열",
                workCategory2 = master.typeOfWork5,
                workCategory3 = master.typeOfWork3,
                address = account.address1,
                latitude = account.latitude?.toDoubleOrNull(),
                longitude = account.longitude?.toDoubleOrNull(),
                isRegistered = false,
                source = "master"
            )
        }

        // 병합: 등록완료 → 임시(미등록) → 일반(미등록), 같은 그룹 내 source=schedule → master
        val allAccounts = (scheduleAccountInfos + masterAccountInfos).sortedWith(
            compareByDescending<AccountInfo> { it.isRegistered }
                .thenByDescending { it.workCategory2?.contains("임시") == true }
                .thenBy { it.source != "schedule" }
        )

        val registeredCount = allAccounts.count { it.isRegistered }

        val now = LocalTime.now(clock.withZone(SEOUL_ZONE))

        return AccountListResponse(
            safetyCheckCompleted = safetyCheckCompleted,
            accounts = allAccounts,
            totalCount = allAccounts.size,
            registeredCount = registeredCount,
            currentDate = today.format(DATE_FORMATTER),
            registrationDeadline = "17:00",
            isRegistrationClosed = !now.isBefore(REGISTRATION_DEADLINE)
        )
    }

    /**
     * 출근 등록
     *
     * scheduleId 또는 displayWorkScheduleId 중 하나를 전달받아 처리:
     * - scheduleId: 기존 방식 (TeamMemberSchedule 직접 조회)
     * - displayWorkScheduleId: 진열마스터 기반으로 TeamMemberSchedule 동적 생성 후 출근 처리
     */
    @Transactional
    fun register(
        userId: Long,
        scheduleId: Long?,
        displayWorkScheduleId: Long?,
        latitude: Double,
        longitude: Double,
        workType: String?
    ): AttendanceRegisterResponse {
        // 상호 배타 검증
        if (scheduleId == null && displayWorkScheduleId == null) {
            throw AttendanceTargetRequiredException()
        }
        if (scheduleId != null && displayWorkScheduleId != null) {
            throw AttendanceTargetConflictException()
        }

        // 시간 제한 검증: 17시 이후 등록 차단
        val now = LocalTime.now(clock.withZone(SEOUL_ZONE))
        if (!now.isBefore(REGISTRATION_DEADLINE)) {
            throw AttendanceTimeExceededException()
        }

        val employee = employeeRepository.findById(userId)
            .orElseThrow { EmployeeNotFoundException() }

        val today = LocalDate.now()

        // 0. 대휴 날짜 충돌 검증
        if (teamMemberScheduleRepository.existsByEmployeeAndWorkingDateAndWorkingType(employee, today, "대휴")) {
            throw AttendanceDayOffConflictException()
        }

        // 1. 안전점검 완료 여부 검증
        if (!safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(employee.id, today)) {
            throw SafetyCheckRequiredException()
        }

        // 2. 스케줄 결정 (기존 방식 vs 진열마스터 기반 동적 생성)
        val (teamMemberSchedule, newlyCreated) = if (scheduleId != null) {
            resolveByScheduleId(scheduleId)
        } else {
            resolveByDisplayWorkSchedule(displayWorkScheduleId!!, employee, today)
        }

        // 3. 중복 등록 검증
        if (teamMemberSchedule.commuteLogId != null) {
            throw AlreadyRegisteredException()
        }

        // 4. 거래처 정보 조회 + 면제 평가 (Spec #586) → 미면제 시 GPS 거리 검증 (Spec #585)
        val account = teamMemberSchedule.account
        val exemptResult = AbcExemptPolicy.evaluate(account)
        if (!exemptResult.skipped) {
            validateDistance(latitude, longitude, account, employee.id)
        } else {
            log.info(
                "ATT_GPS_SKIPPED employeeId={} accountId={} reason={}",
                employee.id, account?.id, exemptResult.reason
            )
        }

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

        // 7. 동적 생성 시 월별 통합일정 갱신
        if (newlyCreated && account != null) {
            adminMonthlyIntegrationService.refreshIntegration(
                employeeId = employee.id,
                accountId = account.id,
                yearMonth = YearMonth.from(today)
            )
        }

        // 8. 출근 현황 집계
        val todayTeamMemberSchedules = teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(employee.id, today)
        val totalCount = todayTeamMemberSchedules.size
        val registeredCount = todayTeamMemberSchedules.count { it.commuteLogId != null || it.id == teamMemberSchedule.id }

        return AttendanceRegisterResponse(
            scheduleId = teamMemberSchedule.id,
            accountName = account?.name ?: "",
            workType = workType ?: teamMemberSchedule.workingType,
            distanceKm = 0.0, // Spec #585 Q4: 실제 거리는 응답에 노출하지 않음 (서버 로그에만 기록)
            totalCount = totalCount,
            registeredCount = registeredCount,
            gpsSkipped = exemptResult.skipped, // Spec #586: ABC 면제 정책 적용 여부
            gpsSkipReason = exemptResult.reason
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
     * 기존 방식: scheduleId로 TeamMemberSchedule 직접 조회
     * @return (TeamMemberSchedule, newlyCreated=false)
     */
    private fun resolveByScheduleId(scheduleId: Long): Pair<TeamMemberSchedule, Boolean> {
        val tms = teamMemberScheduleRepository.findById(scheduleId)
            .orElseThrow { TeamMemberScheduleNotFoundException() }
        return tms to false
    }

    /**
     * 진열마스터 기반: DisplayWorkSchedule 검증 후 TeamMemberSchedule 동적 생성/조회
     * @return (TeamMemberSchedule, newlyCreated)
     */
    private fun resolveByDisplayWorkSchedule(
        displayWorkScheduleId: Long,
        employee: com.otoki.powersales.employee.entity.Employee,
        today: LocalDate
    ): Pair<TeamMemberSchedule, Boolean> {
        val master = displayWorkScheduleRepository.findById(displayWorkScheduleId)
            .orElseThrow { DisplayScheduleNotFoundException() }

        if (master.isDeleted == true) {
            throw DisplayScheduleNotFoundException()
        }
        if (master.confirmed != true) {
            throw DisplayScheduleNotConfirmedException()
        }
        val startDate = master.startDate
        val endDate = master.endDate
        if (startDate != null && today.isBefore(startDate)) {
            throw DisplayScheduleOutOfRangeException()
        }
        if (endDate != null && today.isAfter(endDate)) {
            throw DisplayScheduleOutOfRangeException()
        }
        // startDate가 null이면 기간 조건 없는 것으로 간주

        val account = master.account ?: throw DisplayScheduleNotFoundException()

        // 중복 생성 방지: 동일 사원+거래처+오늘 여사원일정이 이미 존재하면 재사용
        val existing = teamMemberScheduleRepository.findByEmployeeAndAccountAndWorkingDate(employee, account, today)
        if (existing != null) {
            return existing to false
        }

        // 조장 조회
        val teamLeader = findTeamLeader(employee.costCenterCode)

        // 새 TeamMemberSchedule 생성
        val newSchedule = TeamMemberSchedule(
            employee = employee,
            account = account,
            workingDate = today,
            workingType = "근무",
            workingCategory1 = "진열",
            workingCategory2 = mapTypeOfWork5ToCategory2(master.typeOfWork5),
            workingCategory3 = master.typeOfWork3,
            teamLeader = teamLeader
        )
        val saved = teamMemberScheduleRepository.save(newSchedule)
        return saved to true
    }

    /**
     * typeOfWork5 → workingCategory2 매핑
     * "상시" → "전담", "임시" → "임시"
     */
    private fun mapTypeOfWork5ToCategory2(typeOfWork5: String?): String? {
        return when (typeOfWork5) {
            "상시" -> "전담"
            "임시" -> "임시"
            else -> typeOfWork5
        }
    }

    /**
     * 사원의 조직코드 기반 조장 조회
     */
    private fun findTeamLeader(costCenterCode: String?): com.otoki.powersales.employee.entity.Employee? {
        if (costCenterCode.isNullOrBlank()) return null
        val leaders = employeeRepository.findByCostCenterCodeInAndRoleAndAppLoginActiveTrue(
            listOf(costCenterCode), UserRole.LEADER
        )
        return leaders.firstOrNull()
    }

    /**
     * 키워드 필터링 (거래처명, 주소, 거래처코드)
     */
    private fun matchesKeyword(keyword: String?, accountName: String, account: Account?): Boolean {
        if (keyword.isNullOrBlank()) return true
        val lowerKeyword = keyword.lowercase()
        val address = account?.address1 ?: ""
        val accountTypeCode = account?.abcTypeCode ?: ""
        return accountName.lowercase().contains(lowerKeyword) ||
            address.lowercase().contains(lowerKeyword) ||
            accountTypeCode.lowercase().contains(lowerKeyword)
    }

    /**
     * GPS 거리 검증 (Spec #585).
     *
     * 호출 전에 [com.otoki.powersales.schedule.policy.AbcExemptPolicy] 면제 평가가
     * 끝났음을 가정한다 (Spec #586). 따라서 본 메서드는 면제 분기를 수행하지 않는다.
     *
     * 1. 사원 현재 좌표 범위 검증 (lat ±90 / lng ±180) → 위반 시 [InvalidCoordsException]
     * 2. 거래처 위경도 (String?) → Double 파싱 실패/공백/범위 초과 시 [AccountCoordsMissingException]
     * 3. Haversine 으로 거리(m) 계산 후 임계값 비교 → 초과 시 [DistanceExceededException]
     *
     * Q4: 거리 값은 응답에 노출하지 않고 서버 로그에만 기록한다.
     */
    private fun validateDistance(userLat: Double, userLon: Double, account: Account?, employeeId: Long) {
        // 1. 사원 현재 위치 좌표 범위 검증
        if (userLat !in LAT_MIN..LAT_MAX || userLon !in LNG_MIN..LNG_MAX) {
            throw InvalidCoordsException()
        }

        // 2. 거래처 위경도 파싱 (누락/공백/파싱실패/범위초과 → 등록 거부)
        val coords = AccountCoordinateParser.parse(account?.latitude, account?.longitude)
        if (coords is AccountCoordinateParser.Coords.Missing) {
            throw AccountCoordsMissingException()
        }
        coords as AccountCoordinateParser.Coords.Valid

        // 3. Haversine 거리 계산 (m 단위)
        val distanceKm = GeoUtils.calculateDistance(userLat, userLon, coords.latitude, coords.longitude)
        val distanceMeters = distanceKm * 1000.0
        val thresholdMeters = attendanceProperties.gpsThresholdMeters

        if (distanceMeters > thresholdMeters) {
            log.info(
                "ATT_GPS_DISTANCE_EXCEEDED employeeId={} accountId={} distanceMeters={} thresholdMeters={}",
                employeeId, account?.id, distanceMeters, thresholdMeters
            )
            throw DistanceExceededException()
        }

        log.debug(
            "ATT_GPS_DISTANCE_OK employeeId={} accountId={} distanceMeters={} thresholdMeters={}",
            employeeId, account?.id, distanceMeters, thresholdMeters
        )
    }
}
