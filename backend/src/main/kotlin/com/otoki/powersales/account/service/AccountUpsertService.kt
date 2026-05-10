package com.otoki.powersales.account.service

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.account.entity.AccountType
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.account.service.dto.AccountUpsertCommand
import com.otoki.powersales.account.service.dto.AccountUpsertFailedRow
import com.otoki.powersales.account.service.dto.AccountUpsertResult
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.organization.entity.Organization
import com.otoki.powersales.organization.repository.OrganizationRepository
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
 * 2. 캐시 빌드: [AccountRepository.findByExternalKeyIn] / [EmployeeRepository.findByEmployeeCodeIn] / [OrganizationRepository.findAll].
 * 3. 행 단위 검증/적용 (try/catch): 필수값(`externalKey`/`name`) → `employeeCode` 존재 검증 → `consignmentAcc` 화이트리스트(`Y`/`N`/`""`) →
 *    Organization 폴백 lookup(`branchCode` → `salesDeptCode` → `divisionCode`) → `Account` 신규 생성 또는 mutable 필드 업데이트.
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
    private val organizationRepository: OrganizationRepository
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
        val employeeCodeSet: Set<String> = if (employeeCodes.isEmpty()) {
            emptySet()
        } else {
            employeeRepository.findByEmployeeCodeIn(employeeCodes.distinct())
                .map { it.employeeCode }
                .toSet()
        }

        val orgCache = buildOrganizationCache()

        val failures = mutableListOf<AccountUpsertFailedRow>()
        val toSave = mutableListOf<Account>()

        commands.forEach { command ->
            try {
                val externalKey = command.externalKey?.takeIf { it.isNotBlank() }
                    ?: throw IllegalArgumentException("SAPAccountCode 필수")
                val name = command.name?.takeIf { it.isNotBlank() }
                    ?: throw IllegalArgumentException("Name 필수")

                if (!command.employeeCode.isNullOrBlank() && command.employeeCode !in employeeCodeSet) {
                    failures += AccountUpsertFailedRow(externalKey, "employee_code not found: ${command.employeeCode}")
                    return@forEach
                }

                if (command.consignmentAcc != null && command.consignmentAcc !in CONSIGNMENT_ACC_ALLOWED) {
                    failures += AccountUpsertFailedRow(externalKey, "ConsignmentAcc 형식 오류: ${command.consignmentAcc}")
                    return@forEach
                }

                val matchedOrg = lookupOrganization(command, orgCache)
                val account = accountCache[externalKey]?.also { applyToEntity(it, command, name, matchedOrg) }
                    ?: createAccount(externalKey, command, name, matchedOrg).also { accountCache[externalKey] = it }
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

    private fun buildOrganizationCache(): Map<String, Organization> {
        val cache = mutableMapOf<String, Organization>()
        organizationRepository.findAll().forEach { org ->
            val key = sequenceOf(
                org.costCenterLevel5,
                org.costCenterLevel4,
                org.costCenterLevel3
            ).firstOrNull { !it.isNullOrBlank() } ?: return@forEach
            cache.putIfAbsent(key, org)
        }
        return cache
    }

    private fun lookupOrganization(command: AccountUpsertCommand, cache: Map<String, Organization>): Organization? {
        val branch = command.branchCode?.takeIf { it.isNotBlank() }
        if (branch != null) cache[branch]?.let { return it }
        val salesDept = command.salesDeptCode?.takeIf { it.isNotBlank() }
        if (salesDept != null) cache[salesDept]?.let { return it }
        val division = command.divisionCode?.takeIf { it.isNotBlank() }
        if (division != null) cache[division]?.let { return it }
        return null
    }

    private fun createAccount(
        externalKey: String,
        command: AccountUpsertCommand,
        name: String,
        matchedOrg: Organization?
    ): Account {
        val account = Account(externalKey = externalKey, name = name)
        applyMutableFields(account, command, matchedOrg)
        return account
    }

    private fun applyToEntity(
        account: Account,
        command: AccountUpsertCommand,
        name: String,
        matchedOrg: Organization?
    ) {
        account.name = name
        applyMutableFields(account, command, matchedOrg)
    }

    private fun applyMutableFields(account: Account, command: AccountUpsertCommand, matchedOrg: Organization?) {
        account.accountType = AccountType.fromDisplayNameOrNull(command.accountType)
        account.accountStatusName = command.accountStatusName
        account.accountGroup = command.accountGroup
        account.phone = command.phone
        account.mobilePhone = command.mobilePhone
        account.representative = command.representative
        account.zipCode = command.zipcode
        account.address1 = command.address1
        account.address2 = command.address2
        account.closingTime1 = command.closingTime1
        account.closingTime2 = command.closingTime2
        account.closingTime3 = command.closingTime3
        account.abcType = command.abcType
        account.abcTypeCode = command.abcTypeCode
        account.distribution = command.distribution
        account.werk1Tx = command.werk1Tx
        account.werk2Tx = command.werk2Tx
        account.werk3Tx = command.werk3Tx
        account.employeeCode = command.employeeCode

        // Spec #575: SAP 인바운드 레거시 필드 13개 보존 (변환 없이 그대로 set)
        account.accountStatusCode = command.accountStatusCode
        account.businessType = command.businessType
        account.businessCategory = command.businessCategory
        account.businessLicenseNumber = command.businessLicenseNumber
        account.email = command.email
        account.divisionName = command.divisionName
        account.salesDeptName = command.salesDeptName
        account.consignmentAcc = command.consignmentAcc
        account.werk1 = command.werk1
        account.werk2 = command.werk2
        account.werk3 = command.werk3
        account.salesDeptCostCenter = command.salesDeptCode
        account.divisionCostCenter = command.divisionCode

        // 지점 코드/명: Organization 매칭 시 OrgCode/OrgName 의 deepest non-blank 값을 우선 사용,
        // 매칭 실패 시 페이로드 raw 값을 그대로 저장한다 (레거시 폴백 동작과 동일).
        val resolvedBranchCode = matchedOrg?.let { firstNonBlank(it.orgCodeLevel5, it.orgCodeLevel4, it.orgCodeLevel3) }
            ?: command.branchCode?.takeIf { it.isNotBlank() }
        val resolvedBranchName = matchedOrg?.let { firstNonBlank(it.orgNameLevel5, it.orgNameLevel4, it.orgNameLevel3) }
            ?: command.branchName?.takeIf { it.isNotBlank() }
        account.branchCode = resolvedBranchCode
        account.branchName = resolvedBranchName
    }

    private fun firstNonBlank(vararg values: String?): String? =
        values.firstOrNull { !it.isNullOrBlank() }

    companion object {
        // Spec #575: ConsignmentAcc 허용 값. null 은 미입력 → 검증 스킵.
        private val CONSIGNMENT_ACC_ALLOWED = setOf("Y", "N", "")
    }
}
