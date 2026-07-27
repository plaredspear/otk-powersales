package com.otoki.powersales.domain.activity.order.repository

import com.otoki.powersales.domain.activity.order.entity.ErpOrder
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDate

interface ErpOrderRepositoryCustom {

    /**
     * 보관주기 경과 ERP 주문 헤더 hard delete (레거시 `Batch_ERPOrderDel` — `OrderDate__c < LAST_N_MONTHS:6`).
     * `order_date` 가 NULL 인 행은 삭제 대상에서 제외(레거시 동등). 자식 라인 삭제 이후 호출해야 한다.
     */
    fun deleteByOrderDateBefore(cutoff: LocalDate): Int

    /**
     * 거래처별 출하 주문 목록 조회 (Spec #593 — 거래처별 주문 탭).
     *
     * 레거시 Heroku `OrderController#clientlistapi` → SF `ClientOrderSearch` 와 동등.
     * 거래처(account) 단위로 조회하며 담당자(employee) 필터는 적용하지 않는다
     * (특정 거래처에 대한 모든 출하 주문 = 내 주문 + 타 영업사원 주문 포함).
     * 레거시(`DeliveryRequestDate__c =: 단일 날짜`) 와 동등하게 납기일 단일 날짜로 등호 조회한다.
     */
    fun findClientOrders(
        accountId: Long,
        deliveryDate: LocalDate,
        pageable: Pageable
    ): Page<ErpOrder>

    /**
     * 관리자 웹 ERP주문 목록 조회 (기준정보 > ERP주문 조회 화면).
     *
     * 거래처별 조회(`findClientOrders`)와 달리 거래처 제약 없이 전체 ERP주문을 대상으로 하며,
     * 주문번호 정확일치와 납기일 기간으로 좁혀 조회한다. 정렬은 최신 주문(id DESC) 순.
     *
     * 주문번호는 정확일치로만 매칭한다 — 거래처/주문자 포함 다중 컬럼 `%kw%` LIKE 는 인덱스를 타지
     * 못해 대용량 `erp_order` 에서 조회가 지연됐다. 정확일치는 UNIQUE 인덱스
     * (`erp_order_sap_order_number_key`) 를 그대로 사용한다.
     *
     * @param sapOrderNumber 주문번호 정확일치 (null/blank 이면 미적용)
     * @param deliveryDateFrom 납기일 시작 (inclusive, null 이면 미적용)
     * @param deliveryDateTo 납기일 종료 (inclusive, null 이면 미적용)
     */
    fun findAdminErpOrders(
        sapOrderNumber: String?,
        deliveryDateFrom: LocalDate?,
        deliveryDateTo: LocalDate?,
        pageable: Pageable
    ): Page<ErpOrder>
}
