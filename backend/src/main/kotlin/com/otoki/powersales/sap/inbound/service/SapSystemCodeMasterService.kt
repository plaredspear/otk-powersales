package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.common.service.SystemCodeMasterUpsertService
import com.otoki.powersales.common.service.dto.SystemCodeMasterUpsertCommand
import com.otoki.powersales.common.service.dto.SystemCodeMasterUpsertResult
import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditEventType
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.auth.util.ClientIpResolver
import com.otoki.powersales.sap.inbound.dto.product.FailureItem
import com.otoki.powersales.sap.inbound.dto.product.ProductMasterDetail
import com.otoki.powersales.sap.inbound.dto.product.SystemCodeMasterRequestItem
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/**
 * SAP 시스템 공통 코드 마스터 인바운드 어댑터. (Spec #559 / 어댑터-도메인 분리: #635 P1-B)
 *
 * 책임:
 * - SAP 페이로드 [SystemCodeMasterRequestItem] → 도메인 커맨드 [SystemCodeMasterUpsertCommand] 매핑
 * - 도메인 서비스 [SystemCodeMasterUpsertService.upsert] 호출
 * - 도메인 결과 [SystemCodeMasterUpsertResult] → SAP 응답 [ProductMasterDetail] 매핑
 * - [SapInboundAuditService] 감사 기록
 *
 * 트랜잭션은 도메인 측이 관리하며 어댑터는 `@Transactional` 을 부착하지 않는다.
 * UPSERT 키 / 부분 실패 시멘틱은 [SystemCodeMasterUpsertService] KDoc 참조.
 */
@Service
class SapSystemCodeMasterService(
    private val systemCodeMasterUpsertService: SystemCodeMasterUpsertService,
    private val auditService: SapInboundAuditService
) {

    fun upsert(items: List<SystemCodeMasterRequestItem>): ProductMasterDetail {
        val commands = items.map { it.toCommand() }
        val result = try {
            systemCodeMasterUpsertService.upsert(commands)
        } catch (ex: RuntimeException) {
            recordAccepted(items.size, success = 0, failure = commands.size)
            throw ex
        }

        recordAccepted(items.size, success = result.successCount, failure = result.failureCount)
        return ProductMasterDetail(
            successCount = result.successCount,
            failureCount = result.failureCount,
            failures = result.failures.map { FailureItem(it.identifier, it.reason) }
        )
    }

    private fun SystemCodeMasterRequestItem.toCommand(): SystemCodeMasterUpsertCommand =
        SystemCodeMasterUpsertCommand(
            companyCode = companyCode,
            groupCode = groupCode,
            detailCode = detailCode,
            groupCodeName = groupCodeName,
            detailCodeName = detailCodeName,
            seq = seq
        )

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
