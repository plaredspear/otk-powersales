package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.request.LoanInquiryTestRequest
import com.otoki.powersales.admin.dto.request.OrderRequestDetailTestRequest
import com.otoki.powersales.domain.activity.order.repository.OrderRequestProductRepository
import com.otoki.powersales.domain.activity.order.repository.OrderRequestRepository
import com.otoki.powersales.domain.activity.order.sap.OrderRequestCancelPayloadFactory
import com.otoki.powersales.domain.activity.order.sap.sender.OrderRequestRegisterSender
import com.otoki.powersales.domain.activity.promotion.repository.PPTMasterRepository
import com.otoki.powersales.domain.activity.promotion.sap.PPTMasterPayloadFactory
import com.otoki.powersales.external.sap.SapConstants
import com.otoki.powersales.external.sap.outbound.sender.TeamMemberScheduleSapSender
import com.otoki.powersales.external.sap.outbound.sender.DisplayMasterSapSender
import com.otoki.powersales.external.sap.outbound.sender.LoanInquirySender
import com.otoki.powersales.external.sap.outbound.sender.LoanInquirySapResult
import com.otoki.powersales.external.sap.outbound.sender.PPTMasterSapSender
import com.otoki.powersales.external.sap.outbound.sender.OrderRequestCancelSender
import com.otoki.powersales.external.sap.outbound.sender.OrderRequestDetailSapSender
import com.otoki.powersales.domain.activity.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.domain.activity.schedule.sap.TeamMemberScheduleSapPayloadFactory
import com.otoki.powersales.domain.activity.schedule.sap.DisplayMasterPayloadFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@DisplayName("AdminSapOutboundTestService 테스트")
class AdminSapOutboundTestServiceTest {

    private val loanInquirySender: LoanInquirySender = mockk()
    private val orderRequestDetailSapSender: OrderRequestDetailSapSender = mockk()
    private val orderRequestCancelSender: OrderRequestCancelSender = mockk()
    private val orderRequestCancelPayloadFactory: OrderRequestCancelPayloadFactory = mockk()
    private val orderRequestRegisterSender: OrderRequestRegisterSender = mockk()
    private val teamMemberScheduleSapSender: TeamMemberScheduleSapSender = mockk()
    private val teamMemberScheduleSapPayloadFactory: TeamMemberScheduleSapPayloadFactory = mockk()
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository = mockk()
    private val displayMasterSapSender: DisplayMasterSapSender = mockk()
    private val displayMasterPayloadFactory: DisplayMasterPayloadFactory = mockk()
    private val displayWorkScheduleRepository: DisplayWorkScheduleRepository = mockk()
    private val pptMasterSapSender: PPTMasterSapSender = mockk()
    private val pptMasterPayloadFactory: PPTMasterPayloadFactory = mockk()
    private val pptMasterRepository: PPTMasterRepository = mockk()
    private val orderRequestRepository: OrderRequestRepository = mockk()
    private val orderRequestProductRepository: OrderRequestProductRepository = mockk()

    private val service = AdminSapOutboundTestService(
        loanInquirySender = loanInquirySender,
        orderRequestDetailSapSender = orderRequestDetailSapSender,
        orderRequestCancelSender = orderRequestCancelSender,
        orderRequestCancelPayloadFactory = orderRequestCancelPayloadFactory,
        orderRequestRegisterSender = orderRequestRegisterSender,
        teamMemberScheduleSapSender = teamMemberScheduleSapSender,
        teamMemberScheduleSapPayloadFactory = teamMemberScheduleSapPayloadFactory,
        teamMemberScheduleRepository = teamMemberScheduleRepository,
        displayMasterSapSender = displayMasterSapSender,
        displayMasterPayloadFactory = displayMasterPayloadFactory,
        displayWorkScheduleRepository = displayWorkScheduleRepository,
        pptMasterSapSender = pptMasterSapSender,
        pptMasterPayloadFactory = pptMasterPayloadFactory,
        pptMasterRepository = pptMasterRepository,
        orderRequestRepository = orderRequestRepository,
        orderRequestProductRepository = orderRequestProductRepository,
    )

    @Test
    @DisplayName("previewLoanInquiry: 페이로드는 {request:{SAPAccountCode:externalKey}} + SAP 호출 안 함")
    fun previewLoanInquiry_buildsPayloadWithoutCallingSender() {
        val res = service.previewLoanInquiry(LoanInquiryTestRequest(externalKey = "1032619"))

        assertThat(res.interfaceId).isEqualTo(SapConstants.SAP_INTERFACE_LOAN_INQUIRY)
        assertThat(res.endpointPath).isEqualTo("/${SapConstants.SAP_INTERFACE_LOAN_INQUIRY}")
        assertThat(res.payload).isEqualTo(mapOf("request" to mapOf("SAPAccountCode" to "1032619")))
        assertThat(res.summary).contains("1032619")
        verify(exactly = 0) { loanInquirySender.inquire(any()) }
    }

