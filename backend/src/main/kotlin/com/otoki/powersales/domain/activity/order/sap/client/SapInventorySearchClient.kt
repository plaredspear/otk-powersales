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
    /**
     * SAP 가 결정하는 발주 단위 (레거시 `MinOrderingUnit`). 주문 등록 시 클라이언트 unit 을 무시하고
     * 이 값으로 SAP 송신 단위/수량 환산을 강제한다 (레거시 OrderController.java:664 `setUnit(minOrderingUnit)`).
     * SAP 응답에서 공란/누락이면 빈 문자열 — 레거시는 빈 단위를 그대로 SAP 로 전송한다.
     */
    val minOrderingUnit: String,
    /**
     * SAP 환산수량 (`ConversionQuantity`) — 총 EA → 발주단위 수량 환산 분모.
     *
     * **응답 공란/누락/0 이면 null 이며, 기본값 1 로 대체하지 않는다.** 1 로 대체하면 총 EA 가 그대로
     * 박스 수량으로 승격되어 (예: 총 20EA → SAP 로 20 BOX) 입수 배수만큼 과다 주문이 조용히 성립한다
     * (2026-08-12 OR00001615 실사고). 레거시는 이 자리에 sentinel `-1` 이 들어가 `총EA / -1` 음수가
     * SAP 단에서 거절되며 등록이 실패했다 (`OrderController.java:548,641`) — null 로 표현하고 호출자가
     * 라인 차단하는 것이 그 결과와 정합한다.
     */
    val conversionQuantity: Int?,
    val supplyLimitQuantity: Int,
    val unitPrice: BigDecimal,
    /**
     * SAP 제품별 상태 메시지 (레거시 `Message`). `"OK"` 가 아닌 **사유 문자열이 있으면 주문 불가** —
     * 레거시 `OrderController.java:573` (`/* 메시지가 OK가 아니면 주문 불가 */`) 게이트.
     *
     * 레거시는 기본값 `""` 이라 누락도 차단했으나, 신규는 **누락(null/공백)은 통과 + WARN** 으로 둔다.
     * SAP 가 필드를 빼고 응답하는 형태 변화 하나로 전 주문이 마비되는 것을 막기 위함.
     */
    val message: String?,
)
