package com.otoki.powersales.domain.foundation.product.service

import com.otoki.powersales.domain.foundation.product.dto.response.ProductAppVisibility
import com.otoki.powersales.domain.foundation.product.dto.response.ProductAppVisibilityIssue
import com.otoki.powersales.domain.foundation.product.entity.Product
import com.otoki.powersales.domain.foundation.product.entity.ProductBarcode
import com.otoki.powersales.domain.foundation.product.enums.ProductType

/**
 * 제품이 모바일 앱에서 **검색되는지 / 주문서에 담기는지** 를 판정하고 안 되는 사유를 산출한다.
 *
 * ## 도입 배경
 * "제품 상태가 판매중인데 앱에서 검색이 안 된다" 는 현업 문의가 운영자 선에서 종결되지 못하고
 * 개발자 DB 조회로 넘어왔다. 관리자 화면이 보여주던 값(제품상태 "판매중" / 바코드 필드 값 존재)은
 * 전부 정상이었고, 정작 검색을 좌우하는 `product_barcode` 매칭은 화면에 없었기 때문이다.
 * 본 판정기는 그 판정을 운영자가 볼 수 있는 자리로 끌어올린다.
 *
 * ## 판정 기준 — 실제 앱 동작과 동일 술어
 * - 검색 노출: [com.otoki.powersales.domain.foundation.product.repository.ProductRepositoryCustomImpl]
 *   의 `orderableProductFilter()` 3조건 (발주단위 일치 바코드 존재 / 소분류 가정·업소 / 제품상태 없음).
 *   레거시 Heroku `productMapper.xml selectProduct` 고정 WHERE 이식본이다.
 * - 주문 차단: 레거시 `poplayer.js` 전용상품(`prdType=='2'`, 예외코드 1건) + 시식·증정용(`tgType=='x'/'X'`).
 *   서버 가드는 [com.otoki.powersales.domain.activity.order.service.OrderRequestCreateService] 3-1 / 3-1-b.
 *
 * 판정값(카테고리 / 제품유형 / 시식증정 플래그 / 예외코드)은 본 object 의 상수가 단일 출처이며,
 * 검색 쿼리와 주문 가드가 같은 상수를 참조한다 — 화면 진단과 실제 차단이 어긋나지 않게 하기 위함이다.
 */
object ProductAppVisibilityEvaluator {

    /** 모바일 제품검색 소분류 고정 필터 값 (레거시 `label.properties` 의 category3 / category4). */
    val ORDERABLE_CATEGORY3: List<String> = listOf("가정", "업소")

    /** 전용상품 판정값 (`DKRetail__Product__c.DKRetail__ProductType__c`). */
    val EXCLUSIVE_PRODUCT_TYPE: ProductType = ProductType.PRODUCT_TYPE_2

    /** 전용상품 차단 예외 제품코드 — 옛날_구수한끓여먹는누룽지 450g (레거시 poplayer.js 하드코딩 정합). */
    const val EXCLUSIVE_BLOCK_EXEMPT_CODE: String = "20010042"

    /**
     * 시식·증정용 판정값 (`DKRetail__Product__c.TasteGift__c`, SAP 제품마스터 수신값).
     *
     * SF 필드가 Text(1) 자유 입력이라 값 제약이 없어 레거시도 대소문자 양쪽을 비교했다
     * (`tgType == 'x' || tgType == 'X'`). 비교는 항상 ignoreCase 로 수행한다.
     */
    const val TASTE_GIFT_FLAG: String = "x"

    /**
     * 제품 1건의 앱 노출/주문 가능 여부 판정.
     *
     * [barcodes] 는 해당 제품의 바코드 마스터 전량을 넘긴다. 검색 쿼리의 EXISTS 술어가
     * `is_deleted` 를 거르지 않으므로 **여기서도 거르지 않는다** — 화면 표시용으로 필터링한
     * 목록을 넘기면 실제 앱 동작과 진단이 어긋난다.
     *
     * 사유는 사용자가 다음 행동(어느 시스템의 어느 값을 봐야 하는가)을 알 수 있도록
     * [ProductAppVisibilityIssue.action] 까지 함께 채운다.
     */
    fun evaluate(product: Product, barcodes: List<ProductBarcode>): ProductAppVisibility {
        val issues = listOfNotNull(
            barcodeIssue(product, barcodes),
            categoryIssue(product),
            productStatusIssue(product),
            exclusiveIssue(product),
            tastingGiftIssue(product),
        )

        return ProductAppVisibility(
            searchable = issues.none { it.scope == ProductAppVisibilityIssue.SCOPE_SEARCH },
            orderable = issues.none { it.scope == ProductAppVisibilityIssue.SCOPE_ORDER },
            issues = issues,
        )
    }

