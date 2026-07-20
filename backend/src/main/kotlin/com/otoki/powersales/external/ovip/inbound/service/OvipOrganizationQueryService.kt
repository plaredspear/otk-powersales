package com.otoki.powersales.external.ovip.inbound.service

import com.otoki.powersales.domain.org.organization.repository.OrganizationRepository
import com.otoki.powersales.external.ovip.auth.audit.OvipInboundAudit
import com.otoki.powersales.external.ovip.auth.audit.OvipInboundAuditEventType
import com.otoki.powersales.external.ovip.auth.audit.OvipInboundAuditService
import com.otoki.powersales.external.ovip.inbound.dto.OrganizationRow
import com.otoki.powersales.external.ovip.inbound.dto.SnapshotListResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * OVIP 인바운드 — 조직(Organization) 전량 스냅샷 조회 서비스.
 *
 * 거래처([OvipAccountQueryService]) / MFEIS([OvipMfeisQueryService]) 와 달리 **페이지네이션이 없다** —
 * 조직은 SAP 동기화 때 PK 가 전면 재발번되어 keyset 커서가 인출 도중 무효화될 수 있고, 지점 트리
 * 마스터라 건수도 페이지를 나눌 규모가 아니기 때문 (상세 근거는
 * [com.otoki.powersales.domain.org.organization.repository.OrganizationRepositoryCustom.findAllSnapshot] KDoc).
 * 따라서 요청 파라미터도 없고, 한 번의 호출로 전건이 반환된다.
 *
 * 노출 필드는 entity 전 컬럼([OrganizationRow]).
 */
@Service
class OvipOrganizationQueryService(
    private val repository: OrganizationRepository,
    private val auditService: OvipInboundAuditService,
) {

    @Transactional(readOnly = true)
    fun searchAll(
        clientId: String?,
        clientIp: String,
    ): SnapshotListResponse<OrganizationRow> {
        val rows = repository.findAllSnapshot().map(OrganizationRow::from)

        auditService.record(
            OvipInboundAudit(
                eventType = OvipInboundAuditEventType.REQUEST_ACCEPTED,
                clientId = clientId,
                endpoint = ENDPOINT,
                httpMethod = "POST",
                clientIp = clientIp,
                scope = OvipInboundScopes.READ,
                receivedCount = rows.size,
                reason = "full snapshot",
            )
        )

        return SnapshotListResponse.of(rows)
    }

    companion object {
        const val ENDPOINT = "/api/v1/ovip/organization/search"
    }
}
