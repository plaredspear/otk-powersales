package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.auth.util.ClientIpResolver
import com.otoki.powersales.sap.entity.SystemCodeMaster
import com.otoki.powersales.sap.inbound.dto.product.FailureItem
import com.otoki.powersales.sap.inbound.dto.product.ProductMasterDetail
import com.otoki.powersales.sap.inbound.dto.product.SystemCodeMasterRequestItem
import com.otoki.powersales.sap.repository.SystemCodeMasterRepository
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/**
 * SAP 시스템 공통 코드 마스터 인바운드 UPSERT 서비스. (Spec #559)
 *
 * - UPSERT 키: [SystemCodeMaster.externalKey] = `CompanyCode + ';' + GroupCode + ';' + DetailCode`
 * - 부분 실패 허용 (행 단위 검증 후 saveAll 일괄)
 */
@Service
class SapSystemCodeMasterService(
    private val systemCodeMasterRepository: SystemCodeMasterRepository,
    private val auditService: SapInboundAuditService
) {

    @Transactional
    fun upsert(items: List<SystemCodeMasterRequestItem>): ProductMasterDetail {
        val failures = mutableListOf<FailureItem>()
        val toSave = mutableListOf<SystemCodeMaster>()
        val keyCache = mutableMapOf<String, SystemCodeMaster>()

        items.forEach { item ->
            val companyCode = item.companyCode?.takeIf { it.isNotBlank() }
            val groupCode = item.groupCode?.takeIf { it.isNotBlank() }
            val detailCode = item.detailCode?.takeIf { it.isNotBlank() }

            if (companyCode == null) {
                failures += FailureItem(null, "CompanyCode 필수")
                return@forEach
            }
            if (groupCode == null) {
                failures += FailureItem(null, "GroupCode 필수")
                return@forEach
            }
            if (detailCode == null) {
                failures += FailureItem(null, "DetailCode 필수")
                return@forEach
            }

            val externalKey = "$companyCode;$groupCode;$detailCode"
            val existing = keyCache[externalKey]
                ?: systemCodeMasterRepository.findByExternalKey(externalKey)?.also { keyCache[externalKey] = it }

            val entity = if (existing == null) {
                SystemCodeMaster(
                    companyCode = companyCode,
                    groupCode = groupCode,
                    detailCode = detailCode,
                    externalKey = externalKey,
                    groupCodeName = item.groupCodeName,
                    detailCodeName = item.detailCodeName,
                    seq = item.seq
                ).also { keyCache[externalKey] = it }
            } else {
                existing.companyCode = companyCode
                existing.groupCode = groupCode
                existing.detailCode = detailCode
                existing.groupCodeName = item.groupCodeName
                existing.detailCodeName = item.detailCodeName
                existing.seq = item.seq
                existing
            }
            toSave += entity
        }

        if (toSave.isNotEmpty()) {
            systemCodeMasterRepository.saveAll(toSave)
        }

        recordAccepted(items.size, toSave.size, failures.size)
        return ProductMasterDetail(
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