    @Test
    @DisplayName("sendLoanInquiry: sender 가 성공 응답 반환 → success=true + result 전달")
    fun sendLoanInquiry_successPath() {
        every { loanInquirySender.inquire("X1") } returns LoanInquirySapResult(
            totalCredit = BigDecimal("1000000"),
            creditBalance = BigDecimal("250000"),
            currency = "KRW",
        )

        val res = service.sendLoanInquiry(LoanInquiryTestRequest(externalKey = "X1"))

        assertThat(res.success).isTrue
        assertThat(res.interfaceId).isEqualTo(SapConstants.SAP_INTERFACE_LOAN_INQUIRY)
        assertThat(res.result).isInstanceOf(LoanInquirySapResult::class.java)
    }

    @Test
    @DisplayName("sendLoanInquiry: sender 가 throw → success=false + message 보존")
    fun sendLoanInquiry_failurePath() {
        every { loanInquirySender.inquire("XBAD") } throws RuntimeException("SAP 네트워크 오류")

        val res = service.sendLoanInquiry(LoanInquiryTestRequest(externalKey = "XBAD"))

        assertThat(res.success).isFalse
        assertThat(res.message).isEqualTo("SAP 네트워크 오류")
    }

    @Test
    @DisplayName("previewOrderRequestDetail: 페이로드는 {request:{RequestNumber:...}}")
    fun previewOrderRequestDetail_buildsPayload() {
        val res = service.previewOrderRequestDetail(
            OrderRequestDetailTestRequest(requestNumber = "OR000123"),
        )

        assertThat(res.payload).isEqualTo(
            mapOf("request" to mapOf("RequestNumber" to "OR000123")),
        )
        verify(exactly = 0) { orderRequestDetailSapSender.fetchDetail(any()) }
    }

    @Test
    @DisplayName("sendAttendanceEmpty: 조회 없이 sender.sendEmptyForConnectivityCheck 호출 → success=true")
    fun sendAttendanceEmpty_successPath() {
        every { teamMemberScheduleSapSender.sendEmptyForConnectivityCheck() } returns true

        val res = service.sendAttendanceEmpty()

        assertThat(res.success).isTrue
        assertThat(res.interfaceId).isEqualTo(SapConstants.SAP_INTERFACE_ATTENDANCE)
        verify(exactly = 1) { teamMemberScheduleSapSender.sendEmptyForConnectivityCheck() }
        // 조회를 거치지 않으므로 payload factory 는 호출되지 않는다.
        verify(exactly = 0) { teamMemberScheduleSapPayloadFactory.build(any(), any()) }
    }

    @Test
    @DisplayName("sendAttendanceEmpty: sender 가 false 반환 → success=false")
    fun sendAttendanceEmpty_failurePath() {
        every { teamMemberScheduleSapSender.sendEmptyForConnectivityCheck() } returns false

        val res = service.sendAttendanceEmpty()

        assertThat(res.success).isFalse
        assertThat(res.message).contains("실패")
    }

    @Test
    @DisplayName("sendDisplayMasterEmpty: 조회 없이 sender.sendEmptyForConnectivityCheck 호출 → success=true")
    fun sendDisplayMasterEmpty_successPath() {
        every { displayMasterSapSender.sendEmptyForConnectivityCheck() } returns true

        val res = service.sendDisplayMasterEmpty()

        assertThat(res.success).isTrue
        assertThat(res.interfaceId).isEqualTo(SapConstants.SAP_INTERFACE_DISPLAY_MASTER)
        verify(exactly = 1) { displayMasterSapSender.sendEmptyForConnectivityCheck() }
    }

    @Test
    @DisplayName("sendDisplayMasterEmpty: sender 가 false 반환 → success=false")
    fun sendDisplayMasterEmpty_failurePath() {
        every { displayMasterSapSender.sendEmptyForConnectivityCheck() } returns false

        val res = service.sendDisplayMasterEmpty()

        assertThat(res.success).isFalse
        assertThat(res.message).contains("실패")
    }

    @Test
    @DisplayName("sendPPTMasterEmpty: 조회 없이 sender.sendEmptyForConnectivityCheck 호출 → success=true")
    fun sendPPTMasterEmpty_successPath() {
        every { pptMasterSapSender.sendEmptyForConnectivityCheck() } returns true

        val res = service.sendPPTMasterEmpty()

        assertThat(res.success).isTrue
        assertThat(res.interfaceId).isEqualTo(SapConstants.SAP_INTERFACE_PPT_MASTER)
        verify(exactly = 1) { pptMasterSapSender.sendEmptyForConnectivityCheck() }
    }

    @Test
    @DisplayName("sendPPTMasterEmpty: sender 가 false 반환 → success=false")
    fun sendPPTMasterEmpty_failurePath() {
        every { pptMasterSapSender.sendEmptyForConnectivityCheck() } returns false

        val res = service.sendPPTMasterEmpty()

        assertThat(res.success).isFalse
        assertThat(res.message).contains("실패")
    }
}
