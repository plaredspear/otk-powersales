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

	/**
	 * 거래처 N곳 + 기간의 거래처별 POS매출 합계 집계 (web admin 거래처별 명세 테이블용).
	 *
	 * [aggregateByProduct] 가 제품별 집계인 반면, 본 query 는 `CUST_CD` 단위로 묶어 거래처당
	 * `SUM(SALES_AMT)` / `SUM(SALES_QTY)` 1 row 를 반환한다. 권한 범위 거래처 다건을 1 trip 으로
	 * 집계해 N+1 을 회피. 전산매출(`live_tot_sales_dh`)의 `aggregateByCustomer` 와 동일 형태.
	 *
	 * @param custCds 거래처 코드 목록 (legacy 패딩 `"000" + accountCode` 적용된 값)
	 * @param startDate 조회 시작일 `YYYY-MM-DD`
	 * @param endDate 조회 종료일 `YYYY-MM-DD`
	 */
	@Query(
		nativeQuery = true,
		value = """
			SELECT "CUST_CD"           AS "custCd",
			       SUM("SALES_AMT")    AS "salesAmt",
			       SUM("SALES_QTY")    AS "salesQty"
			FROM public.live_pos_sales_dh
			WHERE "DATE" BETWEEN to_date(:startDate, 'YYYY-MM-DD')
			                 AND to_date(:endDate,   'YYYY-MM-DD')
			  AND "CUST_CD" IN (:custCds)
			GROUP BY "CUST_CD"
		"""
	)
	fun aggregateByCustomer(
		@Param("custCds") custCds: List<String>,
		@Param("startDate") startDate: String,
		@Param("endDate") endDate: String,
	): List<PosCustomerSalesRow>

	/**
	 * 거래처 N곳 + 기간 + 바코드 목록(`BARCODE IN`) 의 거래처별 POS매출 합계 집계
	 * (web admin 거래처별 명세 테이블의 제품/분류 필터 분기).
	 *
	 * [aggregateByCustomer] 와 동일 형태에 기존 `BARCODE IN` predicate 만 결합 — 외부(POS) DB 에
	 * 새로 추가되는 조회 조건 형태는 거래처별 GROUP BY 뿐이며, 제품/중분류/소분류 필터는 모두
	 * 메인 DB(Product)에서 바코드 목록으로 해소된 뒤 본 predicate 로 합류한다. IN 목록 비대화는
	 * 서비스 레이어가 청크 분할로 보호한다.
	 *
	 * @param custCds 거래처 코드 목록 (legacy 패딩 `"000" + accountCode` 적용된 값)
	 * @param startDate 조회 시작일 `YYYY-MM-DD`
	 * @param endDate 조회 종료일 `YYYY-MM-DD`
	 * @param barcodes 조회 제품의 바코드 목록 (1건 이상 — 빈 목록으로 호출 금지)
	 */
	@Query(
		nativeQuery = true,
		value = """
			SELECT "CUST_CD"           AS "custCd",
			       SUM("SALES_AMT")    AS "salesAmt",
			       SUM("SALES_QTY")    AS "salesQty"
			FROM public.live_pos_sales_dh
			WHERE "DATE" BETWEEN to_date(:startDate, 'YYYY-MM-DD')
			                 AND to_date(:endDate,   'YYYY-MM-DD')
			  AND "CUST_CD" IN (:custCds)
			  AND "BARCODE" IN (:barcodes)
			GROUP BY "CUST_CD"
		"""
	)
	fun aggregateByCustomerAndBarcodes(
		@Param("custCds") custCds: List<String>,
		@Param("startDate") startDate: String,
		@Param("endDate") endDate: String,
		@Param("barcodes") barcodes: List<String>,
	): List<PosCustomerSalesRow>
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

/**
 * [LivePosSalesDailyRepository.aggregateByCustomer] native 집계 결과 projection.
 * alias(`custCd`/`salesAmt`/`salesQty`) ↔ getter 매핑.
 */
interface PosCustomerSalesRow {
	fun getCustCd(): String
	fun getSalesAmt(): BigDecimal?
	fun getSalesQty(): BigDecimal?
}