    /**
     * 발주단위 일치 바코드 존재 여부 — 검색 탈락 사유 중 가장 진단이 어려운 축.
     *
     * 제품 상세의 "바코드" 필드는 제품마스터 인터페이스 수신값(`product.barcode`)이라 값이 있어도,
     * 검색이 보는 것은 바코드 마스터(`product_barcode`) 행이다. 둘을 혼동하지 않도록 사유를
     * "행 없음" 과 "단위 불일치" 로 나누어 보고한다 — 전자는 전송 누락, 후자는 값 오류다.
     */
    private fun barcodeIssue(product: Product, barcodes: List<ProductBarcode>): ProductAppVisibilityIssue? {
        val usable = barcodes.filter { !it.barcode.isNullOrBlank() }
        if (usable.isEmpty()) {
            return ProductAppVisibilityIssue(
                code = "BARCODE_NOT_REGISTERED",
                scope = ProductAppVisibilityIssue.SCOPE_SEARCH,
                message = "제품 바코드가 등록되어 있지 않습니다 (바코드 마스터 0건).",
                action = "SAP 바코드 마스터(ProductBarcode) 전송 여부를 확인하세요. " +
                    "제품 상세의 '바코드' 값은 제품마스터 수신값이라 값이 있어도 검색에는 쓰이지 않습니다.",
            )
        }

        // 검색 쿼리와 동일 비교 — 컬럼 간 등치라 한쪽이 NULL 이면 SQL 에서도 매칭되지 않는다.
        val productUnit = product.unit
        val matched = productUnit != null && usable.any { it.unit != null && it.unit == productUnit }
        if (matched) return null

        val registeredUnits = usable.mapNotNull { it.unit }.distinct().sorted()
        val registeredUnitsText = if (registeredUnits.isEmpty()) "(단위 없음)" else registeredUnits.joinToString(", ")
        return ProductAppVisibilityIssue(
            code = "BARCODE_UNIT_MISMATCH",
            scope = ProductAppVisibilityIssue.SCOPE_SEARCH,
            message = "발주단위(${productUnit ?: "없음"})와 일치하는 바코드가 없습니다. " +
                "등록된 바코드 단위: $registeredUnitsText",
            action = "SAP 제품마스터의 발주단위(Unit) 또는 바코드 마스터의 단위(ProductUnit) 정정이 필요합니다.",
        )
    }

    private fun categoryIssue(product: Product): ProductAppVisibilityIssue? {
        if (product.productCategory3 in ORDERABLE_CATEGORY3) return null
        return ProductAppVisibilityIssue(
            code = "CATEGORY3_NOT_ORDERABLE",
            scope = ProductAppVisibilityIssue.SCOPE_SEARCH,
            message = "소분류가 ${ORDERABLE_CATEGORY3.joinToString(" / ")} 가 아닙니다 " +
                "(현재: ${product.productCategory3 ?: "없음"}).",
            action = "SAP 제품마스터의 소분류(Category3) 값을 확인하세요.",
        )
    }

    private fun productStatusIssue(product: Product): ProductAppVisibilityIssue? {
        val status = product.productStatus ?: return null
        return ProductAppVisibilityIssue(
            code = "PRODUCT_STATUS_SET",
            scope = ProductAppVisibilityIssue.SCOPE_SEARCH,
            message = "제품상태가 '${status.label}' 입니다 (원본값: ${status.displayName}).",
            action = "판매 중인 제품이면 SAP 제품마스터의 제품상태(ProductStatus) 값을 확인하세요.",
        )
    }

    private fun exclusiveIssue(product: Product): ProductAppVisibilityIssue? {
        if (product.productType != EXCLUSIVE_PRODUCT_TYPE) return null
        if (product.productCode == EXCLUSIVE_BLOCK_EXEMPT_CODE) return null
        return ProductAppVisibilityIssue(
            code = "EXCLUSIVE_PRODUCT",
            scope = ProductAppVisibilityIssue.SCOPE_ORDER,
            message = "전용상품(제품유형 ${EXCLUSIVE_PRODUCT_TYPE.displayName})이라 주문서에 담을 수 없습니다.",
            action = "검색에는 노출되며 주문만 차단됩니다. " +
                "정상 판매 제품이면 SAP 제품마스터의 제품유형(ProductType) 값을 확인하세요.",
        )
    }

    private fun tastingGiftIssue(product: Product): ProductAppVisibilityIssue? {
        if (product.tasteGift?.equals(TASTE_GIFT_FLAG, ignoreCase = true) != true) return null
        return ProductAppVisibilityIssue(
            code = "TASTING_GIFT",
            scope = ProductAppVisibilityIssue.SCOPE_ORDER,
            message = "시식·증정용 상품(TasteGift=${product.tasteGift})이라 주문서에 담을 수 없습니다.",
            action = "검색에는 노출되며 주문만 차단됩니다. " +
                "정상 판매 제품이면 SAP 제품마스터의 증정/시식 구분(TasteGift) 값을 확인하세요.",
        )
    }
}
