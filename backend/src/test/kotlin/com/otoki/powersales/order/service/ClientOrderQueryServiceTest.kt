package com.otoki.powersales.order.service

import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.order.enums.DeliveryStatus
import com.otoki.powersales.order.entity.ErpOrder
import com.otoki.powersales.order.entity.ErpOrderProduct
import com.otoki.powersales.order.exception.ClientOrderForbiddenException
import com.otoki.powersales.order.exception.InvalidSapOrderNumberException
import com.otoki.powersales.order.exception.SapOrderNotFoundException
import com.otoki.powersales.order.repository.ErpOrderProductRepository
import com.otoki.powersales.order.repository.ErpOrderRepository
import com.otoki.powersales.order.repository.OrderHistoryRow
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

@DisplayName("ClientOrderQueryService 테스트 (#593)")
class ClientOrderQueryServiceTest {

    private val erpOrderRepository: ErpOrderRepository = mockk()
    private val erpOrderProductRepository: ErpOrderProductRepository = mockk()
    private val employeeRepository: EmployeeRepository = mockk()
    private val service = ClientOrderQueryService(erpOrderRepository, erpOrderProductRepository, employeeRepository)

    private val sapOrderNumber = "0300011396"
    private val userId = 1L
    private val employeeCode = "20030117"

    @Nested
    @DisplayName("getClientOrderDetail - 정상 조회")
    inner class SuccessCases {

        @Test
        @DisplayName("정상 - 주문 + 라인 N건 매핑 + DeliveryStatus 한글→영문 변환")
        fun success() {
            val order = createOrder(employeeCode = employeeCode)
            val products = listOf(
                createProduct(lineNumber = "10", deliveryStatus = "배송중", productCode = "P001", productName = "예시1", shippingQuantityBox = BigDecimal("5"), unit = "BOX"),
                createProduct(lineNumber = "20", deliveryStatus = "배송 완료", productCode = "P002", productName = "예시2", shippingQuantityBox = BigDecimal("10"), unit = "BOX"),
            )
            every { erpOrderRepository.findBySapOrderNumber(sapOrderNumber) } returns order
            every { employeeRepository.findById(userId) } returns Optional.of(createEmployee())
            every { erpOrderProductRepository.findBySapOrderNumberOrderByLineNumberAsc(sapOrderNumber) } returns products

            val result = service.getClientOrderDetail(userId, sapOrderNumber)

            assertThat(result.sapOrderNumber).isEqualTo(sapOrderNumber)
            assertThat(result.sapAccountCode).isEqualTo("0001234567")
            assertThat(result.sapAccountName).isEqualTo("홍길동마트")
            assertThat(result.clientDeadlineTime).isEqualTo("13:50")
            assertThat(result.orderDate).isEqualTo(LocalDate.of(2026, 5, 4))
            assertThat(result.deliveryDate).isEqualTo(LocalDate.of(2026, 5, 6))
            assertThat(result.totalApprovedAmount).isEqualByComparingTo(BigDecimal.valueOf(1_250_000L))
            assertThat(result.orderedItemCount).isEqualTo(2)
            assertThat(result.orderedItems).hasSize(2)
            assertThat(result.orderedItems[0].deliveryStatus).isEqualTo(DeliveryStatus.SHIPPING)
            assertThat(result.orderedItems[0].deliveredQuantity).isEqualTo("5 BOX")
            assertThat(result.orderedItems[1].deliveryStatus).isEqualTo(DeliveryStatus.DELIVERED)
            assertThat(result.orderedItems[1].deliveredQuantity).isEqualTo("10 BOX")
        }

        @Test
        @DisplayName("정상 - 라인 0건 → orderedItems 빈 배열")
        fun emptyLines() {
            val order = createOrder(employeeCode = employeeCode)
            every { erpOrderRepository.findBySapOrderNumber(sapOrderNumber) } returns order
            every { employeeRepository.findById(userId) } returns Optional.of(createEmployee())
            every { erpOrderProductRepository.findBySapOrderNumberOrderByLineNumberAsc(sapOrderNumber) } returns emptyList()

            val result = service.getClientOrderDetail(userId, sapOrderNumber)

            assertThat(result.orderedItems).isEmpty()
            assertThat(result.orderedItemCount).isEqualTo(0)
        }

        @Test
        @DisplayName("정상 - DB 한글 미정의 라벨 → PENDING fallback")
        fun unknownStatusFallback() {
            val order = createOrder(employeeCode = employeeCode)
            val products = listOf(createProduct(lineNumber = "10", deliveryStatus = "알수없음"))
            every { erpOrderRepository.findBySapOrderNumber(sapOrderNumber) } returns order
            every { employeeRepository.findById(userId) } returns Optional.of(createEmployee())
            every { erpOrderProductRepository.findBySapOrderNumberOrderByLineNumberAsc(sapOrderNumber) } returns products

            val result = service.getClientOrderDetail(userId, sapOrderNumber)

            assertThat(result.orderedItems[0].deliveryStatus).isEqualTo(DeliveryStatus.PENDING)
        }
    }

