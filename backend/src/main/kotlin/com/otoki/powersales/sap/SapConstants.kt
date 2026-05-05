package com.otoki.powersales.sap

/**
 * SAP 인터페이스 공용 상수 (Spec #588 P1-B 도입).
 *
 * 레거시 SF (`Batch_TeamMemberSchedule.cls:43-62`, `IF_REST_SAP_EmployeeMaster.cls:66`) 와
 * 신규 백엔드 (`SapEmployeeMasterService.STATUS_COMPANY_CODE`) 모두 일관되게
 * 회사 코드 `"1000"` 을 사용한다. 환경변수가 아닌 코드 상수로 통합한다 (spec.md §7.2).
 *
 * interfaceId 는 SAP REST Adapter URL 매핑 키로 사용된다. 레거시
 * `IF_Util.httpCall_nonDML('IF_REST_SAP_TeamMemberSchedule', null, jsonBody)` 의
 * 첫 번째 인자에 해당하는 문자열 리터럴 패턴을 코드 상수로 옮긴 것 (v1.9).
 */
object SapConstants {
    /** 오뚜기 SAP 회사 식별자. */
    const val OTOKI_COMPANY_CODE: String = "1000"

    /** 일반 출근 daily batch interfaceId (P1). 레거시 `IF_REST_SAP_TeamMemberSchedule` 등가. */
    const val SAP_INTERFACE_ATTENDANCE: String = "TeamMemberSchedule"

    /** 진열 마스터 daily batch interfaceId (P2). 레거시 `IF_REST_SAP_TeamMemberMasterSchedule` 등가. */
    const val SAP_INTERFACE_DISPLAY_MASTER: String = "TeamMemberMasterSchedule"

    /** 주문 등록 outbox interfaceId (Spec #592). 레거시 `IF_REST_SAP_OrderRequestRegist` 등가. */
    const val SAP_INTERFACE_ORDER_REQUEST_REGIST: String = "IF_REST_SAP_OrderRequestRegist"

    /** [SapOutbox.domainType] 값 — 주문 등록. */
    const val SAP_DOMAIN_ORDER_REQUEST_REGISTER: String = "ORDER_REQUEST_REGISTER"

    /** 거래처 여신 한도 조회 interfaceId (Spec #594). 레거시 `IF_REST_SAP_LoanInquiry` 등가. */
    const val SAP_INTERFACE_LOAN_INQUIRY: String = "LoanInquiry"
}
