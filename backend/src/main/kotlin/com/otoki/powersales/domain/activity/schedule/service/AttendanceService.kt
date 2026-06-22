package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.platform.common.dto.response.AccountInfo
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.platform.common.dto.response.AccountListResponse
import com.otoki.powersales.platform.common.enums.WorkingCategory1
import com.otoki.powersales.platform.common.enums.WorkingCategory2
import com.otoki.powersales.platform.common.enums.WorkingCategory3
import com.otoki.powersales.platform.common.enums.WorkingType
import com.otoki.powersales.platform.common.util.GeoUtils
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.platform.auth.exception.EmployeeNotFoundException
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

        val request = AttendanceRegisterRequest(
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
        attendanceRegistrar.register(request)

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

        // 7. 출근 등록 후 월별여사원 통합일정(환산 일정) 갱신
        // SF 레거시 동등: TeamMemberScheduleTrigger 가 beforeInsert(진열 신규)·beforeUpdate(행사/기존 일정 출근)
        // 양쪽 모두에서 updateMonthlyFemaleEmployeeIntegrationSchedule 를 발화시킨다. 즉 신규 생성뿐 아니라
        // 기존 일정에 출근만 찍는 경우(CommuteLogId__c null→not null)에도 환산 재집계가 돌아야 한다.
        // 집계 모수 전제(CommuteLogId__c != null AND AccountId__c != null)는 여기서 attendanceLog 등록 완료 +
        // account != null 로 충족된다. (이전 newlyCreated 한정은 행사/기존 분기 환산 누락 버그였음)
        if (account != null) {
            adminMonthlyIntegrationService.refreshIntegration(
                employeeId = employee.id,
                accountId = account.id,
                yearMonth = YearMonth.from(today)
            )
        }

        // 8. 출근 현황 집계
        val todayTeamMemberSchedules = teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(employee.id, today)
        val totalCount = todayTeamMemberSchedules.size
        val registeredCount = todayTeamMemberSchedules.count { it.attendanceLog != null || it.id == teamMemberSchedule.id }

        // Spec #587 §1.6/§1.5 — 분기별 attendanceType 결정 + 응답 필드 채움
        val attendanceType = when {
            displayMaster != null -> AttendanceType.DISPLAY
            isEventBranch -> AttendanceType.EVENT
            else -> AttendanceType.REGULAR
        }

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

        val request = AttendanceRegisterRequest(
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
        attendanceRegistrar.register(request)

        // 후속 처리: completeWorkYn='Y' + TMS 안전점검 데이터 반영 (본인 등록과 동일)
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

        // 출근 등록 후 월별여사원 통합일정(환산 일정) 갱신
        // SF 레거시 동등: 신규 생성(진열)뿐 아니라 기존 일정 출근(CommuteLogId__c null→not null) 도 트리거 발화.
        // 대리출근도 동일 출근 등록 경로이므로 account != null 이면 환산 재집계한다.
        val account = teamMemberSchedule.account
        if (account != null) {
            adminMonthlyIntegrationService.refreshIntegration(
                employeeId = targetEmployee.id,
                accountId = account.id,
                yearMonth = YearMonth.from(today)
            )
        }

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
            attendanceType = if (displayMaster != null) AttendanceType.DISPLAY else AttendanceType.REGULAR,
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
        // 단, 다른 거래처에서 동일 working_category3 로 이미 등록된 일정이 있으면 거부 (Spec #587 P1-B Q6).
        val existing = teamMemberScheduleRepository.findByEmployeeAndAccountAndWorkingDate(employee, account, today)
        val masterTypeOfWork3 = master.typeOfWork3
        if (existing == null && masterTypeOfWork3 != null) {
            // typeOfWork3 와 workingCategory3 는 picklist 옵션값(고정/격고/순회) 동일 — displayName 으로 변환 후 매칭
            val workingCategory3 = WorkingCategory3.fromDisplayNameOrNull(masterTypeOfWork3.displayName)
            val duplicateInOtherAccount = workingCategory3 != null && teamMemberScheduleRepository
                .existsByEmployeeAndWorkingDateAndWorkingCategory3(employee, today, workingCategory3)
            if (duplicateInOtherAccount) {
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
            teamLeader = teamLeader,
            ownerUser = ownerUser,
            displayWorkSchedule = master,
        )
        val saved = teamMemberScheduleRepository.save(newSchedule)
        return ResolveResult(schedule = saved, newlyCreated = true, displayMaster = master)
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
