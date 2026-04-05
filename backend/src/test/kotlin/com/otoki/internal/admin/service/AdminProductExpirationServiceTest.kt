package com.otoki.internal.admin.service

import com.otoki.internal.admin.dto.request.AdminProductExpirationBatchDeleteRequest
import com.otoki.internal.admin.dto.request.AdminProductExpirationCreateRequest
import com.otoki.internal.admin.dto.request.AdminProductExpirationUpdateRequest
import com.otoki.internal.admin.dto.response.AdminProductExpirationSummaryResponse
import com.otoki.internal.auth.exception.EmployeeNotFoundException
import com.otoki.internal.common.exception.ProductNotFoundException
import com.otoki.internal.productexpiration.entity.ProductExpiration
import com.otoki.internal.productexpiration.exception.InvalidAlertDateException
import com.otoki.internal.productexpiration.exception.ProductExpirationAccountNotFoundException
import com.otoki.internal.productexpiration.exception.ProductExpirationNotFoundException
import com.otoki.internal.productexpiration.repository.ProductExpirationRepository
import com.otoki.internal.sap.entity.Account
import com.otoki.internal.sap.entity.Employee
import com.otoki.internal.sap.entity.Product
import com.otoki.internal.sap.repository.AccountRepository
import com.otoki.internal.sap.repository.EmployeeRepository
import com.otoki.internal.sap.repository.ProductRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDate
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("AdminProductExpirationService 테스트")
class AdminProductExpirationServiceTest {

    @Mock
    private lateinit var productExpirationRepository: ProductExpirationRepository

    @Mock
    private lateinit var employeeRepository: EmployeeRepository

    @Mock
    private lateinit var accountRepository: AccountRepository

    @Mock
    private lateinit var productRepository: ProductRepository

    @InjectMocks
    private lateinit var adminProductExpirationService: AdminProductExpirationService

    // ── Helper factory methods ──────────────────────────────────────

    private fun createEmployee(
        id: Long = 1L,
        sfid: String? = "EMP_SFID_001",
        employeeCode: String = "E001",
        name: String = "홍길동"
    ) = Employee(id = id, sfid = sfid, employeeCode = employeeCode, name = name)

    private fun createAccount(
        id: Int = 1,
        sfid: String? = "ACC_SFID_001",
        name: String? = "테스트거래처",
        externalKey: String? = "ACC001"
    ) = Account(id = id, sfid = sfid, name = name, externalKey = externalKey)

    private fun createProduct(
        id: Long = 1L,
        sfid: String? = "PRD_SFID_001",
        name: String? = "오뚜기카레",
        productCode: String? = "P001"
    ) = Product(id = id, sfid = sfid, name = name, productCode = productCode)

    private fun createProductExpiration(
        productExpirationId: Int = 1,
        seq: Int = 0,
        employeeId: Long? = 1L,
        employeeSfid: String? = "EMP_SFID_001",
        accountId: Int? = 1,
        accountName: String? = "테스트거래처",
        accountCode: String? = "ACC001",
        productId: Long? = 1L,
        productName: String? = "오뚜기카레",
        productCode: String? = "P001",
        expirationDate: LocalDate? = LocalDate.of(2026, 6, 30),
        alarmDate: LocalDate? = LocalDate.of(2026, 6, 23),
        description: String? = null,
        employee: Employee? = null
    ): ProductExpiration {
        val entity = ProductExpiration(
            productExpirationId = productExpirationId,
            seq = seq,
            employeeId = employeeId,
            employeeSfid = employeeSfid,
            accountId = accountId,
            accountName = accountName,
            accountCode = accountCode,
            productId = productId,
            productName = productName,
            productCode = productCode,
            expirationDate = expirationDate,
            alarmDate = alarmDate,
            description = description
        )
        entity.employee = employee
        return entity
    }

    private fun createCreateRequest(
        employeeCode: String = "E001",
        accountCode: String = "ACC001",
        productCode: String = "P001",
        expirationDate: String = "2026-06-30",
        alarmDate: String = "2026-06-23",
        description: String? = null
    ) = AdminProductExpirationCreateRequest(
        employeeCode = employeeCode,
        accountCode = accountCode,
        productCode = productCode,
        expirationDate = expirationDate,
        alarmDate = alarmDate,
        description = description
    )

