package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.request.BatchDateTestRequest
import com.otoki.powersales.admin.dto.request.LoanInquiryTestRequest
import com.otoki.powersales.admin.dto.request.OrderRequestCancelTestRequest
import com.otoki.powersales.admin.dto.request.DisplayMasterSingleTestRequest
import com.otoki.powersales.admin.dto.request.InventorySearchTestRequest
import com.otoki.powersales.admin.dto.request.OrderRequestDetailTestRequest
import com.otoki.powersales.admin.dto.request.OrderRequestRegisterTestRequest
import com.otoki.powersales.admin.dto.request.PPTMasterTestRequest
import com.otoki.powersales.admin.dto.request.PPTMasterSingleTestRequest
import com.otoki.powersales.admin.dto.request.TeamMemberScheduleSingleTestRequest
import com.otoki.powersales.admin.dto.response.SapOutboundTestPreviewResponse
import com.otoki.powersales.admin.dto.response.SapOutboundTestSendResponse
import com.otoki.powersales.admin.service.AdminSapOutboundTestService
import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.platform.auth.permission.SfSystemPermission
import com.otoki.powersales.platform.common.dto.ApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Admin SAP outbound 테스트 endpoint — 각 sender 에 대한 preview / send.
 *
 * - preview: 페이로드만 빌드, SAP 송신 안 함. SAP 호출 없이 빌드 결과를 검증할 때 사용.
 * - send: 실제 SAP 송신. BATCH 류는 sap_outbound_log 자동 적재, OUTBOX 류는 sap_outbox 적재.
 *
 * 권한: 운영 데이터 변경(외부 시스템 송신)이므로 [SfSystemPermission.MODIFY_ALL_DATA] (SYSTEM_ADMIN 만 보유).
 */
