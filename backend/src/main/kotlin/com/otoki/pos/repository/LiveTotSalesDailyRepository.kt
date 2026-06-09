package com.otoki.pos.repository

import com.otoki.pos.entity.LiveTotSalesDaily
import com.otoki.pos.entity.LiveTotSalesDailyId
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param
import java.math.BigDecimal

/**
 * POS `live_tot_sales_dh` 전산매출 조회 전용 Repository.
 *
 * ## read-only 가드
 * marker [Repository] 만 상속 → mutation API 부재 (컴파일 시점 차단). ORORA repository 와 동일.
 *
 * ## 조회
 * [aggregateByProduct] 는 레거시 `PosMapper.xml#SelectAbcData` 정합 — 단일 거래처(`CUST_CD`) +
 * 기간(`YMD_ID BETWEEN`) 으로 제품별 `SUM(SALES_RAMT)` / `SUM(SALES_RQTY)` 집계.
 */
interface LiveTotSalesDailyRepository : Repository<LiveTotSalesDaily, LiveTotSalesDailyId> {

	/**
	 * 거래처 1곳 + 기간의 제품별 전산매출 집계 (레거시 `SelectAbcData` 동등).
	 *
	 * @param custCd 거래처 코드 (legacy 패딩 `"000" + accountCode` 적용된 값)
	 * @param startDate 조회 시작일 `YYYY-MM-DD`
	 * @param endDate 조회 종료일 `YYYY-MM-DD`
	 */
	@Query(
		nativeQuery = true,
		value = """
			SELECT "ITEM_CD"            AS "itemCd",
			       "ITEM_NM"            AS "itemNm",
			       SUM("SALES_RAMT")    AS "salesAmt",
			       SUM("SALES_RQTY")    AS "salesQty"
			FROM public.live_tot_sales_dh
			WHERE "YMD_ID" BETWEEN to_date(:startDate, 'YYYY-MM-DD')
			                   AND to_date(:endDate,   'YYYY-MM-DD')
			  AND "CUST_CD" = :custCd
			GROUP BY "ITEM_CD", "ITEM_NM"
			ORDER BY SUM("SALES_RAMT") DESC
		"""
	)
	fun aggregateByProduct(
		@Param("custCd") custCd: String,
		@Param("startDate") startDate: String,
		@Param("endDate") endDate: String,
	): List<ElectronicSalesRow>

	/**
	 * 거래처 1곳 + 기간 + 바코드 목록(`UPC_CD IN`) 의 제품별 전산매출 집계
	 * (레거시 `PosMapper.xml#SelectAbcData` 의 `productCd` IN 분기 동등).
	 *
	 * [aggregateByProduct] 와 달리 결과 카드의 "바코드" 표시를 위해 `UPC_CD` 를 함께 반환하고
	 * `UPC_CD` 단위까지 묶어 집계한다. 매출 조회 제품을 선택한 경우에만 사용한다.
	 *
	 * @param custCd 거래처 코드 (legacy 패딩 `"000" + accountCode` 적용된 값)
	 * @param startDate 조회 시작일 `YYYY-MM-DD`
	 * @param endDate 조회 종료일 `YYYY-MM-DD`
	 * @param barcodes 매출 조회 제품의 바코드 목록 (`UPC_CD`)
	 */
	@Query(
		nativeQuery = true,
		value = """
			SELECT "ITEM_CD"            AS "itemCd",
			       "ITEM_NM"            AS "itemNm",
			       "UPC_CD"             AS "barcode",
			       SUM("SALES_RAMT")    AS "salesAmt",
			       SUM("SALES_RQTY")    AS "salesQty"
			FROM public.live_tot_sales_dh
			WHERE "YMD_ID" BETWEEN to_date(:startDate, 'YYYY-MM-DD')
			                   AND to_date(:endDate,   'YYYY-MM-DD')
			  AND "CUST_CD" = :custCd
			  AND "UPC_CD" IN (:barcodes)
			GROUP BY "ITEM_CD", "ITEM_NM", "UPC_CD"
			ORDER BY SUM("SALES_RAMT") DESC
		"""
	)
	fun aggregateByProductBarcodes(
		@Param("custCd") custCd: String,
		@Param("startDate") startDate: String,
		@Param("endDate") endDate: String,
		@Param("barcodes") barcodes: List<String>,
	): List<ElectronicSalesProductRow>

	/**
	 * 거래처 N곳 + 기간의 거래처별 전산매출 합계 집계 (web admin 명세 테이블용).
	 *
	 * [aggregateByProduct] 가 제품별 집계인 반면, 본 query 는 `CUST_CD` 단위로 묶어
	 * 거래처당 `SUM(SALES_RAMT)` / `SUM(SALES_RQTY)` 1 row 를 반환한다. 권한 범위 거래처
	 * 다건을 1 trip 으로 집계해 N+1 을 회피.
	 *
	 * @param custCds 거래처 코드 목록 (legacy 패딩 `"000" + accountCode` 적용된 값)
	 * @param startDate 조회 시작일 `YYYY-MM-DD`
	 * @param endDate 조회 종료일 `YYYY-MM-DD`
	 */
	@Query(
		nativeQuery = true,
		value = """
			SELECT "CUST_CD"           AS "custCd",
			       SUM("SALES_RAMT")   AS "salesAmt",
			       SUM("SALES_RQTY")   AS "salesQty"
			FROM public.live_tot_sales_dh
			WHERE "YMD_ID" BETWEEN to_date(:startDate, 'YYYY-MM-DD')
			                   AND to_date(:endDate,   'YYYY-MM-DD')
			  AND "CUST_CD" IN (:custCds)
			GROUP BY "CUST_CD"
		"""
	)
	fun aggregateByCustomer(
		@Param("custCds") custCds: List<String>,
		@Param("startDate") startDate: String,
		@Param("endDate") endDate: String,
	): List<ElectronicSalesCustomerRow>
}

/**
 * [LiveTotSalesDailyRepository.aggregateByProduct] native 집계 결과 projection.
 * alias(`itemCd`/`itemNm`/`salesAmt`/`salesQty`) ↔ getter 매핑.
 */
interface ElectronicSalesRow {
	fun getItemCd(): String
	fun getItemNm(): String?
	fun getSalesAmt(): BigDecimal?
	fun getSalesQty(): BigDecimal?
}

/**
 * [LiveTotSalesDailyRepository.aggregateByProductBarcodes] native 집계 결과 projection.
 * alias(`itemCd`/`itemNm`/`barcode`/`salesAmt`/`salesQty`) ↔ getter 매핑.
 */
interface ElectronicSalesProductRow {
	fun getItemCd(): String
	fun getItemNm(): String?
	fun getBarcode(): String?
	fun getSalesAmt(): BigDecimal?
	fun getSalesQty(): BigDecimal?
}

/**
 * [LiveTotSalesDailyRepository.aggregateByCustomer] native 집계 결과 projection.
 * alias(`custCd`/`salesAmt`/`salesQty`) ↔ getter 매핑.
 */
interface ElectronicSalesCustomerRow {
	fun getCustCd(): String
	fun getSalesAmt(): BigDecimal?
	fun getSalesQty(): BigDecimal?
}