    private fun createUpdateRequest(
        expirationDate: String = "2026-07-31",
        alarmDate: String = "2026-07-24",
        description: String? = "수정된 설명"
    ) = AdminProductExpirationUpdateRequest(
        expirationDate = expirationDate,
        alarmDate = alarmDate,
        description = description
    )

    // ── getList ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("getList - 유통기한 목록 조회")
    inner class GetListTests {

        @Test
        @DisplayName("기본 조회 - null 파라미터 → 날짜 필터 없이 전체 목록 반환")
        fun getList_withNullParams_returnsListWithDefaults() {
            // Given
            val pageable = PageRequest.of(0, 20)
            val testEmployee = createEmployee()
            val entity = createProductExpiration(employee = testEmployee)
            val page = PageImpl(listOf(entity), pageable, 1L)

            whenever(
                productExpirationRepository.findForAdmin(
                    any(), any(), anyOrNull(), anyOrNull(), anyOrNull(), any(), any()
                )
            ).thenReturn(page)

            // When
            val result = adminProductExpirationService.getList(
                fromDate = null,
                toDate = null,
                employeeKeyword = null,
                accountKeyword = null,
                status = null,
                pageable = pageable
            )

            // Then
            assertThat(result.content).hasSize(1)
            assertThat(result.page).isEqualTo(0)
            assertThat(result.size).isEqualTo(20)
            assertThat(result.totalElements).isEqualTo(1L)
            assertThat(result.totalPages).isEqualTo(1)
            assertThat(result.content[0].productName).isEqualTo("오뚜기카레")
        }
    }

    // ── getDetail ───────────────────────────────────────────────────

    @Nested
    @DisplayName("getDetail - 유통기한 상세 조회")
    inner class GetDetailTests {

        @Test
        @DisplayName("정상 조회 - 존재하는 ID -> 상세 정보 반환")
        fun getDetail_existingId_returnsResponse() {
            // Given
            val testEmployee = createEmployee()
            val entity = createProductExpiration(employee = testEmployee)
            whenever(productExpirationRepository.findById(1)).thenReturn(Optional.of(entity))

            // When
            val result = adminProductExpirationService.getDetail(1)

            // Then
            assertThat(result.id).isEqualTo(1)
            assertThat(result.productName).isEqualTo("오뚜기카레")
            assertThat(result.accountName).isEqualTo("테스트거래처")
            assertThat(result.employeeName).isEqualTo("홍길동")
            assertThat(result.employeeCode).isEqualTo("E001")
        }

        @Test
        @DisplayName("미존재 - 없는 ID -> ProductExpirationNotFoundException")
        fun getDetail_nonExistingId_throwsException() {
            // Given
            whenever(productExpirationRepository.findById(999)).thenReturn(Optional.empty())

            // When & Then
            assertThatThrownBy { adminProductExpirationService.getDetail(999) }
                .isInstanceOf(ProductExpirationNotFoundException::class.java)
        }
    }

