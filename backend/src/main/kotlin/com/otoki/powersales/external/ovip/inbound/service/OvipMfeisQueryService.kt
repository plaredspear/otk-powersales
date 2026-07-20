package com.otoki.powersales.external.ovip.inbound.service

import com.otoki.powersales.domain.activity.schedule.repository.MfeisSnapshotRow
import com.otoki.powersales.domain.activity.schedule.repository.MonthlyFemaleEmployeeIntegrationScheduleRepository
import com.otoki.powersales.external.ovip.auth.audit.OvipInboundAudit
import com.otoki.powersales.external.ovip.auth.audit.OvipInboundAuditEventType
import com.otoki.powersales.external.ovip.auth.audit.OvipInboundAuditService
import com.otoki.powersales.external.ovip.auth.exception.OvipInvalidParameterException
import com.otoki.powersales.external.ovip.inbound.config.OvipInboundProperties
import com.otoki.powersales.external.ovip.inbound.dto.MfeisPageResponse
import com.otoki.powersales.external.ovip.inbound.dto.MfeisScheduleRow
import com.otoki.powersales.external.ovip.inbound.dto.MfeisSearchRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * OVIP 인바운드 — MFEIS(월별 여사원 통합일정) 전량 스냅샷 조회 서비스.
 *
 * 부하 방지: year+month 필수 + PK keyset 커서 + projection 축소 + size clamp.
 * 클라이언트는 nextCursor 가 null 이 될 때까지 반복 호출하여 단일 연월 전량을 순차 인출한다.
 */
@Service
class OvipMfeisQueryService(
    private val repository: MonthlyFemaleEmployeeIntegrationScheduleRepository,
    private val properties: OvipInboundProperties,
    private val auditService: OvipInboundAuditService,
) {

    @Transactional(readOnly = true)
    fun search(request: MfeisSearchRequest, clientId: String?, clientIp: String): MfeisPageResponse {
        val year = request.year?.takeUnless { it.isBlank() }
            ?: throw OvipInvalidParameterException("year 는 필수입니다")
        val month = request.month?.takeUnless { it.isBlank() }
            ?: throw OvipInvalidParameterException("month 는 필수입니다")

        val pageSize = resolvePageSize(request.size)

        // hasNext 판정을 위해 (pageSize + 1) 건 조회 후 초과분으로 다음 페이지 존재 여부 확인.
        val fetched = repository.findSnapshotByKeyset(
            year = year,
            month = month,
            cursor = request.cursor,
            limit = pageSize + 1,
        )

        val hasNext = fetched.size > pageSize
        val pageRows = if (hasNext) fetched.subList(0, pageSize) else fetched
        val nextCursor = if (hasNext) pageRows.last().id else null

        auditService.record(
            OvipInboundAudit(
                eventType = OvipInboundAuditEventType.REQUEST_ACCEPTED,
                clientId = clientId,
                endpoint = ENDPOINT,
                httpMethod = "POST",
                clientIp = clientIp,
                scope = OvipInboundScopes.READ,
                receivedCount = pageRows.size,
                reason = "year=$year month=$month cursor=${request.cursor} size=$pageSize hasNext=$hasNext",
            )
        )

        return MfeisPageResponse(
            items = pageRows.map { it.toResponseRow() },
            nextCursor = nextCursor,
            hasNext = hasNext,
        )
    }

    /** size 미지정/0 이하 → 기본값, 상한 초과 → 상한으로 clamp. */
    private fun resolvePageSize(requested: Int?): Int {
        val max = properties.mfeis.maxPageSize
        val default = properties.mfeis.defaultPageSize
        return when {
            requested == null || requested <= 0 -> default
            requested > max -> max
            else -> requested
        }
    }

    companion object {
        const val ENDPOINT = "/api/v1/ovip/mfeis/search"
    }
}

private fun MfeisSnapshotRow.toResponseRow(): MfeisScheduleRow = MfeisScheduleRow(
    id = id,
    sfid = sfid,
    externalKey = externalKey,
    year = year,
    month = month,
    costCenterCode = costCenterCode,
    orgName = orgName,
    employeeCode = employeeCode,
    employeeName = employeeName,
    title = title,
    accountCode = accountCode,
    accountName = accountName,
    accountBranchName = accountBranchName,
    accountType = accountType,
    abcType = abcType,
    workingCategory1 = workingCategory1,
    workingCategory3 = workingCategory3,
    workingCategory4 = workingCategory4,
    workingCategory5 = workingCategory5,
    numberOfInputs = numberOfInputs,
    equivalentNumberOfWorkingDays = equivalentNumberOfWorkingDays,
    convertedHeadcount = convertedHeadcount,
)
