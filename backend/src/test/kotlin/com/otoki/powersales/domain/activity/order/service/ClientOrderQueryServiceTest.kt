package com.otoki.powersales.domain.activity.order.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.activity.order.enums.DeliveryStatus
import com.otoki.powersales.domain.activity.order.entity.ErpOrder
import com.otoki.powersales.domain.activity.order.entity.ErpOrderProduct
import com.otoki.powersales.domain.activity.order.exception.ClientNotFoundException
import com.otoki.powersales.domain.activity.order.exception.InvalidOrderParameterException
import com.otoki.powersales.domain.activity.order.exception.InvalidSapOrderNumberException
import com.otoki.powersales.domain.activity.order.exception.SapOrderNotFoundException
import com.otoki.powersales.domain.activity.order.repository.ErpOrderProductRepository
import com.otoki.powersales.domain.activity.order.repository.ErpOrderRepository
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.Optional
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@DisplayName("ClientOrderQueryService 테스트 (#593)")
class ClientOrderQueryServiceTest {

    private val erpOrderRepository: ErpOrderRepository = mockk()
    private val erpOrderProductRepository: ErpOrderProductRepository = mockk()
    private val accountRepository: AccountRepository = mockk()
    private val employeeRepository: EmployeeRepository = mockk()
    private val today = LocalDate.of(2026, 5, 6)
    private val clock = Clock.fixed(today.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneId.of("UTC"))
    private val service = ClientOrderQueryService(
        erpOrderRepository, erpOrderProductRepository, accountRepository, employeeRepository, clock,
    )

    private val userId = 1L
    private val sapOrderNumber = "0300011396"
    private val employeeCode = "20030117"

    init {
        // 기본값 — 로그인 사원 미해석(내 주문 아님). isMine 테스트에서만 override.
        every { employeeRepository.findById(any()) } returns Optional.empty()
        // 기본값 — 주문자 사번 미해석(주문자명 폴백). 해석 테스트에서만 override.
        every { employeeRepository.findByEmployeeCode(any()) } returns Optional.empty()
        // 기본값 — 목록 주문자명 배치 조회 미해석. 해석 테스트에서만 override.
        every { employeeRepository.findByEmployeeCodeIn(any()) } returns emptyList()
    }

    @Nested
    @DisplayName("getClientOrderDetail - 정상 조회")
    inner class SuccessCases {

        @Test
        @DisplayName("정상 - 주문 + 라인 N건 매핑 + DeliveryStatus 한글→영문 변환")
        fun success() {
            val order = createOrder(employeeCode = employeeCode)
            val products = listOf(
                createProduct(lineNumber = "10", deliveryStatus = "배송중", productCode = "P001", productName = "예시1", confirmQuantityBox = BigDecimal("5")),
                createProduct(lineNumber = "20", deliveryStatus = "배송 완료", productCode = "P002", productName = "예시2", confirmQuantityBox = BigDecimal("1234.5")),
            )
            every { erpOrderRepository.findBySapOrderNumber(sapOrderNumber) } returns order
            every { erpOrderProductRepository.findBySapOrderNumberOrderByLineNumberAsc(sapOrderNumber) } returns products

            val result = service.getClientOrderDetail(sapOrderNumber)

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
            // 레거시 `#,###.##` 정합 — 천단위 구분 + 소수 최대 2자리
            assertThat(result.orderedItems[1].deliveredQuantity).isEqualTo("1,234.5 BOX")
        }

        @Test
        @DisplayName("정상 - 주문자명은 사번으로 Employee 마스터에서 해석 (employee_name 무시)")
        fun ordererNameResolvedByEmployeeCode() {
            val order = createOrder(employeeCode = employeeCode) // employeeName="사원1"(시스템 계정명)
            every { erpOrderRepository.findBySapOrderNumber(sapOrderNumber) } returns order
            every { erpOrderProductRepository.findBySapOrderNumberOrderByLineNumberAsc(sapOrderNumber) } returns emptyList()
            every { employeeRepository.findByEmployeeCode(employeeCode) } returns
                Optional.of(Employee(id = 7L, employeeCode = employeeCode, name = "김영업"))

            val result = service.getClientOrderDetail(sapOrderNumber)

            assertThat(result.ordererCode).isEqualTo(employeeCode)
            assertThat(result.ordererName).isEqualTo("김영업")
        }

        @Test
        @DisplayName("정상 - 사번으로 사원 미해석 시 employee_name 으로 폴백")
        fun ordererNameFallsBackWhenEmployeeNotFound() {
            val order = createOrder(employeeCode = employeeCode) // employeeName="사원1"
            every { erpOrderRepository.findBySapOrderNumber(sapOrderNumber) } returns order
            every { erpOrderProductRepository.findBySapOrderNumberOrderByLineNumberAsc(sapOrderNumber) } returns emptyList()
            // findByEmployeeCode 는 기본 stub(Optional.empty()) — 미해석

            val result = service.getClientOrderDetail(sapOrderNumber)

            assertThat(result.ordererName).isEqualTo("사원1")
        }

        @Test
        @DisplayName("정상 - 라인 0건 → orderedItems 빈 배열")
        fun emptyLines() {
            val order = createOrder(employeeCode = employeeCode)
            every { erpOrderRepository.findBySapOrderNumber(sapOrderNumber) } returns order
            every { erpOrderProductRepository.findBySapOrderNumberOrderByLineNumberAsc(sapOrderNumber) } returns emptyList()

            val result = service.getClientOrderDetail(sapOrderNumber)

            assertThat(result.orderedItems).isEmpty()
            assertThat(result.orderedItemCount).isEqualTo(0)
        }

        @Test
        @DisplayName("정상 - DB 한글 미정의 라벨 → PENDING fallback")
        fun unknownStatusFallback() {
            val order = createOrder(employeeCode = employeeCode)
            val products = listOf(createProduct(lineNumber = "10", deliveryStatus = "알수없음"))
            every { erpOrderRepository.findBySapOrderNumber(sapOrderNumber) } returns order
            every { erpOrderProductRepository.findBySapOrderNumberOrderByLineNumberAsc(sapOrderNumber) } returns products

            val result = service.getClientOrderDetail(sapOrderNumber)

            assertThat(result.orderedItems[0].deliveryStatus).isEqualTo(DeliveryStatus.PENDING)
        }
    }