    // ── create ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("create - 유통기한 등록")
    inner class CreateTests {

        @Test
        @DisplayName("정상 등록 - 유효한 요청 -> 유통기한 생성 반환")
        fun create_validRequest_returnsResponse() {
            // Given
            val testEmployee = createEmployee()
            val testAccount = createAccount()
            val testProduct = createProduct()
            val request = createCreateRequest()

            whenever(employeeRepository.findByEmployeeCode("E001")).thenReturn(Optional.of(testEmployee))
            whenever(accountRepository.findByExternalKey("ACC001")).thenReturn(testAccount)
            whenever(productRepository.findByProductCode("P001")).thenReturn(testProduct)
            whenever(productExpirationRepository.save(any<ProductExpiration>())).thenAnswer {
                val arg = it.getArgument<ProductExpiration>(0)
                createProductExpiration(
                    productExpirationId = 10,
                    employeeId = arg.employeeId,
                    employeeSfid = arg.employeeSfid,
                    accountId = arg.accountId,
                    accountName = arg.accountName,
                    accountCode = arg.accountCode,
                    productId = arg.productId,
                    productName = arg.productName,
                    productCode = arg.productCode,
                    expirationDate = arg.expirationDate,
                    alarmDate = arg.alarmDate,
                    description = arg.description
                )
            }
            // getDetail(saved.productExpirationId) calls findById
            val entityWithRelation = createProductExpiration(
                productExpirationId = 10,
                employee = testEmployee
            )
            whenever(productExpirationRepository.findById(10)).thenReturn(Optional.of(entityWithRelation))

            // When
            val result = adminProductExpirationService.create(request)

            // Then
            assertThat(result.id).isEqualTo(10)
            assertThat(result.employeeName).isEqualTo("홍길동")
            assertThat(result.accountName).isEqualTo("테스트거래처")
            assertThat(result.productName).isEqualTo("오뚜기카레")
            assertThat(result.expirationDate).isEqualTo("2026-06-30")
            assertThat(result.alarmDate).isEqualTo("2026-06-23")
        }

        @Test
        @DisplayName("사원 없음 - 존재하지 않는 사번 -> EmployeeNotFoundException")
        fun create_employeeNotFound_throwsException() {
            // Given
            val request = createCreateRequest(employeeCode = "INVALID")
            whenever(employeeRepository.findByEmployeeCode("INVALID")).thenReturn(Optional.empty())

            // When & Then
            assertThatThrownBy { adminProductExpirationService.create(request) }
                .isInstanceOf(EmployeeNotFoundException::class.java)
        }

        @Test
        @DisplayName("거래처 없음 - 존재하지 않는 거래처 코드 -> ProductExpirationAccountNotFoundException")
        fun create_accountNotFound_throwsException() {
            // Given
            val request = createCreateRequest(accountCode = "INVALID")
            whenever(employeeRepository.findByEmployeeCode("E001")).thenReturn(Optional.of(createEmployee()))
            whenever(accountRepository.findByExternalKey("INVALID")).thenReturn(null)

            // When & Then
            assertThatThrownBy { adminProductExpirationService.create(request) }
                .isInstanceOf(ProductExpirationAccountNotFoundException::class.java)
        }

        @Test
        @DisplayName("제품 없음 - 존재하지 않는 제품 코드 -> ProductNotFoundException")
        fun create_productNotFound_throwsException() {
            // Given
            val request = createCreateRequest(productCode = "INVALID")
            whenever(employeeRepository.findByEmployeeCode("E001")).thenReturn(Optional.of(createEmployee()))
            whenever(accountRepository.findByExternalKey("ACC001")).thenReturn(createAccount())
            whenever(productRepository.findByProductCode("INVALID")).thenReturn(null)

            // When & Then
            assertThatThrownBy { adminProductExpirationService.create(request) }
                .isInstanceOf(ProductNotFoundException::class.java)
        }

        @Test
        @DisplayName("알림일 오류 - 알림일이 유통기한 이후 -> InvalidAlertDateException")
        fun create_alarmDateNotBeforeExpiration_throwsException() {
            // Given
            val request = createCreateRequest(
                expirationDate = "2026-06-30",
                alarmDate = "2026-06-30" // 같은 날짜 (not before)
            )
            whenever(employeeRepository.findByEmployeeCode("E001")).thenReturn(Optional.of(createEmployee()))
            whenever(accountRepository.findByExternalKey("ACC001")).thenReturn(createAccount())
            whenever(productRepository.findByProductCode("P001")).thenReturn(createProduct())

            // When & Then
            assertThatThrownBy { adminProductExpirationService.create(request) }
                .isInstanceOf(InvalidAlertDateException::class.java)
        }
    }

    // ── update ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("update - 유통기한 수정")
    inner class UpdateTests {

        @Test
        @DisplayName("정상 수정 - 유효한 요청 -> 수정된 정보 반환")
        fun update_validRequest_returnsUpdatedResponse() {
            // Given
            val testEmployee = createEmployee()
            val entity = createProductExpiration(employee = testEmployee)
            val request = createUpdateRequest()
            whenever(productExpirationRepository.findById(1)).thenReturn(Optional.of(entity))

            // When
            val result = adminProductExpirationService.update(1, request)

            // Then
            assertThat(result.id).isEqualTo(1)
            assertThat(result.expirationDate).isEqualTo("2026-07-31")
            assertThat(result.alarmDate).isEqualTo("2026-07-24")
            assertThat(result.description).isEqualTo("수정된 설명")
        }

        @Test
        @DisplayName("미존재 - 없는 ID -> ProductExpirationNotFoundException")
        fun update_nonExistingId_throwsException() {
            // Given
            val request = createUpdateRequest()
            whenever(productExpirationRepository.findById(999)).thenReturn(Optional.empty())

            // When & Then
            assertThatThrownBy { adminProductExpirationService.update(999, request) }
                .isInstanceOf(ProductExpirationNotFoundException::class.java)
        }

        @Test
        @DisplayName("알림일 오류 - 알림일이 유통기한 이후 -> InvalidAlertDateException")
        fun update_alarmDateNotBeforeExpiration_throwsException() {
            // Given
            val entity = createProductExpiration()
            val request = createUpdateRequest(
                expirationDate = "2026-07-31",
                alarmDate = "2026-08-01" // 유통기한 이후
            )
            whenever(productExpirationRepository.findById(1)).thenReturn(Optional.of(entity))

            // When & Then
            assertThatThrownBy { adminProductExpirationService.update(1, request) }
                .isInstanceOf(InvalidAlertDateException::class.java)
        }
    }

