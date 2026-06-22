package com.otoki.powersales.domain.activity.order.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.activity.order.dto.request.OrderRequestCreateLine
import com.otoki.powersales.domain.activity.order.dto.request.OrderRequestCreateRequest
import com.otoki.powersales.domain.activity.order.entity.OrderRequest
import com.otoki.powersales.domain.activity.order.entity.OrderRequestProduct
import com.otoki.powersales.domain.activity.order.enums.OrderRequestStatus
import com.otoki.powersales.domain.activity.order.exception.OrderDeadlinePassedException
import com.otoki.powersales.domain.activity.order.exception.OrderInvalidRequestException
import com.otoki.powersales.domain.activity.order.exception.OrderInvalidUnitException
import com.otoki.powersales.domain.activity.order.exception.OrderLoanExceededException
import com.otoki.powersales.domain.activity.order.exception.OrderProductRestrictedException
import com.otoki.powersales.domain.activity.order.repository.OrderRequestProductRepository
import com.otoki.powersales.domain.activity.order.repository.OrderRequestRepository
import com.otoki.powersales.domain.activity.order.sap.client.InventoryInfo
import com.otoki.powersales.domain.activity.order.sap.client.SapInventorySearchClient
import com.otoki.powersales.domain.activity.order.sap.client.SapLoanInquiryClient
import com.otoki.powersales.domain.activity.order.sap.sender.OrderRequestRegisterSender
import com.otoki.powersales.domain.activity.order.util.OrderDeadlineCalculator
import com.otoki.powersales.external.sap.outbox.SapOutbox
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Optional

@DisplayName("OrderRequestCreateService 테스트 (#592)")
class OrderRequestCreateServiceTest {

    private val orderRequestRepository: OrderRequestRepository = mockk()
    private val orderRequestProductRepository: OrderRequestProductRepository = mockk()
    private val accountRepository: AccountRepository = mockk()
    private val employeeRepository: EmployeeRepository = mockk()
    private val inventorySearchClient: SapInventorySearchClient = mockk()
    private val loanInquiryClient: SapLoanInquiryClient = mockk()
    private val orderRequestRegisterSender: OrderRequestRegisterSender = mockk()
    private val orderDraftService: OrderDraftService = mockk()
    private val orderDeadlineCalculator = OrderDeadlineCalculator()
    private val entityManager: EntityManager = mockk()
    private val nativeQuery: Query = mockk()
    private val service = OrderRequestCreateService(
        orderRequestRepository,
        orderRequestProductRepository,
        accountRepository,
        employeeRepository,
        inventorySearchClient,
        loanInquiryClient,
        orderRequestRegisterSender,
        orderDraftService,
        orderDeadlineCalculator,
        entityManager,
    )

    private val userId = 1L
    private val accountId = 5678L
    private val employeeCode = "20030117"

    @BeforeEach
    fun setUp() {
        every { entityManager.createNativeQuery(any<String>()) } returns nativeQuery
        every { nativeQuery.singleResult } returns 42L
        every { orderRequestRepository.findByClientRequestId(any()) } returns null
        every { orderDraftService.deleteByEmployeeId(any()) } returns Unit
    }

