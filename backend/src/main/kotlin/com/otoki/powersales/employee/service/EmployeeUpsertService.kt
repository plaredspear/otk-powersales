package com.otoki.powersales.employee.service

import com.otoki.powersales.common.repository.SystemCodeMasterRepository
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.entity.EmployeeOrigin
import com.otoki.powersales.employee.entity.Gender
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.employee.service.dto.EmployeeUpsertCommand
import com.otoki.powersales.employee.service.dto.EmployeeUpsertFailedRow
import com.otoki.powersales.employee.service.dto.EmployeeUpsertResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * 직원 마스터 UPSERT 도메인 서비스.
 *
 * ## 레거시 매핑
 * - 진입점: SAP 인바운드 어댑터 [com.otoki.powersales.sap.inbound.service.SapEmployeeMasterService]
 * - origin spec: #557 (SAP 직원 마스터 인바운드) + #579 (origin=MANUAL 보호) — 어댑터/도메인 분리: #635 P2-B
 *
 * ## 레거시 동작 요약
 * 1. 입력: `List<EmployeeUpsertCommand>` (UPSERT 키 [EmployeeUpsertCommand.employeeCode]).
 * 2. 캐시 빌드: [EmployeeRepository.findByEmployeeCodeIn] / status code map ([SystemCodeMasterRepository.findByGroupCodeIn] +
 *    `companyCode == "1000"` 필터).
 * 3. 행 단위 검증/변환/적용 (try/catch):
 *    - 필수값 (`employeeCode`/`employeeName`) 누락 → IllegalArgumentException → failures.
 *    - 기존 entity origin=MANUAL → 갱신 스킵 + [EmployeeUpsertResult.protectedManualCodes] 누적 (#579).
 *    - Gender 변환 (`"1"` → MALE / `"2"` → FEMALE / 그 외 → null), 날짜 (`yyyyMMdd`, `"00000000"` → null), birthdate 정규화,
 *      status code map lookup (매칭 시 detailCodeName, 미매칭 시 raw), LockingFlag (`"Y"` → false / 그 외 → true) 적용.
 * 4. 외부 호출: [EmployeeRepository.saveAll] (성공 행만 일괄). 행 검증 실패는 트랜잭션 롤백하지 않음.
 *    INSERT 시 `Employee.employeeInfo` 가 cascade=ALL 로 자동 영속화됨.
 *
 * ## 신규 차이 — 동등 (생략)
 *
 * cross-domain 의존: [SystemCodeMasterRepository] (status code map lookup) — Q3 옵션 1 정합 (lookup 용도 read-only).
 * 회사 코드 / status group code 는 도메인 자체 상수로 박는다 (`sap.*` 패키지 의존 0건 정합).
 */
@Service
class EmployeeUpsertService(
    private val employeeRepository: EmployeeRepository,
    private val systemCodeMasterRepository: SystemCodeMasterRepository
) {

    @Transactional
    fun upsert(commands: List<EmployeeUpsertCommand>): EmployeeUpsertResult {
        val employeeCodes = commands.mapNotNull { it.employeeCode?.takeIf { code -> code.isNotBlank() } }
        val cache: MutableMap<String, Employee> = if (employeeCodes.isEmpty()) {
            mutableMapOf()
        } else {
            employeeRepository.findByEmployeeCodeIn(employeeCodes.distinct())
                .associateBy { it.employeeCode }
                .toMutableMap()
        }

        val statusCodeMap: Map<String, String> = systemCodeMasterRepository
            .findByGroupCodeIn(listOf(STATUS_GROUP_CODE))
            .asSequence()
            .filter { it.companyCode == OTOKI_COMPANY_CODE }
            .mapNotNull { entry ->
                val name = entry.detailCodeName ?: return@mapNotNull null
                entry.detailCode to name
            }
            .toMap()

        val failures = mutableListOf<EmployeeUpsertFailedRow>()
        val toSave = mutableListOf<Employee>()
        val protectedManualCodes = mutableListOf<String>()

        commands.forEach { command ->
            try {
                val employeeCode = command.employeeCode?.takeIf { it.isNotBlank() }
                    ?: throw IllegalArgumentException("EmployeeCode 필수")
                val employeeName = command.employeeName?.takeIf { it.isNotBlank() }
                    ?: throw IllegalArgumentException("EmployeeName 필수")

                val existing = cache[employeeCode]
                if (existing != null && existing.origin == EmployeeOrigin.MANUAL) {
                    // Spec #579: origin=MANUAL 직원은 SAP 인바운드 갱신 대상에서 제외.
                    // 응답·카운트에 영향 없음. 어댑터가 별도 audit 으로 기록.
                    protectedManualCodes += employeeCode
                    return@forEach
                }

                val convertedGender = Gender.fromSapCode(command.gender)
                val startDate = parseDate(command.startDate, "StartDate")
                val endDate = parseDate(command.endDate, "EndDate")
                val birthDate = normalizeBirthdate(command.birthdate)
                val resolvedStatus = command.status?.let { statusCodeMap[it] ?: it }
                val appLoginActive = command.lockingFlag != "Y"

                val entity = existing?.also {
                    applyMutableFields(it, command, employeeName, convertedGender, startDate, endDate, birthDate, resolvedStatus, appLoginActive)
                } ?: Employee(employeeCode = employeeCode, name = employeeName).also {
                    applyMutableFields(it, command, employeeName, convertedGender, startDate, endDate, birthDate, resolvedStatus, appLoginActive)
                    cache[employeeCode] = it
                }
                toSave += entity
            } catch (ex: IllegalArgumentException) {
                failures += EmployeeUpsertFailedRow(command.employeeCode, ex.message ?: "INVALID")
            }
        }

        if (toSave.isNotEmpty()) {
            employeeRepository.saveAll(toSave)
        }

        return EmployeeUpsertResult(
            successCount = toSave.size,
            failureCount = failures.size,
            failures = failures,
            protectedManualCodes = protectedManualCodes
        )
    }

    private fun applyMutableFields(
        entity: Employee,
        command: EmployeeUpsertCommand,
        name: String,
        gender: Gender?,
        startDate: LocalDate?,
        endDate: LocalDate?,
        birthDate: String?,
        status: String?,
        appLoginActive: Boolean
    ) {
        entity.name = name
        entity.gender = gender
        entity.homePhone = command.homePhone
        entity.workPhone = command.workPhone
        entity.workEmail = command.workEmail
        entity.email = command.email
        entity.startDate = startDate
        entity.endDate = endDate
        entity.status = status
        entity.birthDate = birthDate
        entity.costCenterCode = command.orgCode
        entity.appLoginActive = appLoginActive
    }

    private fun parseDate(value: String?, fieldName: String): LocalDate? {
        val raw = value?.trim().orEmpty()
        if (raw.isEmpty() || raw == EMPTY_DATE) return null
        return try {
            LocalDate.parse(raw, DATE_FORMATTER)
        } catch (ex: DateTimeParseException) {
            throw IllegalArgumentException("$fieldName 형식 오류")
        }
    }

    private fun normalizeBirthdate(value: String?): String? {
        val raw = value?.trim().orEmpty()
        if (raw.isEmpty() || raw == EMPTY_DATE) return null
        return raw
    }

    companion object {
        private const val STATUS_GROUP_CODE = "H10010"
        // 어댑터 측 SapConstants.OTOKI_COMPANY_CODE 와 동일 값. 도메인이 sap 패키지 의존 0건 정합 위해 자체 박음.
        private const val OTOKI_COMPANY_CODE = "1000"
        private const val EMPTY_DATE = "00000000"
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    }
}
