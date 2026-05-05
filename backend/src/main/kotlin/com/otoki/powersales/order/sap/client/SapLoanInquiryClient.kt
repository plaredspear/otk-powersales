package com.otoki.powersales.order.sap.client

import java.math.BigDecimal

/**
 * SAP `LoanInquiry` 실시간 호출 인터페이스 (Spec #592 Q1).
 *
 * 주문 등록 시점 서버 재검증 — `creditBalance >= totalAmount`.
 * 클라이언트 사전 검증을 우회한 직접 API 호출 보안 위험 보강.
 *
 * 실제 SAP 호출 구현은 #594 후속 스펙에서 추가 예정. 본 스펙은 interface 만 정의.
 */
interface SapLoanInquiryClient {

    /**
     * @param accountId 거래처 ID
     * @return 여신 잔액 (creditBalance) — `total_credit - used_credit`. SAP 응답 단위 그대로.
     */
    fun inquireCreditBalance(accountId: Long): BigDecimal
}
