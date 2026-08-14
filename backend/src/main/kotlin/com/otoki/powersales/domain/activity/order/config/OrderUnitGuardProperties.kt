package com.otoki.powersales.domain.activity.order.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * 주문 발주단위 가드 설정 (`app.order.unit-guard.*`) — SAP 제품마스터 오염 임시 방어.
 *
 * 배경: SAP InventorySearch(SD03070) 의 `MinOrderingUnit` 이 일시 오염되면(공란/PAC/EA 요동 +
 * ConversionQuantity=1) 총 EA 가 환산 없이 송신되고, SAP 주문등록(SD03050)이 송신 단위를 무시한 채
 * 수량을 자기 마스터 발주단위(BOX)로 재해석해 박스입수 배수만큼 과다 주문이 성립한다
 * (2026-08-10~13 오쉐프 냉장 4종 실사고, ×4 과다 등록 13라인).
 *
 * 동작: [expectedUnits] 에 등록된 제품은 SD03070 응답 발주단위가 기준 단위와 다르면
 * 해당 라인을 주문 불가로 차단한다 ([com.otoki.powersales.domain.activity.order.exception.OrderLineViolation.Reason.UNIT_MISMATCH]).
 * 미등록 제품은 종전과 동일하게 통과한다.
 *
 * @property expectedUnits 제품코드 → 기준 발주단위 (예: "25120001" → "BOX"). 비면 가드 전체 무효.
 */
@ConfigurationProperties(prefix = "app.order.unit-guard")
data class OrderUnitGuardProperties(
    val expectedUnits: Map<String, String> = emptyMap(),
)

@Configuration
@EnableConfigurationProperties(OrderUnitGuardProperties::class)
class OrderUnitGuardConfig