    @Nested
    @DisplayName("정상 등록")
    inner class SuccessCases {

        @Test
        @DisplayName("단일 BOX 라인 — order_request + product + sap_outbox 적재")
        fun success() {
            stubAuthAndAccount()
            stubInventory(mapOf("P001" to inventoryInfo("P001", conv = 30, supply = 1000)))
            every { loanInquiryClient.inquireCreditBalance(accountId) } returns BigDecimal.valueOf(10_000_000)
            every { orderRequestRepository.save(any<OrderRequest>()) } answers { firstArg() }
            every { orderRequestProductRepository.saveAll(any<List<OrderRequestProduct>>()) } answers { firstArg() }
            every { orderRequestRegisterSender.enqueue(any(), any()) } returns
                SapOutbox(domainType = "X", aggregateId = 1L, interfaceId = "Y", payload = "{}")

            val request = baseRequest(
                lines = listOf(line(productCode = "P001", quantity = 10, unit = "BOX", quantityPieces = 300, quantityBoxes = 10))
            )

            val response = service.create(userId, request)

            assertThat(response.orderRequestNumber).startsWith("ORD-")
            assertThat(response.orderRequestNumber).endsWith("-42")
            assertThat(response.status).isEqualTo(OrderRequestStatus.SENT)
            verify(exactly = 1) { orderRequestRegisterSender.enqueue(any(), any()) }
        }

        @Test
        @DisplayName("박스+낱개 혼합 — 총 EA 가 환산수량 배수면 박스 수로 흡수 저장 (레거시 정합)")
        fun mixedBoxAndPiecesAbsorbed() {
            stubAuthAndAccount()
            stubInventory(mapOf("P001" to inventoryInfo("P001", conv = 8, supply = 1000)))
            every { loanInquiryClient.inquireCreditBalance(accountId) } returns BigDecimal.valueOf(10_000_000)
            every { orderRequestRepository.save(any<OrderRequest>()) } answers { firstArg() }
            val savedLines = slot<List<OrderRequestProduct>>()
            every { orderRequestProductRepository.saveAll(capture(savedLines)) } answers { firstArg() }
            every { orderRequestRegisterSender.enqueue(any(), any()) } returns
                SapOutbox(domainType = "X", aggregateId = 1L, interfaceId = "Y", payload = "{}")

            // 박스 5 + 낱개 8 = 총 48 EA (환산 8 의 배수). 클라이언트 박스 입력값(5)은 신뢰하지 않고
            // 서버가 48 / 8 = 6 박스로 역산해야 한다.
            val request = baseRequest(
                lines = listOf(line(productCode = "P001", quantity = 5, unit = "BOX", quantityPieces = 48, quantityBoxes = 5))
            )

            service.create(userId, request)

            val saved = savedLines.captured.single()
            assertThat(saved.quantityPieces).isEqualByComparingTo(BigDecimal.valueOf(48))
            assertThat(saved.quantityBoxes).isEqualByComparingTo(BigDecimal.valueOf(6))
        }

        @Test
        @DisplayName("박스류 공급제한 — 공급제한(박스)×환산수량 한도 내 박스 주문 통과 (레거시 정합)")
        fun boxSupplyLimitConvertedToPieces() {
            stubAuthAndAccount()
            // 공급제한 5(박스) → 환산 8 적용 시 40 EA 한도. 5박스(=40 EA) 주문은 한도 내 → 통과.
            stubInventory(mapOf("P001" to inventoryInfo("P001", conv = 8, supply = 5)))
            every { loanInquiryClient.inquireCreditBalance(accountId) } returns BigDecimal.valueOf(10_000_000)
            every { orderRequestRepository.save(any<OrderRequest>()) } answers { firstArg() }
            every { orderRequestProductRepository.saveAll(any<List<OrderRequestProduct>>()) } answers { firstArg() }
            every { orderRequestRegisterSender.enqueue(any(), any()) } returns
                SapOutbox(domainType = "X", aggregateId = 1L, interfaceId = "Y", payload = "{}")

            val request = baseRequest(
                lines = listOf(line(productCode = "P001", quantity = 5, unit = "BOX", quantityPieces = 40, quantityBoxes = 5))
            )

            val response = service.create(userId, request)

            assertThat(response.status).isEqualTo(OrderRequestStatus.SENT)
            verify(exactly = 1) { orderRequestRegisterSender.enqueue(any(), any()) }
        }

        @Test
        @DisplayName("담당사원이 다른(일정만 잡힌) 거래처도 주문 통과 — 거래처 담당 재검증 제거(레거시 정합)")
        fun nonOwnedAccountAllowed() {
            // 거래처 마스터 담당사원(employeeCode)이 로그인 사원과 달라도(일정 기반 셀렉터에 노출된 거래처)
            // 주문 등록이 거부되지 않아야 한다. 레거시는 저장 시 거래처 담당 재검증이 없었다.
            every { employeeRepository.findById(userId) } returns Optional.of(employee(employeeCode = employeeCode))
            every { accountRepository.findById(accountId) } returns Optional.of(account(employeeCode = "OTHER"))
            stubInventory(mapOf("P001" to inventoryInfo("P001", conv = 30, supply = 1000)))
            every { loanInquiryClient.inquireCreditBalance(accountId) } returns BigDecimal.valueOf(10_000_000)
            every { orderRequestRepository.save(any<OrderRequest>()) } answers { firstArg() }
            every { orderRequestProductRepository.saveAll(any<List<OrderRequestProduct>>()) } answers { firstArg() }
            every { orderRequestRegisterSender.enqueue(any(), any()) } returns
                SapOutbox(domainType = "X", aggregateId = 1L, interfaceId = "Y", payload = "{}")

            val request = baseRequest(
                lines = listOf(line(productCode = "P001", quantity = 10, unit = "BOX", quantityPieces = 300, quantityBoxes = 10))
            )

            val response = service.create(userId, request)

            assertThat(response.status).isEqualTo(OrderRequestStatus.SENT)
            verify(exactly = 1) { orderRequestRegisterSender.enqueue(any(), any()) }
        }

        @Test
        @DisplayName("멱등 — 동일 clientRequestId 재요청 시 기존 row 응답, 신규 INSERT 없음")
        fun idempotent() {
            val existing = mockHeader(orderRequestNumber = "ORD-20260505-1", status = OrderRequestStatus.APPROVED)
            every { orderRequestRepository.findByClientRequestId("idem-1") } returns existing

            val request = baseRequest(
                clientRequestId = "idem-1",
                lines = listOf(line()),
            )

            val response = service.create(userId, request)

            assertThat(response.orderRequestId).isEqualTo(existing.id)
            assertThat(response.status).isEqualTo(OrderRequestStatus.APPROVED)
            verify(exactly = 0) { inventorySearchClient.search(any(), any(), any()) }
            verify(exactly = 0) { loanInquiryClient.inquireCreditBalance(any()) }
            verify(exactly = 0) { orderRequestRepository.save(any<OrderRequest>()) }
            verify(exactly = 0) { orderRequestRegisterSender.enqueue(any(), any()) }
        }
    }

