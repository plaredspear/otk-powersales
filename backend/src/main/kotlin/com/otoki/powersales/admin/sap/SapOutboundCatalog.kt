package com.otoki.powersales.admin.sap

import com.otoki.powersales.external.sap.SapConstants

/**
 * SAP 아웃바운드 호출 트리거 분류.
 *
 * - [BATCH]    `@Scheduled` 잡 또는 수동 트리거 batch (예: AttendanceSapSender)
 * - [REALTIME] 사용자 액션 기반 즉시 동기 호출 (예: 거래처 여신 한도 조회)
 * - [OUTBOX]   `sap_outbox` 큐 적재 후 비동기 워커 송신 (예: 주문 등록)
 */
enum class OutboundTriggerType {
    BATCH,
    REALTIME,
    OUTBOX,
}

/**
 * SAP 아웃바운드 interface 운영 카탈로그 SoT.
 *
 * `interfaceId` 는 반드시 [SapConstants] 의 const 참조 (문자열 리터럴 금지)
 * — 인터페이스 id 변경 시 카탈로그가 자동으로 따라가도록.
 *
 * 신규 sender 추가 시 본 object 의 [ITEMS] 리스트에 1행 추가.
 */
data class SapOutboundCatalogItem(
    val interfaceId: String,
    val koreanName: String,
    val triggerType: OutboundTriggerType,
    val senderClass: String,
    val description: String,
)

object SapOutboundCatalog {

    val ITEMS: List<SapOutboundCatalogItem> = listOf(
        SapOutboundCatalogItem(
            interfaceId = SapConstants.SAP_INTERFACE_ATTENDANCE,
            koreanName = "여사원일정 스케줄 배치",
            triggerType = OutboundTriggerType.BATCH,
            senderClass = "com.otoki.powersales.external.sap.outbound.sender.AttendanceSapSender",
            description = "매일 새벽 여사원일정(TeamMemberSchedule) 페이지 단위 SAP REST Adapter POST.",
        ),
        SapOutboundCatalogItem(
            interfaceId = SapConstants.SAP_INTERFACE_DISPLAY_MASTER,
            koreanName = "여사원 진열마스터 스케줄 배치",
            triggerType = OutboundTriggerType.BATCH,
            senderClass = "com.otoki.powersales.external.sap.outbound.sender.DisplayMasterSapSender",
            description = "매일 새벽 진열사원 일정 마스터 페이지 단위 SAP REST Adapter POST.",
        ),
        SapOutboundCatalogItem(
            interfaceId = SapConstants.SAP_INTERFACE_PPT_MASTER,
            koreanName = "전문행사조 SAP 송신 배치",
            triggerType = OutboundTriggerType.BATCH,
            senderClass = "com.otoki.powersales.external.sap.outbound.sender.PPTMasterSapSender",
            description = "매일 정오 전문행사조 마스터 SAP 송신 (#765).",
        ),
        SapOutboundCatalogItem(
            interfaceId = SapConstants.SAP_INTERFACE_ORDER_REQUEST_REGIST,
            koreanName = "주문 등록 (Outbox)",
            triggerType = OutboundTriggerType.OUTBOX,
            senderClass = "com.otoki.powersales.order.sap.sender.OrderRequestRegisterSender",
            description = "sap_outbox 큐를 폴링하여 SAP 로 주문 등록 페이로드 송신.",
        ),
        SapOutboundCatalogItem(
            interfaceId = SapConstants.SAP_INTERFACE_ORDER_REQUEST_DETAIL,
            koreanName = "주문요청 상세 조회",
            triggerType = OutboundTriggerType.REALTIME,
            senderClass = "com.otoki.powersales.external.sap.outbound.sender.OrderRequestDetailSapSender",
            description = "본인 주문요청 상세 동기 callout.",
        ),
        SapOutboundCatalogItem(
            interfaceId = SapConstants.SAP_INTERFACE_ORDER_REQUEST_CANCEL,
            koreanName = "주문 취소",
            triggerType = OutboundTriggerType.REALTIME,
            senderClass = "com.otoki.powersales.external.sap.outbound.sender.OrderRequestCancelSender",
            description = "사용자 주문 취소 시 SAP 동기 callout.",
        ),
        SapOutboundCatalogItem(
            interfaceId = SapConstants.SAP_INTERFACE_LOAN_INQUIRY,
            koreanName = "거래처 여신 한도 조회",
            triggerType = OutboundTriggerType.REALTIME,
            senderClass = "com.otoki.powersales.external.sap.outbound.sender.LoanInquirySender",
            description = "거래처 여신 한도 동기 조회.",
        ),
    )
}
