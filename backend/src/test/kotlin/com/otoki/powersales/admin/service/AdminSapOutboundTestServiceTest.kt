package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.request.InventorySearchTestRequest
import com.otoki.powersales.admin.dto.request.LoanInquiryTestRequest
import com.otoki.powersales.admin.dto.request.OrderRequestDetailTestRequest
import com.otoki.powersales.admin.dto.request.TeamMemberScheduleSingleTestRequest
import com.otoki.powersales.admin.dto.request.DisplayMasterSingleTestRequest
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
import com.otoki.powersales.external.sap.outbound.sender.InventorySearchSapItem
import com.otoki.powersales.external.sap.outbound.sender.InventorySearchSender
import com.otoki.powersales.external.sap.outbound.sender.OrderRequestCancelSender
import com.otoki.powersales.external.sap.outbound.sender.OrderRequestDetailSapSender
import com.otoki.powersales.domain.activity.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.domain.activity.schedule.sap.TeamMemberScheduleSapPayloadFactory
import com.otoki.powersales.domain.activity.schedule.sap.TeamMemberScheduleSapPayload
import com.otoki.powersales.domain.activity.schedule.sap.TeamMemberScheduleSapPayloadRow
import com.otoki.powersales.domain.activity.schedule.enums.SecondWorkType
import com.otoki.powersales.domain.activity.schedule.sap.DisplayMasterPayloadFactory
import com.otoki.powersales.domain.activity.schedule.sap.DisplayMasterSapPayload
import com.otoki.powersales.domain.activity.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.domain.activity.schedule.enums.TypeOfWork1
import com.otoki.powersales.domain.activity.schedule.enums.TypeOfWork3
import com.otoki.powersales.domain.activity.schedule.enums.TypeOfWork5
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.foundation.account.entity.Account
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

@DisplayName("AdminSapOutboundTestService 테스트")
class AdminSapOutboundTestServiceTest {

    private val loanInquirySender: LoanInquirySender = mockk()
    private val orderRequestDetailSapSender: OrderRequestDetailSapSender = mockk()
    private val inventorySearchSender: InventorySearchSender = mockk()
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
        inventorySearchSender = inventorySearchSender,
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
    @DisplayName("previewInventorySearch: 페이로드는 라인별 {SAPAccountCode,ProductCode,DeliveryRequestDate} 배열 + SAP 호출 안 함")
    fun previewInventorySearch_buildsPayload() {
        val res = service.previewInventorySearch(
            InventorySearchTestRequest(
                externalKey = "1032619",
                productCodes = listOf("1000123", " 1000456 ", ""),
                deliveryDate = LocalDate.of(2026, 6, 23),
            ),
        )

        assertThat(res.interfaceId).isEqualTo(SapConstants.SAP_INTERFACE_INVENTORY_SEARCH)
        assertThat(res.endpointPath).isEqualTo("/${SapConstants.SAP_INTERFACE_INVENTORY_SEARCH}")
        // 공백 제거 + 빈 코드 제외 → 2건. 날짜는 yyyyMMdd.
        assertThat(res.payload).isEqualTo(
            mapOf(
                "request" to listOf(
                    mapOf(
                        "SAPAccountCode" to "1032619",
                        "ProductCode" to "1000123",
                        "DeliveryRequestDate" to "20260623",
                    ),
                    mapOf(
                        "SAPAccountCode" to "1032619",
                        "ProductCode" to "1000456",
                        "DeliveryRequestDate" to "20260623",
                    ),
                ),
            ),
        )
        verify(exactly = 0) { inventorySearchSender.search(any(), any(), any()) }
    }

    @Test
    @DisplayName("sendInventorySearch: sender 응답을 result 로 전달 + success=true")
    fun sendInventorySearch_successPath() {
        val date = LocalDate.of(2026, 6, 23)
        every {
            inventorySearchSender.search("1032619", listOf("1000123"), date)
        } returns listOf(
            InventorySearchSapItem(
                productCode = "1000123",
                productName = "테스트상품",
                stockQuantity = "100",
                dcLimitQuantity = null,
                supplyLimitQuantity = "50",
                supplyCenterName = "센터",
                closingTime = null,
                message = null,
                minOrderingUnit = "BOX",
                conversionQuantity = "12",
            ),
        )

        val res = service.sendInventorySearch(
            InventorySearchTestRequest(
                externalKey = "1032619",
                productCodes = listOf("1000123"),
                deliveryDate = date,
            ),
        )

        assertThat(res.success).isTrue
        assertThat(res.interfaceId).isEqualTo(SapConstants.SAP_INTERFACE_INVENTORY_SEARCH)
        @Suppress("UNCHECKED_CAST")
        val items = res.result as List<InventorySearchSapItem>
        assertThat(items).hasSize(1)
        assertThat(items[0].productCode).isEqualTo("1000123")
    }

