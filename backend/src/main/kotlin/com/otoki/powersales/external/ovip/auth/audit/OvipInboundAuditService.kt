package com.otoki.powersales.external.ovip.auth.audit

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class OvipInboundAuditService(
    private val auditRepository: OvipInboundAuditRepository
) {

    /**
     * 감사 로그 적재. 호출 컨텍스트의 트랜잭션 결과(예외 롤백)와 무관하게 로그가 남도록
     * REQUIRES_NEW 트랜잭션을 사용한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun record(audit: OvipInboundAudit): OvipInboundAudit {
        return auditRepository.save(audit)
    }
}
