package com.otoki.powersales.admin.sap

/**
 * SAP 인바운드 endpoint 운영 카탈로그 SoT.
 *
 * 운영자가 admin 화면에서 "현재 등록된 SAP 인바운드 endpoint 가 무엇인지" 를 일람할 수 있도록
 * 9개 endpoint 의 path / scope / 적재 entity / 컨트롤러 / 한글명 / 운영 설명을 코드 상수로 보유한다.
 *
 * 신규 inbound endpoint 추가 시 본 object 의 [ITEMS] 리스트에 1행 추가.
 *
 * Reference data SoT 정책 — CLAUDE.md § "Flyway 마이그레이션에 reference data INSERT 금지" 따름.
 */
data class SapInboundCatalogItem(
    val endpointPath: String,
    val koreanName: String,
    val requiredScope: String,
    val targetEntity: String,
    val controllerClass: String,
    val description: String,
)

object SapInboundCatalog {

    val ITEMS: List<SapInboundCatalogItem> = listOf(
        SapInboundCatalogItem(
            endpointPath = "/api/v1/sap/organization",
            koreanName = "조직 마스터 수신",
            requiredScope = "sap.org.write",
            targetEntity = "Organization",
            controllerClass = "SapOrganizeMasterController",
            description = "조직 마스터 전량 UPSERT. 변동량 임계 초과 시 SANITY_CHECK_FAILED (422) 거절.",
        ),
        SapInboundCatalogItem(
            endpointPath = "/api/v1/sap/employee",
            koreanName = "사원 마스터 수신",
            requiredScope = "sap.employee.write",
            targetEntity = "Employee",
            controllerClass = "SapEmployeeMasterController",
            description = "사원 마스터 페이지 단위 UPSERT. origin=MANUAL 사원은 보호 차단(MANUAL_ORIGIN_PROTECTED).",
        ),
        SapInboundCatalogItem(
            endpointPath = "/api/v1/sap/product",
            koreanName = "제품 마스터 수신",
            requiredScope = "sap.product.write",
            targetEntity = "Product",
            controllerClass = "SapProductMasterController",
            description = "제품 마스터 + 제품 바코드 + 시스템 코드 UPSERT.",
        ),
        SapInboundCatalogItem(
            endpointPath = "/api/v1/sap/account",
            koreanName = "거래처 마스터 수신",
            requiredScope = "sap.account.write",
            targetEntity = "Account",
            controllerClass = "SapAccountMasterController",
            description = "거래처 마스터 + 거래처 카테고리 UPSERT.",
        ),
        SapInboundCatalogItem(
            endpointPath = "/api/v1/sap/erp-order",
            koreanName = "ERP 주문 수신",
            requiredScope = "sap.order.write",
            targetEntity = "ErpOrder",
            controllerClass = "SapErpOrderController",
            description = "ERP 주문 헤더 + 라인 UPSERT.",
        ),
        SapInboundCatalogItem(
            endpointPath = "/api/v1/sap/appointment",
            koreanName = "발령 정보 수신",
            requiredScope = "sap.attendance.write",
            targetEntity = "Appointment",
            controllerClass = "SapAppointmentController",
            description = "발령(이동/승진/퇴직) 정보 수신.",
        ),
        SapInboundCatalogItem(
            endpointPath = "/api/v1/sap/attend-info",
            koreanName = "근태 정보 수신",
            requiredScope = "sap.attendance.write",
            targetEntity = "TeamMemberSchedule",
            controllerClass = "SapAttendInfoController",
            description = "근태 정보 페이지 단위 수신. SCHEDULE_CONVERSION 으로 team_member_schedule 변환 적재.",
        ),
    )
}
