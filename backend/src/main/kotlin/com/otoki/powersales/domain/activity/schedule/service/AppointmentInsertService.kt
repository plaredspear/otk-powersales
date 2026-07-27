package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.activity.schedule.entity.Appointment
import com.otoki.powersales.domain.activity.schedule.repository.AppointmentRepository
import com.otoki.powersales.domain.activity.schedule.service.dto.AppointmentInsertCommand
import com.otoki.powersales.domain.activity.schedule.service.dto.AppointmentInsertFailedRow
import com.otoki.powersales.domain.activity.schedule.service.dto.AppointmentInsertResult
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
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
 *    수신 사번 null 은 SF `empSet.contains(null)` 정합으로 [EmployeeRepository.existsByEmployeeCodeIsNull] 로 판정.
 * 3. 레거시 정합 — 수신 필드 명시 필수/형식 검증으로 행을 거부하지 않는다 (레거시 IF_REST_SAP_Appointment 에
 *    검증 게이트 없음, 검증 없이 전 행 INSERT). EmployeeCode/JobCode 누락도 그대로 적재하고, AppointDate 는
 *    빈값/null/`00000000`/형식오류를 모두 `2999-12-31` 센티넬로 흡수 ([parseAppointDate]).
 * 4. 문자열 필드는 빈 문자열('') 을 null 로 정규화해 적재 — SF 플랫폼이 텍스트 필드 '' 를 null 로 저장하는
 *    동작 정합 (공백만 있는 문자열은 SF 처럼 그대로 저장).
 * 5. name 은 SF Appointment__c Name(AutoNumber `AP{00000000}`) 정합으로 행마다 채번
 *    ([AppointmentRepository.getNextAppointmentNameSeq] — SF sync 추월 대비 GREATEST setval 패턴).
 * 6. 외부 호출: [AppointmentRepository.save] — 행마다 REQUIRES_NEW 트랜잭션으로 격리해 SF
 *    `Database.insert(allOrNone=false)` 의 행 단위 부분 실패를 재현한다 (실패 행만 [AppointmentInsertResult.failures],
 *    성공 행은 유지). 적재된 entity 는 [AppointmentInsertResult.savedAppointments] 로 return —
 *    어댑터가 후처리 트리거 호출 시 사용.
 *
 * cross-domain 의존: [EmployeeRepository] (직원 매칭 lookup) — Q3 옵션 1 정합 (lookup 용도 read-only).
 * 후처리 트리거 (`AppointmentUserProfileUpdater`) 는 어댑터 잔류 — 도메인이 SAP 인입 후처리에 결합되지 않도록.
 */
