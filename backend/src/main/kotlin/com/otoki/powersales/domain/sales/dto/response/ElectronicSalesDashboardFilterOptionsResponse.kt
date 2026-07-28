package com.otoki.powersales.domain.sales.dto.response

import java.math.BigDecimal

/**
 * 전산실적 대시보드 조회 조건 옵션 — 유통형태 / 거래처유형 / 제품 중·소분류.
 *
 * 「월 매출(전산실적)」 화면 필터 드롭다운의 옵션 원천. 모두 메인 DB 마스터에서 distinct 산출:
 * - 유통형태 = Account 거래처상태코드+거래처타입 조합 라벨 (예 "02 슈퍼")
 * - 거래처유형 = Account ABC유형코드+ABC유형 조합 라벨 (예 "6111 이마트")
 * - 분류 = Product 중분류(category2) → 소분류(category3) 종속 트리 (바코드 보유 제품 한정 —
 *   POS `UPC_CD` 매칭이 불가능한 제품의 분류는 옵션에서 제외)
 * - [dependentAccountTypes] = 유통형태 라벨 → 해당 유통형태에 실제 존재하는 거래처유형 라벨 목록.
 *   유통형태 선택 시 거래처유형 드롭다운을 종속 필터링하는 데 사용 (미선택 시 [accountTypes] 전체).
 */
data class ElectronicSalesDashboardFilterOptionsResponse(
    val distributionChannels: List<String>,
    val accountTypes: List<String>,
    val categories: List<CategoryGroup>,
    val dependentAccountTypes: Map<String, List<String>>,
) {
    /** 중분류 1건과 그 하위 소분류 목록. */
    data class CategoryGroup(
        val category2: String,
        val category3s: List<String>,
    )
}

/**
 * 전산실적 대시보드 제품 검색 결과 1건 — 제품명/제품코드/바코드 keyword 매칭.
 * 바코드 보유 제품만 반환 (POS `UPC_CD IN` 필터에 사용 가능해야 함).
 */
data class ElectronicSalesProductLookupItem(
    val productId: Long,
    val name: String?,
    val productCode: String?,
    val barcode: String,
)

/**
 * 전산실적/POS 제품 고급 검색 응답 (페이징).
 *
 * 드롭다운 빠른 검색([ElectronicSalesProductLookupItem])이 상위 N건만 반환해 결과가 많은 키워드에서
 * 뒤쪽 제품에 도달할 수 없는 문제를 해소한다. 제품 관리 화면의 `ProductListResponse` 를 쓰지 않고
 * 전용 DTO 를 두는 이유는 [ElectronicSalesProductAdvancedItem] 의 `barcode` 때문이다.
 */
data class ElectronicSalesProductAdvancedResponse(
    val content: List<ElectronicSalesProductAdvancedItem>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

/**
 * 제품 고급 검색 결과 1건 — 결과 그리드 표시 컬럼 + 대표 바코드.
 *
 * [barcode] 는 화면이 드롭다운과 동일한 `제품명 (제품코드 / 바코드)` 라벨을 만들 수 있게 함께
 * 내린다. 검색 대상이 바코드 보유 제품 한정이므로 항상 값이 존재한다 (방어적으로 nullable 유지).
 * `productStatus` 는 저장값이 아니라 화면 표시명("판매중"/"단종") 이다.
 */
data class ElectronicSalesProductAdvancedItem(
    val id: Long,
    val productCode: String?,
    val name: String?,
    val barcode: String?,
    val category1: String?,
    val category2: String?,
    val category3: String?,
    val standardUnitPrice: BigDecimal?,
    val unit: String?,
    val storageCondition: String?,
    val productStatus: String?,
    val launchDate: String?,
)
