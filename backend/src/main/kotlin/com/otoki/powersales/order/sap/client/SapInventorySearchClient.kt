package com.otoki.powersales.order.sap.client

/**
 * SAP `InventorySearch` 실시간 호출 인터페이스 (Spec #592 Q4/Q5).
 *
 * 주문 등록 사전 검증에서 단위 환산(`ConversionQuantity`) / 공급제한(`SupplyLimitQTY`) /
 * 제품마스터 메타(`ProductName`/`UnitPrice`) 를 일괄 조회한다.
 *
 * 단일 SAP 호출로 라인 productCode 전체를 한 번에 조회 (트랜잭션 내 재사용).
 *
 * 실제 SAP 호출 구현은 #594 후속 스펙에서 추가 예정. 본 스펙은 interface 만 정의.
 */
interface SapInventorySearchClient {

    /**
     * @param accountId 거래처 ID (SAP 측 거래처 코드 매핑 책임)
     * @param productCodes 조회할 제품 코드 목록 (요청 라인 전체)
     * @return productCode → 제품별 정보 맵. 응답에 누락된 productCode 는 맵에 없음.
     */
    fun search(accountId: Long, productCodes: List<String>): Map<String, InventoryInfo>
}

data class InventoryInfo(
    val productCode: String,
    val productName: String,
    val conversionQuantity: Int,
    val supplyLimitQuantity: Int,
    val unitPrice: java.math.BigDecimal,
)