@Service
class AppointmentInsertService(
    private val appointmentRepository: AppointmentRepository,
    private val employeeRepository: EmployeeRepository,
    transactionManager: PlatformTransactionManager
) {

    private val log = LoggerFactory.getLogger(javaClass)

    // SF Database.insert(allOrNone=false) 행 격리 정합 — 행마다 독립 트랜잭션으로 커밋해
    // 한 행의 DB 오류가 나머지 행의 적재를 무효화하지 않도록 한다.
    private val rowTransaction = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    fun insert(commands: List<AppointmentInsertCommand>): AppointmentInsertResult {
        val empCodes = commands.mapNotNull { it.employeeCode?.takeIf { c -> c.isNotEmpty() } }.distinct()
        val existingEmpCodes: Set<String> = if (empCodes.isEmpty()) {
            emptySet()
        } else {
            employeeRepository.findByEmployeeCodeIn(empCodes)
                .mapNotNull { it.employeeCode }
                .toHashSet()
        }
        // SF empSet 은 전 사원 EmpCode 를 담아 null 도 포함한다 — 수신 사번 null 이면 contains(null) 이
        // "EmpCode null 사원 존재 여부" 로 판정된다 (우발적 동작이지만 SF 정합으로 재현).
        val nullEmpCodeEmployeeExists = commands.any { it.employeeCode == null } &&
            employeeRepository.existsByEmployeeCodeIsNull()

        val failures = mutableListOf<AppointmentInsertFailedRow>()
        val saved = mutableListOf<Appointment>()

        commands.forEach { command ->
            // 레거시 IF_REST_SAP_Appointment 정합 — 수신 필드(EmployeeCode/JobCode 등)에 대한 명시적
            // 필수/형식 검증으로 행을 거부하는 코드가 레거시에 전무하다 (검증 없이 전 행 INSERT,
            // Database.insert allOrNone=false). EmployeeCode/JobCode 누락도 그대로 적재하고, AppointDate 는
            // Util.convertStringToDate 정합으로 빈값/null/00000000/형식오류를 모두 2999-12-31 센티넬로 흡수한다
            // (레거시는 형식 오류 시 Date.valueOf 예외가 배치 전체를 ERROR 로 만드는 우발 버그이나, 그 전체
            // 실패는 재현하지 않고 센티넬 흡수로 행 격리를 유지).
            //
            // empCodeExist 는 SF 처럼 정규화 전 수신값으로 판정한다 — '' 는 사원 집합에 존재할 수 없어 false,
            // null 은 contains(null) 정합으로 EmpCode null 사원 존재 여부.
            val rawEmployeeCode = command.employeeCode
            val empCodeExist = if (rawEmployeeCode == null) {
                nullEmpCodeEmployeeExists
            } else {
                rawEmployeeCode in existingEmpCodes
            }
            val parsedAppointDate = parseAppointDate(command.appointDate)

            try {
                val persisted = rowTransaction.execute {
                    // SF AutoNumber(AP{00000000}) 정합 채번 — 실패 행의 번호 공백(gap)은 SF AutoNumber 와 동일.
                    val seq = appointmentRepository.getNextAppointmentNameSeq()
                    appointmentRepository.save(
                        Appointment(
                            name = "AP" + String.format("%08d", seq),
                            employeeCode = rawEmployeeCode.emptyToNull(),
                            empCodeExist = empCodeExist,
                            afterOrgCode = command.afterOrgCode.emptyToNull(),
                            afterOrgName = command.afterOrgName.emptyToNull(),
                            jikchak = command.jikchak.emptyToNull(),
                            jikwee = command.jikwee.emptyToNull(),
                            jikgub = command.jikgub.emptyToNull(),
                            workType = command.workType.emptyToNull(),
                            manageType = command.manageType.emptyToNull(),
                            jobCode = command.jobCode.emptyToNull(),
                            workArea = command.workArea.emptyToNull(),
                            jikjong = command.jikjong.emptyToNull(),
                            appointDate = parsedAppointDate,
                            jobName = command.jobName.emptyToNull(),
                            ordDetailCode = command.ordDetailCode.emptyToNull(),
                            ordDetailNode = command.ordDetailNode.emptyToNull()
                        )
                    )
                }
                saved += persisted
            } catch (e: Exception) {
                log.warn("발령 행 적재 실패 (행 격리 — 나머지 행 진행): employeeCode={}, error={}",
                    rawEmployeeCode, e.message)
                failures += AppointmentInsertFailedRow(
                    identifier = rawEmployeeCode,
                    reason = e.message ?: e.javaClass.simpleName
                )
            }
        }

        return AppointmentInsertResult(
            successCount = saved.size,
            failureCount = failures.size,
            failures = failures,
            savedAppointments = saved
        )
    }

    /**
     * SF 플랫폼 텍스트 필드 저장 동작 정합 — 빈 문자열('') 은 null 로 저장된다.
     * 공백만 있는 문자열(' ' 등)은 SF 도 그대로 저장하므로 정규화하지 않는다.
     */
    private fun String?.emptyToNull(): String? = this?.takeIf { it.isNotEmpty() }

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
