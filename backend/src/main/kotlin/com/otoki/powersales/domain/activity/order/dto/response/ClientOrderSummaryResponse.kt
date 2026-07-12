package com.otoki.powersales.domain.activity.order.dto.response

import com.otoki.powersales.domain.activity.order.entity.ErpOrder

/**
 * 거래처별 주문 목록 응답 DTO
 * SAP 주문번호 기준으로 그룹핑된 주문 요약 정보
 */
data class ClientOrderSummaryResponse(
    val sapOrderNumber: String,
    val clientId: Long,
    val clientName: String,
    val totalAmount: Long,
    // 로그인 사용자가 등록한 주문인지 여부(주문자사번 == 로그인 사원 사번). 목록에서 "내 주문" 시각 강조용.
    // 거래처별 주문은 담당자 무관 전체 노출이므로, 본인 주문을 쉽게 식별하도록 서버가 권위 판정한다.
    val isMine: Boolean,
    // 주문자명 — erp_order.employee_name(시스템 계정명 오적재 가능) 대신 사번 기반 배치 조회로 해석한 실제 이름.
    val ordererName: String?
) {
    companion object {
        /**
         * `erp_order` 엔티티 → 거래처별 주문 목록 요약 변환.
         *
         * 레거시 응답 필드 매핑:
         * - SAPOrderNumber → sapOrderNumber
         * - SAPAccountName → clientName (없으면 account.name 폴백)
         * - TotalOrderAmount → totalAmount
         *
         * @param currentEmployeeCode 로그인 사원 사번(null/blank 이면 전부 `isMine=false`).
         * @param ordererName 주문자 사번으로 배치 조회한 실제 주문자명(미해석 시 호출부가 `employee_name` 폴백).
         */
        fun from(
            order: ErpOrder,
            currentEmployeeCode: String?,
            ordererName: String?,
        ): ClientOrderSummaryResponse {
            return ClientOrderSummaryResponse(
                sapOrderNumber = order.sapOrderNumber,
                clientId = order.account?.id ?: 0L,
                clientName = order.sapAccountName ?: order.account?.name ?: "",
                totalAmount = order.orderSalesAmount?.toLong() ?: 0L,
                isMine = !currentEmployeeCode.isNullOrBlank() &&
                    order.employeeCode == currentEmployeeCode,
                ordererName = ordererName
            )
        }
    }
}
