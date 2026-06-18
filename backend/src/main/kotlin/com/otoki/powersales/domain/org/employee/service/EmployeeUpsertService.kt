package com.otoki.powersales.domain.org.employee.service

import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.enums.EmployeeOrigin
import com.otoki.powersales.domain.org.employee.enums.Gender
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.org.employee.service.dto.EmployeeUpsertCommand
import com.otoki.powersales.platform.common.repository.SystemCodeMasterRepository
import com.otoki.powersales.domain.org.employee.service.dto.EmployeeUpsertFailedRow
import com.otoki.powersales.domain.org.employee.service.dto.EmployeeUpsertResult
import com.otoki.powersales.user.event.EmployeeSnapshot
import com.otoki.powersales.user.event.EmployeesCreatedEvent
import com.otoki.powersales.user.repository.UserRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * 직원 마스터 UPSERT 도메인 서비스.
 *
 * ## 레거시 매핑
 * - 진입점: SAP 인바운드 어댑터 [com.otoki.powersales.external.sap.inbound.service.SapEmployeeMasterService]
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
 * ## User 행 자동 생성 (별도 트랜잭션, bulk)
 * Employee 신규 INSERT 집합을 [EmployeesCreatedEvent] **1건**으로 발행한다. User 행 생성은
 * [com.otoki.powersales.user.service.UserProvisioningService.handleEmployeesCreated]
 * 가 `AFTER_COMMIT + @Async` 로 수신해 별도 트랜잭션에서 **일괄** 수행한다.
 * SF 레거시 `IF_REST_SAP_EmployeeMaster.upsertUser(@future)` 동등 (N명을 future 1회 호출로 bulk 처리) —
 * User 생성 실패는 Employee 트랜잭션에 영향을 주지 않는다.
 *
 * cross-domain 의존: [SystemCodeMasterRepository] (status code map lookup) — Q3 옵션 1 정합 (lookup 용도 read-only).
 * 회사 코드 / status group code 는 도메인 자체 상수로 박는다 (`sap.*` 패키지 의존 0건 정합).
 */
