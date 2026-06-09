package com.otoki.pos.repository

import com.otoki.pos.entity.LivePosSalesDaily
import com.otoki.pos.entity.LivePosSalesDailyId
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param
import java.math.BigDecimal

/**
 * POS `live_pos_sales_dh` POS매출 조회 전용 Repository.
 *
 * ## read-only 가드
 * marker [Repository] 만 상속 → mutation API 부재 (컴파일 시점 차단). ORORA / 전산매출 repository 와 동일.
 *
 * ## 조회
 * [aggregateByProduct] 는 레거시 `PosMapper.xml#selectPosData` 정합 — 단일 거래처(`CUST_CD`) +
 * 기간(`DATE BETWEEN`) 으로 제품별 `SUM(SALES_AMT)` / `SUM(SALES_QTY)` 집계.
 */
interface LivePosSalesDailyRepository : Repository<LivePosSalesDaily, LivePosSalesDailyId> {

	/**
	 * 거래처 1곳 + 기간의 제품별 POS매출 집계 (레거시 `selectPosData` 동등).
	 *
	 * 레거시는 `BARCODE` 도 group key 였으나, 신규는 제품(`ITEM_CD`) 단위 집계로 통일하고 대표
	 * `BARCODE` 1건을 `MAX` 로 노출한다 (제품 1건당 바코드 1개 가정).
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
			       MAX("BARCODE")       AS "barcode",
			       SUM("SALES_AMT")     AS "salesAmt",
			       SUM("SALES_QTY")     AS "salesQty"
			FROM public.live_pos_sales_dh
			WHERE "DATE" BETWEEN to_date(:startDate, 'YYYY-MM-DD')
			                 AND to_date(:endDate,   'YYYY-MM-DD')
			  AND "CUST_CD" = :custCd
			GROUP BY "ITEM_CD", "ITEM_NM"
			ORDER BY SUM("SALES_AMT") DESC
		"""
	)
	fun aggregateByProduct(
		@Param("custCd") custCd: String,
		@Param("startDate") startDate: String,
		@Param("endDate") endDate: String,
	): List<PosSalesRow>

	/**
	 * 거래처 1곳 + 기간 + 선택 바코드 목록의 제품별 POS매출 집계 (레거시 `selectPosData` 의
	 * `BARCODE IN (...)` 필터 동등).
	 *
	 * 레거시 posmain.jsp 는 매출 조회 제품(`#add-prd-list`) 에서 체크한 제품의 `BARCODE` 목록으로
	 * 필터해 품목별 명세를 조회했다. [barcodes] 가 비어 있으면 호출하지 말 것(빈 `IN ()` 은 SQL
	 * 오류) — 미지정 조회는 [aggregateByProduct] 를 사용한다.
	 *
	 * @param custCd 거래처 코드 (legacy 패딩 `"000" + accountCode` 적용된 값)
	 * @param startDate 조회 시작일 `YYYY-MM-DD`
	 * @param endDate 조회 종료일 `YYYY-MM-DD`
	 * @param barcodes 필터할 바코드 목록 (1건 이상)
	 */
	@Query(
		nativeQuery = true,
		value = """
			SELECT "ITEM_CD"            AS "itemCd",
			       "ITEM_NM"            AS "itemNm",
			       MAX("BARCODE")       AS "barcode",
			       SUM("SALES_AMT")     AS "salesAmt",
			       SUM("SALES_QTY")     AS "salesQty"
			FROM public.live_pos_sales_dh
			WHERE "DATE" BETWEEN to_date(:startDate, 'YYYY-MM-DD')
			                 AND to_date(:endDate,   'YYYY-MM-DD')
			  AND "CUST_CD" = :custCd
			  AND "BARCODE" IN (:barcodes)
			GROUP BY "ITEM_CD", "ITEM_NM"
			ORDER BY SUM("SALES_AMT") DESC
		"""
	)
	fun aggregateByProductAndBarcodes(
		@Param("custCd") custCd: String,
		@Param("startDate") startDate: String,
		@Param("endDate") endDate: String,
		@Param("barcodes") barcodes: List<String>,
	): List<PosSalesRow>
}

/**
 * [LivePosSalesDailyRepository.aggregateByProduct] native 집계 결과 projection.
 * alias(`itemCd`/`itemNm`/`barcode`/`salesAmt`/`salesQty`) ↔ getter 매핑.
 */
interface PosSalesRow {
	fun getItemCd(): String
	fun getItemNm(): String?
	fun getBarcode(): String?
	fun getSalesAmt(): BigDecimal?
	fun getSalesQty(): BigDecimal?
}
