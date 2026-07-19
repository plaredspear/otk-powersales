package com.otoki.powersales.external.rdp.inbound.service

import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.external.rdp.auth.audit.RdpInboundAuditService
import com.otoki.powersales.external.rdp.inbound.config.RdpInboundProperties
import com.otoki.powersales.external.rdp.inbound.dto.AccountRow
import com.otoki.powersales.external.rdp.inbound.dto.SnapshotPageResponse
import com.otoki.powersales.external.rdp.inbound.dto.SnapshotSearchRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * RDP 인바운드 — 거래처(Account) 전량 스냅샷 조회 서비스.
 *
 * MFEIS([RdpMfeisQueryService]) 와 달리 필수 필터가 없다 — 거래처는 마스터라 year/month 같은 자연
 * 파티션 축이 없고 전량 인출 자체가 목적이므로, 부하 방어는 PK keyset 커서 + size clamp 로만 수행한다.
 * 노출 필드는 entity 전 컬럼([AccountRow]).
 *
 * 클라이언트는 nextCursor 가 null 이 될 때까지 반복 호출하여 전량을 순차 인출한다.
 */
@Service
class RdpAccountQueryService(
    private val repository: AccountRepository,
    private val properties: RdpInboundProperties,
    private val auditService: RdpInboundAuditService,
) {

    @Transactional(readOnly = true)
    fun search(
        request: SnapshotSearchRequest,
        clientId: String?,
        clientIp: String,
    ): SnapshotPageResponse<AccountRow> = SnapshotKeysetPager.page(
        request = request,
        pagination = properties.account,
        endpoint = ENDPOINT,
        clientId = clientId,
        clientIp = clientIp,
        auditService = auditService,
        fetch = { cursor, limit -> repository.findSnapshotByKeyset(cursor, limit) },
        idOf = { it.account.id },
        toRow = AccountRow::from,
    )

    companion object {
        const val ENDPOINT = "/api/v1/rdp/account/search"
    }
}
