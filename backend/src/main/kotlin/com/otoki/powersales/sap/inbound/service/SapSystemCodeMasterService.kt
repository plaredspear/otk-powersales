package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.common.service.SystemCodeMasterUpsertService
import com.otoki.powersales.common.service.dto.SystemCodeMasterUpsertCommand
import com.otoki.powersales.sap.auth.audit.SapInboundAccepted
import com.otoki.powersales.sap.inbound.dto.product.FailureItem
import com.otoki.powersales.sap.inbound.dto.product.ProductMasterDetail
import com.otoki.powersales.sap.inbound.dto.product.SystemCodeMasterRequestItem
import org.springframework.stereotype.Service

/**
 * SAP 시스템 공통 코드 마스터 인바운드 어댑터. (Spec #559 / 어댑터-도메인 분리: #635 P1-B / audit AOP 통합: #639)
 *
 * 책임:
 * - SAP 페이로드 [SystemCodeMasterRequestItem] → 도메인 커맨드 [SystemCodeMasterUpsertCommand] 매핑
 * - 도메인 서비스 [SystemCodeMasterUpsertService.upsert] 호출
 * - 도메인 결과 → SAP 응답 [ProductMasterDetail] 매핑
 *
 * `REQUEST_ACCEPTED` audit 기록은 [com.otoki.powersales.sap.auth.audit.SapInboundAuditAspect] 가
 * `@SapInboundAccepted("items")` annotation 을 트리거로 공통 처리 (#639).
 *
 * 트랜잭션은 도메인 측이 관리하며 어댑터는 `@Transactional` 을 부착하지 않는다.
 * UPSERT 키 / 부분 실패 시멘틱은 [SystemCodeMasterUpsertService] KDoc 참조.
 */
@Service
class SapSystemCodeMasterService(
    private val systemCodeMasterUpsertService: SystemCodeMasterUpsertService
) {

    @SapInboundAccepted("items")
    fun upsert(items: List<SystemCodeMasterRequestItem>): ProductMasterDetail {
        val commands = items.map { it.toCommand() }
        val result = systemCodeMasterUpsertService.upsert(commands)
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
}
