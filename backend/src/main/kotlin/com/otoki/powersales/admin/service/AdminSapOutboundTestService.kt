package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.request.BatchDateTestRequest
import com.otoki.powersales.admin.dto.request.LoanInquiryTestRequest
import com.otoki.powersales.admin.dto.request.OrderRequestCancelTestRequest
import com.otoki.powersales.admin.dto.request.OrderRequestDetailTestRequest
import com.otoki.powersales.admin.dto.request.OrderRequestRegisterTestRequest
import com.otoki.powersales.admin.dto.request.PPTMasterTestRequest
import com.otoki.powersales.admin.dto.response.SapOutboundTestPreviewResponse
import com.otoki.powersales.admin.dto.response.SapOutboundTestSendResponse
import com.otoki.powersales.platform.common.exception.BusinessException
import com.otoki.powersales.domain.activity.order.entity.OrderRequest
import com.otoki.powersales.domain.activity.order.entity.OrderRequestProduct
import com.otoki.powersales.domain.activity.order.repository.OrderRequestProductRepository
import com.otoki.powersales.domain.activity.order.repository.OrderRequestRepository
import com.otoki.powersales.domain.activity.order.sap.OrderRequestCancelPayloadFactory
import com.otoki.powersales.domain.activity.order.sap.sender.OrderRequestRegisterSender
import com.otoki.powersales.domain.activity.promotion.repository.PPTMasterRepository
import com.otoki.powersales.domain.activity.promotion.sap.PPTMasterSapPayload
import com.otoki.powersales.domain.activity.promotion.sap.PPTMasterPayloadFactory
import com.otoki.powersales.external.sap.SapConstants
import com.otoki.powersales.external.sap.outbound.sender.AttendanceSapSender
import com.otoki.powersales.external.sap.outbound.sender.DisplayMasterSapSender
import com.otoki.powersales.external.sap.outbound.sender.LoanInquirySender
import com.otoki.powersales.external.sap.outbound.sender.OrderRequestCancelSender
import com.otoki.powersales.external.sap.outbound.sender.OrderRequestDetailSapSender
import com.otoki.powersales.external.sap.outbound.sender.PPTMasterSapSender
import com.otoki.powersales.domain.activity.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.domain.activity.schedule.sap.AttendancePayloadFactory
import com.otoki.powersales.domain.activity.schedule.sap.AttendanceSapPayload
import com.otoki.powersales.domain.activity.schedule.sap.DisplayMasterPayloadFactory
import com.otoki.powersales.domain.activity.schedule.sap.DisplayMasterSapPayload
import com.otoki.powersales.domain.activity.schedule.sap.DisplayMasterSapPayloadRow
import com.otoki.powersales.domain.activity.schedule.entity.DisplayWorkSchedule
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * Admin SAP outbound 테스트 service — 7개 sender 모두에 대한 preview / send 진입점.
 *
 * - preview: payload 빌드까지만 수행, 송신 안 함
 * - send: 실제 sender 호출. BATCH 류는 `sap_outbound_log` 적재, OUTBOX 류는 `sap_outbox` 적재,
 *   REALTIME 류는 동기 응답을 그대로 반환
 *
 * 호출자(`AdminSapOutboundTestController`)에 `SYSTEM_ADMIN` 권한이 강제되어 있으므로
 * 본 service 는 별도 권한 체크를 하지 않는다.
 */
