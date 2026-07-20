package com.otoki.powersales.external.ovip.inbound.service

import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.external.ovip.auth.audit.OvipInboundAuditService
import com.otoki.powersales.external.ovip.inbound.config.OvipInboundProperties
import com.otoki.powersales.external.ovip.inbound.dto.EmployeeRow
import com.otoki.powersales.external.ovip.inbound.dto.SnapshotPageResponse
import com.otoki.powersales.external.ovip.inbound.dto.SnapshotSearchRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * OVIP 인바운드 — 사원(Employee) 전량 스냅샷 조회 서비스.
 *
 * MFEIS([OvipMfeisQueryService]) 와 달리 필수 필터가 없다 — 사원은 마스터라 year/month 같은 자연
 * 파티션 축이 없고 전량 인출 자체가 목적이므로, 부하 방어는 PK keyset 커서 + size clamp 로만 수행한다.
 * 노출 필드는 `employee` 테이블 전 컬럼([EmployeeRow]) — 인증 정보(`employee_info`) 는 제외한다.
 *
 * 클라이언트는 nextCursor 가 null 이 될 때까지 반복 호출하여 전량을 순차 인출한다.
 */
@Service
class OvipEmployeeQueryService(
    private val repository: EmployeeRepository,
    private val properties: OvipInboundProperties,
    private val auditService: OvipInboundAuditService,
) {

    @Transactional(readOnly = true)
    fun search(
        request: SnapshotSearchRequest,
        clientId: String?,
        clientIp: String,
    ): SnapshotPageResponse<EmployeeRow> = SnapshotKeysetPager.page(
        request = request,
        pagination = properties.employee,
        endpoint = ENDPOINT,
        clientId = clientId,
        clientIp = clientIp,
        auditService = auditService,
        fetch = { cursor, limit -> repository.findSnapshotByKeyset(cursor, limit) },
        idOf = { it.employee.id },
        toRow = EmployeeRow::from,
    )

    companion object {
        const val ENDPOINT = "/api/v1/ovip/employee/search"
    }
}
