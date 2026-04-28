package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.auth.util.ClientIpResolver
import com.otoki.powersales.sap.entity.AccountCategoryMaster
import com.otoki.powersales.sap.inbound.dto.account.AccountCategoryRequestItem
import com.otoki.powersales.sap.inbound.dto.account.AccountMasterDetail
import com.otoki.powersales.sap.inbound.dto.account.FailureItem
import com.otoki.powersales.sap.repository.AccountCategoryMasterRepository
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/**
 * SAP 거래처 카테고리 마스터 인바운드 UPSERT 서비스. (Spec #558)
 * UPSERT 키: [AccountCategoryMaster.accountCode] (= 페이로드 AccountCode)
 */
@Service
class SapAccountCategoryService(
    private val accountCategoryMasterRepository: AccountCategoryMasterRepository,
    private val auditService: SapInboundAuditService
) {

    @Transactional
    fun upsert(items: List<AccountCategoryRequestItem>): AccountMasterDetail {
        val failures = mutableListOf<FailureItem>()
        val toSave = mutableListOf<AccountCategoryMaster>()

        items.forEach { item ->
            val accountCode = item.accountCode?.takeIf { it.isNotBlank() }
            val name = item.name?.takeIf { it.isNotBlank() }
            if (accountCode == null) {
                failures += FailureItem(null, "AccountCode 필수")
                return@forEach
            }
            if (name == null) {
                failures += FailureItem(accountCode, "Name 필수")
                return@forEach
            }
            val existing = accountCategoryMasterRepository.findByAccountCode(accountCode)
            val entity = if (existing == null) {
                AccountCategoryMaster(accountCode = accountCode, name = name)
            } else {
                existing.name = name
                existing
            }
            toSave += entity
        }

        if (toSave.isNotEmpty()) {
            accountCategoryMasterRepository.saveAll(toSave)
        }

        recordAccepted(items.size, toSave.size, failures.size)
        return AccountMasterDetail(
            successCount = toSave.size,
            failureCount = failures.size,
            failures = failures
        )
    }

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
}
