package com.otoki.powersales.order.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.draft.entity.TmpOrder
import com.otoki.powersales.draft.entity.TmpOrderProduct
import com.otoki.powersales.draft.repository.TmpOrderRepository
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.order.dto.request.OrderDraftLineRequest
import com.otoki.powersales.order.dto.request.OrderDraftRequest
import com.otoki.powersales.order.exception.OrderDraftAccountForbiddenException
import com.otoki.powersales.order.exception.OrderDraftInvalidRequestException
import com.otoki.powersales.product.entity.Product
import com.otoki.powersales.product.repository.ProductRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.Optional

@DisplayName("OrderDraftService 테스트 (#596)")
class OrderDraftServiceTest {

    private val tmpOrderRepository: TmpOrderRepository = mockk()
    private val accountRepository: AccountRepository = mockk()
    private val employeeRepository: EmployeeRepository = mockk()
    private val productRepository: ProductRepository = mockk()
    private val service = OrderDraftService(tmpOrderRepository, accountRepository, employeeRepository, productRepository)

    private val userId = 1L
    private val accountId = 5678L
    private val employeeCode = "20030117"

    private fun employee(code: String = employeeCode) = Employee(
        id = userId,
        employeeCode = code,
        name = "홍길동",
    )

    private fun account(empCode: String? = employeeCode) = Account(
        id = accountId,
        name = "테스트거래처",
        externalKey = "EK001",
        employeeCode = empCode,
    )

    private fun line(
        lineNumber: Int = 10,
        productCode: String = "P001",
        unit: String = "BOX",
        quantity: BigDecimal = BigDecimal("10"),
    ) = OrderDraftLineRequest(
        lineNumber = lineNumber,
        productCode = productCode,
        unit = unit,
        quantity = quantity,
        quantityPieces = 100,
        quantityBoxes = BigDecimal("10"),
        unitPrice = BigDecimal("12345"),
        amount = BigDecimal("123450"),
    )

    private fun req(lines: List<OrderDraftLineRequest> = listOf(line())) = OrderDraftRequest(
        accountId = accountId,
        totalAmount = 123450,
        lines = lines,
    )

    /** 리플렉션으로 ID 강제 설정 (테스트 전용 — IDENTITY 시뮬레이션). */
    private fun setId(o: TmpOrder, id: Long) {
        val field = TmpOrder::class.java.getDeclaredField("id").apply { isAccessible = true }
        field.setLong(o, id)
    }

    @Nested
    @DisplayName("save - 등록 (UPSERT)")
    inner class SaveTests {

        @Test
        @DisplayName("H1 - 신규 등록 (기존 임시저장 없음) → tmp_order 1행 + 라인 1행")
        fun newSave() {
            every { employeeRepository.findById(eq(userId)) } returns Optional.of(employee())
            every { accountRepository.findById(eq(accountId)) } returns Optional.of(account())
            every { tmpOrderRepository.findByEmployeeIdForUpdate(userId) } returns null
            every { productRepository.findByProductCodeIn(any()) } returns listOf(Product(id = 99L, productCode = "P001", name = "진라면"))
            every { tmpOrderRepository.save(any<TmpOrder>()) } answers {
                val o = firstArg<TmpOrder>()
                setId(o, 99L)
                o
            }

            val response = service.save(userId, req())

            assertThat(response.draftId).isEqualTo(99L)
            verify(exactly = 0) { tmpOrderRepository.delete(any<TmpOrder>()) }
            verify { tmpOrderRepository.save(any<TmpOrder>()) }
        }

        @Test
        @DisplayName("H2 - 덮어쓰기 등록 (기존 임시저장 있음) → 기존 삭제 후 새 IDENTITY")
        fun overwriteSave() {
            val existing = TmpOrder(id = 10L, employeeId = userId)
            every { employeeRepository.findById(eq(userId)) } returns Optional.of(employee())
            every { accountRepository.findById(eq(accountId)) } returns Optional.of(account())
            every { tmpOrderRepository.findByEmployeeIdForUpdate(userId) } returns existing
            every { productRepository.findByProductCodeIn(any()) } returns emptyList()
            every { tmpOrderRepository.delete(any<TmpOrder>()) } returns Unit
            every { tmpOrderRepository.flush() } returns Unit
            every { tmpOrderRepository.save(any<TmpOrder>()) } answers {
                val o = firstArg<TmpOrder>()
                setId(o, 11L)
                o
            }

            val response = service.save(userId, req())

            assertThat(response.draftId).isEqualTo(11L)
            assertThat(response.draftId).isNotEqualTo(10L) // 새 IDENTITY
            verify { tmpOrderRepository.delete(eq(existing)) }
            verify { tmpOrderRepository.save(any<TmpOrder>()) }
        }

        @Test
        @DisplayName("E1 - 본인 담당 거래처 아님 → ORD_DRAFT_ACCOUNT_FORBIDDEN")
        fun notMyAccount() {
            every { employeeRepository.findById(eq(userId)) } returns Optional.of(employee())
            every { accountRepository.findById(eq(accountId)) } returns Optional.of(account(empCode = "OTHER"))

            assertThatThrownBy { service.save(userId, req()) }
                .isInstanceOf(OrderDraftAccountForbiddenException::class.java)

            verify(exactly = 0) { tmpOrderRepository.save(any<TmpOrder>()) }
        }

        @Test
        @DisplayName("E1 - 거래처 employeeCode 가 null → ORD_DRAFT_ACCOUNT_FORBIDDEN")
        fun nullAccountEmpCode() {
            every { employeeRepository.findById(eq(userId)) } returns Optional.of(employee())
            every { accountRepository.findById(eq(accountId)) } returns Optional.of(account(empCode = null))

            assertThatThrownBy { service.save(userId, req()) }
                .isInstanceOf(OrderDraftAccountForbiddenException::class.java)
        }

        @Test
        @DisplayName("E3 - 단위 잘못된 값 → ORD_DRAFT_INVALID_REQUEST")
        fun invalidUnit() {
            val request = req(lines = listOf(line(unit = "CASE")))
            assertThatThrownBy { service.save(userId, request) }
                .isInstanceOf(OrderDraftInvalidRequestException::class.java)
        }

        @Test
        @DisplayName("E2 - 동일 요청 내 lineNumber 중복 → ORD_DRAFT_INVALID_REQUEST")
        fun duplicateLineNumber() {
            val request = req(
                lines = listOf(line(lineNumber = 1, productCode = "P1"), line(lineNumber = 1, productCode = "P2")),
            )
            assertThatThrownBy { service.save(userId, request) }
                .isInstanceOf(OrderDraftInvalidRequestException::class.java)
        }
    }