    @Nested
    @DisplayName("검증 실패")
    inner class ErrorCases {

        @Test
        @DisplayName("InventorySearch 응답 라인 누락 → ORD_INVALID_REQUEST")
        fun missingInventoryLine() {
            stubAuthAndAccount()
            stubInventory(emptyMap())

            assertThatThrownBy { service.create(userId, baseRequest(lines = listOf(line(productCode = "P_MISSING")))) }
                .isInstanceOf(OrderInvalidRequestException::class.java)
        }

        @Test
        @DisplayName("단위 환산 정합 위반 → ORD_INVALID_UNIT")
        fun invalidUnit() {
            stubAuthAndAccount()
            stubInventory(mapOf("P001" to inventoryInfo("P001", conv = 30, supply = 1000)))

            // BOX × 10 × conv 30 = 300, 그러나 quantityPieces = 999 송신
            val request = baseRequest(
                lines = listOf(line(productCode = "P001", quantity = 10, unit = "BOX", quantityPieces = 999, quantityBoxes = 10))
            )
            assertThatThrownBy { service.create(userId, request) }
                .isInstanceOf(OrderInvalidUnitException::class.java)
        }

        @Test
        @DisplayName("공급제한 초과 → ORD_PRODUCT_RESTRICTED")
        fun productRestricted() {
            stubAuthAndAccount()
            stubInventory(mapOf("P001" to inventoryInfo("P001", conv = 1, supply = 50)))

            val request = baseRequest(
                lines = listOf(line(productCode = "P001", quantity = 100, unit = "EA", quantityPieces = 100, quantityBoxes = 0))
            )
            assertThatThrownBy { service.create(userId, request) }
                .isInstanceOf(OrderProductRestrictedException::class.java)
        }

        @Test
        @DisplayName("여신 한도 초과 → ORD_LOAN_EXCEEDED")
        fun loanExceeded() {
            stubAuthAndAccount()
            stubInventory(mapOf("P001" to inventoryInfo("P001", conv = 1, supply = 1000)))
            every { loanInquiryClient.inquireCreditBalance(accountId) } returns BigDecimal.valueOf(500_000)

            val request = baseRequest(
                totalAmount = 1_234_567,
                lines = listOf(line(productCode = "P001", quantity = 10, unit = "EA", quantityPieces = 10, quantityBoxes = 0))
            )
            assertThatThrownBy { service.create(userId, request) }
                .isInstanceOf(OrderLoanExceededException::class.java)
        }

        @Test
        @DisplayName("미래 일자 위반 (과거 납기일) → ORD_INVALID_REQUEST")
        fun pastDeliveryDate() {
            val request = baseRequest(
                deliveryDate = LocalDate.now().minus(1, ChronoUnit.DAYS),
                lines = listOf(line()),
            )
            assertThatThrownBy { service.create(userId, request) }
                .isInstanceOf(OrderInvalidRequestException::class.java)
        }

        @Test
        @DisplayName("마감 시각 초과 (납기일=오늘) → ORD_DEADLINE_PASSED")
        fun deadlinePassed() {
            // 납기일이 오늘이면 (now + 1일) 이 (오늘 13:50) 보다 항상 미래 → 마감 후.
            // "오늘 이후" 검증은 통과하지만 server-side 마감 가드에 걸린다.
            val request = baseRequest(
                deliveryDate = LocalDate.now(),
                lines = listOf(line()),
            )
            assertThatThrownBy { service.create(userId, request) }
                .isInstanceOf(OrderDeadlinePassedException::class.java)
        }

        @Test
        @DisplayName("unit enum 위반 → ORD_INVALID_UNIT")
        fun invalidUnitEnum() {
            val request = baseRequest(
                lines = listOf(line(unit = "CASE")),
            )
            assertThatThrownBy { service.create(userId, request) }
                .isInstanceOf(OrderInvalidUnitException::class.java)
        }
    }

