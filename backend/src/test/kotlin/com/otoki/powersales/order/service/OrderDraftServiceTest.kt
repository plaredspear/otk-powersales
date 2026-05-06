package com.otoki.powersales.order.service

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.account.repository.AccountRepository
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
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.math.BigDecimal
import java.util.Optional

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OrderDraftService 테스트 (#596)")
class OrderDraftServiceTest {

    @Mock private lateinit var tmpOrderRepository: TmpOrderRepository
    @Mock private lateinit var accountRepository: AccountRepository
    @Mock private lateinit var employeeRepository: EmployeeRepository
    @Mock private lateinit var productRepository: ProductRepository
    @InjectMocks private lateinit var service: OrderDraftService

    private val userId = 1L
    private val accountId = 5678L
    private val employeeCode = "20030117"

    private fun employee(code: String = employeeCode) = Employee(
        id = userId,
        employeeCode = code,
        name = "홍길동",
    )

    private fun account(empCode: String? = employeeCode) = Account(
        id = accountId.toInt(),
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
            whenever(employeeRepository.findById(eq(userId))).thenReturn(Optional.of(employee()))
            whenever(accountRepository.findById(eq(accountId.toInt()))).thenReturn(Optional.of(account()))
            whenever(tmpOrderRepository.findByEmployeeIdForUpdate(userId)).thenReturn(null)
            whenever(productRepository.findByProductCodeIn(any())).thenReturn(
                listOf(Product(id = 99L, productCode = "P001", name = "진라면")),
            )
            whenever(tmpOrderRepository.save(any<TmpOrder>())).thenAnswer { inv ->
                val o = inv.arguments[0] as TmpOrder
                setId(o, 99L)
                o
            }

            val response = service.save(userId, req())

            assertThat(response.draftId).isEqualTo(99L)
            verify(tmpOrderRepository, never()).delete(any<TmpOrder>())
            verify(tmpOrderRepository).save(any<TmpOrder>())
        }

        @Test
        @DisplayName("H2 - 덮어쓰기 등록 (기존 임시저장 있음) → 기존 삭제 후 새 IDENTITY")
        fun overwriteSave() {
            val existing = TmpOrder(id = 10L, employeeId = userId)
            whenever(employeeRepository.findById(eq(userId))).thenReturn(Optional.of(employee()))
            whenever(accountRepository.findById(eq(accountId.toInt()))).thenReturn(Optional.of(account()))
            whenever(tmpOrderRepository.findByEmployeeIdForUpdate(userId)).thenReturn(existing)
            whenever(productRepository.findByProductCodeIn(any())).thenReturn(emptyList())
            whenever(tmpOrderRepository.save(any<TmpOrder>())).thenAnswer { inv ->
                val o = inv.arguments[0] as TmpOrder
                setId(o, 11L)
                o
            }

            val response = service.save(userId, req())

            assertThat(response.draftId).isEqualTo(11L)
            assertThat(response.draftId).isNotEqualTo(10L) // 새 IDENTITY
            verify(tmpOrderRepository).delete(eq(existing))
            verify(tmpOrderRepository).save(any<TmpOrder>())
        }

        @Test
        @DisplayName("E1 - 본인 담당 거래처 아님 → ORD_DRAFT_ACCOUNT_FORBIDDEN")
        fun notMyAccount() {
            whenever(employeeRepository.findById(eq(userId))).thenReturn(Optional.of(employee()))
            whenever(accountRepository.findById(eq(accountId.toInt())))
                .thenReturn(Optional.of(account(empCode = "OTHER")))

            assertThatThrownBy { service.save(userId, req()) }
                .isInstanceOf(OrderDraftAccountForbiddenException::class.java)

            verify(tmpOrderRepository, never()).save(any<TmpOrder>())
        }

        @Test
        @DisplayName("E1 - 거래처 employeeCode 가 null → ORD_DRAFT_ACCOUNT_FORBIDDEN")
        fun nullAccountEmpCode() {
            whenever(employeeRepository.findById(eq(userId))).thenReturn(Optional.of(employee()))
            whenever(accountRepository.findById(eq(accountId.toInt())))
                .thenReturn(Optional.of(account(empCode = null)))

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
            whenever(tmpOrderRepository.findByEmployeeId(userId)).thenReturn(null)
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
            whenever(tmpOrderRepository.findByEmployeeId(userId)).thenReturn(draft)
            whenever(accountRepository.findById(eq(accountId.toInt()))).thenReturn(Optional.of(account()))
            whenever(productRepository.findByProductCodeIn(eq(listOf("P001")))).thenReturn(
                listOf(Product(id = 99L, productCode = "P001", name = "진라면")),
            )

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
            whenever(tmpOrderRepository.deleteByEmployeeId(userId)).thenReturn(1L)
            service.deleteByEmployeeId(userId)
            verify(tmpOrderRepository).deleteByEmployeeId(userId)

            whenever(tmpOrderRepository.deleteByEmployeeId(userId)).thenReturn(0L)
            service.deleteByEmployeeId(userId)
        }
    }
}
