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
 * **User 가 아직 없는 사원** 집합을 [EmployeesCreatedEvent] **1건**으로 발행한다 (SF cls:131 의
 * `userMap.containsKey` 분기 동등 — 신규 Employee 뿐 아니라 "기존 Employee 인데 User 부재" 도 포함).
 * User 행 생성은 [com.otoki.powersales.user.service.UserProvisioningService.handleEmployeesCreated]
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
                applyLockingFlagException(entity)
                toSave += entity
            } catch (ex: IllegalArgumentException) {
                failures += EmployeeUpsertFailedRow(command.employeeCode, ex.message ?: "INVALID")
            }
        }

        if (toSave.isNotEmpty()) {
            employeeRepository.saveAll(toSave)
        }

        // User 행 동기화 — SF IF_REST_SAP_EmployeeMaster.upsertUser(@future) (cls:201-307) 동등.
        //
        // 레거시 분기 기준은 "Employee 신규/기존" 이 아니라 **User 존재 여부** (cls:131
        // `userMap.containsKey(EmployeeCode)` → 있으면 updateCode, 없으면 insertCode). 따라서
        // 기존 Employee 라도 매칭되는 User 가 없으면 insert(신규 생성) 경로로 간다.
        // 신규 구현도 toSave 전체 사번으로 기존 User 를 한 번에 조회한 뒤, User 유무로 update/insert 분기한다.
        val savedByCode = toSave.filter { !it.employeeCode.isNullOrBlank() }.associateBy { it.employeeCode!! }
        if (savedByCode.isNotEmpty()) {
            val existingUsersByCode = userRepository.findByEmployeeCodeIn(savedByCode.keys.toList())
                .filter { it.employeeCode != null }
                .associateBy { it.employeeCode!! }

            // update 경로 — User 가 이미 존재하는 사원 (SF cls:239-274).
            // excHrCode (본사 10개 코스트센터) 는 레거시 update SOQL 에서 제외 → User 무변경.
            existingUsersByCode.forEach { (empCode, user) ->
                val employee = savedByCode[empCode] ?: return@forEach
                if (employee.costCenterCode in EXCLUDED_COST_CENTER_CODES) return@forEach
                user.costCenterCode = employee.costCenterCode
                // SF cls:264-270 동등 — 재직상태 퇴직 → 기존 User 비활성화. 그 외 → 활성.
                user.isActive = employee.status != STATUS_RETIRED
            }

            // insert 경로 — User 가 없는 사원 (SF cls:277, insert SOQL). 신규 Employee + "기존 Employee 인데 User 부재" 포함.
            // insert SOQL WHERE 의 두 가드를 적용: Status != '퇴직', CostCenterCode NOT IN excHrCode.
            // 실제 User INSERT 는 UserProvisioningService 가 AFTER_COMMIT + @Async 로 별도 트랜잭션에서 일괄 처리.
            // 레거시가 사원 N명을 future 1회 호출(bulk)로 처리하듯, 신규도 1건의 이벤트로 발행해 비동기 작업 1개로 일괄 처리.
            val provisionTargets = savedByCode.values.filter { employee ->
                employee.employeeCode !in existingUsersByCode.keys &&
                    employee.status != STATUS_RETIRED &&
                    employee.costCenterCode !in EXCLUDED_COST_CENTER_CODES
            }
            if (provisionTargets.isNotEmpty()) {
                eventPublisher.publishEvent(
                    EmployeesCreatedEvent(
                        provisionTargets.map { employee ->
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

        // SF IF_REST_SAP_EmployeeMaster.cls:231-236 의 excHrCode 동등 — 본사 부서 등 User 계정을
        // 만들지/갱신하지 않는 코스트센터. 레거시에서 4972/4970 은 주석처리(// excHrCode.add)되어
        // 실제 active 는 아래 10건. update/insert 두 SOQL WHERE 의 `CostCenterCode NOT IN:excHrCode`.
        private val EXCLUDED_COST_CENTER_CODES = setOf(
            "4606", "5526", "4946", "5636", "5637", "5650", "5710", "5366", "3474", "4971",
        )
    }
}
