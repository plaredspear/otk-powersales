package com.otoki.powersales.domain.activity.productexpiration.service

import com.otoki.powersales.platform.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.activity.productexpiration.dto.request.ProductExpirationBatchDeleteRequest
import com.otoki.powersales.domain.activity.productexpiration.dto.request.ProductExpirationCreateRequest
import com.otoki.powersales.domain.activity.productexpiration.dto.request.ProductExpirationUpdateRequest
import com.otoki.powersales.domain.activity.productexpiration.entity.ProductExpiration
import com.otoki.powersales.domain.activity.productexpiration.exception.InvalidAlertDateException
import com.otoki.powersales.domain.activity.productexpiration.exception.InvalidProductExpirationDateRangeException
import com.otoki.powersales.domain.activity.productexpiration.exception.ProductExpirationForbiddenException
import com.otoki.powersales.domain.activity.productexpiration.exception.ProductExpirationNotFoundException
import com.otoki.powersales.domain.activity.productexpiration.service.ProductExpirationService
import com.otoki.powersales.domain.activity.productexpiration.repository.ProductExpirationRepository
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.foundation.product.entity.Product
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.Optional

@DisplayName("ProductExpirationService 테스트")
class ProductExpirationServiceTest {

    private val productExpirationRepository: ProductExpirationRepository = mockk()
    private val employeeRepository: EmployeeRepository = mockk()
    private val accountRepository: AccountRepository = mockk()
    private val productRepository: ProductRepository = mockk()

    private val productExpirationService = ProductExpirationService(
        productExpirationRepository,
        employeeRepository,
        accountRepository,
        productRepository,
    )

    private val userId = 1L
    private val employeeCodeVal = "20030117"

    /** create 시 FK 조회 stub — 기본은 매칭 성공 (account_id/product_id 채워짐). */
    private fun stubFkLookup(accountId: Long? = 10L, productId: Long? = 20L) {
        every { accountRepository.findByExternalKey(any()) } returns
            accountId?.let { Account(id = it) }
        every { productRepository.findByProductCode(any()) } returns
            productId?.let { Product(id = it) }
    }

    private fun createEmployee(id: Long = userId, sfid: String? = "EMP_SFID_001"): Employee {
        return Employee(id = id, sfid = sfid, employeeCode = employeeCodeVal, name = "테스트사원")
    }

    private fun createProductExpiration(
        seq: Int = 1,
        productExpirationId: Int = seq,
        employeeId: Long = userId,
        accountCode: String = "1025172",
        accountName: String = "(유)경산식품",
        productCode: String = "30310009",
        productName: String = "고등어김치&무조림(캔)280G",
        expirationDate: LocalDate = LocalDate.of(2026, 3, 10),
        alarmDate: LocalDate = LocalDate.of(2026, 3, 9),
        description: String? = null
    ): ProductExpiration {
        return ProductExpiration(
            seq = seq,
            productExpirationId = productExpirationId,
            employeeId = employeeId,
            accountCode = accountCode,
            accountName = accountName,
            productCode = productCode,
            productName = productName,
            expirationDate = expirationDate,
            alarmDate = alarmDate,
            description = description
        )
    }

    private fun stubUser(id: Long = userId) {
        every { employeeRepository.findById(id) } returns Optional.of(createEmployee(id = id))
    }

