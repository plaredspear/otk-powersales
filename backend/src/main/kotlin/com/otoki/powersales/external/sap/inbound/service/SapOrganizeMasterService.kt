package com.otoki.powersales.external.sap.inbound.service

import com.otoki.powersales.external.sap.auth.audit.SapInboundAccepted
import com.otoki.powersales.external.sap.auth.sanity.SapDestructiveEndpoint
import com.otoki.powersales.external.sap.inbound.dto.organize.OrganizeMasterDetail
import com.otoki.powersales.external.sap.inbound.dto.organize.OrganizeMasterRequestItem
import com.otoki.powersales.external.sap.inbound.exception.SapInvalidPayloadException
import com.otoki.powersales.domain.org.organization.service.OrganizationReplaceService
import com.otoki.powersales.domain.org.organization.service.dto.OrganizationReplaceCommand
import org.springframework.stereotype.Service

/**
 * SAP 조직 마스터 인바운드 어댑터. (Spec #556 / 어댑터-도메인 분리: #635 P3-B / audit AOP 통합: #639)
 *
 * 책임:
 * - 페이로드 형식 검증 ([validateItems]) — 행 전체 null 거부
 * - SAP 페이로드 [OrganizeMasterRequestItem] → 도메인 커맨드 [OrganizationReplaceCommand] 매핑
 * - 도메인 서비스 [OrganizationReplaceService.replaceAll] 호출 (파괴적 전체 교체)
 * - 도메인 결과 [com.otoki.powersales.domain.org.organization.service.dto.OrganizationReplaceResult] → SAP 응답 [OrganizeMasterDetail] 매핑
 * - `@SapDestructiveEndpoint(threshold = 20)` AOP 어노테이션 잔류 — sanity check (받은 건수 0 / ±20% 변동) 는 본 어댑터 메서드 진입 전 처리
 * - `@SapInboundAccepted` annotation — `REQUEST_ACCEPTED` audit (reason="success={N} failure=0") 은 [com.otoki.powersales.external.sap.auth.audit.SapInboundAuditAspect]
 *   가 처리 (#639). sanity aspect 가 별도로 기록하는 reason 미포함 `REQUEST_ACCEPTED` 와 공존하며,
 *   sanity 거부 시 Aspect 는 [com.otoki.powersales.external.sap.auth.exception.SapSanityCheckFailedException] 을 catch 하여 즉시 rethrow (audit 미기록).
 *
 * 트랜잭션은 도메인 측이 관리하며 어댑터는 `@Transactional` 을 부착하지 않는다.
 * advisory lock 도 도메인 서비스 내부로 이동 (도메인 무결성 책임).
 */
@Service
class SapOrganizeMasterService(
    private val organizationReplaceService: OrganizationReplaceService
) {

    @SapDestructiveEndpoint(threshold = 20, countArgName = "items")
    @SapInboundAccepted("items")
    fun replaceAll(items: List<OrganizeMasterRequestItem>): OrganizeMasterDetail {
        validateItems(items)
        val commands = items.map { it.toCommand() }
        val result = organizationReplaceService.replaceAll(commands)
        return OrganizeMasterDetail(
            successCount = result.replacedCount,
            failureCount = 0,
            failures = emptyList()
        )
    }

    private fun validateItems(items: List<OrganizeMasterRequestItem>) {
        items.forEachIndexed { index, item ->
            if (item.isAllNull()) {
                throw SapInvalidPayloadException("필수 필드 누락 (line ${index + 1})")
            }
        }
    }

    private fun OrganizeMasterRequestItem.toCommand(): OrganizationReplaceCommand = OrganizationReplaceCommand(
        ccCd2 = ccCd2,
        orgCd2 = orgCd2,
        orgNm2 = orgNm2,
        ccCd3 = ccCd3,
        orgCd3 = orgCd3,
        orgNm3 = orgNm3,
        ccCd4 = ccCd4,
        orgCd4 = orgCd4,
        orgNm4 = orgNm4,
        ccCd5 = ccCd5,
        orgCd5 = orgCd5,
        orgNm5 = orgNm5
    )
}
