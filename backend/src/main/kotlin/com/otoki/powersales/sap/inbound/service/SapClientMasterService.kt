package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.auth.util.ClientIpResolver
import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.account.entity.AccountType
import com.otoki.powersales.organization.entity.Organization
import com.otoki.powersales.sap.inbound.dto.account.AccountMasterDetail
import com.otoki.powersales.sap.inbound.dto.account.ClientMasterRequestItem
import com.otoki.powersales.sap.inbound.dto.account.FailureItem
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.organization.repository.OrganizationRepository
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/**
 * SAP 거래처 마스터 인바운드 UPSERT 서비스. (Spec #558)
 *
 * - UPSERT 키: [Account.externalKey] (= 페이로드 SAPAccountCode)
 * - 담당자: EmployeeCode 매칭 실패 시 해당 행 failures 처리 (D3 — Account 적재 자체 스킵)
 * - 조직 폴백: BranchCode → SalesDeptCode → DivisionCode 순서로 [Organization] 캐시 조회
 * - 부분 실패 허용 (행 단위 검증 후 saveAll 일괄)
 */
@Service
class SapClientMasterService(
    private val accountRepository: AccountRepository,
    private val employeeRepository: EmployeeRepository,
    private val organizationRepository: OrganizationRepository,
    private val auditService: SapInboundAuditService
) {

    @Transactional
    fun upsert(items: List<ClientMasterRequestItem>): AccountMasterDetail {
        val externalKeys = items.mapNotNull { it.sapAccountCode?.takeIf { code -> code.isNotBlank() } }
        val accountCache = if (externalKeys.isEmpty()) {
            mutableMapOf()
        } else {
            accountRepository.findByExternalKeyIn(externalKeys.distinct())
                .associateBy { it.externalKey!! }
                .toMutableMap()
        }

        val employeeCodes = items.mapNotNull { it.employeeCode?.takeIf { code -> code.isNotBlank() } }
        val employeeCodeSet: Set<String> = if (employeeCodes.isEmpty()) {
            emptySet()
        } else {
            employeeRepository.findByEmployeeCodeIn(employeeCodes.distinct())
                .map { it.employeeCode }
                .toSet()
        }

        val orgCache = buildOrganizationCache()

        val failures = mutableListOf<FailureItem>()
        val toSave = mutableListOf<Account>()

        items.forEach { item ->
            try {
                val externalKey = item.sapAccountCode?.takeIf { it.isNotBlank() }
                    ?: throw IllegalArgumentException("SAPAccountCode 필수")
                val name = item.name?.takeIf { it.isNotBlank() }
                    ?: throw IllegalArgumentException("Name 필수")

                if (!item.employeeCode.isNullOrBlank() && item.employeeCode !in employeeCodeSet) {
                    failures += FailureItem(externalKey, "employee_code not found: ${item.employeeCode}")
                    return@forEach
                }

                if (item.consignmentAcc != null && item.consignmentAcc !in CONSIGNMENT_ACC_ALLOWED) {
                    failures += FailureItem(externalKey, "ConsignmentAcc 형식 오류: ${item.consignmentAcc}")
                    return@forEach
                }

                val matchedOrg = lookupOrganization(item, orgCache)
                val account = accountCache[externalKey]?.also { applyToEntity(it, item, name, matchedOrg) }
                    ?: createAccount(externalKey, item, name, matchedOrg).also { accountCache[externalKey] = it }
                toSave += account
            } catch (ex: IllegalArgumentException) {
                failures += FailureItem(item.sapAccountCode, ex.message ?: "INVALID")
            }
        }

        if (toSave.isNotEmpty()) {
            accountRepository.saveAll(toSave)
        }

        recordAccepted(items.size, toSave.size, failures.size)
        return AccountMasterDetail(
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

    private fun lookupOrganization(item: ClientMasterRequestItem, cache: Map<String, Organization>): Organization? {
        val branch = item.branchCode?.takeIf { it.isNotBlank() }
        if (branch != null) cache[branch]?.let { return it }
        val salesDept = item.salesDeptCode?.takeIf { it.isNotBlank() }
        if (salesDept != null) cache[salesDept]?.let { return it }
        val division = item.divisionCode?.takeIf { it.isNotBlank() }
        if (division != null) cache[division]?.let { return it }
        return null
    }

    private fun createAccount(
        externalKey: String,
        item: ClientMasterRequestItem,
        name: String,
        matchedOrg: Organization?
    ): Account {
        val account = Account(externalKey = externalKey, name = name)
        applyMutableFields(account, item, matchedOrg)
        return account
    }

    private fun applyToEntity(
        account: Account,
        item: ClientMasterRequestItem,
        name: String,
        matchedOrg: Organization?
    ) {
        account.name = name
        applyMutableFields(account, item, matchedOrg)
    }

    private fun applyMutableFields(account: Account, item: ClientMasterRequestItem, matchedOrg: Organization?) {
        account.accountType = AccountType.fromDisplayNameOrNull(item.accountType)
        account.accountStatusName = item.accountStatusName
        account.accountGroup = item.accountGroup
        account.phone = item.phone
        account.mobilePhone = item.mobilePhone
        account.representative = item.representative
        account.zipCode = item.zipcode
        account.address1 = item.address1
        account.address2 = item.address2
        account.closingTime1 = item.closingTime1
        account.closingTime2 = item.closingTime2
        account.closingTime3 = item.closingTime3
        account.abcType = item.abcType
        account.abcTypeCode = item.abcTypeCode
        account.distribution = item.distribution
        account.werk1Tx = item.werk1Tx
        account.werk2Tx = item.werk2Tx
        account.werk3Tx = item.werk3Tx
        account.employeeCode = item.employeeCode

        // Spec #575: SAP 인바운드 레거시 필드 13개 보존 (변환 없이 그대로 set)
        account.accountStatusCode = item.accountStatusCode
        account.businessType = item.businessType
        account.businessCategory = item.businessCategory
        account.businessLicenseNumber = item.businessLicenseNumber
        account.email = item.email
        account.divisionName = item.divisionName
        account.salesDeptName = item.salesDeptName
        account.consignmentAcc = item.consignmentAcc
        account.werk1 = item.werk1
        account.werk2 = item.werk2
        account.werk3 = item.werk3
        account.salesDeptCostCenter = item.salesDeptCode
        account.divisionCostCenter = item.divisionCode

        // 지점 코드/명: Organization 매칭 시 OrgCode/OrgName 의 deepest non-blank 값을 우선 사용,
        // 매칭 실패 시 페이로드 raw 값을 그대로 저장한다 (레거시 폴백 동작과 동일).
        val resolvedBranchCode = matchedOrg?.let { firstNonBlank(it.orgCodeLevel5, it.orgCodeLevel4, it.orgCodeLevel3) }
            ?: item.branchCode?.takeIf { it.isNotBlank() }
        val resolvedBranchName = matchedOrg?.let { firstNonBlank(it.orgNameLevel5, it.orgNameLevel4, it.orgNameLevel3) }
            ?: item.branchName?.takeIf { it.isNotBlank() }
        account.branchCode = resolvedBranchCode
        account.branchName = resolvedBranchName
    }

    private fun firstNonBlank(vararg values: String?): String? =
        values.firstOrNull { !it.isNullOrBlank() }

    private fun recordAccepted(received: Int, success: Int, failure: Int) {
        val request = currentRequest()
        val endpoint = request?.requestURI ?: ""
        val httpMethod = request?.method
        val clientIp = request?.let { ClientIpResolver.resolve(it) } ?: ""
        val clientId = SecurityContextHolder.getContext().authentication?.name
        auditService.record(
            SapInboundAudit(
                eventType = SapInboundAuditEventType.REQUEST_ACCEPTED,
                clientId = clientId,
                endpoint = endpoint,
                httpMethod = httpMethod,
                clientIp = clientIp,
                receivedCount = received,
                reason = "success=$success failure=$failure"
            )
        )
    }

    private fun currentRequest(): HttpServletRequest? {
        val attrs = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
        return attrs?.request
    }

    companion object {
        // Spec #575: ConsignmentAcc 허용 값. null 은 미입력 → 검증 스킵.
        private val CONSIGNMENT_ACC_ALLOWED = setOf("Y", "N", "")
    }
}
