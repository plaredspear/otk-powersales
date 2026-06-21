package com.otoki.pos.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import java.math.BigDecimal
import java.time.LocalDate
import com.otoki.powersales.platform.common.entity.DomainName

/**
 * POS DB `public.live_pos_sales_dh` 직매핑 entity (POS매출 = 매장 POS 스캔 실적, 제품·일별).
 *
 * ## 권위 출처
 * - **DB**: POS PostgreSQL `public.live_pos_sales_dh` (read-only)
 * - **DataSource**: `posDataSource` ([com.otoki.powersales.platform.common.integration.pos.config.PosDataSourceConfig])
 * - **EntityManagerFactory**: `posEntityManagerFactory` ([com.otoki.powersales.platform.common.integration.pos.config.PosJpaConfig])
 *
 * ## 레거시 동등
 * 레거시 Heroku `PosMapper.xml`(datasource=`pos`) 의 `selectPosData`/`posSumAmount` 가 조회하던
 * 바로 그 테이블. POS매출 화면(`promotion/month/posmain.jsp`) 의 source.
 * 전산매출([LiveTotSalesDaily], `live_tot_sales_dh`) 과는 별개 테이블 — 컬럼명(`DATE`/`SALES_AMT`/
 * `SALES_QTY`/`BARCODE`) 이 다르다.
 *
 * ## 컬럼 (legacy 쿼리 기준 — 모두 대문자 quoted identifier)
 * - `ITEM_CD` 제품코드 / `ITEM_NM` 제품명 / `BARCODE` 바코드
 * - `CUST_CD` 거래처코드 (legacy 는 `"000" + accountCode` 패딩)
 * - `SALES_AMT` 매출 금액(원) / `SALES_QTY` 매출 수량(EA)
 * - `DATE` 일자
 *
 * ## read-only 가드 ([LiveTotSalesDaily] 와 동일)
 * - 모든 필드 `val` + `@Immutable` → dirty checking / flush 스킵
 * - Repository 가 `Repository<>` marker 만 상속 → mutation API 컴파일 차단
 *
 * 실제 조회는 [com.otoki.pos.repository.LivePosSalesDailyRepository] 의 native 집계 query 로만 수행.
 */
@DomainName("POS매출일별")
@Entity
@Immutable
@Table(name = "live_pos_sales_dh", schema = "public")
@IdClass(LivePosSalesDailyId::class)
class LivePosSalesDaily(
	@Id
	@Column(name = "`DATE`")
	val date: LocalDate,

	@Id
	@Column(name = "`CUST_CD`")
	val custCd: String,

	@Id
	@Column(name = "`ITEM_CD`")
	val itemCd: String,

	@Column(name = "`ITEM_NM`")
	val itemNm: String? = null,

	@Column(name = "`BARCODE`")
	val barcode: String? = null,

	@Column(name = "`SALES_AMT`", precision = 18, scale = 0)
	val salesAmt: BigDecimal? = null,

	@Column(name = "`SALES_QTY`", precision = 18, scale = 0)
	val salesQty: BigDecimal? = null,
)
