package com.otoki.powersales.external.sap.inbound.service

import com.otoki.powersales.external.sap.auth.audit.SapInboundAccepted
import com.otoki.powersales.external.sap.inbound.dto.product.FailureItem
import com.otoki.powersales.external.sap.inbound.dto.product.ProductMasterDetail
import com.otoki.powersales.external.sap.inbound.dto.product.ProductMasterRequestItem
import com.otoki.powersales.product.service.ProductUpsertService
import com.otoki.powersales.product.service.dto.ProductUpsertCommand
import org.springframework.stereotype.Service

/**
 * SAP 제품 마스터 인바운드 어댑터. (Spec #559, #575 / 어댑터-도메인 분리: #635 P1-B / audit AOP 통합: #639)
 *
 * 책임:
 * - SAP 페이로드 [ProductMasterRequestItem] → 도메인 커맨드 [ProductUpsertCommand] 매핑
 * - 도메인 서비스 [ProductUpsertService.upsert] 호출
 * - 도메인 결과 → SAP 응답 [ProductMasterDetail] 매핑
 *
 * `REQUEST_ACCEPTED` audit 기록은 [com.otoki.powersales.external.sap.auth.audit.SapInboundAuditAspect] 가
 * `@SapInboundAccepted("items")` annotation 을 트리거로 공통 처리 (#639).
 *
 * 트랜잭션은 도메인 측이 관리하며 어댑터는 `@Transactional` 을 부착하지 않는다.
 * UPSERT 키 / 변환 정책 / 부분 실패 시멘틱은 [ProductUpsertService] KDoc 참조.
 */
@Service
class SapProductMasterService(
    private val productUpsertService: ProductUpsertService
) {

    @SapInboundAccepted("items")
    fun upsert(items: List<ProductMasterRequestItem>): ProductMasterDetail {
        val commands = items.map { it.toCommand() }
        val result = productUpsertService.upsert(commands)
        return ProductMasterDetail(
            successCount = result.successCount,
            failureCount = result.failureCount,
            failures = result.failures.map { FailureItem(it.identifier, it.reason) }
        )
    }

    private fun ProductMasterRequestItem.toCommand(): ProductUpsertCommand = ProductUpsertCommand(
        productCode = productCode,
        productName = productName,
        productBarcode = productBarcode,
        logisticsBarCode = logisticsBarCode,
        categoryCode1 = categoryCode1,
        category1 = category1,
        categoryCode2 = categoryCode2,
        category2 = category2,
        categoryCode3 = categoryCode3,
        category3 = category3,
        productStatus = productStatus,
        standardPrice = standardPrice,
        unit = unit,
        boxReceivingQuantity = boxReceivingQuantity,
        shelfLife = shelfLife,
        shelfLifeUnit = shelfLifeUnit,
        launchDate = launchDate,
        storeCondition = storeCondition,
        productType = productType,
        superTax = superTax,
        tasteGift = tasteGift,
        pallet = pallet
    )
}
