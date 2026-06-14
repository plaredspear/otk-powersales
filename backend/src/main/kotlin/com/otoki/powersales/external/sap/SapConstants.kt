package com.otoki.powersales.external.sap

/**
 * SAP 인터페이스 공용 상수 (Spec #588 P1-B 도입).
 *
 * 레거시 SF (`Batch_TeamMemberSchedule.cls:43-62`, `IF_REST_SAP_EmployeeMaster.cls:66`) 와
 * 신규 백엔드 (`SapEmployeeMasterService.STATUS_COMPANY_CODE`) 모두 일관되게
 * 회사 코드 `"1000"` 을 사용한다. 환경변수가 아닌 코드 상수로 통합한다 (spec.md §7.2).
 *
 * interfaceId 는 SAP REST Adapter 의 service path (`SDxxxxx`) 로, `baseUrl + "/$interfaceId"`
 * 형태의 실제 호출 endpoint 가 되는 동시에 카탈로그/연동 정보/호출 이력의 식별자로도 쓰인다.
 * 실제 호출 경로·화면·카탈로그가 모두 본 상수 한 곳을 바라보므로, 경로 변경은 본 파일만 고친다.
 *
 * 값은 레거시 운영 SAP PI/PO REST Adapter 의 service 코드와 동일하다. 레거시 SF Custom
 * Metadata `Meta_IF_Master__mdt.<레코드>.EndPoint__c` (운영 base
 * `http://61.255.195.75:50000/RESTAdapter/`) 의 service path 와 1:1 정합:
 * - LoanInquiry            → SD03040   (`IF_REST_SAP_LoanInquiry`)
 * - OrderRequestDetail     → SD03052   (`IF_REST_SAP_OrderRequestDetail`)
 * - OrderChange (취소)      → SD03051   (`IF_REST_SAP_OrderChange`)
 * - OrderRequest (등록)     → SD03050   (`IF_REST_SAP_OrderRequest`)
 * - TeamMemberSchedule     → SD03130   (`IF_REST_SAP_TeamMemberSchedule`)
 * - TeamMemberMasterSchedule → SD03131 (`IF_REST_SAP_TeamMemberMasterSchedule`)
 * - PPTM                   → SD03300   (`IF_REST_SAP_PPTMToSAP`, 레거시 하드코딩)
 */
object SapConstants {
    /** 오뚜기 SAP 회사 식별자. */
    const val OTOKI_COMPANY_CODE: String = "1000"

    /** 일반 출근 daily batch SAP service path. 레거시 `IF_REST_SAP_TeamMemberSchedule` (SD03130). */
    const val SAP_INTERFACE_ATTENDANCE: String = "SD03130"

    /** 진열 마스터 daily batch SAP service path. 레거시 `IF_REST_SAP_TeamMemberMasterSchedule` (SD03131). */
    const val SAP_INTERFACE_DISPLAY_MASTER: String = "SD03131"

    /** 주문 등록 outbox SAP service path (Spec #592). 레거시 `IF_REST_SAP_OrderRequest` (SD03050). */
    const val SAP_INTERFACE_ORDER_REQUEST_REGIST: String = "SD03050"

    /** [SapOutbox.domainType] 값 — 주문 등록. */
    const val SAP_DOMAIN_ORDER_REQUEST_REGISTER: String = "ORDER_REQUEST_REGISTER"

    /** 거래처 여신 한도 조회 SAP service path (Spec #594). 레거시 `IF_REST_SAP_LoanInquiry` (SD03040). */
    const val SAP_INTERFACE_LOAN_INQUIRY: String = "SD03040"

    /** 본인 주문요청 상세 조회 SAP service path (Spec #595). 레거시 `IF_REST_SAP_OrderRequestDetail` (SD03052). */
    const val SAP_INTERFACE_ORDER_REQUEST_DETAIL: String = "SD03052"

    /** 주문 취소 동기 callout SAP service path (Spec #597). 레거시 `IF_REST_SAP_OrderChange` (SD03051). */
    const val SAP_INTERFACE_ORDER_REQUEST_CANCEL: String = "SD03051"

    /** 전문행사조 마스터 hourly batch SAP service path. 레거시 `IF_REST_SAP_PPTMToSAP` 의 endpoint `/SD03300`. */
    const val SAP_INTERFACE_PPT_MASTER: String = "SD03300"
}
