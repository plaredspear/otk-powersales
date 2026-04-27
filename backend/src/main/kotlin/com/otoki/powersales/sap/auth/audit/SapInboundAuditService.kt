package com.otoki.powersales.sap.auth.audit

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class SapInboundAuditService(
    private val auditRepository: SapInboundAuditRepository
) {

    /**
     * 감사 로그 적재. 호출 컨텍스트의 트랜잭션 결과(예외 롤백)와 무관하게 로그가 남도록
     * REQUIRES_NEW 트랜잭션을 사용한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun record(audit: SapInboundAudit): SapInboundAudit {
        return auditRepository.save(audit)
    }

    /**
     * 직전 REQUEST_ACCEPTED 이벤트의 received_count 조회. 없으면 null.
     */
    @Transactional(readOnly = true)
    fun findLatestAcceptedCount(endpoint: String, clientId: String): Int? {
        return auditRepository.findLatestByEndpointAndClientAndEvent(
            endpoint = endpoint,
            clientId = clientId,
            eventType = SapInboundAuditEventType.REQUEST_ACCEPTED,
            pageable = PageRequest.of(0, 1)
        ).firstOrNull()?.receivedCount
    }
}
