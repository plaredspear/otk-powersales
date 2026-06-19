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
 * 3. 행 단위 검증:
 *    - 필수값 (`employeeCode`/`jobCode`) 누락 → failures.
 *    - AppointDate (`yyyyMMdd`. 빈값/null/`00000000` → `2999-12-31` 센티넬) 형식 위반만 → failures.
 *    - 정상 행: 신규 [Appointment] 생성 (empCodeExist 매칭 결과 적용).
 * 4. 외부 호출: [AppointmentRepository.saveAll] (성공 행만 일괄). 행 검증 실패는 트랜잭션 롤백하지 않음.
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
            val employeeCode = command.employeeCode?.takeIf { it.isNotBlank() }
            val jobCode = command.jobCode?.takeIf { it.isNotBlank() }

            if (employeeCode == null) {
                failures += AppointmentInsertFailedRow(null, "EmployeeCode 필수")
                return@forEach
            }
            if (jobCode == null) {
                failures += AppointmentInsertFailedRow(employeeCode, "JobCode 필수")
                return@forEach
            }
            // 레거시 IF_REST_SAP_Appointment 정합 — AppointDate 는 Util.convertStringToDate 로 파싱.
            // 빈값/null/00000000 → 2999-12-31 센티넬 (발령일 미정), 형식 오류만 행 failure.
            val parsedAppointDate = parseAppointDate(command.appointDate)
            if (parsedAppointDate == null) {
                failures += AppointmentInsertFailedRow(
                    employeeCode + command.appointDate.orEmpty(),
                    "AppointDate YYYYMMDD 형식 오류: ${command.appointDate}"
                )
                return@forEach
            }

            toSave += Appointment(
                employeeCode = employeeCode,
                empCodeExist = employeeCode in existingEmpCodes,
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
     * 형식 오류(8자리 yyyyMMdd 가 아님) → null (행 failure).
     */
    private fun parseAppointDate(value: String?): LocalDate? {
        val trimmed = value?.trim()
        if (trimmed.isNullOrEmpty() || trimmed == "00000000") return DATE_SENTINEL
        return try {
            LocalDate.parse(trimmed, DATE_FORMAT)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    companion object {
        private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        // SF Util.convertStringToDate 빈값/00000000 센티넬 (발령일 미정). 타 endpoint 와 동일 정합.
        private val DATE_SENTINEL: LocalDate = LocalDate.of(2999, 12, 31)
    }
}
