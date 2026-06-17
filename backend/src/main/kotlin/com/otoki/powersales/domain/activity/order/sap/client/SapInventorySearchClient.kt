package com.otoki.powersales.domain.activity.order.sap.client

import java.math.BigDecimal
import java.time.LocalDate

/**
 * SAP `InventorySearch` 실시간 호출 인터페이스 (Spec #592 Q4/Q5).
 *
 * 주문 등록 사전 검증에서 단위 환산(`ConversionQuantity`) / 공급제한(`SupplyLimitQTY`) 을 SAP(SD03070) 에서,
 * 제품마스터 메타(`ProductName`/`UnitPrice`) 를 자체 `Product` 마스터에서 보강하여 일괄 조회한다.
 * (레거시 `IF_REST_MOBILE_InventorySearch` 응답엔 단가가 없으므로 단가는 SAP 가 아닌 자체 마스터 출처.)
 *
 * 단일 SAP 호출로 라인 productCode 전체를 한 번에 조회 (트랜잭션 내 재사용).
 *
 * - prod/dev/staging: [RealSapInventorySearchClient] (`@Profile("!local")`) — 실제 SAP 호출
 * - local: [StubSapInventorySearchClient] (`@Profile("local")`) — 검증 통과 stub
 */
interface SapInventorySearchClient {

    /**
     * @param accountId 거래처 ID (impl 이 SAP 거래처 코드 `external_key` 로 매핑)
     * @param productCodes 조회할 제품 코드 목록 (요청 라인 전체)
     * @param deliveryDate 납기 요청일 (레거시 `DeliveryRequestDate` — 재고/공급 가용성 기준일)
     * @return productCode → 제품별 정보 맵. 응답에 누락된 productCode 는 맵에 없음.
     */
    fun search(accountId: Long, productCodes: List<String>, deliveryDate: LocalDate): Map<String, InventoryInfo>
}

data class InventoryInfo(
    val productCode: String,
    val productName: String,
    val conversionQuantity: Int,
    val supplyLimitQuantity: Int,
    val unitPrice: BigDecimal,
)