    // ── delete ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("delete - 유통기한 삭제")
    inner class DeleteTests {

        @Test
        @DisplayName("정상 삭제 - 존재하는 ID -> 삭제 수행")
        fun delete_existingId_deletesEntity() {
            // Given
            val entity = createProductExpiration()
            whenever(productExpirationRepository.findById(1)).thenReturn(Optional.of(entity))

            // When
            adminProductExpirationService.delete(1)

            // Then
            verify(productExpirationRepository).delete(entity)
        }

        @Test
        @DisplayName("미존재 - 없는 ID -> ProductExpirationNotFoundException")
        fun delete_nonExistingId_throwsException() {
            // Given
            whenever(productExpirationRepository.findById(999)).thenReturn(Optional.empty())

            // When & Then
            assertThatThrownBy { adminProductExpirationService.delete(999) }
                .isInstanceOf(ProductExpirationNotFoundException::class.java)
        }
    }

    // ── batchDelete ─────────────────────────────────────────────────

    @Nested
    @DisplayName("batchDelete - 유통기한 일괄 삭제")
    inner class BatchDeleteTests {

        @Test
        @DisplayName("정상 일괄 삭제 - 모든 ID 존재 -> 삭제 건수 반환")
        fun batchDelete_allExist_returnsDeletedCount() {
            // Given
            val ids = listOf(1, 2, 3)
            val request = AdminProductExpirationBatchDeleteRequest(ids = ids)
            val entities = ids.map { createProductExpiration(productExpirationId = it) }
            whenever(productExpirationRepository.findAllById(ids)).thenReturn(entities)

            // When
            val result = adminProductExpirationService.batchDelete(request)

            // Then
            assertThat(result.deletedCount).isEqualTo(3)
            verify(productExpirationRepository).deleteAll(entities)
        }

        @Test
        @DisplayName("일부 미존재 - 요청 ID 중 일부 미존재 -> ProductExpirationNotFoundException")
        fun batchDelete_someNotFound_throwsException() {
            // Given
            val ids = listOf(1, 2, 999)
            val request = AdminProductExpirationBatchDeleteRequest(ids = ids)
            // 2건만 조회됨 (999는 미존재)
            val entities = listOf(
                createProductExpiration(productExpirationId = 1),
                createProductExpiration(productExpirationId = 2)
            )
            whenever(productExpirationRepository.findAllById(ids)).thenReturn(entities)

            // When & Then
            assertThatThrownBy { adminProductExpirationService.batchDelete(request) }
                .isInstanceOf(ProductExpirationNotFoundException::class.java)
        }
    }

    // ── getSummary ──────────────────────────────────────────────────

    @Nested
    @DisplayName("getSummary - 유통기한 요약 조회")
    inner class GetSummaryTests {

        @Test
        @DisplayName("정상 조회 - 요약 통계 반환")
        fun getSummary_returnsSummaryResponse() {
            // Given
            val summary = AdminProductExpirationSummaryResponse(
                totalCount = 100L,
                expiredCount = 10L,
                imminentCount = 20L,
                normalCount = 70L
            )
            whenever(productExpirationRepository.getSummary(any())).thenReturn(summary)

            // When
            val result = adminProductExpirationService.getSummary()

            // Then
            assertThat(result.totalCount).isEqualTo(100L)
            assertThat(result.expiredCount).isEqualTo(10L)
            assertThat(result.imminentCount).isEqualTo(20L)
            assertThat(result.normalCount).isEqualTo(70L)
        }
    }
}