@Service
class EmployeeUpsertService(
    private val employeeRepository: EmployeeRepository,
    private val systemCodeMasterRepository: SystemCodeMasterRepository,
    private val eventPublisher: ApplicationEventPublisher,
    private val userRepository: UserRepository,
) {

    @Transactional
    fun upsert(commands: List<EmployeeUpsertCommand>): EmployeeUpsertResult {
        val employeeCodes = commands.mapNotNull { it.employeeCode?.takeIf { code -> code.isNotBlank() } }
        val cache: MutableMap<String, Employee> = if (employeeCodes.isEmpty()) {
            mutableMapOf()
        } else {
            employeeRepository.findByEmployeeCodeIn(employeeCodes.distinct())
                .filter { it.employeeCode != null }
                .associateBy { it.employeeCode!! }
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
        val newEmployees = mutableListOf<Employee>()
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
                    newEmployees += it
                }
                applyLockingFlagException(entity)
                toSave += entity
            } catch (ex: IllegalArgumentException) {
                failures += EmployeeUpsertFailedRow(command.employeeCode, ex.message ?: "INVALID")
            }
        }

        if (toSave.isNotEmpty()) {
            employeeRepository.saveAll(toSave)
        }

        // Employee.cost_center_code derived 캐시 동기화 — 기존 user 행에 대해서만 즉시 갱신.
        // 신규 사원의 User 행은 아래 EmployeesCreatedEvent 흐름에서 새로 생성된다.
        val existingCodes = toSave.mapNotNull { it.employeeCode }.filter { it.isNotBlank() } - newEmployees.mapNotNull { it.employeeCode }.toSet()
        if (existingCodes.isNotEmpty()) {
            val users = userRepository.findByEmployeeCodeIn(existingCodes)
            val empByCode = toSave.filter { it.employeeCode != null }.associateBy { it.employeeCode!! }
            users.forEach { user ->
                val empCode = user.employeeCode ?: return@forEach
                user.costCenterCode = empByCode[empCode]?.costCenterCode
            }
        }

        // SF IF_REST_SAP_EmployeeMaster.upsertUser(@future) 동등 — 신규 Employee 집합을 1건의 이벤트로 발행.
        // 레거시가 사원 N명을 future 1회 호출(bulk)로 처리하는 것과 동일하게, 신규도 1개의 비동기 작업으로
        // 일괄 처리한다 (사원 1명당 이벤트 발행 시의 @Async 작업 폭증 회피).
        // 실제 User INSERT 는 UserProvisioningService 가 AFTER_COMMIT + @Async 로 별도 트랜잭션에서 일괄 처리.
        if (newEmployees.isNotEmpty()) {
            eventPublisher.publishEvent(
                EmployeesCreatedEvent(
                    newEmployees.map { employee ->
                        EmployeeSnapshot(
                            employeeCode = employee.employeeCode ?: error("신규 Employee 의 사번이 null - 비정상 (사번 필수 검증 후 생성)"),
                            name = employee.name,
                            workEmail = employee.workEmail,
                            email = employee.email,
                            birthDate = employee.birthDate,
                            role = employee.role,
                            appLoginActive = employee.appLoginActive,
                            costCenterCode = employee.costCenterCode,
                        )
                    }
                )
            )
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
        // SF EmployeeTrigger.upsertPhoneNumber() 동등 — HomePhone → Phone 무조건 복사 (조건 없음).
        entity.phone = command.homePhone
        entity.workPhone = command.workPhone
        entity.workEmail = command.workEmail
        entity.email = command.email
        entity.startDate = startDate
        entity.endDate = endDate
        entity.status = status
        entity.birthDate = birthDate
        entity.costCenterCode = command.orgCode
        // SAP LockingFlag raw 반영 (Y → 잠금). appLoginActive 는 그 역(Y → false).
        // 판촉/레이디/OSC 여사원·조장 보호는 applyLockingFlagException 이 이후 단계에서 덮어쓴다.
        entity.lockingFlag = command.lockingFlag == "Y"
        entity.appLoginActive = appLoginActive
    }

    /**
     * SF EmployeeTrigger.lockingFlagException() 동등 — 판촉/레이디/OSC 여사원·조장 계정 잠금 보호.
     *
     * 레거시 조건 (`EmployeeTriggerHandler.lockingFlagException`, beforeInsert/beforeUpdate):
     *   `(JobCode ∈ {판촉직,레이디직,OSC직}) AND (Status != 퇴직) AND (AppAuthority ∈ {여사원,조장})`
     * → `LockingFlag=false`, `APPLoginActive=true` 로 강제 복원.
     *
     * 즉 SAP 가 LockingFlag='Y'(잠금) 로 보내도 현장 여사원/조장(판촉직군) 의 앱 로그인은 막지 않는다.
     * 미적용 시 SAP 데이터 오류로 현장 인력 계정이 의도치 않게 잠긴다. lockingFlag 컬럼은 SAP raw 값을
     * 보존하지 않고 보호 결과(false)를 반영한다 — 레거시가 LockingFlag__c 자체를 덮어쓰는 것과 동일.
     *
     * 판정 기준은 적용 완료된 entity (라벨 변환된 jobCode/status + 발령으로 채워진 role) — 레거시가
     * upsert 최종 레코드 기준으로 trigger 평가하는 것과 정합.
     */
    private fun applyLockingFlagException(entity: Employee) {
        val protected = entity.jobCode in PROTECTED_JOB_CODES &&
            entity.status != STATUS_RETIRED &&
            entity.role in PROTECTED_APP_AUTHORITIES
        if (protected) {
            entity.lockingFlag = false
            entity.appLoginActive = true
        }
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

        // SF EmployeeTrigger.lockingFlagException 보호 대상. jobCode 는 H10060 라벨 변환 후 한글값.
        // (2024-01-02 레이디직 → OSC직 명칭 변경, 하위호환으로 레이디직 유지)
        private val PROTECTED_JOB_CODES = setOf("판촉직", "레이디직", "OSC직")
        // role 은 AppAuthority picklist 한글 raw value (여사원 / 조장).
        private val PROTECTED_APP_AUTHORITIES = setOf("여사원", "조장")
        private const val STATUS_RETIRED = "퇴직"
    }
}