    @Nested
    @DisplayName("getClientOrderDetail - 검증/예외")
    inner class ErrorCases {

        @Test
        @DisplayName("실패 - 형식 오류 (영문) → InvalidSapOrderNumberException")
        fun invalidFormatAlpha() {
            assertThatThrownBy { service.getClientOrderDetail("abc123") }
                .isInstanceOf(InvalidSapOrderNumberException::class.java)
        }

        @Test
        @DisplayName("실패 - 형식 오류 (빈 문자) → InvalidSapOrderNumberException")
        fun invalidFormatEmpty() {
            assertThatThrownBy { service.getClientOrderDetail("") }
                .isInstanceOf(InvalidSapOrderNumberException::class.java)
        }

        @Test
        @DisplayName("실패 - SAP 주문번호 미존재 → SapOrderNotFoundException")
        fun notFound() {
            every { erpOrderRepository.findBySapOrderNumber(sapOrderNumber) } returns null

            assertThatThrownBy { service.getClientOrderDetail(sapOrderNumber) }
                .isInstanceOf(SapOrderNotFoundException::class.java)
        }

        @Test
        @DisplayName("정상 - 담당자 불일치(타 사원 주문)여도 조회 가능 (레거시 정합, 권한 게이트 없음)")
        fun otherEmployeeOrderAccessible() {
            val order = createOrder(employeeCode = "OTHER_CODE")
            every { erpOrderRepository.findBySapOrderNumber(sapOrderNumber) } returns order
            every { erpOrderProductRepository.findBySapOrderNumberOrderByLineNumberAsc(sapOrderNumber) } returns emptyList()

            val result = service.getClientOrderDetail(sapOrderNumber)

            assertThat(result.sapOrderNumber).isEqualTo(sapOrderNumber)
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
    @DisplayName("getClientOrders - 거래처별 주문 목록")
    inner class ClientOrderListCases {

        private val clientId = 10L
        private val deliveryDate = LocalDate.of(2026, 5, 6)

        @Test
        @DisplayName("정상 - 거래처 단위 조회 + DTO 매핑 (담당자 필터 없음)")
        fun success() {
            val order = createOrder(employeeCode = "OTHER_CODE").apply {
                account = Account(id = clientId, name = "홍길동마트")
            }
            every { accountRepository.existsById(clientId) } returns true
            every {
                erpOrderRepository.findClientOrders(clientId, deliveryDate, any())
            } returns PageImpl(listOf(order), PageRequest.of(0, 20), 1)

            val result = service.getClientOrders(userId, clientId, deliveryDate, null, null)

            assertThat(result.totalElements).isEqualTo(1)
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].sapOrderNumber).isEqualTo(sapOrderNumber)
            assertThat(result.content[0].clientId).isEqualTo(clientId)
            assertThat(result.content[0].clientName).isEqualTo("홍길동마트")
            assertThat(result.content[0].totalAmount).isEqualTo(1_250_000L)
            // 로그인 사원 미해석(기본 stub) → 내 주문 아님
            assertThat(result.content[0].isMine).isFalse()
        }

        @Test
        @DisplayName("정상 - 주문자사번 == 로그인 사원 사번 → isMine=true (내 주문 강조)")
        fun isMineTrueWhenEmployeeCodeMatches() {
            val order = createOrder(employeeCode = "MINE_CODE").apply {
                account = Account(id = clientId, name = "홍길동마트")
            }
            every { accountRepository.existsById(clientId) } returns true
            every { employeeRepository.findById(userId) } returns
                Optional.of(Employee(id = userId, employeeCode = "MINE_CODE", name = "me"))
            every {
                erpOrderRepository.findClientOrders(clientId, deliveryDate, any())
            } returns PageImpl(listOf(order), PageRequest.of(0, 20), 1)

            val result = service.getClientOrders(userId, clientId, deliveryDate, null, null)

            assertThat(result.content[0].isMine).isTrue()
        }

        @Test
        @DisplayName("정상 - 주문자명은 사번 배치 조회로 해석 (findByEmployeeCodeIn 1회, N+1 아님)")
        fun ordererNameResolvedInBatch() {
            val order = createOrder(employeeCode = "OTHER_CODE").apply {
                account = Account(id = clientId, name = "홍길동마트")
            }
            every { accountRepository.existsById(clientId) } returns true
            every {
                erpOrderRepository.findClientOrders(clientId, deliveryDate, any())
            } returns PageImpl(listOf(order), PageRequest.of(0, 20), 1)
            every { employeeRepository.findByEmployeeCodeIn(listOf("OTHER_CODE")) } returns
                listOf(Employee(id = 8L, employeeCode = "OTHER_CODE", name = "박담당"))

            val result = service.getClientOrders(userId, clientId, deliveryDate, null, null)

            assertThat(result.content[0].ordererName).isEqualTo("박담당")
            // 페이지 전체를 IN 절 1회로 조회 — N+1 아님
            verify(exactly = 1) { employeeRepository.findByEmployeeCodeIn(any()) }
        }

        @Test
        @DisplayName("정상 - 기본 정렬은 납기일/주문번호 내림차순 + 기본 페이지 20")
        fun defaultPaging() {
            val pageableSlot = slot<Pageable>()
            every { accountRepository.existsById(clientId) } returns true
            every {
                erpOrderRepository.findClientOrders(clientId, today, capture(pageableSlot))
            } returns PageImpl(emptyList(), PageRequest.of(0, 20), 0)

            service.getClientOrders(userId, clientId, null, null, null)

            assertThat(pageableSlot.captured.pageNumber).isEqualTo(0)
            assertThat(pageableSlot.captured.pageSize).isEqualTo(20)
            val order = pageableSlot.captured.sort.getOrderFor("deliveryRequestDate")
            assertThat(order?.isDescending).isTrue()
        }

        @Test
        @DisplayName("정상 - 납기일 미지정 시 오늘로 기본 적용 (레거시 단일 날짜 정합, 전체 조회 아님)")
        fun defaultsToTodayWhenNull() {
            val deliveryDateSlot = slot<LocalDate>()
            every { accountRepository.existsById(clientId) } returns true
            every {
                erpOrderRepository.findClientOrders(eq(clientId), capture(deliveryDateSlot), any())
            } returns PageImpl(emptyList(), PageRequest.of(0, 20), 0)

            service.getClientOrders(userId, clientId, null, null, null)

            assertThat(deliveryDateSlot.captured).isEqualTo(today)
        }

        @Test
        @DisplayName("실패 - 거래처 미존재 → ClientNotFoundException")
        fun clientNotFound() {
            every { accountRepository.existsById(clientId) } returns false

            assertThatThrownBy { service.getClientOrders(userId, clientId, deliveryDate, null, null) }
                .isInstanceOf(ClientNotFoundException::class.java)
        }

        @Test
        @DisplayName("실패 - 페이지 음수 → InvalidOrderParameterException")
        fun negativePage() {
            assertThatThrownBy { service.getClientOrders(userId, clientId, deliveryDate, -1, null) }
                .isInstanceOf(InvalidOrderParameterException::class.java)
        }

        @Test
        @DisplayName("실패 - 페이지 크기 초과 → InvalidOrderParameterException")
        fun sizeTooLarge() {
            assertThatThrownBy { service.getClientOrders(userId, clientId, deliveryDate, 0, 101) }
                .isInstanceOf(InvalidOrderParameterException::class.java)
        }
    }

    private fun createOrder(employeeCode: String?): ErpOrder = ErpOrder(
        sapOrderNumber = sapOrderNumber,
        sapAccountCode = "0001234567",
        sapAccountName = "홍길동마트",
        deliveryRequestDate = LocalDate.of(2026, 5, 6),
        orderDate = LocalDate.of(2026, 5, 4),
        employeeCode = employeeCode,
        employeeName = "사원1",
        orderSalesAmount = BigDecimal.valueOf(1_250_000L),
    )

    private fun createProduct(
        lineNumber: String,
        deliveryStatus: String?,
        productCode: String? = "P001",
        productName: String? = "예시 상품",
        confirmQuantityBox: BigDecimal? = BigDecimal.ZERO,
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
            confirmQuantityBox = confirmQuantityBox,
            unit = unit,
            deliveryStatus = deliveryStatus,
        )
    }
}
