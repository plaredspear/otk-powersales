package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.activity.schedule.entity.Appointment
import com.otoki.powersales.domain.activity.schedule.repository.AppointmentRepository
import com.otoki.powersales.domain.activity.schedule.service.dto.AppointmentInsertCommand
import com.otoki.powersales.domain.activity.schedule.service.dto.AppointmentInsertFailedRow
import com.otoki.powersales.domain.activity.schedule.service.dto.AppointmentInsertResult
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * 인사발령 INSERT 도메인 서비스.
 *
 * ## 레거시 매핑
 * - 진입점: SAP 인바운드 어댑터 [com.otoki.powersales.external.sap.inbound.service.SapAppointmentService]
 * - origin spec: #562 (SAP 인사발령 인바운드) — 어댑터/도메인 분리: #635 P2-B
 *
 * ## 레거시 동작 요약
 * 1. 입력: `List<AppointmentInsertCommand>` — INSERT only, 멱등성 미보장 (후속 스펙 #567).
 * 2. cross-domain lookup: [EmployeeRepository.findByEmployeeCodeIn] (직원 매칭 — 매칭 실패는 행 진행 + `empCodeExist=false`).
 * 3. 레거시 정합 — 수신 필드 명시 필수/형식 검증으로 행을 거부하지 않는다 (레거시 IF_REST_SAP_Appointment 에
 *    검증 게이트 없음, 검증 없이 전 행 INSERT). EmployeeCode/JobCode 누락도 그대로 적재하고, AppointDate 는
 *    빈값/null/`00000000`/형식오류를 모두 `2999-12-31` 센티넬로 흡수 ([parseAppointDate]).
 * 4. 외부 호출: [AppointmentRepository.saveAll] (전 행 일괄).
 *    적재된 entity 는 [AppointmentInsertResult.savedAppointments] 로 return — 어댑터가 후처리 트리거 호출 시 사용.
 *
 * ## 신규 차이 — 동등 (생략)
 *
 * cross-domain 의존: [EmployeeRepository] (직원 매칭 lookup) — Q3 옵션 1 정합 (lookup 용도 read-only).
 * 후처리 트리거 (`AppointmentUserProfileUpdater`) 는 어댑터 잔류 — 도메인이 SAP 인입 후처리에 결합되지 않도록.
 */
@Service
class AppointmentInsertService(
    private val appointmentRepository: AppointmentRepository,
    private val employeeRepository: EmployeeRepository
) {

    @Transactional
    fun insert(commands: List<AppointmentInsertCommand>): AppointmentInsertResult {
        val empCodes = commands.mapNotNull { it.employeeCode?.takeIf { c -> c.isNotBlank() } }.distinct()
        val existingEmpCodes: Set<String> = if (empCodes.isEmpty()) {
            emptySet()
        } else {
            employeeRepository.findByEmployeeCodeIn(empCodes)
                .mapNotNull { it.employeeCode }
                .toHashSet()
        }

        val failures = mutableListOf<AppointmentInsertFailedRow>()
        val toSave = mutableListOf<Appointment>()

        commands.forEach { command ->
            // 레거시 IF_REST_SAP_Appointment 정합 — 수신 필드(EmployeeCode/JobCode 등)에 대한 명시적
            // 필수/형식 검증으로 행을 거부하는 코드가 레거시에 전무하다 (검증 없이 전 행 INSERT,
            // Database.insert allOrNone=false). EmployeeCode/JobCode 누락도 그대로 적재하고, AppointDate 는
            // Util.convertStringToDate 정합으로 빈값/null/00000000/형식오류를 모두 2999-12-31 센티넬로 흡수한다
            // (레거시는 형식 오류 시 Date.valueOf 예외가 배치 전체를 ERROR 로 만드는 우발 버그이나, 그 전체
            // 실패는 재현하지 않고 센티넬 흡수로 행 격리를 유지).
            val employeeCode = command.employeeCode?.takeIf { it.isNotBlank() }
            val jobCode = command.jobCode?.takeIf { it.isNotBlank() }
            val parsedAppointDate = parseAppointDate(command.appointDate)

            toSave += Appointment(
                employeeCode = employeeCode,
                empCodeExist = employeeCode != null && employeeCode in existingEmpCodes,
                afterOrgCode = command.afterOrgCode,
                afterOrgName = command.afterOrgName,
                jikchak = command.jikchak,
                jikwee = command.jikwee,
                jikgub = command.jikgub,
                workType = command.workType,
                manageType = command.manageType,
                jobCode = jobCode,
                workArea = command.workArea,
                jikjong = command.jikjong,
                appointDate = parsedAppointDate,
                jobName = command.jobName,
                ordDetailCode = command.ordDetailCode,
                ordDetailNode = command.ordDetailNode
            )
        }

        val saved = if (toSave.isNotEmpty()) {
            appointmentRepository.saveAll(toSave).toList()
        } else {
            emptyList()
        }

        return AppointmentInsertResult(
            successCount = saved.size,
            failureCount = failures.size,
            failures = failures,
            savedAppointments = saved
        )
    }

    /**
     * SF `Util.convertStringToDate` 정합 — 빈값/`null`/`"00000000"` → `2999-12-31` 센티넬 (발령일 미정).
     * 형식 오류(8자리 yyyyMMdd 가 아님) 도 행을 거부하지 않고 동일 센티넬로 흡수한다 — 레거시는 명시적
     * 형식 검증 게이트가 없고(검증 없이 전 행 INSERT), `Date.valueOf` 우발 예외로 배치 전체를 ERROR 로
     * 만드는 버그성 동작만 있으므로, 그 전체 실패를 재현하지 않고 센티넬 흡수로 행 격리를 유지한다.
     */
    private fun parseAppointDate(value: String?): LocalDate {
        val trimmed = value?.trim()
        if (trimmed.isNullOrEmpty() || trimmed == "00000000") return DATE_SENTINEL
        return try {
            LocalDate.parse(trimmed, DATE_FORMAT)
        } catch (_: DateTimeParseException) {
            DATE_SENTINEL
        }
    }

    companion object {
        private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        // SF Util.convertStringToDate 빈값/00000000 센티넬 (발령일 미정). 타 endpoint 와 동일 정합.
        private val DATE_SENTINEL: LocalDate = LocalDate.of(2999, 12, 31)
    }
}