    @Test
    @DisplayName("sendInventorySearch: sender 가 throw → success=false + message 보존")
    fun sendInventorySearch_failurePath() {
        every { inventorySearchSender.search(any(), any(), any()) } throws
            RuntimeException("SAP HTTP 503")

        val res = service.sendInventorySearch(
            InventorySearchTestRequest(externalKey = "X", productCodes = listOf("P1")),
        )

        assertThat(res.success).isFalse
        assertThat(res.message).isEqualTo("SAP HTTP 503")
    }

    @Test
    @DisplayName("sendInventorySearch: productCodes 가 비면 검증 예외 + sender 미호출")
    fun sendInventorySearch_emptyProductCodes_throws() {
        assertThatThrownBy {
            service.sendInventorySearch(
                InventorySearchTestRequest(externalKey = "1032619", productCodes = emptyList()),
            )
        }.hasMessageContaining("productCodes")
        verify(exactly = 0) { inventorySearchSender.search(any(), any(), any()) }
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

    // ===== Attendance 단건 =====

    /** 실제 payload factory 를 주입한 service — 전일보정(WorkingCategory4) 채움 판정까지 검증하기 위함. */
    private val serviceWithRealFactory = AdminSapOutboundTestService(
        loanInquirySender = loanInquirySender,
        orderRequestDetailSapSender = orderRequestDetailSapSender,
        inventorySearchSender = inventorySearchSender,
        orderRequestCancelSender = orderRequestCancelSender,
        orderRequestCancelPayloadFactory = orderRequestCancelPayloadFactory,
        orderRequestRegisterSender = orderRequestRegisterSender,
        teamMemberScheduleSapSender = teamMemberScheduleSapSender,
        teamMemberScheduleSapPayloadFactory = TeamMemberScheduleSapPayloadFactory(),
        teamMemberScheduleRepository = teamMemberScheduleRepository,
        displayMasterSapSender = displayMasterSapSender,
        displayMasterPayloadFactory = DisplayMasterPayloadFactory(),
        displayWorkScheduleRepository = displayWorkScheduleRepository,
        pptMasterSapSender = pptMasterSapSender,
        pptMasterPayloadFactory = pptMasterPayloadFactory,
        pptMasterRepository = pptMasterRepository,
        orderRequestRepository = orderRequestRepository,
        orderRequestProductRepository = orderRequestProductRepository,
    )

    private fun sapRow(
        scheduleId: Long,
        workingDate: LocalDate,
        secondWorkType: SecondWorkType? = null,
    ) = TeamMemberScheduleSapPayloadRow(
        scheduleId = scheduleId,
        workingDate = workingDate,
        employeeCode = "E001",
        accountExternalKey = "1032619",
        workingCategory1 = null,
        workingCategory2 = null,
        workingCategory3 = null,
        secondWorkType = secondWorkType,
    )

    @Test
    @DisplayName("previewAttendanceSingle: scheduleId 로 단건 조회 후 payload 1건 빌드 + SAP 미송신")
    fun previewAttendanceSingle_buildsSingleRowPayload() {
        val workingDate = LocalDate.of(2026, 7, 15)
        every { teamMemberScheduleRepository.findByIdForSap(42L) } returns sapRow(42L, workingDate)

        val res = serviceWithRealFactory.previewAttendanceSingle(
            TeamMemberScheduleSingleTestRequest(scheduleId = 42L),
        )

        assertThat(res.interfaceId).isEqualTo(SapConstants.SAP_INTERFACE_ATTENDANCE)
        val payload = res.payload as TeamMemberScheduleSapPayload
        assertThat(payload.request).hasSize(1)
        assertThat(payload.request[0].EmployeeCode).isEqualTo("E001")
        assertThat(res.summary).contains("scheduleId=42").contains("당일분")
        // 미리보기는 송신하지 않는다.
        verify(exactly = 0) { teamMemberScheduleSapSender.sendPage(any()) }
    }

    @Test
    @DisplayName("previewAttendanceSingle: referenceDate 미지정 → 당일분(WorkingCategory4=null)")
    fun previewAttendanceSingle_defaultReferenceDate_isSameDay() {
        val workingDate = LocalDate.of(2026, 7, 15)
        every { teamMemberScheduleRepository.findByIdForSap(1L) } returns
            sapRow(1L, workingDate, secondWorkType = SecondWorkType.ROOM_TEMP)

        val res = serviceWithRealFactory.previewAttendanceSingle(
            TeamMemberScheduleSingleTestRequest(scheduleId = 1L),
        )

        val payload = res.payload as TeamMemberScheduleSapPayload
        // referenceDate = workingDate → isBefore(false) → WorkingCategory4 는 채우지 않는다.
        assertThat(payload.request[0].WorkDate).isEqualTo("20260715")
        assertThat(payload.request[0].WorkingCategory4).isNull()
    }

    @Test
    @DisplayName("previewAttendanceSingle: referenceDate=workingDate+1 → 전일보정분(WorkingCategory4 채움)")
    fun previewAttendanceSingle_referenceDatePlusOne_fillsCategory4() {
        val workingDate = LocalDate.of(2026, 7, 15)
        every { teamMemberScheduleRepository.findByIdForSap(1L) } returns
            sapRow(1L, workingDate, secondWorkType = SecondWorkType.FROZEN_REFRIGERATED)

        val res = serviceWithRealFactory.previewAttendanceSingle(
            TeamMemberScheduleSingleTestRequest(
                scheduleId = 1L,
                referenceDate = workingDate.plusDays(1),
            ),
        )

        val payload = res.payload as TeamMemberScheduleSapPayload
        // workingDate < referenceDate → 전일보정분으로 판정 → secondWorkType 의 displayName 이 채워진다.
        assertThat(payload.request[0].WorkDate).isEqualTo("20260715")
        assertThat(payload.request[0].WorkingCategory4).isEqualTo("냉동/냉장")
        assertThat(res.summary).contains("전일보정분")
    }

    @Test
    @DisplayName("sendAttendanceSingle: 조회 후 sender.sendPage 로 payload 1건 송신 → success=true")
    fun sendAttendanceSingle_successPath() {
        val workingDate = LocalDate.of(2026, 7, 15)
        every { teamMemberScheduleRepository.findByIdForSap(7L) } returns sapRow(7L, workingDate)
        val captured = slot<TeamMemberScheduleSapPayload>()
        every { teamMemberScheduleSapSender.sendPage(capture(captured)) } returns true

        val res = serviceWithRealFactory.sendAttendanceSingle(
            TeamMemberScheduleSingleTestRequest(scheduleId = 7L),
        )

        assertThat(res.success).isTrue
        assertThat(captured.captured.request).hasSize(1)
        verify(exactly = 1) { teamMemberScheduleSapSender.sendPage(any()) }
    }

    @Test
    @DisplayName("sendAttendanceSingle: sender 가 false 반환 → success=false + message 에 실패 표기")
    fun sendAttendanceSingle_failurePath() {
        every { teamMemberScheduleRepository.findByIdForSap(7L) } returns
            sapRow(7L, LocalDate.of(2026, 7, 15))
        every { teamMemberScheduleSapSender.sendPage(any()) } returns false

        val res = serviceWithRealFactory.sendAttendanceSingle(
            TeamMemberScheduleSingleTestRequest(scheduleId = 7L),
        )

        assertThat(res.success).isFalse
        assertThat(res.message).contains("실패")
    }

    @Test
    @DisplayName("sendAttendanceSingle: 존재하지 않는 scheduleId → NOT_FOUND 예외 + sender 미호출")
    fun sendAttendanceSingle_notFound_throws() {
        every { teamMemberScheduleRepository.findByIdForSap(999L) } returns null

        assertThatThrownBy {
            serviceWithRealFactory.sendAttendanceSingle(
                TeamMemberScheduleSingleTestRequest(scheduleId = 999L),
            )
        }.hasMessageContaining("999")
        verify(exactly = 0) { teamMemberScheduleSapSender.sendPage(any()) }
    }

    // ===== DisplayMaster 단건 =====

    private fun displaySchedule(
        scheduleId: Long,
        employeeCode: String? = "E001",
        accountExternalKey: String? = "1032619",
        typeOfWork1: TypeOfWork1? = TypeOfWork1.DISPLAY,
        typeOfWork3: TypeOfWork3? = TypeOfWork3.FIXED,
        typeOfWork5: TypeOfWork5? = TypeOfWork5.REGULAR,
    ): DisplayWorkSchedule {
        val employeeMock: Employee? = employeeCode?.let { code ->
            mockk { every { this@mockk.employeeCode } returns code }
        }
        val accountMock: Account? = accountExternalKey?.let { key ->
            mockk { every { externalKey } returns key }
        }
        return mockk {
            every { id } returns scheduleId
            every { this@mockk.employee } returns employeeMock
            every { this@mockk.account } returns accountMock
            every { this@mockk.typeOfWork1 } returns typeOfWork1
            every { this@mockk.typeOfWork3 } returns typeOfWork3
            every { this@mockk.typeOfWork5 } returns typeOfWork5
        }
    }

    @Test
    @DisplayName("previewDisplayMasterSingle: scheduleId 로 단건 조회 후 payload 1건 빌드 + SAP 미송신")
    fun previewDisplayMasterSingle_buildsSingleRowPayload() {
        every { displayWorkScheduleRepository.findByIdForDisplayMasterSap(11L) } returns
            displaySchedule(11L)

        val res = serviceWithRealFactory.previewDisplayMasterSingle(
            DisplayMasterSingleTestRequest(scheduleId = 11L, workDate = LocalDate.of(2026, 7, 15)),
        )

        assertThat(res.interfaceId).isEqualTo(SapConstants.SAP_INTERFACE_DISPLAY_MASTER)
        val payload = res.payload as DisplayMasterSapPayload
        assertThat(payload.request).hasSize(1)
        assertThat(payload.request[0].EmployeeCode).isEqualTo("E001")
        assertThat(payload.request[0].SAPAccountCode).isEqualTo("1032619")
        assertThat(payload.request[0].WorkDate).isEqualTo("20260715")
        assertThat(payload.request[0].WorkingCategory1).isEqualTo("진열")
        assertThat(payload.request[0].WorkingCategory3).isEqualTo("고정")
        assertThat(payload.request[0].WorkingCategory5).isEqualTo("상시")
        assertThat(res.summary).contains("scheduleId=11").contains("workDate=2026-07-15")
        verify(exactly = 0) { displayMasterSapSender.sendPage(any()) }
    }

    @Test
    @DisplayName("sendDisplayMasterSingle: 조회 후 sender.sendPage 로 payload 1건 송신 → success=true")
    fun sendDisplayMasterSingle_successPath() {
        every { displayWorkScheduleRepository.findByIdForDisplayMasterSap(22L) } returns
            displaySchedule(22L)
        val captured = slot<DisplayMasterSapPayload>()
        every { displayMasterSapSender.sendPage(capture(captured)) } returns true

        val res = serviceWithRealFactory.sendDisplayMasterSingle(
            DisplayMasterSingleTestRequest(scheduleId = 22L),
        )

        assertThat(res.success).isTrue
        assertThat(captured.captured.request).hasSize(1)
        verify(exactly = 1) { displayMasterSapSender.sendPage(any()) }
    }

    @Test
    @DisplayName("sendDisplayMasterSingle: sender 가 false 반환 → success=false + message 에 실패 표기")
    fun sendDisplayMasterSingle_failurePath() {
        every { displayWorkScheduleRepository.findByIdForDisplayMasterSap(22L) } returns
            displaySchedule(22L)
        every { displayMasterSapSender.sendPage(any()) } returns false

        val res = serviceWithRealFactory.sendDisplayMasterSingle(
            DisplayMasterSingleTestRequest(scheduleId = 22L),
        )

        assertThat(res.success).isFalse
        assertThat(res.message).contains("실패")
    }

    @Test
    @DisplayName("sendDisplayMasterSingle: 존재하지 않는 scheduleId → NOT_FOUND 예외 + sender 미호출")
    fun sendDisplayMasterSingle_notFound_throws() {
        every { displayWorkScheduleRepository.findByIdForDisplayMasterSap(999L) } returns null

        assertThatThrownBy {
            serviceWithRealFactory.sendDisplayMasterSingle(
                DisplayMasterSingleTestRequest(scheduleId = 999L),
            )
        }.hasMessageContaining("999")
        verify(exactly = 0) { displayMasterSapSender.sendPage(any()) }
    }
}
