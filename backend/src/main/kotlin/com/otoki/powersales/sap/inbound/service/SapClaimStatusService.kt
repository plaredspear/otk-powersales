package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.claim.entity.Claim
import com.otoki.powersales.claim.repository.ClaimRepository
import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.auth.util.ClientIpResolver
import com.otoki.powersales.sap.inbound.dto.claim.ClaimStatusDetail
import com.otoki.powersales.sap.inbound.dto.claim.ClaimStatusFailure
import com.otoki.powersales.sap.inbound.dto.claim.ClaimStatusRequestItem
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/**
 * SAP 클레임 상태 인바운드 UPDATE 전용 서비스. (Spec #561)
 *
 * - INSERT 미수행: 신규 클레임은 모바일 앱이 등록하고, SAP 는 후속 처리상태만 갱신한다.
 * - 조회 키: [Claim.name] (Salesforce DKRetail__Claim__c.Name 매핑)
 * - 미존재 시 행 단위 failure (`reason: "claim not found"`)
 * - 갱신 필드: counselNumber, actionCode, actionStatus, actContent, reasonType, cosmosKey
 * - actionStatus 길이 제한: [ACTION_STATUS_MAX_LENGTH] 초과 시 행 단위 failure
 * - 부분 실패 허용. saveAll 일괄 저장.
 */
@Service
class SapClaimStatusService(
    private val claimRepository: ClaimRepository,
    private val auditService: SapInboundAuditService
) {

    @Transactional
    fun update(items: List<ClaimStatusRequestItem>): ClaimStatusDetail {
        val names = items.mapNotNull { it.name?.takeIf { n -> n.isNotBlank() } }.distinct()
        val byName: Map<String, Claim> = if (names.isEmpty()) {
            emptyMap()
        } else {
            claimRepository.findAllByNameIn(names).associateBy { it.name!! }
        }

        val failures = mutableListOf<ClaimStatusFailure>()
        val toSave = mutableListOf<Claim>()

        items.forEach { item ->
            val name = item.name?.takeIf { it.isNotBlank() }
            if (name == null) {
                failures += ClaimStatusFailure(item.name, "Name 필수")
                return@forEach
            }
            val claimStatus = item.claimStatus
            if (claimStatus != null && claimStatus.length > ACTION_STATUS_MAX_LENGTH) {
                failures += ClaimStatusFailure(name, "actionStatus length exceeded")
                return@forEach
            }
            val claim = byName[name]
            if (claim == null) {
                failures += ClaimStatusFailure(name, "claim not found")
                return@forEach
            }
            applyFields(claim, item)
            toSave += claim
        }

        if (toSave.isNotEmpty()) {
            claimRepository.saveAll(toSave)
        }

        recordAccepted(items.size, toSave.size, failures.size)

        return ClaimStatusDetail(
            successCount = toSave.size,
            failureCount = failures.size,
            failures = failures
        )
    }

    private fun applyFields(claim: Claim, item: ClaimStatusRequestItem) {
        claim.counselNumber = item.claimSequence
        claim.actionCode = item.actionCode
        claim.actionStatus = item.claimStatus
        claim.actContent = item.content
        claim.reasonType = item.reasonType
        claim.cosmosKey = item.cosmosKey
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

    companion object {
        const val ACTION_STATUS_MAX_LENGTH: Int = 50
    }
}