    @Nested
    @DisplayName("getClientOrderDetail - 검증/예외")
    inner class ErrorCases {

        @Test
        @DisplayName("실패 - 형식 오류 (영문) → InvalidSapOrderNumberException")
        fun invalidFormatAlpha() {
            assertThatThrownBy { service.getClientOrderDetail(userId, "abc123") }
                .isInstanceOf(InvalidSapOrderNumberException::class.java)
        }

        @Test
        @DisplayName("실패 - 형식 오류 (빈 문자) → InvalidSapOrderNumberException")
        fun invalidFormatEmpty() {
            assertThatThrownBy { service.getClientOrderDetail(userId, "") }
                .isInstanceOf(InvalidSapOrderNumberException::class.java)
        }

        @Test
        @DisplayName("실패 - SAP 주문번호 미존재 → SapOrderNotFoundException")
        fun notFound() {
            every { erpOrderRepository.findBySapOrderNumber(sapOrderNumber) } returns null

            assertThatThrownBy { service.getClientOrderDetail(userId, sapOrderNumber) }
                .isInstanceOf(SapOrderNotFoundException::class.java)
        }

        @Test
        @DisplayName("실패 - employee_code 불일치 → ClientOrderForbiddenException")
        fun forbiddenMismatch() {
            val order = createOrder(employeeCode = "OTHER_CODE")
            every { erpOrderRepository.findBySapOrderNumber(sapOrderNumber) } returns order
            every { employeeRepository.findById(userId) } returns Optional.of(createEmployee())

            assertThatThrownBy { service.getClientOrderDetail(userId, sapOrderNumber) }
                .isInstanceOf(ClientOrderForbiddenException::class.java)
        }

        @Test
        @DisplayName("실패 - employee_code 가 null → ClientOrderForbiddenException")
        fun forbiddenNullCode() {
            val order = createOrder(employeeCode = null)
            every { erpOrderRepository.findBySapOrderNumber(sapOrderNumber) } returns order
            every { employeeRepository.findById(userId) } returns Optional.of(createEmployee())

            assertThatThrownBy { service.getClientOrderDetail(userId, sapOrderNumber) }
                .isInstanceOf(ClientOrderForbiddenException::class.java)
        }

        @Test
        @DisplayName("실패 - 사용자 미존재 → ClientOrderForbiddenException")
        fun forbiddenUserMissing() {
            val order = createOrder(employeeCode = employeeCode)
            every { erpOrderRepository.findBySapOrderNumber(sapOrderNumber) } returns order
            every { employeeRepository.findById(userId) } returns Optional.empty()

            assertThatThrownBy { service.getClientOrderDetail(userId, sapOrderNumber) }
                .isInstanceOf(ClientOrderForbiddenException::class.java)
        }
    }

    @Nested
    @DisplayName("DeliveryStatus.fromKoreanLabel")
    inner class DeliveryStatusConversion {

        @Test
        @DisplayName("4종 한글 라벨 변환")
        fun fourLabels() {
            assertThat(DeliveryStatus.fromKoreanLabel("대기")).isEqualTo(DeliveryStatus.PENDING)
            assertThat(DeliveryStatus.fromKoreanLabel("배송중")).isEqualTo(DeliveryStatus.SHIPPING)
            assertThat(DeliveryStatus.fromKoreanLabel("배송 완료")).isEqualTo(DeliveryStatus.DELIVERED)
            assertThat(DeliveryStatus.fromKoreanLabel("결품")).isEqualTo(DeliveryStatus.OUT_OF_STOCK)
        }

        @Test
        @DisplayName("미정의 라벨 → PENDING fallback")
        fun unknownLabel() {
            assertThat(DeliveryStatus.fromKoreanLabel("알수없음")).isEqualTo(DeliveryStatus.PENDING)
            assertThat(DeliveryStatus.fromKoreanLabel(null)).isEqualTo(DeliveryStatus.PENDING)
            assertThat(DeliveryStatus.fromKoreanLabel("")).isEqualTo(DeliveryStatus.PENDING)
        }
    }

