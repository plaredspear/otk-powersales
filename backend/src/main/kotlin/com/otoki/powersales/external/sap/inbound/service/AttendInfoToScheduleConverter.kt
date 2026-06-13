package com.otoki.powersales.external.sap.inbound.service

import com.otoki.powersales.common.enums.WorkingType
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.external.sap.inbound.dto.attendance.ScheduleConversionSummary
import com.otoki.powersales.schedule.entity.AttendInfo
import com.otoki.powersales.schedule.enums.AttendType
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.schedule.service.TeamMemberScheduleOwnerResolver
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * SAP `attend_info` 인바운드 후처리: 연차류 코드를 `team_member_schedule` 일정으로 변환. (Spec #553)
 *
 * 호출 컨텍스트: [SapAttendInfoService] 가 청크 단위 INSERT REQUIRES_NEW 커밋 후 호출.
 * 본 메서드는 별도 REQUIRES_NEW 트랜잭션에서 동작하므로 변환 실패는 INSERT 트랜잭션에 영향을 주지 않는다.
 *
 * 처리 규칙:
 * - AttendType 필터: [AttendType.ANNUAL_LEAVE_CODES] (10/14/20/90/120/133)
 * - Status 필터: `N`/`n` (신규 생성), `Y`/`y` (해제 삭제)
 * - 직무 필터: `Status='N'` 분기에만 적용 — `판촉직`/`레이디직`/`OSC직` 만 통과 (레거시 동작 보존)
 * - WorkingType: 6개 코드 모두 `'연차'` 단일 변환
 * - 멱등성: `(employee, working_date, working_type='연차')` 응용 레벨 검증
 */
@Service
class AttendInfoToScheduleConverter(
    private val employeeRepository: EmployeeRepository,
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
    private val teamMemberScheduleOwnerResolver: TeamMemberScheduleOwnerResolver
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun convert(attendInfos: List<AttendInfo>): ScheduleConversionSummary {
        if (attendInfos.isEmpty()) return ScheduleConversionSummary.Companion.ZERO

        val (annualLeave, others) = attendInfos.partition { it.attendType in AttendType.ANNUAL_LEAVE_CODES }
        val skippedAttendType = others.size

        val targets = annualLeave.filter { it.status?.uppercase() in STATUS_TARGETS }
        if (targets.isEmpty()) {
            return ScheduleConversionSummary(
                convertedScheduleCount = 0,
                deletedScheduleCount = 0,
                skippedEmployeeNotFound = 0,
                skippedJobFilter = 0,
                skippedAttendTypeFilter = skippedAttendType,
                skippedIdempotent = 0
            )
        }

        val employeeCodes = targets.map { it.employeeCode }.distinct()
        val employeeMap = employeeRepository.findByEmployeeCodeIn(employeeCodes)
            .associateBy { it.employeeCode }

        // owner = 대상 직원의 소속 조장 User (레거시 TeamMemberScheduleTriggerHandler.insertOwner 동등).
        // 배치(인증 context 부재) 경로라 OwnerUserDefaultListener 의 생성자 채움이 동작하지 않으므로
        // 명시 해소가 없으면 owner 가 null 로 남는다.
        val ownerByCostCenterCode = teamMemberScheduleOwnerResolver
            .resolveOwnersByCostCenterCode(employeeMap.values)

        var converted = 0
        var deleted = 0
        var skippedEmpNotFound = 0
        var skippedJob = 0
        var skippedIdempotent = 0

        targets.forEach { record ->
            val employee = employeeMap[record.employeeCode]
            if (employee == null) {
                skippedEmpNotFound++
                return@forEach
            }
            val startDate = parseDate(record.startDate)
            val endDate = parseDate(record.endDate)
            if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
                log.warn(
                    "AttendInfo schedule conversion skip: invalid date range record_id={} start={} end={}",
                    record.id, record.startDate, record.endDate
                )
                return@forEach
            }

            when (record.status?.uppercase()) {
                STATUS_NEW -> {
                    if (employee.jobCode !in TARGET_JOB_CODES) {
                        skippedJob++
                        return@forEach
                    }
                    val toSave = mutableListOf<TeamMemberSchedule>()
                    var date: LocalDate = startDate
                    while (!date.isAfter(endDate)) {
                        if (teamMemberScheduleRepository.existsByEmployeeAndWorkingDateAndWorkingType(
                                employee, date, ANNUAL_LEAVE_TYPE
                            )
                        ) {
                            skippedIdempotent++
                        } else {
                            toSave += TeamMemberSchedule(
                                employee = employee,
                                workingDate = date,
                                workingType = ANNUAL_LEAVE_TYPE,
                                ownerUser = employee.costCenterCode?.let { ownerByCostCenterCode[it] }
                            )
                        }
                        date = date.plusDays(1)
                    }
                    if (toSave.isNotEmpty()) {
                        teamMemberScheduleRepository.saveAll(toSave)
                        converted += toSave.size
                    }
                }
                STATUS_DELETE -> {
                    val existing = teamMemberScheduleRepository
                        .findAllByEmployeeAndWorkingDateBetweenAndWorkingType(
                            employee, startDate, endDate, ANNUAL_LEAVE_TYPE
                        )
                    if (existing.isNotEmpty()) {
                        teamMemberScheduleRepository.deleteAll(existing)
                        deleted += existing.size
                    }
                }
            }
        }

        return ScheduleConversionSummary(
            convertedScheduleCount = converted,
            deletedScheduleCount = deleted,
            skippedEmployeeNotFound = skippedEmpNotFound,
            skippedJobFilter = skippedJob,
            skippedAttendTypeFilter = skippedAttendType,
            skippedIdempotent = skippedIdempotent
        )
    }

    private fun parseDate(value: String?): LocalDate? = try {
        value?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it, DATE_FORMAT) }
    } catch (_: DateTimeParseException) {
        null
    }

    companion object {
        private const val STATUS_NEW = "N"
        private const val STATUS_DELETE = "Y"
        private val STATUS_TARGETS = setOf(STATUS_NEW, STATUS_DELETE)
        private val ANNUAL_LEAVE_TYPE = WorkingType.ANNUAL_LEAVE
        private val TARGET_JOB_CODES = setOf("판촉직", "레이디직", "OSC직")
        private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}