    @Nested
    @DisplayName("getProductExpirationList - 유통기한 목록 조회")
    inner class GetProductExpirationListTests {

        @Test
        @DisplayName("전체 거래처 조회 - accountCode 미지정 -> 전체 목록 반환")
        fun getList_allAccounts_success() {
            stubUser()
            val items = listOf(createProductExpiration(seq = 1), createProductExpiration(seq = 2))
            every {
                productExpirationRepository.findByEmployeeIdAndExpirationDateBetweenOrderByExpirationDateAsc(
                    userId, any(), any()
                )
            } returns items

            val result = productExpirationService.getProductExpirationList(userId, null, "2026-03-01", "2026-03-31")

            assertThat(result).hasSize(2)
        }

        @Test
        @DisplayName("특정 거래처 조회 - accountCode 지정 -> 필터링된 목록 반환")
        fun getList_specificAccount_success() {
            stubUser()
            val items = listOf(createProductExpiration(seq = 1))
            every {
                productExpirationRepository.findByEmployeeIdAndAccountCodeAndExpirationDateBetweenOrderByExpirationDateAsc(
                    userId, "1025172", any(), any()
                )
            } returns items

            val result = productExpirationService.getProductExpirationList(userId, "1025172", "2026-03-01", "2026-03-31")

            assertThat(result).hasSize(1)
            assertThat(result[0].accountCode).isEqualTo("1025172")
        }

        @Test
        @DisplayName("빈 결과 - 데이터 없음 -> 빈 리스트 반환")
        fun getList_empty() {
            stubUser()
            every {
                productExpirationRepository.findByEmployeeIdAndExpirationDateBetweenOrderByExpirationDateAsc(
                    userId, any(), any()
                )
            } returns emptyList()

            val result = productExpirationService.getProductExpirationList(userId, null, "2026-03-01", "2026-03-31")

            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("데이터 없음 - employeeCode로 조회 결과 없음 -> 빈 리스트 반환")
        fun getList_noData_returnsEmpty() {
            stubUser()
            every {
                productExpirationRepository.findByEmployeeIdAndExpirationDateBetweenOrderByExpirationDateAsc(
                    userId, any(), any()
                )
            } returns emptyList()

            val result = productExpirationService.getProductExpirationList(userId, null, "2026-03-01", "2026-03-31")

            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("날짜 범위 초과 - 180일 초과 -> InvalidProductExpirationDateRangeException")
        fun getList_dateRangeExceeded() {
            assertThatThrownBy {
                productExpirationService.getProductExpirationList(userId, null, "2026-01-01", "2026-12-31")
            }.isInstanceOf(InvalidProductExpirationDateRangeException::class.java)
        }

        @Test
        @DisplayName("종료일 < 시작일 - 역전된 날짜 -> InvalidProductExpirationDateRangeException")
        fun getList_invalidDateOrder() {
            assertThatThrownBy {
                productExpirationService.getProductExpirationList(userId, null, "2026-03-31", "2026-03-01")
            }.isInstanceOf(InvalidProductExpirationDateRangeException::class.java)
        }

        @Test
        @DisplayName("잘못된 날짜 형식 -> InvalidProductExpirationDateRangeException")
        fun getList_invalidDateFormat() {
            assertThatThrownBy {
                productExpirationService.getProductExpirationList(userId, null, "invalid", "2026-03-31")
            }.isInstanceOf(InvalidProductExpirationDateRangeException::class.java)
        }
    }

    @Nested
    @DisplayName("createProductExpiration - 유통기한 등록")
    inner class CreateProductExpirationTests {

        @Test
        @DisplayName("정상 등록 - 유효한 요청 -> ProductExpiration 생성 + FK(account_id/product_id) 채움")
        fun create_success() {
            stubUser()
            stubFkLookup(accountId = 10L, productId = 20L)
            val request = ProductExpirationCreateRequest(
                accountCode = "1025172",
                accountName = "(유)경산식품",
                productCode = "30310009",
                productName = "고등어김치&무조림(캔)280G",
                expirationDate = "2026-03-10",
                alarmDate = "2026-03-09",
                description = "테스트"
            )
            val savedSlot = mutableListOf<ProductExpiration>()
            every { productExpirationRepository.save(capture(savedSlot)) } answers {
                firstArg<ProductExpiration>()
            }

            val result = productExpirationService.createProductExpiration(userId, request)

            assertThat(result.productCode).isEqualTo("30310009")
            assertThat(result.accountCode).isEqualTo("1025172")
            // 코드 → 신규 account/product 조회로 FK 가 채워졌는지 검증.
            assertThat(savedSlot.single().accountId).isEqualTo(10L)
            assertThat(savedSlot.single().productId).isEqualTo(20L)
        }

        @Test
        @DisplayName("코드 미매칭 - account/product 부재 -> 저장 허용 + FK NULL (등록 차단 안 함)")
        fun create_fkUnmatched_savesWithNullFk() {
            stubUser()
            stubFkLookup(accountId = null, productId = null)
            val request = ProductExpirationCreateRequest(
                accountCode = "UNKNOWN_ACC",
                accountName = "미등록거래처",
                productCode = "UNKNOWN_PRD",
                productName = "미등록상품",
                expirationDate = "2026-03-10",
                alarmDate = "2026-03-09"
            )
            val savedSlot = mutableListOf<ProductExpiration>()
            every { productExpirationRepository.save(capture(savedSlot)) } answers {
                firstArg<ProductExpiration>()
            }

            val result = productExpirationService.createProductExpiration(userId, request)

            // 매칭 실패해도 등록은 성공하고 코드/명 텍스트는 보존, FK 만 NULL.
            assertThat(result.accountCode).isEqualTo("UNKNOWN_ACC")
            assertThat(savedSlot.single().accountId).isNull()
            assertThat(savedSlot.single().productId).isNull()
        }

        @Test
        @DisplayName("알림일 오류 - alarm_date >= expiration_date -> InvalidAlertDateException")
        fun create_invalidAlertDate() {
            stubUser()
            val request = ProductExpirationCreateRequest(
                accountCode = "1025172",
                accountName = "(유)경산식품",
                productCode = "30310009",
                productName = "제품명",
                expirationDate = "2026-03-10",
                alarmDate = "2026-03-10"
            )

            assertThatThrownBy {
                productExpirationService.createProductExpiration(userId, request)
            }.isInstanceOf(InvalidAlertDateException::class.java)
        }

        @Test
        @DisplayName("사용자 없음 - 존재하지 않는 userId -> EmployeeNotFoundException")
        fun create_userNotFound() {
            every { employeeRepository.findById(999L) } returns Optional.empty()

            val request = ProductExpirationCreateRequest(
                accountCode = "1025172",
                accountName = "테스트",
                productCode = "30310009",
                productName = "제품명",
                expirationDate = "2026-03-10",
                alarmDate = "2026-03-09"
            )

            assertThatThrownBy {
                productExpirationService.createProductExpiration(999L, request)
            }.isInstanceOf(EmployeeNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("updateProductExpiration - 유통기한 수정")
    inner class UpdateProductExpirationTests {

        @Test
        @DisplayName("정상 수정 - 유효한 요청 -> 수정된 항목 반환")
        fun update_success() {
            stubUser()
            val entity = createProductExpiration(seq = 1)
            every { productExpirationRepository.findById(1) } returns Optional.of(entity)

            val request = ProductExpirationUpdateRequest(
                expirationDate = "2026-04-10",
                alarmDate = "2026-04-09",
                description = "수정된 설명"
            )

            val result = productExpirationService.updateProductExpiration(userId, 1, request)

            assertThat(result.expirationDate).isEqualTo("2026-04-10")
            assertThat(result.description).isEqualTo("수정된 설명")
        }

        @Test
        @DisplayName("타인 데이터 수정 - employeeCode 불일치 -> ProductExpirationForbiddenException")
        fun update_forbidden() {
            stubUser()
            val entity = createProductExpiration(seq = 1, employeeId = 999L)
            every { productExpirationRepository.findById(1) } returns Optional.of(entity)

            val request = ProductExpirationUpdateRequest(
                expirationDate = "2026-04-10",
                alarmDate = "2026-04-09"
            )

            assertThatThrownBy {
                productExpirationService.updateProductExpiration(userId, 1, request)
            }.isInstanceOf(ProductExpirationForbiddenException::class.java)
        }

        @Test
        @DisplayName("존재하지 않는 seq - 없는 seq -> ProductExpirationNotFoundException")
        fun update_notFound() {
            stubUser()
            every { productExpirationRepository.findById(999) } returns Optional.empty()

            val request = ProductExpirationUpdateRequest(
                expirationDate = "2026-04-10",
                alarmDate = "2026-04-09"
            )

            assertThatThrownBy {
                productExpirationService.updateProductExpiration(userId, 999, request)
            }.isInstanceOf(ProductExpirationNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("deleteProductExpiration - 유통기한 단건 삭제")
    inner class DeleteProductExpirationTests {

        @Test
        @DisplayName("정상 삭제 - 본인 데이터 -> 성공")
        fun delete_success() {
            stubUser()
            val entity = createProductExpiration(seq = 1)
            every { productExpirationRepository.findById(1) } returns Optional.of(entity)
            every { productExpirationRepository.delete(entity) } just Runs

            productExpirationService.deleteProductExpiration(userId, 1)
        }

        @Test
        @DisplayName("존재하지 않는 seq -> ProductExpirationNotFoundException")
        fun delete_notFound() {
            stubUser()
            every { productExpirationRepository.findById(999) } returns Optional.empty()

            assertThatThrownBy {
                productExpirationService.deleteProductExpiration(userId, 999)
            }.isInstanceOf(ProductExpirationNotFoundException::class.java)
        }

        @Test
        @DisplayName("타인 데이터 삭제 - employeeCode 불일치 -> ProductExpirationForbiddenException")
        fun delete_forbidden() {
            stubUser()
            val entity = createProductExpiration(seq = 1, employeeId = 999L)
            every { productExpirationRepository.findById(1) } returns Optional.of(entity)

            assertThatThrownBy {
                productExpirationService.deleteProductExpiration(userId, 1)
            }.isInstanceOf(ProductExpirationForbiddenException::class.java)
        }
    }

    @Nested
    @DisplayName("deleteProductExpirationBatch - 유통기한 일괄 삭제")
    inner class DeleteProductExpirationBatchTests {

        @Test
        @DisplayName("정상 일괄 삭제 - 본인 데이터 3건 -> deletedCount: 3")
        fun batchDelete_success() {
            stubUser()
            val items = listOf(
                createProductExpiration(seq = 1),
                createProductExpiration(seq = 2),
                createProductExpiration(seq = 3)
            )
            every {
                productExpirationRepository.findByProductExpirationIdInAndEmployeeId(listOf(1, 2, 3), userId)
            } returns items
            every { productExpirationRepository.deleteAll(items) } just Runs

            val request = ProductExpirationBatchDeleteRequest(ids = listOf(1, 2, 3))
            val result = productExpirationService.deleteProductExpirationBatch(userId, request)

            assertThat(result.deletedCount).isEqualTo(3)
        }

        @Test
        @DisplayName("타인 데이터 포함 - 1건이 타인 소유 -> ProductExpirationForbiddenException")
        fun batchDelete_forbidden() {
            stubUser()
            val myItem = createProductExpiration(seq = 1)
            every {
                productExpirationRepository.findByProductExpirationIdInAndEmployeeId(listOf(1, 2), userId)
            } returns listOf(myItem)
            every { productExpirationRepository.findAllById(listOf(2)) } returns
                listOf(createProductExpiration(seq = 2, employeeId = 999L))

            val request = ProductExpirationBatchDeleteRequest(ids = listOf(1, 2))

            assertThatThrownBy {
                productExpirationService.deleteProductExpirationBatch(userId, request)
            }.isInstanceOf(ProductExpirationForbiddenException::class.java)
        }
    }
}
