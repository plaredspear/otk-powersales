package com.otoki.powersales.account.service

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.account.service.dto.AccountUpsertCommand
import com.otoki.powersales.account.service.dto.AccountUpsertFailedRow
import com.otoki.powersales.account.service.dto.AccountUpsertResult
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.organization.repository.OrganizationRepository
import com.otoki.powersales.user.entity.User
import com.otoki.powersales.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 거래처 UPSERT 도메인 서비스.
 *
 * ## 레거시 매핑
 * - 진입점: SAP 인바운드 어댑터 [com.otoki.powersales.sap.inbound.service.SapClientMasterService]
 * - origin spec: #558 (SAP 거래처 마스터 인바운드) — 본 도메인 분리 PoC: #634
 *
 * ## 레거시 동작 요약
 * 1. 입력: `List<AccountUpsertCommand>` (외부 키 [AccountUpsertCommand.externalKey] = SAP 거래처 코드).
 * 2. 캐시 빌드: [AccountRepository.findByExternalKeyIn] / [EmployeeRepository.findByEmployeeCodeIn] / [OrganizationLookup.build].
 * 3. 행 단위 검증 (try/catch): 필수값(`externalKey`/`name`) → `employeeCode` 존재 검증 → `consignmentAcc` 화이트리스트(`Y`/`N`/`""`) →
 *    [OrganizationLookup.match] 폴백 lookup → [AccountUpsertMapper] 로 신규 생성 또는 기존 갱신.
 * 4. 외부 호출: [AccountRepository.saveAll] (성공 행만 일괄). 행 검증 실패는 [AccountUpsertResult.failures] 누적, 트랜잭션 롤백하지 않음.
 *
 * ## 신규 차이 — 동등 (생략)
 *
 * `sap.*` 패키지 의존 0건 — SAP audit / `ClientIpResolver` / `RequestContextHolder` / `SapResultWrapper` 침투 금지.
 */
@Service
class AccountUpsertService(
    private val accountRepository: AccountRepository,
    private val employeeRepository: EmployeeRepository,
    private val organizationRepository: OrganizationRepository,
    private val userRepository: UserRepository,
    private val mapper: AccountUpsertMapper
) {

    @Transactional
    fun upsert(commands: List<AccountUpsertCommand>): AccountUpsertResult {
        val externalKeys = commands.mapNotNull { it.externalKey?.takeIf { key -> key.isNotBlank() } }
        val accountCache = if (externalKeys.isEmpty()) {
            mutableMapOf()
        } else {
            accountRepository.findByExternalKeyIn(externalKeys.distinct())
                .associateBy { it.externalKey!! }
                .toMutableMap()
        }

        val employeeCodes = commands.mapNotNull { it.employeeCode?.takeIf { code -> code.isNotBlank() } }
        val employeeByCode: Map<String, Employee> = if (employeeCodes.isEmpty()) {
            emptyMap()
        } else {
            employeeRepository.findByEmployeeCodeIn(employeeCodes.distinct())
                .associateBy { it.employeeCode }
        }

        // Spec #758: owner FK 가 User 로 전환됨. Employee 매칭은 존재 검증용으로 유지하고
        // owner 적재는 User.employee_code 매칭 결과를 사용한다.
        // invariant: Employee 신규 생성 시 같은 Transaction 으로 User 도 생성됨 (EmployeeUpsertService).
        val userByEmployeeCode: Map<String, User> = if (employeeCodes.isEmpty()) {
            emptyMap()
        } else {
            userRepository.findByEmployeeCodeIn(employeeCodes.distinct())
                .associateBy { it.employeeCode }
        }

        val orgLookup = OrganizationLookup.build(organizationRepository)

        val failures = mutableListOf<AccountUpsertFailedRow>()
        val toSave = mutableListOf<Account>()

        commands.forEach { command ->
            try {
                val externalKey = command.externalKey?.takeIf { it.isNotBlank() }
                    ?: throw IllegalArgumentException(Reasons.EXTERNAL_KEY_REQUIRED)
                val name = command.name?.takeIf { it.isNotBlank() }
                    ?: throw IllegalArgumentException(Reasons.NAME_REQUIRED)

                if (!command.employeeCode.isNullOrBlank() && command.employeeCode !in employeeByCode) {
                    failures += AccountUpsertFailedRow(externalKey, Reasons.employeeNotFound(command.employeeCode))
                    return@forEach
                }

                if (command.consignmentAcc != null && command.consignmentAcc !in CONSIGNMENT_ACC_ALLOWED) {
                    failures += AccountUpsertFailedRow(externalKey, Reasons.consignmentAccInvalid(command.consignmentAcc))
                    return@forEach
                }

                val matchedOrg = orgLookup.match(command)
                val matchedUser = command.employeeCode?.let { userByEmployeeCode[it] }
                val account = accountCache[externalKey]?.also { mapper.update(it, name, command, matchedOrg, matchedUser) }
                    ?: mapper.newAccount(externalKey, name, command, matchedOrg, matchedUser)
                        .also { accountCache[externalKey] = it }
                toSave += account
            } catch (ex: IllegalArgumentException) {
                failures += AccountUpsertFailedRow(command.externalKey, ex.message ?: "INVALID")
            }
        }

        if (toSave.isNotEmpty()) {
            accountRepository.saveAll(toSave)
        }

        return AccountUpsertResult(
            successCount = toSave.size,
            failureCount = failures.size,
            failures = failures
        )
    }

    companion object {
        // Spec #575: ConsignmentAcc 허용 값. null 은 미입력 → 검증 스킵.
        private val CONSIGNMENT_ACC_ALLOWED = setOf("Y", "N", "")

        private object Reasons {
            const val EXTERNAL_KEY_REQUIRED = "SAPAccountCode 필수"
            const val NAME_REQUIRED = "Name 필수"
            fun employeeNotFound(code: String) = "employee_code not found: $code"
            fun consignmentAccInvalid(value: String) = "ConsignmentAcc 형식 오류: $value"
        }
    }
}
