package com.otoki.powersales.domain.sales.service

/**
 * 매출 카테고리 — 레거시 SF `IF_REST_MOBILE_MonthlySalesHistory.cls` 가공 로직 정합.
 *
 * 카테고리 1~4 의 매출 합산은 ORORA `OroraMonthlySalesHistory` 의 ABCClosingAmount1~4 + ShipClosingAmount1~4
 * 필드에 매핑된다 (ABC 채널 + 물류 Ship 합산값).
 */
enum class SalesCategory {
    AMBIENT,
    NOODLE,
    FROZEN_REFRIGERATED,
    OIL_FAT,
}
