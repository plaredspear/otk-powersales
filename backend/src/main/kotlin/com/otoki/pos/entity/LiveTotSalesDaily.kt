package com.otoki.pos.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import java.math.BigDecimal
import java.time.LocalDate

/**
 * POS DB `public.live_tot_sales_dh` 직매핑 entity (전산매출 = ABC 매출, 제품·일별 실적).
 *
 * ## 권위 출처
 * - **DB**: POS PostgreSQL `public.live_tot_sales_dh` (read-only)
 * - **DataSource**: `posDataSource` ([com.otoki.powersales.platform.common.integration.pos.config.PosDataSourceConfig])
 * - **EntityManagerFactory**: `posEntityManagerFactory` ([com.otoki.powersales.platform.common.integration.pos.config.PosJpaConfig])
 *
 * ## 레거시 동등
 * 레거시 Heroku `PosMapper.xml`(datasource=`pos`) 의 `SelectAbcData`/`AbcSumAmount` 가 조회하던
 * 바로 그 테이블. 전산매출 화면(`promotion/month/abcmain.jsp`) 의 source.
 *
 * ## 컬럼 (legacy 쿼리 기준 — 모두 대문자 quoted identifier)
 * - `ITEM_CD` 제품코드 / `ITEM_NM` 제품명 / `UPC_CD` 바코드
 * - `CUST_CD` 거래처코드 (legacy 는 `"000" + accountCode` 패딩)
 * - `SALES_RAMT` 매출 실적 금액(원) / `SALES_RQTY` 매출 실적 수량
 * - `YMD_ID` 일자
 *
 * ## read-only 가드 ([com.otoki.orora.entity.OroraMonthlySalesHistory] 와 동일)
 * - 모든 필드 `val` + `@Immutable` → dirty checking / flush 스킵
 * - Repository 가 `Repository<>` marker 만 상속 → mutation API 컴파일 차단
 *
 * 실제 조회는 [com.otoki.pos.repository.LiveTotSalesDailyRepository] 의 native 집계 query 로만 수행.
 */
@Entity
@Immutable
@Table(name = "live_tot_sales_dh", schema = "public")
@IdClass(LiveTotSalesDailyId::class)
class LiveTotSalesDaily(
	@Id
	@Column(name = "`YMD_ID`")
	val ymdId: LocalDate,

	@Id
	@Column(name = "`CUST_CD`")
	val custCd: String,

	@Id
	@Column(name = "`ITEM_CD`")
	val itemCd: String,

	@Column(name = "`ITEM_NM`")
	val itemNm: String? = null,

	@Column(name = "`UPC_CD`")
	val upcCd: String? = null,

	@Column(name = "`SALES_RAMT`", precision = 18, scale = 0)
	val salesRamt: BigDecimal? = null,

	@Column(name = "`SALES_RQTY`", precision = 18, scale = 0)
	val salesRqty: BigDecimal? = null,
)