    @Nested
    @DisplayName("getAccountOrderHistory - 거래처 주문이력")
    inner class AccountOrderHistoryCases {

        private val accountCode = "0001234567"
        private val from = LocalDate.of(2026, 5, 4)
        private val to = LocalDate.of(2026, 5, 6)

        @Test
        @DisplayName("정상 - 주문일 내림차순 그룹 + 그룹내 제품코드 중복제거")
        fun success() {
            every { employeeRepository.findById(userId) } returns Optional.of(createEmployee())
            every {
                erpOrderProductRepository.findOrderHistory(accountCode, employeeCode, from, to)
            } returns listOf(
                OrderHistoryRow(LocalDate.of(2026, 5, 6), "P001", "참깨라면"),
                OrderHistoryRow(LocalDate.of(2026, 5, 6), "P001", "참깨라면"), // 중복
                OrderHistoryRow(LocalDate.of(2026, 5, 6), "P002", "진라면"),
                OrderHistoryRow(LocalDate.of(2026, 5, 4), "P003", "열라면"),
            )

            val result = service.getAccountOrderHistory(userId, accountCode, from, to)

            assertThat(result).hasSize(2)
            // 주문일 내림차순
            assertThat(result[0].orderDate).isEqualTo("2026-05-06")
            assertThat(result[1].orderDate).isEqualTo("2026-05-04")
            // 제품코드 중복제거 (P001 1건)
            assertThat(result[0].products).hasSize(2)
            assertThat(result[0].products[0].productCode).isEqualTo("P001")
            assertThat(result[1].products).hasSize(1)
            assertThat(result[1].products[0].productCode).isEqualTo("P003")
        }

        @Test
        @DisplayName("정상 - 결과 없음 → 빈 목록")
        fun empty() {
            every { employeeRepository.findById(userId) } returns Optional.of(createEmployee())
            every {
                erpOrderProductRepository.findOrderHistory(accountCode, employeeCode, from, to)
            } returns emptyList()

            assertThat(service.getAccountOrderHistory(userId, accountCode, from, to)).isEmpty()
        }

        @Test
        @DisplayName("실패 - 사용자 미존재 → ClientOrderForbiddenException")
        fun forbiddenUserMissing() {
            every { employeeRepository.findById(userId) } returns Optional.empty()

            assertThatThrownBy { service.getAccountOrderHistory(userId, accountCode, from, to) }
                .isInstanceOf(ClientOrderForbiddenException::class.java)
        }
    }

    private fun createEmployee(): Employee = Employee(id = userId, employeeCode = employeeCode, name = "Test")

    private fun createOrder(employeeCode: String?): ErpOrder = ErpOrder(
        sapOrderNumber = sapOrderNumber,
        sapAccountCode = "0001234567",
        sapAccountName = "홍길동마트",
        deliveryRequestDate = java.time.LocalDate.of(2026, 5, 6),
        orderDate = java.time.LocalDate.of(2026, 5, 4),
        employeeCode = employeeCode,
        employeeName = "사원1",
        orderSalesAmount = BigDecimal.valueOf(1_250_000L),
    )

    private fun createProduct(
        lineNumber: String,
        deliveryStatus: String?,
        productCode: String? = "P001",
        productName: String? = "예시 상품",
        shippingQuantityBox: BigDecimal? = BigDecimal.ZERO,
        unit: String? = "BOX",
    ): ErpOrderProduct {
        val order = createOrder(employeeCode = employeeCode)
        return ErpOrderProduct(
            erpOrder = order,
            sapOrderNumber = sapOrderNumber,
            lineNumber = lineNumber,
            externalKey = "$sapOrderNumber-$lineNumber",
            productCode = productCode,
            productName = productName,
            shippingQuantityBox = shippingQuantityBox,
            unit = unit,
            deliveryStatus = deliveryStatus,
        )
    }
}