@Service
@Transactional(readOnly = true)
class AdminSapOutboundTestService(
    // realtime sender 들
    private val loanInquirySender: LoanInquirySender,
    private val orderRequestDetailSapSender: OrderRequestDetailSapSender,
    private val orderRequestCancelSender: OrderRequestCancelSender,
    private val orderRequestCancelPayloadFactory: OrderRequestCancelPayloadFactory,
    // outbox sender
    private val orderRequestRegisterSender: OrderRequestRegisterSender,
    // batch sender 들 + payload factory + repo
    private val attendanceSapSender: AttendanceSapSender,
    private val attendancePayloadFactory: AttendancePayloadFactory,
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
    private val displayMasterSapSender: DisplayMasterSapSender,
    private val displayMasterPayloadFactory: DisplayMasterPayloadFactory,
    private val displayWorkScheduleRepository: DisplayWorkScheduleRepository,
    private val pptMasterSapSender: PPTMasterSapSender,
    private val pptMasterPayloadFactory: PPTMasterPayloadFactory,
    private val pptMasterRepository: PPTMasterRepository,
    // domain repo (OrderRequest register/cancel)
    private val orderRequestRepository: OrderRequestRepository,
    private val orderRequestProductRepository: OrderRequestProductRepository,
) {

    // ===== LoanInquiry =====

    fun previewLoanInquiry(req: LoanInquiryTestRequest): SapOutboundTestPreviewResponse {
        val interfaceId = SapConstants.SAP_INTERFACE_LOAN_INQUIRY
        return SapOutboundTestPreviewResponse(
            interfaceId = interfaceId,
            endpointPath = "/$interfaceId",
            payload = mapOf("request" to mapOf("SAPAccountCode" to req.externalKey)),
            summary = "externalKey=${req.externalKey}",
        )
    }

    fun sendLoanInquiry(req: LoanInquiryTestRequest): SapOutboundTestSendResponse {
        val interfaceId = SapConstants.SAP_INTERFACE_LOAN_INQUIRY
        return try {
            val result = loanInquirySender.inquire(req.externalKey)
            SapOutboundTestSendResponse(
                interfaceId = interfaceId,
                success = true,
                message = "SAP 여신 한도 조회 성공",
                result = result,
            )
        } catch (ex: Exception) {
            SapOutboundTestSendResponse(
                interfaceId = interfaceId,
                success = false,
                message = ex.message ?: ex.javaClass.simpleName,
            )
        }
    }

    // ===== OrderRequestDetail =====

    fun previewOrderRequestDetail(req: OrderRequestDetailTestRequest): SapOutboundTestPreviewResponse {
        val interfaceId = SapConstants.SAP_INTERFACE_ORDER_REQUEST_DETAIL
        return SapOutboundTestPreviewResponse(
            interfaceId = interfaceId,
            endpointPath = "/$interfaceId",
            payload = mapOf("request" to mapOf("RequestNumber" to req.requestNumber)),
            summary = "requestNumber=${req.requestNumber}",
        )
    }

    fun sendOrderRequestDetail(req: OrderRequestDetailTestRequest): SapOutboundTestSendResponse {
        val interfaceId = SapConstants.SAP_INTERFACE_ORDER_REQUEST_DETAIL
        val result = orderRequestDetailSapSender.fetchDetail(req.requestNumber)
        return SapOutboundTestSendResponse(
            interfaceId = interfaceId,
            success = result != null,
            message = if (result == null) "SAP 호출 실패 또는 응답 오류" else "라인 ${result.size}건 수신",
            result = result,
        )
    }

    // ===== OrderRequestCancel =====

    fun previewOrderRequestCancel(req: OrderRequestCancelTestRequest): SapOutboundTestPreviewResponse {
        val interfaceId = SapConstants.SAP_INTERFACE_ORDER_REQUEST_CANCEL
        val (orderRequest, targetLines) = loadCancelTarget(req)
        val payload = orderRequestCancelPayloadFactory.build(orderRequest, targetLines)
        return SapOutboundTestPreviewResponse(
            interfaceId = interfaceId,
            endpointPath = "/$interfaceId",
            payload = payload,
            summary = "orderRequestId=${orderRequest.id} lines=${targetLines.size}",
        )
    }

    fun sendOrderRequestCancel(req: OrderRequestCancelTestRequest): SapOutboundTestSendResponse {
        val interfaceId = SapConstants.SAP_INTERFACE_ORDER_REQUEST_CANCEL
        val (orderRequest, targetLines) = loadCancelTarget(req)
        val payload = orderRequestCancelPayloadFactory.build(orderRequest, targetLines)
        return try {
            orderRequestCancelSender.send(payload)
            SapOutboundTestSendResponse(
                interfaceId = interfaceId,
                success = true,
                message = "SAP 주문 취소 송신 성공 (DB 상태 변경은 일어나지 않음)",
            )
        } catch (ex: Exception) {
            SapOutboundTestSendResponse(
                interfaceId = interfaceId,
                success = false,
                message = ex.message ?: ex.javaClass.simpleName,
            )
        }
    }

    private fun loadCancelTarget(
        req: OrderRequestCancelTestRequest,
    ): Pair<OrderRequest, List<OrderRequestProduct>> {
        val orderRequest = orderRequestRepository.findById(req.orderRequestId).orElseThrow {
            BusinessException(
                errorCode = "ORDER_REQUEST_NOT_FOUND",
                message = "주문 요청을 찾을 수 없습니다: ${req.orderRequestId}",
                httpStatus = HttpStatus.NOT_FOUND,
            )
        }
        val allLines = orderRequestProductRepository
            .findByOrderRequest_IdOrderByLineNumberAsc(req.orderRequestId)
        val target = if (req.orderProductIds.isEmpty()) {
            allLines.filter { !it.isCancelled() }
        } else {
            val byId = allLines.associateBy { it.id }
            val invalid = req.orderProductIds.filter { it !in byId }
            if (invalid.isNotEmpty()) {
                throw BusinessException(
                    errorCode = "ORDER_PRODUCT_NOT_FOUND",
                    message = "다음 orderProductIds 가 해당 주문에 속하지 않습니다: $invalid",
                    httpStatus = HttpStatus.BAD_REQUEST,
                )
            }
            req.orderProductIds.mapNotNull { byId[it] }
        }
        if (target.isEmpty()) {
            throw BusinessException(
                errorCode = "EMPTY_CANCEL_TARGET",
                message = "취소 대상 라인이 없습니다.",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
        return orderRequest to target
    }

    // ===== OrderRequestRegister (Outbox) =====

    fun previewOrderRequestRegister(req: OrderRequestRegisterTestRequest): SapOutboundTestPreviewResponse {
        val interfaceId = SapConstants.SAP_INTERFACE_ORDER_REQUEST_REGIST
        val (orderRequest, products) = loadRegisterTarget(req)
        val payload = orderRequestRegisterSender.buildPayload(
            orderRequest,
            orderRequest.account!!,
            orderRequest.employee!!,
            products,
        )
        return SapOutboundTestPreviewResponse(
            interfaceId = interfaceId,
            endpointPath = "/$interfaceId",
            payload = payload,
            summary = "orderRequestId=${orderRequest.id} products=${products.size}",
        )
    }

    @Transactional
    fun sendOrderRequestRegister(req: OrderRequestRegisterTestRequest): SapOutboundTestSendResponse {
        val interfaceId = SapConstants.SAP_INTERFACE_ORDER_REQUEST_REGIST
        val (orderRequest, products) = loadRegisterTarget(req)
        val outbox = orderRequestRegisterSender.enqueue(orderRequest, products)
        return SapOutboundTestSendResponse(
            interfaceId = interfaceId,
            success = true,
            message = "sap_outbox 적재 완료. SapOutboxWorker 가 곧 송신을 시도합니다.",
            sapOutboxId = outbox.id,
        )
    }

    private fun loadRegisterTarget(
        req: OrderRequestRegisterTestRequest,
    ): Pair<OrderRequest, List<OrderRequestProduct>> {
        val orderRequest = orderRequestRepository.findById(req.orderRequestId).orElseThrow {
            BusinessException(
                errorCode = "ORDER_REQUEST_NOT_FOUND",
                message = "주문 요청을 찾을 수 없습니다: ${req.orderRequestId}",
                httpStatus = HttpStatus.NOT_FOUND,
            )
        }
        if (orderRequest.account == null || orderRequest.employee == null) {
            throw BusinessException(
                errorCode = "ORDER_REQUEST_MISSING_RELATIONS",
                message = "주문 요청에 account 또는 employee 가 비어있어 SAP 페이로드를 만들 수 없습니다.",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
        val products = orderRequestProductRepository
            .findByOrderRequest_IdOrderByLineNumberAsc(orderRequest.id)
        return orderRequest to products
    }

    // ===== Attendance (Batch) =====

    fun previewAttendance(req: BatchDateTestRequest): SapOutboundTestPreviewResponse {
        val interfaceId = SapConstants.SAP_INTERFACE_ATTENDANCE
        val (payload, summary) = buildAttendancePayload(req)
        return SapOutboundTestPreviewResponse(
            interfaceId = interfaceId,
            endpointPath = "/$interfaceId",
            payload = payload,
            summary = summary,
        )
    }

    fun sendAttendance(req: BatchDateTestRequest): SapOutboundTestSendResponse {
        val interfaceId = SapConstants.SAP_INTERFACE_ATTENDANCE
        val (payload, summary) = buildAttendancePayload(req)
        val ok = attendanceSapSender.sendPage(payload)
        return SapOutboundTestSendResponse(
            interfaceId = interfaceId,
            success = ok,
            message = if (ok) "송신 성공 ($summary)" else "송신 실패 ($summary) — sap_outbound_log 확인",
        )
    }

    private fun buildAttendancePayload(req: BatchDateTestRequest):
            Pair<AttendanceSapPayload, String> {
        val today = req.targetDate
        val yesterday = today.minusDays(1)
        val size = req.pageSize ?: ATTENDANCE_DEFAULT_PAGE_SIZE
        val rows = teamMemberScheduleRepository.findRegularAttendancesForSapPaged(
            today = today,
            yesterday = yesterday,
            limit = size,
            offset = 0,
        )
        val payload = attendancePayloadFactory.build(rows, today)
        return payload to "targetDate=$today rows=${rows.size} (page-size=$size)"
    }

    // ===== DisplayMaster (Batch) =====

    fun previewDisplayMaster(req: BatchDateTestRequest): SapOutboundTestPreviewResponse {
        val interfaceId = SapConstants.SAP_INTERFACE_DISPLAY_MASTER
        val (payload, summary) = buildDisplayMasterPayload(req)
        return SapOutboundTestPreviewResponse(
            interfaceId = interfaceId,
            endpointPath = "/$interfaceId",
            payload = payload,
            summary = summary,
        )
    }

    fun sendDisplayMaster(req: BatchDateTestRequest): SapOutboundTestSendResponse {
        val interfaceId = SapConstants.SAP_INTERFACE_DISPLAY_MASTER
        val (payload, summary) = buildDisplayMasterPayload(req)
        val ok = displayMasterSapSender.sendPage(payload)
        return SapOutboundTestSendResponse(
            interfaceId = interfaceId,
            success = ok,
            message = if (ok) "송신 성공 ($summary)" else "송신 실패 ($summary) — sap_outbound_log 확인",
        )
    }

    private fun buildDisplayMasterPayload(req: BatchDateTestRequest):
            Pair<DisplayMasterSapPayload, String> {
        val today = req.targetDate
        val size = req.pageSize ?: DISPLAY_DEFAULT_PAGE_SIZE
        val entities = displayWorkScheduleRepository.findValidForDisplayMasterSapPaged(
            date = today, limit = size, offset = 0,
        )
        val rows = entities.map { it.toSapPayloadRow() }
        val payload = displayMasterPayloadFactory.build(rows, today)
        return payload to "targetDate=$today rows=${rows.size} (page-size=$size)"
    }

    private fun DisplayWorkSchedule.toSapPayloadRow(): DisplayMasterSapPayloadRow =
        DisplayMasterSapPayloadRow(
            displayWorkScheduleId = id,
            employeeCode = employee?.employeeCode,
            accountExternalKey = account?.externalKey,
            typeOfWork1 = typeOfWork1?.displayName,
            typeOfWork3 = typeOfWork3?.displayName,
            typeOfWork5 = typeOfWork5?.displayName,
        )

    // ===== PPTMaster (Batch) =====

    fun previewPPTMaster(req: PPTMasterTestRequest): SapOutboundTestPreviewResponse {
        val interfaceId = SapConstants.SAP_INTERFACE_PPT_MASTER
        val (payload, summary) = buildPPTMasterPayload(req)
        return SapOutboundTestPreviewResponse(
            interfaceId = interfaceId,
            endpointPath = "/$interfaceId",
            payload = payload,
            summary = summary,
        )
    }

    fun sendPPTMaster(req: PPTMasterTestRequest): SapOutboundTestSendResponse {
        val interfaceId = SapConstants.SAP_INTERFACE_PPT_MASTER
        val (payload, summary) = buildPPTMasterPayload(req)
        val ok = pptMasterSapSender.sendPage(payload)
        return SapOutboundTestSendResponse(
            interfaceId = interfaceId,
            success = ok,
            message = if (ok) "송신 성공 ($summary)" else "송신 실패 ($summary) — sap_outbound_log 확인",
        )
    }

    private fun buildPPTMasterPayload(req: PPTMasterTestRequest):
            Pair<PPTMasterSapPayload, String> {
        val today = req.targetDate ?: LocalDate.now()
        val size = req.pageSize ?: PPT_DEFAULT_PAGE_SIZE
        val monthFirstDay = today.withDayOfMonth(1)
        val monthLastDay = monthFirstDay.plusMonths(1).minusDays(1)
        val all = pptMasterRepository.findSapOutboundTargets(monthFirstDay, monthLastDay)
        val firstPage = all.take(size)
        val payload = pptMasterPayloadFactory.build(firstPage, today)
        return payload to "targetDate=$today totalRows=${all.size} firstPage=${firstPage.size} (page-size=$size)"
    }

    companion object {
        private const val ATTENDANCE_DEFAULT_PAGE_SIZE = 100
        private const val DISPLAY_DEFAULT_PAGE_SIZE = 100
        private const val PPT_DEFAULT_PAGE_SIZE = 100
    }
}