    // ───── Test fixtures ─────

    private fun stubAuthAndAccount() {
        every { employeeRepository.findById(userId) } returns Optional.of(employee(employeeCode = employeeCode))
        every { accountRepository.findById(accountId) } returns Optional.of(account(employeeCode = employeeCode))
    }

    private fun stubInventory(map: Map<String, InventoryInfo>) {
        every { inventorySearchClient.search(eq(accountId), any(), any()) } returns map
    }

    private fun baseRequest(
        clientRequestId: String? = null,
        deliveryDate: LocalDate = LocalDate.now().plus(2, ChronoUnit.DAYS),
        totalAmount: Long = 100_000,
        lines: List<OrderRequestCreateLine> = listOf(line()),
    ) = OrderRequestCreateRequest(
        clientRequestId = clientRequestId,
        accountId = accountId,
        deliveryDate = deliveryDate,
        totalAmount = totalAmount,
        lines = lines,
    )

    private fun line(
        lineNumber: Int = 10,
        productCode: String = "P001",
        quantity: Int = 10,
        unit: String = "EA",
        quantityPieces: Int = 10,
        quantityBoxes: Int = 0,
    ) = OrderRequestCreateLine(
        lineNumber = lineNumber,
        productCode = productCode,
        quantity = BigDecimal.valueOf(quantity.toLong()),
        unit = unit,
        quantityPieces = quantityPieces,
        quantityBoxes = BigDecimal.valueOf(quantityBoxes.toLong()),
    )

    private fun employee(employeeCode: String) = Employee(id = userId, employeeCode = employeeCode, name = "Test")

    private fun account(employeeCode: String?) = Account(
        id = accountId,
        name = "Test 거래처",
        externalKey = "EXT-$accountId",
        employeeCode = employeeCode,
    )

    private fun mockHeader(orderRequestNumber: String, status: OrderRequestStatus): OrderRequest = OrderRequest(
        id = 999L,
        orderRequestNumber = orderRequestNumber,
        clientRequestId = "idem-1",
        orderDate = LocalDateTime.now(),
        deliveryDate = LocalDate.now().plus(2, ChronoUnit.DAYS),
        totalAmount = BigDecimal.valueOf(100_000),
        orderRequestStatus = status,
        employee = employee(employeeCode = employeeCode),
        account = account(employeeCode = employeeCode),
    )

    private fun inventoryInfo(productCode: String, conv: Int, supply: Int) = InventoryInfo(
        productCode = productCode,
        productName = "STUB_$productCode",
        conversionQuantity = conv,
        supplyLimitQuantity = supply,
        unitPrice = BigDecimal.ZERO,
    )
}
