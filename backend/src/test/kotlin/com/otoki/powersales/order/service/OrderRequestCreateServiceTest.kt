package com.otoki.powersales.order.service

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.order.dto.request.OrderRequestCreateLine
import com.otoki.powersales.order.dto.request.OrderRequestCreateRequest
import com.otoki.powersales.order.entity.OrderRequest
import com.otoki.powersales.order.entity.OrderRequestProduct
import com.otoki.powersales.order.entity.OrderRequestStatus
import com.otoki.powersales.order.exception.OrderAccountForbiddenException
import com.otoki.powersales.order.exception.OrderInvalidRequestException
import com.otoki.powersales.order.exception.OrderInvalidUnitException
import com.otoki.powersales.order.exception.OrderLoanExceededException
import com.otoki.powersales.order.exception.OrderProductRestrictedException
import com.otoki.powersales.order.repository.OrderRequestProductRepository
import com.otoki.powersales.order.repository.OrderRequestRepository
import com.otoki.powersales.order.sap.client.InventoryInfo
import com.otoki.powersales.order.sap.client.SapInventorySearchClient
import com.otoki.powersales.order.sap.client.SapLoanInquiryClient
import com.otoki.powersales.order.sap.sender.OrderRequestRegisterSender
import com.otoki.powersales.sap.outbox.SapOutbox
import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@DisplayName("OrderRequestCreateService 테스트 (#592)")
class OrderRequestCreateServiceTest {

    @Mock private lateinit var orderRequestRepository: OrderRequestRepository
    @Mock private lateinit var orderRequestProductRepository: OrderRequestProductRepository
    @Mock private lateinit var accountRepository: AccountRepository
    @Mock private lateinit var employeeRepository: EmployeeRepository
    @Mock private lateinit var inventorySearchClient: SapInventorySearchClient
    @Mock private lateinit var loanInquiryClient: SapLoanInquiryClient
    @Mock private lateinit var orderRequestRegisterSender: OrderRequestRegisterSender
    @Mock private lateinit var orderDraftService: OrderDraftService
    @Mock private lateinit var entityManager: EntityManager
    @Mock private lateinit var nativeQuery: Query
    @InjectMocks private lateinit var service: OrderRequestCreateService

    private val userId = 1L
    private val accountId = 5678L
    private val employeeCode = "20030117"

    @BeforeEach
    fun setUp() {
        whenever(entityManager.createNativeQuery(any<String>())).thenReturn(nativeQuery)
        whenever(nativeQuery.singleResult).thenReturn(42L)
    }

    @Nested
    @DisplayName("정상 등록")
    inner class SuccessCases {

        @Test
        @DisplayName("단일 BOX 라인 — order_request + product + sap_outbox 적재")
        fun success() {
            stubAuthAndAccount()
            stubInventory(mapOf("P001" to inventoryInfo("P001", conv = 30, supply = 1000)))
            whenever(loanInquiryClient.inquireCreditBalance(accountId)).thenReturn(BigDecimal.valueOf(10_000_000))
            whenever(orderRequestRepository.save(any<OrderRequest>())).thenAnswer { invocation ->
                val arg = invocation.arguments[0] as OrderRequest
                arg
            }
            whenever(orderRequestProductRepository.saveAll(any<List<OrderRequestProduct>>()))
                .thenAnswer { invocation -> invocation.arguments[0] }
            whenever(orderRequestRegisterSender.enqueue(any(), any()))
                .thenReturn(SapOutbox(domainType = "X", aggregateId = 1L, interfaceId = "Y", payload = "{}"))

            val request = baseRequest(
                lines = listOf(line(productCode = "P001", quantity = 10, unit = "BOX", quantityPieces = 300, quantityBoxes = 10))
            )

            val response = service.create(userId, request)

            assertThat(response.orderRequestNumber).startsWith("ORD-")
            assertThat(response.orderRequestNumber).endsWith("-42")
            assertThat(response.status).isEqualTo(OrderRequestStatus.SENT)
            verify(orderRequestRegisterSender, times(1)).enqueue(any(), any())
        }

        @Test
        @DisplayName("멱등 — 동일 clientRequestId 재요청 시 기존 row 응답, 신규 INSERT 없음")
        fun idempotent() {
            val existing = mockHeader(orderRequestNumber = "ORD-20260505-1", status = OrderRequestStatus.APPROVED)
            whenever(orderRequestRepository.findByClientRequestId("idem-1")).thenReturn(existing)

            val request = baseRequest(
                clientRequestId = "idem-1",
                lines = listOf(line()),
            )

            val response = service.create(userId, request)

            assertThat(response.orderRequestId).isEqualTo(existing.id)
            assertThat(response.status).isEqualTo(OrderRequestStatus.APPROVED)
            verify(inventorySearchClient, times(0)).search(any(), any())
            verify(loanInquiryClient, times(0)).inquireCreditBalance(any())
            verify(orderRequestRepository, times(0)).save(any<OrderRequest>())
            verify(orderRequestRegisterSender, times(0)).enqueue(any(), any())
        }
    }

    @Nested
    @DisplayName("검증 실패")
    inner class ErrorCases {

        @Test
        @DisplayName("본인 담당 거래처 아님 → ORD_ACCOUNT_FORBIDDEN")
        fun accountForbidden() {
            whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee(employeeCode = employeeCode)))
            whenever(accountRepository.findById(accountId.toInt())).thenReturn(Optional.of(account(employeeCode = "OTHER")))

            assertThatThrownBy { service.create(userId, baseRequest(lines = listOf(line()))) }
                .isInstanceOf(OrderAccountForbiddenException::class.java)
        }

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

            // BOX × 10 × conv 30 = 300, 그러나 quantityPieces=999 송신
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
            whenever(loanInquiryClient.inquireCreditBalance(accountId)).thenReturn(BigDecimal.valueOf(500_000))

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
                deliveryDate = LocalDate.now().minusDays(1),
                lines = listOf(line()),
            )
            assertThatThrownBy { service.create(userId, request) }
                .isInstanceOf(OrderInvalidRequestException::class.java)
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
        whenever(employeeRepository.findById(userId)).thenReturn(Optional.of(employee(employeeCode = employeeCode)))
        whenever(accountRepository.findById(accountId.toInt())).thenReturn(Optional.of(account(employeeCode = employeeCode)))
    }

    private fun stubInventory(map: Map<String, InventoryInfo>) {
        whenever(inventorySearchClient.search(eq(accountId), any())).thenReturn(map)
    }

    private fun baseRequest(
        clientRequestId: String? = null,
        deliveryDate: LocalDate = LocalDate.now().plusDays(2),
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
        id = accountId.toInt(),
        name = "Test 거래처",
        externalKey = "EXT-$accountId",
        employeeCode = employeeCode,
    )

    private fun mockHeader(orderRequestNumber: String, status: OrderRequestStatus): OrderRequest = OrderRequest(
        id = 999L,
        orderRequestNumber = orderRequestNumber,
        clientRequestId = "idem-1",
        orderDate = java.time.LocalDateTime.now(),
        deliveryDate = LocalDate.now().plusDays(2),
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