@RestController
@RequestMapping("/api/v1/admin/sap-integration/outbound/test")
class AdminSapOutboundTestController(
    private val service: AdminSapOutboundTestService,
) {

    // ===== LoanInquiry =====

    @PostMapping("/loan-inquiry/preview")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.VIEW_ALL_DATA)
    fun previewLoanInquiry(
        @RequestBody req: LoanInquiryTestRequest,
    ): ResponseEntity<ApiResponse<SapOutboundTestPreviewResponse>> =
        ResponseEntity.ok(ApiResponse.success(service.previewLoanInquiry(req)))

    @PostMapping("/loan-inquiry/send")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    fun sendLoanInquiry(
        @RequestBody req: LoanInquiryTestRequest,
    ): ResponseEntity<ApiResponse<SapOutboundTestSendResponse>> =
        ResponseEntity.ok(ApiResponse.success(service.sendLoanInquiry(req)))

    // ===== OrderRequestDetail =====

    @PostMapping("/order-request-detail/preview")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.VIEW_ALL_DATA)
    fun previewOrderRequestDetail(
        @RequestBody req: OrderRequestDetailTestRequest,
    ): ResponseEntity<ApiResponse<SapOutboundTestPreviewResponse>> =
        ResponseEntity.ok(ApiResponse.success(service.previewOrderRequestDetail(req)))

    @PostMapping("/order-request-detail/send")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    fun sendOrderRequestDetail(
        @RequestBody req: OrderRequestDetailTestRequest,
    ): ResponseEntity<ApiResponse<SapOutboundTestSendResponse>> =
        ResponseEntity.ok(ApiResponse.success(service.sendOrderRequestDetail(req)))

    // ===== InventorySearch =====

    @PostMapping("/inventory-search/preview")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.VIEW_ALL_DATA)
    fun previewInventorySearch(
        @RequestBody req: InventorySearchTestRequest,
    ): ResponseEntity<ApiResponse<SapOutboundTestPreviewResponse>> =
        ResponseEntity.ok(ApiResponse.success(service.previewInventorySearch(req)))

    @PostMapping("/inventory-search/send")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    fun sendInventorySearch(
        @RequestBody req: InventorySearchTestRequest,
    ): ResponseEntity<ApiResponse<SapOutboundTestSendResponse>> =
        ResponseEntity.ok(ApiResponse.success(service.sendInventorySearch(req)))

    // ===== OrderRequestCancel =====

    @PostMapping("/order-request-cancel/preview")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.VIEW_ALL_DATA)
    fun previewOrderRequestCancel(
        @RequestBody req: OrderRequestCancelTestRequest,
    ): ResponseEntity<ApiResponse<SapOutboundTestPreviewResponse>> =
        ResponseEntity.ok(ApiResponse.success(service.previewOrderRequestCancel(req)))

    @PostMapping("/order-request-cancel/send")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    fun sendOrderRequestCancel(
        @RequestBody req: OrderRequestCancelTestRequest,
    ): ResponseEntity<ApiResponse<SapOutboundTestSendResponse>> =
        ResponseEntity.ok(ApiResponse.success(service.sendOrderRequestCancel(req)))

    // ===== OrderRequestRegister (Outbox) =====

    @PostMapping("/order-request-register/preview")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.VIEW_ALL_DATA)
    fun previewOrderRequestRegister(
        @RequestBody req: OrderRequestRegisterTestRequest,
    ): ResponseEntity<ApiResponse<SapOutboundTestPreviewResponse>> =
        ResponseEntity.ok(ApiResponse.success(service.previewOrderRequestRegister(req)))

    @PostMapping("/order-request-register/send")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    fun sendOrderRequestRegister(
        @RequestBody req: OrderRequestRegisterTestRequest,
    ): ResponseEntity<ApiResponse<SapOutboundTestSendResponse>> =
        ResponseEntity.ok(ApiResponse.success(service.sendOrderRequestRegister(req)))

    // ===== Attendance =====

    @PostMapping("/attendance/preview")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.VIEW_ALL_DATA)
    fun previewAttendance(
        @RequestBody req: BatchDateTestRequest,
    ): ResponseEntity<ApiResponse<SapOutboundTestPreviewResponse>> =
        ResponseEntity.ok(ApiResponse.success(service.previewAttendance(req)))

    @PostMapping("/attendance/send")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    fun sendAttendance(
        @RequestBody req: BatchDateTestRequest,
    ): ResponseEntity<ApiResponse<SapOutboundTestSendResponse>> =
        ResponseEntity.ok(ApiResponse.success(service.sendAttendance(req)))

    /** 조회 없이 빈 배열을 실제 SAP 으로 송신 (outbound 인터페이스 연결성 확인 전용). */
    @PostMapping("/attendance/send-empty")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    fun sendAttendanceEmpty(): ResponseEntity<ApiResponse<SapOutboundTestSendResponse>> =
        ResponseEntity.ok(ApiResponse.success(service.sendAttendanceEmpty()))

    // ===== Attendance 단건 =====

    @PostMapping("/attendance-single/preview")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.VIEW_ALL_DATA)
    fun previewAttendanceSingle(
        @RequestBody req: TeamMemberScheduleSingleTestRequest,
    ): ResponseEntity<ApiResponse<SapOutboundTestPreviewResponse>> =
        ResponseEntity.ok(ApiResponse.success(service.previewAttendanceSingle(req)))

    @PostMapping("/attendance-single/send")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    fun sendAttendanceSingle(
        @RequestBody req: TeamMemberScheduleSingleTestRequest,
    ): ResponseEntity<ApiResponse<SapOutboundTestSendResponse>> =
        ResponseEntity.ok(ApiResponse.success(service.sendAttendanceSingle(req)))

    // ===== DisplayMaster =====

    @PostMapping("/display-master/preview")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.VIEW_ALL_DATA)
    fun previewDisplayMaster(
        @RequestBody req: BatchDateTestRequest,
    ): ResponseEntity<ApiResponse<SapOutboundTestPreviewResponse>> =
        ResponseEntity.ok(ApiResponse.success(service.previewDisplayMaster(req)))

    @PostMapping("/display-master/send")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    fun sendDisplayMaster(
        @RequestBody req: BatchDateTestRequest,
    ): ResponseEntity<ApiResponse<SapOutboundTestSendResponse>> =
        ResponseEntity.ok(ApiResponse.success(service.sendDisplayMaster(req)))

    /** 조회 없이 빈 배열을 실제 SAP 으로 송신 (outbound 인터페이스 연결성 확인 전용). */
    @PostMapping("/display-master/send-empty")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    fun sendDisplayMasterEmpty(): ResponseEntity<ApiResponse<SapOutboundTestSendResponse>> =
        ResponseEntity.ok(ApiResponse.success(service.sendDisplayMasterEmpty()))

    // ===== DisplayMaster 단건 =====

    @PostMapping("/display-master-single/preview")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.VIEW_ALL_DATA)
    fun previewDisplayMasterSingle(
        @RequestBody req: DisplayMasterSingleTestRequest,
    ): ResponseEntity<ApiResponse<SapOutboundTestPreviewResponse>> =
        ResponseEntity.ok(ApiResponse.success(service.previewDisplayMasterSingle(req)))

    @PostMapping("/display-master-single/send")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    fun sendDisplayMasterSingle(
        @RequestBody req: DisplayMasterSingleTestRequest,
    ): ResponseEntity<ApiResponse<SapOutboundTestSendResponse>> =
        ResponseEntity.ok(ApiResponse.success(service.sendDisplayMasterSingle(req)))

    // ===== PPTMaster =====

    @PostMapping("/ppt-master/preview")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.VIEW_ALL_DATA)
    fun previewPPTMaster(
        @RequestBody req: PPTMasterTestRequest,
    ): ResponseEntity<ApiResponse<SapOutboundTestPreviewResponse>> =
        ResponseEntity.ok(ApiResponse.success(service.previewPPTMaster(req)))

    @PostMapping("/ppt-master/send")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    fun sendPPTMaster(
        @RequestBody req: PPTMasterTestRequest,
    ): ResponseEntity<ApiResponse<SapOutboundTestSendResponse>> =
        ResponseEntity.ok(ApiResponse.success(service.sendPPTMaster(req)))

    /** 조회 없이 빈 배열을 실제 SAP 으로 송신 (outbound 인터페이스 연결성 확인 전용). */
    @PostMapping("/ppt-master/send-empty")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    fun sendPPTMasterEmpty(): ResponseEntity<ApiResponse<SapOutboundTestSendResponse>> =
        ResponseEntity.ok(ApiResponse.success(service.sendPPTMasterEmpty()))

    // ===== PPTMaster 단건 =====

    @PostMapping("/ppt-master-single/preview")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.VIEW_ALL_DATA)
    fun previewPPTMasterSingle(
        @RequestBody req: PPTMasterSingleTestRequest,
    ): ResponseEntity<ApiResponse<SapOutboundTestPreviewResponse>> =
        ResponseEntity.ok(ApiResponse.success(service.previewPPTMasterSingle(req)))

    @PostMapping("/ppt-master-single/send")
    @RequiresSfPermission(operation = SfPermissionOperation.SYSTEM, systemPermission = SfSystemPermission.MODIFY_ALL_DATA)
    fun sendPPTMasterSingle(
        @RequestBody req: PPTMasterSingleTestRequest,
    ): ResponseEntity<ApiResponse<SapOutboundTestSendResponse>> =
        ResponseEntity.ok(ApiResponse.success(service.sendPPTMasterSingle(req)))
}
