package com.otoki.powersales.external.sap.inbound.service

import com.otoki.powersales.external.sap.auth.audit.SapInboundAccepted
import com.otoki.powersales.external.sap.inbound.dto.product.BarcodeMasterRequestItem
import com.otoki.powersales.external.sap.inbound.dto.product.FailureItem
import com.otoki.powersales.external.sap.inbound.dto.product.ProductMasterDetail
import com.otoki.powersales.domain.foundation.product.service.ProductBarcodeUpsertService
import com.otoki.powersales.domain.foundation.product.service.dto.ProductBarcodeUpsertCommand
import org.springframework.stereotype.Service

/**
 * SAP 제품 바코드 마스터 인바운드 어댑터. (Spec #559 / 어댑터-도메인 분리: #635 P1-B / audit AOP 통합: #639)
 *
 * 책임:
 * - SAP 페이로드 [BarcodeMasterRequestItem] → 도메인 커맨드 [ProductBarcodeUpsertCommand] 매핑
 * - 도메인 서비스 [ProductBarcodeUpsertService.upsert] 호출
 * - 도메인 결과 → SAP 응답 [ProductMasterDetail] 매핑
 *
 * `REQUEST_ACCEPTED` audit 기록은 [com.otoki.powersales.external.sap.auth.audit.SapInboundAuditAspect] 가
 * `@SapInboundAccepted("items")` annotation 을 트리거로 공통 처리 (#639).
 *
 * 트랜잭션은 도메인 측이 관리하며 어댑터는 `@Transactional` 을 부착하지 않는다 (audit 가 commit 후 기록되어야 함).
 * UPSERT 키 / Product 매칭 / 부분 실패 시멘틱은 [ProductBarcodeUpsertService] KDoc 참조.
 */
@Service
class SapBarcodeMasterService(
    private val productBarcodeUpsertService: ProductBarcodeUpsertService
) {

    @SapInboundAccepted("items")
    fun upsert(items: List<BarcodeMasterRequestItem>): ProductMasterDetail {
        val commands = items.map { it.toCommand() }
        val result = productBarcodeUpsertService.upsert(commands)
        return ProductMasterDetail(
            successCount = result.successCount,
            failureCount = result.failureCount,
            failures = result.failures.map { FailureItem(it.identifier, it.reason) }
        )
    }

    private fun BarcodeMasterRequestItem.toCommand(): ProductBarcodeUpsertCommand =
        ProductBarcodeUpsertCommand(
            productCode = productCode,
            productName = productName,
            productUnit = productUnit,
            productSequence = productSequence,
            productBarcode = productBarcode
        )
}
