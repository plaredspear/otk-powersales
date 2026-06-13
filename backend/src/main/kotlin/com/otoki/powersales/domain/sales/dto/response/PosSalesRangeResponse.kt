package com.otoki.powersales.domain.sales.dto.response

/**
 * POS매출 조회(date range) 응답 DTO.
 *
 * ## 데이터 source
 * POS DB `public.live_pos_sales_dh` ([com.otoki.pos.entity.LivePosSalesDaily]) — 레거시 Heroku
 * `PosMapper.xml#selectPosData` 가 조회하던 제품·일별 POS매출. 거래처 1곳 + 기간(시작/종료일) 의
 * 제품별 `SUM(SALES_AMT)`(금액) / `SUM(SALES_QTY)`(수량) 집계.
 *
 * ## 레거시 매핑
 * - 레거시 posmain.jsp 는 ① 품목 미지정 시 합계만(`posSumAmount`, 채널 매핑 JOIN) ② 품목 지정 시
 *   품목별 명세(`selectPosData`) 두 모드였다. 신규는 두 모드를 단일 엔드포인트로 통합한다:
 *   바코드 목록이 비면 거래처 전체 제품 집계, 1건 이상이면 해당 바코드만 집계. 합계금액([totalAmount])
 *   은 항상 서버에서 산출해 내려보낸다(레거시 `#totalAmount`). 채널 매핑 합계 SQL 은 미이관.
 * - 레거시 BARCODE group key 는 제품(`ITEM_CD`) 단위 집계로 단순화하고 대표 바코드 1건만 노출.
 */
data class PosSalesRangeResponse(
	val customerId: Long,
	val customerName: String,
	val sapAccountCode: String,
	/** 조회 시작일 `YYYY-MM-DD`. */
	val startDate: String,
	/** 조회 종료일 `YYYY-MM-DD`. */
	val endDate: String,
	/** 합계금액(원). 명세 항목 `amount` 의 총합 (레거시 `#totalAmount`). */
	val totalAmount: Long,
	/** 합계수량(EA). 명세 항목 `quantity` 의 총합. */
	val totalQuantity: Long,
	/** 제품별 POS매출 명세. */
	val items: List<PosSalesResponse.ProductSales>,
)