    @Nested
    @DisplayName("findByUserId - 조회")
    inner class FindTests {

        @Test
        @DisplayName("H4 - 임시저장 없음 → null")
        fun notFound() {
            every { tmpOrderRepository.findByEmployeeId(userId) } returns null
            val response = service.findByUserId(userId)
            assertThat(response).isNull()
        }

        @Test
        @DisplayName("H3 - 임시저장 있음 → 헤더+라인 매핑")
        fun foundWithLines() {
            val draft = TmpOrder(
                id = 99L,
                tmpEmployeeCode = employeeCode,
                tmpAccountCode = "EK001",
                tmpTotalAmount = "1234567",
                accountId = accountId,
                employeeId = userId,
            )
            val product = TmpOrderProduct(
                id = 1L,
                tmpProductCode = "P001",
                employeeId = userId,
                productId = 99L,
                tmpOrder = draft,
                lineNumber = 10,
                unit = "BOX",
                quantity = BigDecimal("10"),
                quantityPieces = 100,
                quantityBoxes = BigDecimal("10"),
                unitPrice = BigDecimal("12345"),
                amount = BigDecimal("123450"),
            )
            draft.products += product
            every { tmpOrderRepository.findByEmployeeId(userId) } returns draft
            every { accountRepository.findById(eq(accountId)) } returns Optional.of(account())
            every { productRepository.findByProductCodeIn(eq(listOf("P001"))) } returns listOf(Product(id = 99L, productCode = "P001", name = "진라면"))

            val response = service.findByUserId(userId)

            assertThat(response).isNotNull
            assertThat(response!!.draftId).isEqualTo(99L)
            assertThat(response.accountId).isEqualTo(accountId)
            assertThat(response.accountName).isEqualTo("테스트거래처")
            assertThat(response.accountExternalKey).isEqualTo("EK001")
            assertThat(response.totalAmount).isEqualTo(1234567L)
            assertThat(response.lines).hasSize(1)
            assertThat(response.lines[0].productCode).isEqualTo("P001")
            assertThat(response.lines[0].productName).isEqualTo("진라면")
            assertThat(response.lines[0].unit).isEqualTo("BOX")
            assertThat(response.lines[0].quantity).isEqualByComparingTo("10")
        }
    }

    @Nested
    @DisplayName("deleteByEmployeeId - 멱등 삭제")
    inner class DeleteTests {

        @Test
        @DisplayName("H5/H6 - 있어도 없어도 호출 가능 (멱등)")
        fun deleteIdempotent() {
            every { tmpOrderRepository.deleteByEmployeeId(userId) } returns 1L
            service.deleteByEmployeeId(userId)
            verify { tmpOrderRepository.deleteByEmployeeId(userId) }

            every { tmpOrderRepository.deleteByEmployeeId(userId) } returns 0L
            service.deleteByEmployeeId(userId)
        }
    }
}
