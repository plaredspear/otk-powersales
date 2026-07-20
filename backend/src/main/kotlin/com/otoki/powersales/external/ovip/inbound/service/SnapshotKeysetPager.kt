package com.otoki.powersales.external.ovip.inbound.service

import com.otoki.powersales.external.ovip.auth.audit.OvipInboundAudit
import com.otoki.powersales.external.ovip.auth.audit.OvipInboundAuditEventType
import com.otoki.powersales.external.ovip.auth.audit.OvipInboundAuditService
import com.otoki.powersales.external.ovip.inbound.config.OvipInboundProperties
import com.otoki.powersales.external.ovip.inbound.dto.SnapshotPageResponse
import com.otoki.powersales.external.ovip.inbound.dto.SnapshotSearchRequest

/**
 * 마스터 전량 스냅샷 keyset 페이지네이션 공통 처리.
 *
 * size clamp → (pageSize + 1) 조회 → hasNext 판정 → nextCursor 산출 → audit 적재 순서가 커서 기반
 * 조회 전반에 동일하므로 본 헬퍼로 단일화한다. 각 서비스는 "무엇을 조회하고 어떻게 row 로 변환하는지"만 넘긴다.
 *
 * 조직처럼 페이지를 나누지 않고 전건을 반환하는 조회는 본 헬퍼를 쓰지 않는다
 * ([OvipOrganizationQueryService] 참조).
 */
object SnapshotKeysetPager {

    /**
     * @param request    커서/size 요청
     * @param pagination 해당 엔드포인트의 기본/최대 페이지 크기 설정
     * @param endpoint   audit 에 남길 엔드포인트 경로
     * @param fetch      `(cursor, limit)` → entity 목록. limit 은 hasNext 판정분(+1)이 포함된 값
     * @param idOf       entity → PK (keyset 커서 기준)
     * @param toRow      entity → 외부 노출 row
     */
    fun <E, R> page(
        request: SnapshotSearchRequest,
        pagination: OvipInboundProperties.Pagination,
        endpoint: String,
        clientId: String?,
        clientIp: String,
        auditService: OvipInboundAuditService,
        fetch: (cursor: Long?, limit: Int) -> List<E>,
        idOf: (E) -> Long,
        toRow: (E) -> R,
    ): SnapshotPageResponse<R> {
        val pageSize = resolvePageSize(request.size, pagination)

        // hasNext 판정을 위해 (pageSize + 1) 건 조회 후 초과분으로 다음 페이지 존재 여부 확인.
        val fetched = fetch(request.cursor, pageSize + 1)

        val hasNext = fetched.size > pageSize
        val pageRows = if (hasNext) fetched.subList(0, pageSize) else fetched
        val nextCursor = if (hasNext) idOf(pageRows.last()) else null

        auditService.record(
            OvipInboundAudit(
                eventType = OvipInboundAuditEventType.REQUEST_ACCEPTED,
                clientId = clientId,
                endpoint = endpoint,
                httpMethod = "POST",
                clientIp = clientIp,
                scope = OvipInboundScopes.READ,
                receivedCount = pageRows.size,
                reason = "cursor=${request.cursor} size=$pageSize hasNext=$hasNext",
            )
        )

        return SnapshotPageResponse(
            items = pageRows.map(toRow),
            nextCursor = nextCursor,
            hasNext = hasNext,
        )
    }

    /** size 미지정/0 이하 → 기본값, 상한 초과 → 상한으로 clamp. */
    private fun resolvePageSize(requested: Int?, pagination: OvipInboundProperties.Pagination): Int {
        val max = pagination.maxPageSize
        val default = pagination.defaultPageSize
        return when {
            requested == null || requested <= 0 -> default
            requested > max -> max
            else -> requested
        }
    }
}
