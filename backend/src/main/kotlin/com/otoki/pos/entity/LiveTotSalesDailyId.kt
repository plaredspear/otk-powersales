package com.otoki.pos.entity

import java.io.Serializable
import java.time.LocalDate

/**
 * [LiveTotSalesDaily] 복합 PK. POS 테이블 `live_tot_sales_dh` 는 view/적재 테이블이라 DB PK 부재 —
 * application 단에서 `(ymdId, custCd, itemCd)` 복합 unique 가정 (ORORA entity 와 동일 정책).
 *
 * 전산매출 조회는 native 집계 query (projection) 로만 수행하므로 본 PK 는 entity 매핑 형식 요건
 * 충족용 (실제 entity 로딩 없음).
 */
data class LiveTotSalesDailyId(
	val ymdId: LocalDate = LocalDate.MIN,
	val custCd: String = "",
	val itemCd: String = "",
) : Serializable
