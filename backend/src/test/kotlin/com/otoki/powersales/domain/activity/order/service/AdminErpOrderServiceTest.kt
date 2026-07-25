package com.otoki.powersales.domain.activity.order.service

import com.otoki.powersales.domain.activity.order.entity.ErpOrder
import com.otoki.powersales.domain.activity.order.entity.ErpOrderProduct
import com.otoki.powersales.domain.activity.order.exception.ErpOrderNotFoundException
import com.otoki.powersales.domain.activity.order.exception.InvalidOrderParameterException
import com.otoki.powersales.domain.activity.order.repository.ErpOrderProductRepository
import com.otoki.powersales.domain.activity.order.repository.ErpOrderRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

@DisplayName("AdminErpOrderService 테스트 (기준정보 > ERP주문 조회)")
class AdminErpOrderServiceTest {

    private val erpOrderRepository: ErpOrderRepository = mockk()
    private val erpOrderProductRepository: ErpOrderProductRepository = mockk()
    private val service = AdminErpOrderService(erpOrderRepository, erpOrderProductRepository)

    @Nested
    @DisplayName("getErpOrders - 목록 조회")
    inner class ListCases {

        @Test
        @DisplayName("정상 - 페이징 봉투(content/page/size/totalElements/totalPages) 매핑")
        fun success() {
            val orders = listOf(
                createOrder(id = 2, sapOrderNumber = "0300000002"),
                createOrder(id = 1, sapOrderNumber = "0300000001"),
            )
            every {
                erpOrderRepository.findAdminErpOrders(any(), any(), any(), any(), any(), any())
            } returns PageImpl(orders, PageRequest.of(0, 20), 2)

            val result = service.getErpOrders(null, null, null, null, null, 0, 20)

            assertThat(result.content).hasSize(2)
            assertThat(result.content[0].sapOrderNumber).isEqualTo("0300000002")
            assertThat(result.page).isEqualTo(0)
            assertThat(result.size).isEqualTo(20)
            assertThat(result.totalElements).isEqualTo(2)
            assertThat(result.totalPages).isEqualTo(1)
        }

        @Test
        @DisplayName("정상 - 필터 인자가 repository 로 그대로 전달된다")
        fun passesFilters() {
            every {
                erpOrderRepository.findAdminErpOrders(any(), any(), any(), any(), any(), any())
            } returns PageImpl(emptyList(), PageRequest.of(0, 20), 0)

            service.getErpOrders(
                keyword = "홍길동",
                deliveryDateFrom = LocalDate.of(2026, 5, 1),
                deliveryDateTo = LocalDate.of(2026, 5, 31),
                orderDateFrom = LocalDate.of(2026, 4, 1),
                orderDateTo = LocalDate.of(2026, 4, 30),
                page = 0,
                size = 20,
            )

            verify {
                erpOrderRepository.findAdminErpOrders(
                    keyword = "홍길동",
                    deliveryDateFrom = LocalDate.of(2026, 5, 1),
                    deliveryDateTo = LocalDate.of(2026, 5, 31),
                    orderDateFrom = LocalDate.of(2026, 4, 1),
                    orderDateTo = LocalDate.of(2026, 4, 30),
                    pageable = any(),
                )
            }
        }

        @Test
        @DisplayName("정상 - page/size null 이면 기본값(0/20) 적용")
        fun defaultsPagination() {
            val pageableSlot = slot<Pageable>()
            every {
                erpOrderRepository.findAdminErpOrders(any(), any(), any(), any(), any(), capture(pageableSlot))
            } returns PageImpl(emptyList(), PageRequest.of(0, 20), 0)

            service.getErpOrders(null, null, null, null, null, null, null)

            assertThat(pageableSlot.captured.pageNumber).isEqualTo(0)
            assertThat(pageableSlot.captured.pageSize).isEqualTo(20)
        }

        @Test
        @DisplayName("에러 - page 음수면 InvalidOrderParameterException")
        fun negativePage() {
            assertThatThrownBy { service.getErpOrders(null, null, null, null, null, -1, 20) }
                .isInstanceOf(InvalidOrderParameterException::class.java)
        }

        @Test
        @DisplayName("에러 - size 가 최대(100) 초과면 InvalidOrderParameterException")
        fun oversizePage() {
            assertThatThrownBy { service.getErpOrders(null, null, null, null, null, 0, 101) }
                .isInstanceOf(InvalidOrderParameterException::class.java)
        }
    }

    @Nested
    @DisplayName("getErpOrderDetail - 상세 조회")
    inner class DetailCases {

        @Test
        @DisplayName("정상 - 헤더 + 라인(lineNumber 오름차순) 매핑")
        fun success() {
            val order = createOrder(id = 1, sapOrderNumber = "0300011396")
            val products = listOf(
                createProduct(id = 10, lineNumber = "10", productCode = "P001", productName = "예시1"),
                createProduct(id = 20, lineNumber = "20", productCode = "P002", productName = "예시2"),
            )
            every { erpOrderRepository.findById(1) } returns Optional.of(order)
            every {
                erpOrderProductRepository.findBySapOrderNumberOrderByLineNumberAsc("0300011396")
            } returns products

            val result = service.getErpOrderDetail(1)

            assertThat(result.id).isEqualTo(1)
            assertThat(result.sapOrderNumber).isEqualTo("0300011396")
            assertThat(result.products).hasSize(2)
            assertThat(result.products[0].productCode).isEqualTo("P001")
            assertThat(result.products[1].productName).isEqualTo("예시2")
        }

        @Test
        @DisplayName("정상 - 라인이 없어도 빈 목록으로 정상 반환")
        fun noProducts() {
            val order = createOrder(id = 5, sapOrderNumber = "0300099999")
            every { erpOrderRepository.findById(5) } returns Optional.of(order)
            every {
                erpOrderProductRepository.findBySapOrderNumberOrderByLineNumberAsc("0300099999")
            } returns emptyList()

            val result = service.getErpOrderDetail(5)

            assertThat(result.products).isEmpty()
        }

        @Test
        @DisplayName("에러 - 미존재 id 면 ErpOrderNotFoundException + 라인 조회하지 않음")
        fun notFound() {
            every { erpOrderRepository.findById(999) } returns Optional.empty()

            assertThatThrownBy { service.getErpOrderDetail(999) }
                .isInstanceOf(ErpOrderNotFoundException::class.java)

            verify(exactly = 0) { erpOrderProductRepository.findBySapOrderNumberOrderByLineNumberAsc(any()) }
        }
    }

    private fun createOrder(id: Long, sapOrderNumber: String): ErpOrder = ErpOrder(
        id = id,
        sapOrderNumber = sapOrderNumber,
        sapAccountCode = "0001234567",
        sapAccountName = "홍길동마트",
        deliveryRequestDate = LocalDate.of(2026, 5, 6),
        orderDate = LocalDate.of(2026, 5, 4),
        employeeCode = "20030117",
        employeeName = "사원1",
        orderSalesAmount = BigDecimal.valueOf(1_250_000L),
        orderChannelNm = "APP",
        orderTypeNm = "일반주문",
    )

    private fun createProduct(
        id: Long,
        lineNumber: String,
        productCode: String?,
        productName: String?,
    ): ErpOrderProduct = ErpOrderProduct(
        id = id,
        sapOrderNumber = "0300011396",
        lineNumber = lineNumber,
        productCode = productCode,
        productName = productName,
        orderQuantity = BigDecimal.TEN,
        unit = "BOX",
    )
}
