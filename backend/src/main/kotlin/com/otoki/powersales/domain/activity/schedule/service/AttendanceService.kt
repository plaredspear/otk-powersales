package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.platform.common.dto.response.AccountInfo
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.platform.common.dto.response.AccountListResponse
import com.otoki.powersales.platform.common.enums.WorkingCategory1
import com.otoki.powersales.platform.common.enums.WorkingCategory2
import com.otoki.powersales.platform.common.enums.WorkingCategory3
import com.otoki.powersales.platform.common.enums.WorkingCategory5
import com.otoki.powersales.platform.common.enums.WorkingType
import com.otoki.powersales.domain.activity.promotion.enums.ProfessionalPromotionTeamType
import com.otoki.powersales.platform.common.util.GeoUtils
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.platform.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.domain.activity.safetycheck.entity.SafetyCheckSubmission
import com.otoki.powersales.domain.activity.safetycheck.repository.SafetyCheckSubmissionRepository
import com.otoki.powersales.domain.activity.schedule.attendance.AttendanceRegisterRequest
import com.otoki.powersales.domain.activity.schedule.attendance.AttendanceRegistrar
import com.otoki.powersales.domain.activity.schedule.config.AttendanceProperties
import com.otoki.powersales.domain.activity.schedule.dto.response.AttendanceRegisterResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.AttendanceStatusItem
import com.otoki.powersales.domain.activity.schedule.dto.response.AttendanceStatusResponse
import com.otoki.powersales.domain.activity.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.domain.activity.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.domain.activity.schedule.enums.AttendanceType
import com.otoki.powersales.domain.activity.schedule.exception.AccountCoordsMissingException
import com.otoki.powersales.domain.activity.schedule.exception.AlreadyRegisteredException
import com.otoki.powersales.domain.activity.schedule.exception.AttendanceDayOffConflictException
import com.otoki.powersales.domain.activity.schedule.exception.AttendanceDualBranchException
import com.otoki.powersales.domain.activity.schedule.exception.AttendanceTargetConflictException
import com.otoki.powersales.domain.activity.schedule.exception.AttendanceTargetRequiredException
import com.otoki.powersales.domain.activity.schedule.exception.AttendanceTimeExceededException
import com.otoki.powersales.domain.activity.schedule.exception.DisplayAttendanceDuplicateException
import com.otoki.powersales.domain.activity.schedule.exception.DisplayScheduleNotAssignedException
import com.otoki.powersales.domain.activity.schedule.exception.DisplayScheduleNotConfirmedException
import com.otoki.powersales.domain.activity.schedule.exception.DisplayScheduleNotFoundException
import com.otoki.powersales.domain.activity.schedule.exception.DisplayScheduleOutOfRangeException
import com.otoki.powersales.domain.activity.schedule.exception.DistanceExceededException
import com.otoki.powersales.domain.activity.schedule.exception.EventAttendanceDuplicateException
import com.otoki.powersales.domain.activity.schedule.exception.EventScheduleDateMismatchException
import com.otoki.powersales.domain.activity.schedule.exception.EventScheduleNotAssignedException
import com.otoki.powersales.domain.activity.schedule.exception.EventScheduleNotFoundException
import com.otoki.powersales.domain.activity.schedule.exception.InvalidCoordsException
import com.otoki.powersales.domain.activity.schedule.exception.SafetyCheckRequiredException
import com.otoki.powersales.domain.activity.schedule.exception.TeamMemberScheduleNotFoundException
import com.otoki.powersales.domain.activity.schedule.policy.AbcExemptPolicy
import com.otoki.powersales.domain.activity.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.domain.activity.schedule.util.AccountCoordinateParser
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
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
    private val attendanceRegistrar: AttendanceRegistrar,
    private val adminMonthlyIntegrationService: AdminMonthlyIntegrationService,
    private val attendanceProperties: AttendanceProperties,
    private val teamMemberScheduleOwnerResolver: TeamMemberScheduleOwnerResolver,
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
                workCategory = tms.workingCategory1?.displayName ?: "",
                workCategory2 = tms.workingCategory2?.displayName,
                workCategory3 = tms.workingCategory3?.displayName,
                address = account?.address1,
                latitude = account?.latitude?.toDoubleOrNull(),
                longitude = account?.longitude?.toDoubleOrNull(),
                isRegistered = tms.attendanceLog != null,
                source = "schedule"
            )
        }

        // 진열마스터 기반 거래처 조회 (confirmed=true, 오늘 유효, 미삭제)
        val validMasters = displayWorkScheduleRepository.findConfirmedValidByEmployeeAndDate(employee.id, today)

        // 이미 TMS 에 "진열(DISPLAY)" 일정으로 잡힌 거래처만 마스터 항목에서 제외한다.
        // (진열 출근 등록 시 마스터→TMS DISPLAY row 가 생성되므로, 그 거래처의 마스터를 다시 더하면 진열이 2번 노출됨.)
        // dedup 키를 account.id 단독으로 두면 같은 거래처에 행사(EVENT)만 TMS 에 있고 진열은 마스터에만 있는 경우에도
        // 진열 마스터가 제거되어, 홈 화면(행사+진열 2건)과 건수가 어긋난다(출근등록은 행사 1건). 진열로 잡힌
        // 거래처로 dedup 을 한정해 같은 거래처의 행사·진열 공존을 보존한다.
        val displayScheduledAccountIds = teamMemberSchedules
            .filter { it.workingCategory1 == WorkingCategory1.DISPLAY }
            .mapNotNull { it.account?.id }
            .toSet()

        val masterAccountInfos = validMasters.mapNotNull { master ->
            val account = master.account
            val accountId = account?.id ?: return@mapNotNull null
            if (accountId in displayScheduledAccountIds) return@mapNotNull null

            val accountName = account.name ?: ""
            if (!matchesKeyword(keyword, accountName, account)) return@mapNotNull null

            AccountInfo(
                displayWorkScheduleId = master.id,
                accountId = accountId,
                accountName = accountName,
                accountTypeCode = account.abcTypeCode,
                workCategory = "진열",
                workCategory2 = master.typeOfWork5?.displayName,
                workCategory3 = master.typeOfWork3?.displayName,
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
     * scheduleId / displayWorkScheduleId / eventScheduleId 중 하나를 전달받아 처리:
     * - scheduleId: 기존 방식 (TeamMemberSchedule 직접 조회 — 일반/사전 배정 일정)
     * - displayWorkScheduleId: 진열마스터 기반으로 TeamMemberSchedule 동적 생성 후 출근 처리 (Spec #587 P1-B)
     * - eventScheduleId: 행사 분기 — 사전 배정된 TMS row UPDATE (Spec #587 P2-B)
     *
     * 분기 우선순위/배타: displayWorkScheduleId + eventScheduleId 동시 → ATT_DUAL_BRANCH.
     * scheduleId 와 displayWorkScheduleId/eventScheduleId 동시 → ATT_TARGET_CONFLICT.
     */
    @Transactional
    fun register(
        userId: Long,
        scheduleId: Long?,
        displayWorkScheduleId: Long?,
        eventScheduleId: Long?,
        latitude: Double,
        longitude: Double,
        workType: String?
    ): AttendanceRegisterResponse {
        // Spec #587 P2-B §1.1 — 진열/행사 동시 입력 거부
        if (displayWorkScheduleId != null && eventScheduleId != null) {
            throw AttendanceDualBranchException()
        }

        // 상호 배타 검증 — scheduleId 와 다른 분기 식별자 동시는 기존 conflict
        val nonNullCount = listOf(scheduleId, displayWorkScheduleId, eventScheduleId).count { it != null }
        if (nonNullCount == 0) {
            throw AttendanceTargetRequiredException()
        }
        if (scheduleId != null && (displayWorkScheduleId != null || eventScheduleId != null)) {
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
        if (teamMemberScheduleRepository.existsByEmployeeAndWorkingDateAndWorkingType(employee, today, WorkingType.ALT_HOLIDAY)) {
            throw AttendanceDayOffConflictException()
        }

        // 1. 안전점검 완료 여부 검증
        if (!safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(employee.id, today)) {
            throw SafetyCheckRequiredException()
        }

        // 2. 스케줄 결정 — scheduleId / displayWorkScheduleId / eventScheduleId 분기
        val resolved = when {
            scheduleId != null -> resolveByScheduleId(scheduleId)
            displayWorkScheduleId != null -> resolveByDisplayWorkSchedule(displayWorkScheduleId, employee, today)
            else -> resolveByEventSchedule(eventScheduleId!!, employee, today)
        }
        val teamMemberSchedule = resolved.schedule
        val displayMaster = resolved.displayMaster
        val isEventBranch = resolved.isEventBranch

        // 3. 중복 등록 검증 — 분기별 적절한 예외 throw (Spec #789 — id-FK 가드)
        if (teamMemberSchedule.attendanceLog != null) {
            when {
                displayMaster != null -> throw DisplayAttendanceDuplicateException()
                isEventBranch -> throw EventAttendanceDuplicateException()
                else -> throw AlreadyRegisteredException()
            }
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

        // 5. SafetyCheckSubmission 조회 + 출근 등록 요청 구성 + 등록
        val safetyCheckSubmission = safetyCheckSubmissionRepository
            .findByEmployeeIdAndWorkingDate(employee.id, today)
            .orElse(null)

        // Spec #587 §1.6/§1.5 — 분기별 출근종류 결정 (attendance_log 적재용 — resolve 단계 결과로 확정)
        val attendanceType = when {
            displayMaster != null -> AttendanceType.DISPLAY
            isEventBranch -> AttendanceType.EVENT
            else -> AttendanceType.REGULAR
        }

        // reason(출근 사유)은 신규 모바일 등록 API 에 입력 필드가 없어 항상 null (레거시 home.jsp #reason textarea 미재현).
        val savedLog = attendanceRegistrar.register(
            AttendanceRegisterRequest(
                employeeId = employee.id,
                accountId = account?.id,
                attendanceType = attendanceType,
            )
        )
        // 백링크는 bulk UPDATE 가 아니라 managed entity 에 직접 세팅한다 — bulk 는 persistence context 를
        // 우회하므로, 이후 stampLegacyWorkReportMeta/refreshIntegration 이 dirty 로 만든 entity 의
        // 전체 컬럼 flush 가 stale null 로 백링크를 덮어써 고아 출근로그 + 미등록 표시가 발생했다.
        teamMemberSchedule.attendanceLog = savedLog

        // 6. 후속 처리: SafetyCheckSubmission.completeWorkYn = 'Y' + TMS 안전점검 stamp
        if (safetyCheckSubmission != null) {
            safetyCheckSubmission.completeWorkYn = "Y"
            safetyCheckSubmissionRepository.save(safetyCheckSubmission)
            applySafetyCheckStamp(teamMemberSchedule, safetyCheckSubmission)
        }

        // 7. 레거시 WorkReport TMS 메타 stamp + 월별여사원 통합일정(환산 일정) 재집계
        // SF 레거시 동등: IF_REST_MOBILE_WorkReport 가 upsert 시마다 CostCenterCode/전문판촉팀을 TMS 에
        // 재기록(cls:89-90, 107-112)하고, TeamMemberScheduleTrigger 가 beforeInsert(진열 신규)·
        // beforeUpdate(행사/기존 일정 출근) 양쪽에서 updateMonthlyFemaleEmployeeIntegrationSchedule 를 발화 —
        // 사원+월 전체 조합 재집계 (같은 날 다른 거래처의 1/N 변화 포함).
        stampLegacyWorkReportMeta(teamMemberSchedule, employee)
        adminMonthlyIntegrationService.refreshIntegration(
            employeeId = employee.id,
            yearMonth = YearMonth.from(today)
        )

        // 8. 출근 현황 집계
        val todayTeamMemberSchedules = teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(employee.id, today)
        val totalCount = todayTeamMemberSchedules.size
        val registeredCount = todayTeamMemberSchedules.count { it.attendanceLog != null || it.id == teamMemberSchedule.id }

        // Spec #587 §1.6/§1.5 — attendanceType 은 step 5 (등록 요청 구성) 에서 이미 결정됨 (응답 필드에 재사용)

        return AttendanceRegisterResponse(
            scheduleId = teamMemberSchedule.id,
            accountName = account?.name ?: "",
            workType = workType ?: teamMemberSchedule.workingType?.displayName,
            distanceKm = 0.0, // Spec #585 Q4: 실제 거리는 응답에 노출하지 않음 (서버 로그에만 기록)
            totalCount = totalCount,
            registeredCount = registeredCount,
            gpsSkipped = exemptResult.skipped, // Spec #586: ABC 면제 정책 적용 여부
            gpsSkipReason = exemptResult.reason,
            attendanceType = attendanceType,
            // 진열 분기 메타 (Spec #587 P1-B §1.6)
            displayWorkScheduleId = displayMaster?.id,
            scheduleStartDate = displayMaster?.startDate,
            scheduleEndDate = displayMaster?.endDate,
            // 행사 분기 메타 (Spec #587 P2-B §1.5)
            eventScheduleId = if (isEventBranch) teamMemberSchedule.id else null,
            scheduleWorkingDate = if (isEventBranch) teamMemberSchedule.workingDate else null,
            promotionEmployeeId = if (isEventBranch) teamMemberSchedule.promotionEmployee?.id else null,
        )
    }

    /**
     * 조장 대리출근 등록 (레거시 mngDaily `addScheduleProc` 동등).
     *
     * 본인 출근 등록([register])과의 차이:
     * - **GPS 거리 검증 없음** (조장은 현장에 없음 — 레거시 addScheduleProc 동일).
     * - 안전점검 완료 검증 대상이 **대상 여사원([targetEmployee])** 의 당일 안전점검.
     * - 시간 제한(서울 17:00) 은 서버에서 동일하게 재검증.
     *
     * 진열=displayWorkScheduleId(마스터→TMS 동적 생성/재사용), 행사·기배정=scheduleId 분기.
     * 조장 권한/팀원 검증은 호출부([LeaderScheduleService.registerProxyAttendance]) 책임.
     */
    @Transactional
    fun registerProxy(
        targetEmployee: Employee,
        scheduleId: Long?,
        displayWorkScheduleId: Long?,
    ): AttendanceRegisterResponse {
        // 타깃 식별자 배타 검증
        val nonNullCount = listOf(scheduleId, displayWorkScheduleId).count { it != null }
        if (nonNullCount == 0) throw AttendanceTargetRequiredException()
        if (nonNullCount > 1) throw AttendanceTargetConflictException()

        // 시간 제한: 17시 이후 차단 (서버 재검증)
        val now = LocalTime.now(clock.withZone(SEOUL_ZONE))
        if (!now.isBefore(REGISTRATION_DEADLINE)) {
            throw AttendanceTimeExceededException()
        }

        val today = LocalDate.now()

        // 대휴 충돌 검증
        if (teamMemberScheduleRepository.existsByEmployeeAndWorkingDateAndWorkingType(targetEmployee, today, WorkingType.ALT_HOLIDAY)) {
            throw AttendanceDayOffConflictException()
        }

        // 대상 여사원 안전점검 완료 검증 (레거시 surveyCnt='Y')
        if (!safetyCheckSubmissionRepository.existsByEmployeeIdAndWorkingDate(targetEmployee.id, today)) {
            throw SafetyCheckRequiredException()
        }

        // 스케줄 해석 (진열=마스터 기반 동적 생성, 행사·기배정=scheduleId)
        val resolved = when {
            scheduleId != null -> resolveByScheduleId(scheduleId)
            else -> resolveByDisplayWorkSchedule(displayWorkScheduleId!!, targetEmployee, today)
        }
        val teamMemberSchedule = resolved.schedule
        val displayMaster = resolved.displayMaster

        // scheduleId 분기는 대상 여사원 본인 스케줄인지 확인 (타인 스케줄 등록 차단)
        if (scheduleId != null && teamMemberSchedule.employee?.id != targetEmployee.id) {
            throw TeamMemberScheduleNotFoundException()
        }

        // 중복 등록 검증
        if (teamMemberSchedule.attendanceLog != null) {
            if (displayMaster != null) throw DisplayAttendanceDuplicateException()
            else throw AlreadyRegisteredException()
        }

        // GPS 거리 검증 없음 (대리등록)

        // 안전점검 데이터 기반 출근 등록 (본인 등록과 동일 경로)
        val safetyCheckSubmission = safetyCheckSubmissionRepository
            .findByEmployeeIdAndWorkingDate(targetEmployee.id, today)
            .orElse(null)

        // 대리등록 분기: 진열=DISPLAY / scheduleId(행사·기배정)=REGULAR (본인 등록과 동일 규칙, 행사 분기 없음)
        val attendanceType = if (displayMaster != null) AttendanceType.DISPLAY else AttendanceType.REGULAR

        val savedLog = attendanceRegistrar.register(
            AttendanceRegisterRequest(
                employeeId = targetEmployee.id,
                accountId = teamMemberSchedule.account?.id,
                attendanceType = attendanceType,
            )
        )
        // 백링크는 managed entity 에 직접 세팅 (본인 등록과 동일 — bulk UPDATE 는 이후 dirty flush 에 덮여 유실).
        teamMemberSchedule.attendanceLog = savedLog

        // 후속 처리: completeWorkYn='Y' + TMS 안전점검 stamp (본인 등록과 동일)
        if (safetyCheckSubmission != null) {
            safetyCheckSubmission.completeWorkYn = "Y"
            safetyCheckSubmissionRepository.save(safetyCheckSubmission)
            applySafetyCheckStamp(teamMemberSchedule, safetyCheckSubmission)
        }

        // 레거시 WorkReport TMS 메타 stamp + 월별여사원 통합일정(환산 일정) 재집계
        // 대리출근도 레거시 mngDaily → SF WorkReport 동일 REST 경로 (EmployeeController.java:858) —
        // 본인 출근과 동일하게 사원+월 전체 조합을 재집계한다.
        val account = teamMemberSchedule.account
        stampLegacyWorkReportMeta(teamMemberSchedule, targetEmployee)
        adminMonthlyIntegrationService.refreshIntegration(
            employeeId = targetEmployee.id,
            yearMonth = YearMonth.from(today)
        )

        // 출근 현황 집계
        val todayTeamMemberSchedules = teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(targetEmployee.id, today)
        val totalCount = todayTeamMemberSchedules.size
        val registeredCount = todayTeamMemberSchedules.count { it.attendanceLog != null || it.id == teamMemberSchedule.id }

        return AttendanceRegisterResponse(
            scheduleId = teamMemberSchedule.id,
            accountName = account?.name ?: "",
            workType = teamMemberSchedule.workingType?.displayName,
            distanceKm = 0.0,
            totalCount = totalCount,
            registeredCount = registeredCount,
            gpsSkipped = true, // 대리등록은 GPS 미적용
            attendanceType = attendanceType,
            displayWorkScheduleId = displayMaster?.id,
            scheduleStartDate = displayMaster?.startDate,
            scheduleEndDate = displayMaster?.endDate,
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
                workCategory = teamMemberSchedule.workingCategory1?.displayName ?: "",
                status = if (teamMemberSchedule.attendanceLog != null) "REGISTERED" else "PENDING"
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
     * 분기 결정 결과.
     * - `displayMaster`: 진열 분기일 때만 NOT NULL (Spec #587 P1-B §1.6)
     * - `isEventBranch`: 행사 분기일 때만 true (Spec #587 P2-B §1.5)
     */
    private data class ResolveResult(
        val schedule: TeamMemberSchedule,
        val newlyCreated: Boolean,
        val displayMaster: DisplayWorkSchedule? = null,
        val isEventBranch: Boolean = false,
    )

    /**
     * 기존 방식: scheduleId로 TeamMemberSchedule 직접 조회
     */
    private fun resolveByScheduleId(scheduleId: Long): ResolveResult {
        val tms = teamMemberScheduleRepository.findById(scheduleId)
            .orElseThrow { TeamMemberScheduleNotFoundException() }
        return ResolveResult(schedule = tms, newlyCreated = false)
    }

    /**
     * 진열마스터 기반 (Spec #587 P1-B §1.2): DisplayWorkSchedule 검증 후 TeamMemberSchedule 동적 생성/조회.
     * 검증 순서: 마스터 존재 → 본인 할당 → 일자 범위 → 중복 (working_category3) → 재사용 / 새 row INSERT.
     */
    private fun resolveByDisplayWorkSchedule(
        displayWorkScheduleId: Long,
        employee: Employee,
        today: LocalDate
    ): ResolveResult {
        // step 1: 마스터 존재 (ATT_DISPLAY_SCHEDULE_NOT_FOUND)
        val master = displayWorkScheduleRepository.findById(displayWorkScheduleId)
            .orElseThrow { DisplayScheduleNotFoundException() }
        if (master.isDeleted == true) {
            throw DisplayScheduleNotFoundException()
        }
        if (master.confirmed != true) {
            throw DisplayScheduleNotConfirmedException()
        }

        // step 2: 본인 할당 검증 (ATT_DISPLAY_SCHEDULE_NOT_ASSIGNED) — Spec #587 P1-B Q4 보안 강화
        if (master.employee?.id != employee.id) {
            throw DisplayScheduleNotAssignedException()
        }

        // step 3: 일자 범위 검증 (ATT_DISPLAY_SCHEDULE_DATE_OUT_OF_RANGE)
        val startDate = master.startDate
        val endDate = master.endDate
        if (startDate != null && today.isBefore(startDate)) {
            throw DisplayScheduleOutOfRangeException()
        }
        if (endDate != null && today.isAfter(endDate)) {
            throw DisplayScheduleOutOfRangeException()
        }

        val account = master.account ?: throw DisplayScheduleNotFoundException()

        // step 4: 중복 검증 — 동일 사원+거래처+오늘 일정이 있으면 그 row 재사용 (기존 정책 유지).
        // 단, 다른 거래처라도 동일 사원·날짜의 근무유형3(고정/격고/순회) 양립 매트릭스에 위배되면 거부.
        // 레거시 TeamMemberScheduleTriggerHandler.checkDuplicatedSchedule (insert 경로) 동등 —
        // 단순 "동일 유형 1건이라도 있으면 거부" 가 아니라 유형별 허용 개수를 반영한다.
        val existing = teamMemberScheduleRepository.findByEmployeeAndAccountAndWorkingDate(employee, account, today)
        val masterTypeOfWork3 = master.typeOfWork3
        if (existing == null && masterTypeOfWork3 != null) {
            // typeOfWork3 와 workingCategory3 는 picklist 옵션값(고정/격고/순회) 동일 — displayName 으로 변환 후 매칭
            val workingCategory3 = WorkingCategory3.fromDisplayNameOrNull(masterTypeOfWork3.displayName)
            if (workingCategory3 != null && violatesWorkingCategory3Matrix(employee, today, workingCategory3)) {
                throw DisplayAttendanceDuplicateException()
            }
        }
        if (existing != null) {
            return ResolveResult(schedule = existing, newlyCreated = false, displayMaster = master)
        }

        // step 5: 새 TMS row INSERT — 마스터→TMS 메타 카피 (Spec #587 P1-B §1.3)
        // owner 는 대상 직원의 소속 조장 User (레거시 TeamMemberScheduleTriggerHandler.insertOwner 동등).
        val teamLeader = findTeamLeader(employee.costCenterCode)
        val ownerUser = teamMemberScheduleOwnerResolver.resolveOwner(employee)
        val newSchedule = TeamMemberSchedule(
            employee = employee,
            account = account,
            workingDate = today,
            workingType = WorkingType.WORK,
            workingCategory1 = WorkingCategory1.DISPLAY,
            workingCategory2 = WorkingCategory2.fromDisplayNameOrNull(mapTypeOfWork5ToCategory2(master.typeOfWork5?.displayName)),
            workingCategory3 = master.typeOfWork3?.displayName?.let { WorkingCategory3.fromDisplayNameOrNull(it) },
            // 레거시 IF_REST_MOBILE_WorkReport 진열 분기 (cls:102-103) — 마스터 근무형태4/5 를 TMS 에 카피.
            // MFEIS 집계 키(ExternalKey) 컴포넌트라 출근 row 에 반드시 실려야 레거시 집계와 일치한다.
            secondWorkType = master.typeOfWork4?.displayName,
            workingCategory5 = WorkingCategory5.fromDisplayNameOrNull(master.typeOfWork5?.displayName),
            teamLeader = teamLeader,
            ownerUser = ownerUser,
            displayWorkSchedule = master,
        )
        val saved = teamMemberScheduleRepository.save(newSchedule)
        return ResolveResult(schedule = saved, newlyCreated = true, displayMaster = master)
    }

    /**
     * 근무유형3(고정/격고/순회) 양립 매트릭스 검증 — 레거시 SF
     * `TeamMemberScheduleTriggerHandler.checkDuplicatedSchedule` 의 신규생성일정 체크(insert 경로) 동등.
     *
     * 동일 사원·날짜의 기존 일정 건수를 유형별로 집계(거래처/출근여부 무관)한 뒤, 등록하려는 유형에 따라:
     *  - 고정: 기존 고정 ≥ 1, 또는 기존 격고 ≥ 1 / 순회 ≥ 1
     *  - 격고: 기존 격고 ≥ 2, 또는 기존 고정 ≥ 1, 또는 (격고 ≥ 1 AND 순회 ≥ 1)
     *  - 순회: 기존 고정 ≥ 1, 또는 기존 격고 ≥ 2
     * 위 조건에 해당하면 true(중복 거부). 레거시 update 전용 bypass 분기는 출근(insert) 경로에 무관하여 제외.
     */
    private fun violatesWorkingCategory3Matrix(
        employee: Employee,
        today: LocalDate,
        category3: WorkingCategory3
    ): Boolean {
        val fixed = teamMemberScheduleRepository
            .countByEmployeeAndWorkingDateAndWorkingCategory3(employee, today, WorkingCategory3.FIXED)
        val alternate = teamMemberScheduleRepository
            .countByEmployeeAndWorkingDateAndWorkingCategory3(employee, today, WorkingCategory3.ALTERNATE)
        val patrol = teamMemberScheduleRepository
            .countByEmployeeAndWorkingDateAndWorkingCategory3(employee, today, WorkingCategory3.PATROL)

        return when (category3) {
            WorkingCategory3.FIXED -> fixed >= 1 || alternate >= 1 || patrol >= 1
            WorkingCategory3.ALTERNATE -> alternate >= 2 || fixed >= 1 || (alternate >= 1 && patrol >= 1)
            WorkingCategory3.PATROL -> fixed >= 1 || alternate >= 2
        }
    }

    /**
     * 행사 분기 (Spec #587 P2-B §1.2): 사전 배정된 TMS row 를 직접 조회하고 본인/일자/미출근 검증을 수행한다.
     * 검증 순서: 일정 존재 → 본인 할당 → 일자 일치 → 미출근 (attendance_log IS NULL).
     * 미출근 검증은 step 3 의 공통 attendanceLog 가드에서 수행 (분기별 적절한 예외 throw). (Spec #789)
     */
    private fun resolveByEventSchedule(
        eventScheduleId: Long,
        employee: Employee,
        today: LocalDate
    ): ResolveResult {
        // step 1: TMS 존재 + is_deleted=false (ATT_EVENT_SCHEDULE_NOT_FOUND)
        val tms = teamMemberScheduleRepository.findById(eventScheduleId)
            .orElseThrow { EventScheduleNotFoundException() }
        if (tms.isDeleted == true) {
            throw EventScheduleNotFoundException()
        }

        // step 2: 본인 할당 (ATT_EVENT_SCHEDULE_NOT_ASSIGNED)
        if (tms.employee?.id != employee.id) {
            throw EventScheduleNotAssignedException()
        }

        // step 3: 일자 일치 (ATT_EVENT_SCHEDULE_DATE_MISMATCH)
        if (tms.workingDate != today) {
            throw EventScheduleDateMismatchException()
        }

        return ResolveResult(schedule = tms, newlyCreated = false, isEventBranch = true)
    }

    /**
     * typeOfWork5 → workingCategory2 매핑
     * "상시" → "전담". "임시" 는 SF picklist `DKRetail__WorkingCategory2__c` 옵션 미정의 도메인 (sf-align-teammemberschedule #762) — NULL 반환.
     */
    private fun mapTypeOfWork5ToCategory2(typeOfWork5: String?): String? {
        return when (typeOfWork5) {
            "상시" -> "전담"
            "임시" -> null
            else -> typeOfWork5
        }
    }

    /**
     * 사원의 조직코드 기반 조장 조회
     */
    /**
     * 레거시 `IF_REST_MOBILE_WorkReport.handleInboundData` 의 TMS 메타 재기록 동등 (cls:89-90, 107-112).
     *
     * 출근 upsert 시마다 사원의 현재 CostCenterCode 와 전문판촉팀을 TMS 에 stamp 한다. 두 값 모두
     * MFEIS 집계 키(ExternalKey)·당월근무일수 그룹핑의 컴포넌트라, 출근 row 에 실려야 레거시 집계와 일치한다.
     * 전문판촉팀 미지정은 레거시가 `ProfessionalPromotionTeam__c = '일반'` 문자열로 저장했으므로
     * (신규 Employee 는 미지정=null) '일반' 으로 stamp 해 마이그레이션 row 의 키 포맷과 정합시킨다.
     */
    private fun stampLegacyWorkReportMeta(teamMemberSchedule: TeamMemberSchedule, employee: Employee) {
        // managed entity — @Transactional dirty checking 으로 flush (별도 save 불필요)
        teamMemberSchedule.costCenterCode = employee.costCenterCode
        teamMemberSchedule.professionalPromotionTeam =
            employee.professionalPromotionTeam?.displayName
                ?: ProfessionalPromotionTeamType.GENERAL_DISPLAY_NAME
    }

    /**
     * 안전점검 제출 데이터를 TMS 에 stamp — managed entity 직접 반영.
     * bulk UPDATE 는 persistence context 를 우회해, 같은 트랜잭션에서 뒤이어 dirty 가 된 entity 의
     * flush(전체 컬럼 UPDATE)에 덮여 유실된다 ([AttendanceRegistrar] 문서 참조).
     */
    private fun applySafetyCheckStamp(
        teamMemberSchedule: TeamMemberSchedule,
        submission: SafetyCheckSubmission
    ) {
        teamMemberSchedule.equipment1 = submission.equipment1
        teamMemberSchedule.equipment2 = submission.equipment2
        teamMemberSchedule.equipment3 = submission.equipment3
        teamMemberSchedule.equipment4 = submission.equipment4
        teamMemberSchedule.equipment5 = submission.equipment5
        teamMemberSchedule.equipment6 = submission.equipment6
        teamMemberSchedule.equipment7 = submission.equipment7
        teamMemberSchedule.equipment8 = submission.equipment8
        teamMemberSchedule.equipment9 = submission.equipment9
        teamMemberSchedule.yesChkCnt = submission.yesCheckCount?.toDouble()
        teamMemberSchedule.noChkCnt = submission.noCheckCount?.toDouble()
        teamMemberSchedule.startTime = submission.startTime
        teamMemberSchedule.completeTime = submission.completeTime
        teamMemberSchedule.precaution = submission.precaution
        teamMemberSchedule.precautionChk = submission.precautionCheckCount?.toDouble()
        teamMemberSchedule.traversalFlag = submission.traversalFlag
    }

    private fun findTeamLeader(costCenterCode: String?): Employee? {
        if (costCenterCode.isNullOrBlank()) return null
        val leaders = employeeRepository.findByCostCenterCodeInAndRoleAndAppLoginActiveTrue(
            listOf(costCenterCode), AppAuthority.LEADER
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
     * 호출 전에 [AbcExemptPolicy] 면제 평가가
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
