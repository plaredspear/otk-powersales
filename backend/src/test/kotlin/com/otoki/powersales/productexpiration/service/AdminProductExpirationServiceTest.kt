package com.otoki.powersales.productexpiration.service

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.auth.exception.EmployeeNotFoundException
import com.otoki.powersales.common.exception.ProductNotFoundException
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.product.entity.Product
import com.otoki.powersales.product.repository.ProductRepository
import com.otoki.powersales.productexpiration.dto.request.AdminProductExpirationBatchDeleteRequest
import com.otoki.powersales.productexpiration.dto.request.AdminProductExpirationCreateRequest
import com.otoki.powersales.productexpiration.dto.request.AdminProductExpirationUpdateRequest
import com.otoki.powersales.productexpiration.dto.response.AdminProductExpirationSummaryResponse
import com.otoki.powersales.productexpiration.entity.ProductExpiration
import com.otoki.powersales.productexpiration.exception.InvalidAlertDateException
import com.otoki.powersales.productexpiration.exception.ProductExpirationAccountNotFoundException
import com.otoki.powersales.productexpiration.exception.ProductExpirationForbiddenException
import com.otoki.powersales.productexpiration.exception.ProductExpirationNotFoundException
import com.otoki.powersales.productexpiration.repository.ProductExpirationRepository
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDate
import java.util.Optional

@DisplayName("AdminProductExpirationService 테스트")
class AdminProductExpirationServiceTest {

    private val productExpirationRepository: ProductExpirationRepository = mockk()
    private val employeeRepository: EmployeeRepository = mockk()
    private val accountRepository: AccountRepository = mockk()
    private val productRepository: ProductRepository = mockk()

    private val adminProductExpirationService = AdminProductExpirationService(
        productExpirationRepository,
        employeeRepository,
        accountRepository,
        productRepository,
    )

    // ── Helper factory methods ──────────────────────────────────────

    private fun createEmployee(
        id: Long = 1L,
        sfid: String? = "EMP_SFID_001",
        employeeCode: String = "E001",
        name: String = "홍길동",
        role: UserRole? = UserRole.BRANCH_MANAGER,
        orgName: String? = "서울1조"
    ) = Employee(id = id, sfid = sfid, employeeCode = employeeCode, name = name, role = role, orgName = orgName)

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

    private fun mockAdminEmployee(userId: Long = 1L) {
        val admin = createEmployee(id = userId, role = UserRole.BRANCH_MANAGER)
        every { employeeRepository.findById(userId) } returns Optional.of(admin)
    }

    private fun mockLeaderEmployee(userId: Long = 2L, orgName: String = "서울1조"): List<Employee> {
        val leader = createEmployee(id = userId, employeeCode = "E002", name = "김조장", role = UserRole.LEADER, orgName = orgName)
        every { employeeRepository.findById(userId) } returns Optional.of(leader)
        val teamMember = createEmployee(id = 3L, employeeCode = "E003", name = "박여사", role = UserRole.WOMAN, orgName = orgName)
        val teamMembers = listOf(leader, teamMember)
        every { employeeRepository.findByOrgName(orgName) } returns teamMembers
        return teamMembers
    }

    private fun mockUserEmployee(userId: Long = 4L) {
        val user = createEmployee(id = userId, employeeCode = "E004", name = "이사원", role = UserRole.WOMAN)
        every { employeeRepository.findById(userId) } returns Optional.of(user)
    }

    private fun stubFindForAdmin(
        page: Page<ProductExpiration>,
        employeeIds: List<Long>? = null
    ) {
        every {
            productExpirationRepository.findForAdmin(
                any(), any(), any(), any(), any(), any(), any(), employeeIds
            )
        } returns page
    }

    // ── getList ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("getList - 유통기한 목록 조회")
    inner class GetListTests {

        @Test
        @DisplayName("ADMIN - null 파라미터 → 전체 목록 반환")
        fun getList_admin_returnsAllData() {
            // Given
            val userId = 1L
            mockAdminEmployee(userId)
            val pageable = PageRequest.of(0, 20)
            val testEmployee = createEmployee()
            val entity = createProductExpiration(employee = testEmployee)
            val page = PageImpl(listOf(entity), pageable, 1L)

            stubFindForAdmin(page)

            // When
            val result = adminProductExpirationService.getList(
                userId = userId,
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

        @Test
        @DisplayName("LEADER - 팀원 범위 데이터만 반환")
        fun getList_leader_returnsTeamData() {
            // Given
            val userId = 2L
            mockLeaderEmployee(userId)
            val pageable = PageRequest.of(0, 20)
            val page = PageImpl(emptyList<ProductExpiration>(), pageable, 0L)

            stubFindForAdmin(page, employeeIds = listOf(2L, 3L))

            // When
            val result = adminProductExpirationService.getList(
                userId = userId, fromDate = null, toDate = null,
                employeeKeyword = null, accountKeyword = null, status = null, pageable = pageable
            )

            // Then
            assertThat(result.totalElements).isEqualTo(0L)
        }

        @Test
        @DisplayName("USER - 본인 데이터만 반환")
        fun getList_user_returnsSelfData() {
            // Given
            val userId = 4L
            mockUserEmployee(userId)
            val pageable = PageRequest.of(0, 20)
            val page = PageImpl(emptyList<ProductExpiration>(), pageable, 0L)

            stubFindForAdmin(page, employeeIds = listOf(4L))

            // When
            val result = adminProductExpirationService.getList(
                userId = userId, fromDate = null, toDate = null,
                employeeKeyword = null, accountKeyword = null, status = null, pageable = pageable
            )

            // Then
            assertThat(result.totalElements).isEqualTo(0L)
        }
    }

    // ── getDetail ───────────────────────────────────────────────────

    @Nested
    @DisplayName("getDetail - 유통기한 상세 조회")
    inner class GetDetailTests {

        @Test
        @DisplayName("ADMIN - 존재하는 ID -> 상세 정보 반환")
        fun getDetail_admin_returnsResponse() {
            // Given
            val userId = 1L
            mockAdminEmployee(userId)
            val testEmployee = createEmployee()
            val entity = createProductExpiration(employee = testEmployee)
            every { productExpirationRepository.findById(1) } returns Optional.of(entity)

            // When
            val result = adminProductExpirationService.getDetail(userId, 1)

            // Then
            assertThat(result.id).isEqualTo(1)
            assertThat(result.productName).isEqualTo("오뚜기카레")
            assertThat(result.accountName).isEqualTo("테스트거래처")
            assertThat(result.employeeName).isEqualTo("홍길동")
            assertThat(result.employeeCode).isEqualTo("E001")
        }

        @Test
        @DisplayName("LEADER - 팀원 데이터 조회 → 성공")
        fun getDetail_leader_teamMember_returnsResponse() {
            // Given
            val userId = 2L
            mockLeaderEmployee(userId) // teamMembers = [2L, 3L]
            val teamMemberEmployee = createEmployee(id = 3L, employeeCode = "E003", name = "박여사")
            val entity = createProductExpiration(employeeId = 3L, employee = teamMemberEmployee)
            every { productExpirationRepository.findById(1) } returns Optional.of(entity)

            // When
            val result = adminProductExpirationService.getDetail(userId, 1)

            // Then
            assertThat(result.employeeName).isEqualTo("박여사")
        }

        @Test
        @DisplayName("LEADER - 타팀 데이터 조회 → ProductExpirationNotFoundException")
        fun getDetail_leader_otherTeam_throwsNotFound() {
            // Given
            val userId = 2L
            mockLeaderEmployee(userId) // teamMembers = [2L, 3L]
            val entity = createProductExpiration(employeeId = 99L)
            every { productExpirationRepository.findById(1) } returns Optional.of(entity)

            // When & Then
            assertThatThrownBy { adminProductExpirationService.getDetail(userId, 1) }
                .isInstanceOf(ProductExpirationNotFoundException::class.java)
        }

        @Test
        @DisplayName("USER - 타인 데이터 조회 → ProductExpirationNotFoundException")
        fun getDetail_user_otherPerson_throwsNotFound() {
            // Given
            val userId = 4L
            mockUserEmployee(userId)
            val entity = createProductExpiration(employeeId = 1L)
            every { productExpirationRepository.findById(1) } returns Optional.of(entity)

            // When & Then
            assertThatThrownBy { adminProductExpirationService.getDetail(userId, 1) }
                .isInstanceOf(ProductExpirationNotFoundException::class.java)
        }

        @Test
        @DisplayName("미존재 - 없는 ID -> ProductExpirationNotFoundException")
        fun getDetail_nonExistingId_throwsException() {
            // Given (findById throws before resolveEmployeeScope is called)
            every { productExpirationRepository.findById(999) } returns Optional.empty()

            // When & Then
            assertThatThrownBy { adminProductExpirationService.getDetail(1L, 999) }
                .isInstanceOf(ProductExpirationNotFoundException::class.java)
        }
    }

    // ── create ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("create - 유통기한 등록")
    inner class CreateTests {

        @Test
        @DisplayName("ADMIN - 유효한 요청 -> 유통기한 생성 반환")
        fun create_admin_validRequest_returnsResponse() {
            // Given
            val userId = 1L
            mockAdminEmployee(userId)
            val testEmployee = createEmployee()
            val testAccount = createAccount()
            val testProduct = createProduct()
            val request = createCreateRequest()

            every { employeeRepository.findByEmployeeCode("E001") } returns Optional.of(testEmployee)
            every { accountRepository.findByExternalKey("ACC001") } returns testAccount
            every { productRepository.findByProductCode("P001") } returns testProduct
            every { productExpirationRepository.save(any<ProductExpiration>()) } answers {
                val arg = firstArg<ProductExpiration>()
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
            val entityWithRelation = createProductExpiration(productExpirationId = 10, employee = testEmployee)
            every { productExpirationRepository.findById(10) } returns Optional.of(entityWithRelation)

            // When
            val result = adminProductExpirationService.create(userId, request)

            // Then
            assertThat(result.id).isEqualTo(10)
            assertThat(result.employeeName).isEqualTo("홍길동")
            assertThat(result.accountName).isEqualTo("테스트거래처")
            assertThat(result.productName).isEqualTo("오뚜기카레")
            assertThat(result.expirationDate).isEqualTo("2026-06-30")
            assertThat(result.alarmDate).isEqualTo("2026-06-23")
        }

        @Test
        @DisplayName("LEADER - 팀원 대상 등록 → 성공")
        fun create_leader_teamMember_success() {
            // Given
            val userId = 2L
            mockLeaderEmployee(userId) // teamMembers = [2L, 3L]
            val targetEmployee = createEmployee(id = 3L, employeeCode = "E003", name = "박여사")
            val testAccount = createAccount()
            val testProduct = createProduct()
            val request = createCreateRequest(employeeCode = "E003")

            every { employeeRepository.findByEmployeeCode("E003") } returns Optional.of(targetEmployee)
            every { accountRepository.findByExternalKey("ACC001") } returns testAccount
            every { productRepository.findByProductCode("P001") } returns testProduct
            every { productExpirationRepository.save(any<ProductExpiration>()) } answers {
                createProductExpiration(productExpirationId = 20, employeeId = 3L)
            }
            val entityWithRelation = createProductExpiration(productExpirationId = 20, employeeId = 3L, employee = targetEmployee)
            every { productExpirationRepository.findById(20) } returns Optional.of(entityWithRelation)

            // When
            val result = adminProductExpirationService.create(userId, request)

            // Then
            assertThat(result.id).isEqualTo(20)
        }

        @Test
        @DisplayName("LEADER - 타팀 대상 등록 → ProductExpirationForbiddenException")
        fun create_leader_otherTeam_throwsForbidden() {
            // Given
            val userId = 2L
            mockLeaderEmployee(userId) // teamMembers = [2L, 3L]
            val otherEmployee = createEmployee(id = 99L, employeeCode = "E099", name = "다른팀")
            val request = createCreateRequest(employeeCode = "E099")

            every { employeeRepository.findByEmployeeCode("E099") } returns Optional.of(otherEmployee)

            // When & Then
            assertThatThrownBy { adminProductExpirationService.create(userId, request) }
                .isInstanceOf(ProductExpirationForbiddenException::class.java)
        }

        @Test
        @DisplayName("USER - 타인 대상 등록 → ProductExpirationForbiddenException")
        fun create_user_otherPerson_throwsForbidden() {
            // Given
            val userId = 4L
            mockUserEmployee(userId)
            val otherEmployee = createEmployee(id = 1L)
            val request = createCreateRequest()

            every { employeeRepository.findByEmployeeCode("E001") } returns Optional.of(otherEmployee)

            // When & Then
            assertThatThrownBy { adminProductExpirationService.create(userId, request) }
                .isInstanceOf(ProductExpirationForbiddenException::class.java)
        }

        @Test
        @DisplayName("사원 없음 - 존재하지 않는 사번 -> EmployeeNotFoundException")
        fun create_employeeNotFound_throwsException() {
            // Given (findByEmployeeCode fails before resolveEmployeeScope is called)
            val request = createCreateRequest(employeeCode = "INVALID")
            every { employeeRepository.findByEmployeeCode("INVALID") } returns Optional.empty()

            // When & Then
            assertThatThrownBy { adminProductExpirationService.create(1L, request) }
                .isInstanceOf(EmployeeNotFoundException::class.java)
        }

        @Test
        @DisplayName("거래처 없음 - 존재하지 않는 거래처 코드 -> ProductExpirationAccountNotFoundException")
        fun create_accountNotFound_throwsException() {
            // Given
            val userId = 1L
            mockAdminEmployee(userId)
            val request = createCreateRequest(accountCode = "INVALID")
            every { employeeRepository.findByEmployeeCode("E001") } returns Optional.of(createEmployee())
            every { accountRepository.findByExternalKey("INVALID") } returns null

            // When & Then
            assertThatThrownBy { adminProductExpirationService.create(userId, request) }
                .isInstanceOf(ProductExpirationAccountNotFoundException::class.java)
        }

        @Test
        @DisplayName("제품 없음 - 존재하지 않는 제품 코드 -> ProductNotFoundException")
        fun create_productNotFound_throwsException() {
            // Given
            val userId = 1L
            mockAdminEmployee(userId)
            val request = createCreateRequest(productCode = "INVALID")
            every { employeeRepository.findByEmployeeCode("E001") } returns Optional.of(createEmployee())
            every { accountRepository.findByExternalKey("ACC001") } returns createAccount()
            every { productRepository.findByProductCode("INVALID") } returns null

            // When & Then
            assertThatThrownBy { adminProductExpirationService.create(userId, request) }
                .isInstanceOf(ProductNotFoundException::class.java)
        }

        @Test
        @DisplayName("알림일 오류 - 알림일이 유통기한 이후 -> InvalidAlertDateException")
        fun create_alarmDateNotBeforeExpiration_throwsException() {
            // Given
            val userId = 1L
            mockAdminEmployee(userId)
            val request = createCreateRequest(
                expirationDate = "2026-06-30",
                alarmDate = "2026-06-30"
            )
            every { employeeRepository.findByEmployeeCode("E001") } returns Optional.of(createEmployee())
            every { accountRepository.findByExternalKey("ACC001") } returns createAccount()
            every { productRepository.findByProductCode("P001") } returns createProduct()

            // When & Then
            assertThatThrownBy { adminProductExpirationService.create(userId, request) }
                .isInstanceOf(InvalidAlertDateException::class.java)
        }
    }

    // ── update ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("update - 유통기한 수정")
    inner class UpdateTests {

        @Test
        @DisplayName("ADMIN - 유효한 요청 -> 수정된 정보 반환")
        fun update_admin_validRequest_returnsUpdatedResponse() {
            // Given
            val userId = 1L
            mockAdminEmployee(userId)
            val testEmployee = createEmployee()
            val entity = createProductExpiration(employee = testEmployee)
            val request = createUpdateRequest()
            every { productExpirationRepository.findById(1) } returns Optional.of(entity)

            // When
            val result = adminProductExpirationService.update(userId, 1, request)

            // Then
            assertThat(result.id).isEqualTo(1)
            assertThat(result.expirationDate).isEqualTo("2026-07-31")
            assertThat(result.alarmDate).isEqualTo("2026-07-24")
            assertThat(result.description).isEqualTo("수정된 설명")
        }

        @Test
        @DisplayName("LEADER - 타팀 데이터 수정 → ProductExpirationForbiddenException")
        fun update_leader_otherTeam_throwsForbidden() {
            // Given
            val userId = 2L
            mockLeaderEmployee(userId)
            val entity = createProductExpiration(employeeId = 99L)
            val request = createUpdateRequest()
            every { productExpirationRepository.findById(1) } returns Optional.of(entity)

            // When & Then
            assertThatThrownBy { adminProductExpirationService.update(userId, 1, request) }
                .isInstanceOf(ProductExpirationForbiddenException::class.java)
        }

        @Test
        @DisplayName("미존재 - 없는 ID -> ProductExpirationNotFoundException")
        fun update_nonExistingId_throwsException() {
            // Given (findById throws before resolveEmployeeScope)
            val request = createUpdateRequest()
            every { productExpirationRepository.findById(999) } returns Optional.empty()

            // When & Then
            assertThatThrownBy { adminProductExpirationService.update(1L, 999, request) }
                .isInstanceOf(ProductExpirationNotFoundException::class.java)
        }

        @Test
        @DisplayName("알림일 오류 - 알림일이 유통기한 이후 -> InvalidAlertDateException")
        fun update_alarmDateNotBeforeExpiration_throwsException() {
            // Given
            val userId = 1L
            mockAdminEmployee(userId)
            val entity = createProductExpiration(employeeId = userId)
            val request = createUpdateRequest(
                expirationDate = "2026-07-31",
                alarmDate = "2026-08-01"
            )
            every { productExpirationRepository.findById(1) } returns Optional.of(entity)

            // When & Then
            assertThatThrownBy { adminProductExpirationService.update(userId, 1, request) }
                .isInstanceOf(InvalidAlertDateException::class.java)
        }
    }

    // ── delete ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("delete - 유통기한 삭제")
    inner class DeleteTests {

        @Test
        @DisplayName("ADMIN - 존재하는 ID -> 삭제 수행")
        fun delete_admin_existingId_deletesEntity() {
            // Given
            val userId = 1L
            mockAdminEmployee(userId)
            val entity = createProductExpiration()
            every { productExpirationRepository.findById(1) } returns Optional.of(entity)
            every { productExpirationRepository.delete(entity) } just Runs

            // When
            adminProductExpirationService.delete(userId, 1)

            // Then
            verify { productExpirationRepository.delete(entity) }
        }

        @Test
        @DisplayName("LEADER - 타팀 데이터 삭제 → ProductExpirationForbiddenException")
        fun delete_leader_otherTeam_throwsForbidden() {
            // Given
            val userId = 2L
            mockLeaderEmployee(userId)
            val entity = createProductExpiration(employeeId = 99L)
            every { productExpirationRepository.findById(1) } returns Optional.of(entity)

            // When & Then
            assertThatThrownBy { adminProductExpirationService.delete(userId, 1) }
                .isInstanceOf(ProductExpirationForbiddenException::class.java)
        }

        @Test
        @DisplayName("미존재 - 없는 ID -> ProductExpirationNotFoundException")
        fun delete_nonExistingId_throwsException() {
            // Given (findById throws before resolveEmployeeScope)
            every { productExpirationRepository.findById(999) } returns Optional.empty()

            // When & Then
            assertThatThrownBy { adminProductExpirationService.delete(1L, 999) }
                .isInstanceOf(ProductExpirationNotFoundException::class.java)
        }
    }

    // ── batchDelete ─────────────────────────────────────────────────

    @Nested
    @DisplayName("batchDelete - 유통기한 일괄 삭제")
    inner class BatchDeleteTests {

        @Test
        @DisplayName("ADMIN - 모든 ID 존재 -> 삭제 건수 반환")
        fun batchDelete_admin_allExist_returnsDeletedCount() {
            // Given
            val userId = 1L
            mockAdminEmployee(userId)
            val ids = listOf(1, 2, 3)
            val request = AdminProductExpirationBatchDeleteRequest(ids = ids)
            val entities = ids.map { createProductExpiration(productExpirationId = it) }
            every { productExpirationRepository.findAllById(ids) } returns entities
            every { productExpirationRepository.deleteAll(entities) } just Runs

            // When
            val result = adminProductExpirationService.batchDelete(userId, request)

            // Then
            assertThat(result.deletedCount).isEqualTo(3)
            verify { productExpirationRepository.deleteAll(entities) }
        }

        @Test
        @DisplayName("LEADER - 타팀 데이터 포함 일괄 삭제 → ProductExpirationForbiddenException")
        fun batchDelete_leader_otherTeam_throwsForbidden() {
            // Given
            val userId = 2L
            mockLeaderEmployee(userId) // teamMembers = [2L, 3L]
            val ids = listOf(1, 2)
            val request = AdminProductExpirationBatchDeleteRequest(ids = ids)
            val entities = listOf(
                createProductExpiration(productExpirationId = 1, employeeId = 3L),
                createProductExpiration(productExpirationId = 2, employeeId = 99L) // 타팀
            )
            every { productExpirationRepository.findAllById(ids) } returns entities

            // When & Then
            assertThatThrownBy { adminProductExpirationService.batchDelete(userId, request) }
                .isInstanceOf(ProductExpirationForbiddenException::class.java)
        }

        @Test
        @DisplayName("일부 미존재 - 요청 ID 중 일부 미존재 -> ProductExpirationNotFoundException")
        fun batchDelete_someNotFound_throwsException() {
            // Given
            val userId = 1L
            mockAdminEmployee(userId)
            val ids = listOf(1, 2, 999)
            val request = AdminProductExpirationBatchDeleteRequest(ids = ids)
            val entities = listOf(
                createProductExpiration(productExpirationId = 1),
                createProductExpiration(productExpirationId = 2)
            )
            every { productExpirationRepository.findAllById(ids) } returns entities

            // When & Then
            assertThatThrownBy { adminProductExpirationService.batchDelete(userId, request) }
                .isInstanceOf(ProductExpirationNotFoundException::class.java)
        }
    }

    // ── getSummary ──────────────────────────────────────────────────

    @Nested
    @DisplayName("getSummary - 유통기한 요약 조회")
    inner class GetSummaryTests {

        @Test
        @DisplayName("ADMIN - 전체 데이터 요약 반환")
        fun getSummary_admin_returnsSummaryResponse() {
            // Given
            val userId = 1L
            mockAdminEmployee(userId)
            val summary = AdminProductExpirationSummaryResponse(
                totalCount = 100L,
                expiredCount = 10L,
                imminentCount = 20L,
                normalCount = 70L
            )
            every { productExpirationRepository.getSummary(any(), null) } returns summary

            // When
            val result = adminProductExpirationService.getSummary(userId)

            // Then
            assertThat(result.totalCount).isEqualTo(100L)
            assertThat(result.expiredCount).isEqualTo(10L)
            assertThat(result.imminentCount).isEqualTo(20L)
            assertThat(result.normalCount).isEqualTo(70L)
        }

        @Test
        @DisplayName("LEADER - 팀원 범위 요약 반환")
        fun getSummary_leader_returnsTeamSummary() {
            // Given
            val userId = 2L
            mockLeaderEmployee(userId)
            val summary = AdminProductExpirationSummaryResponse(
                totalCount = 10L, expiredCount = 2L, imminentCount = 3L, normalCount = 5L
            )
            every { productExpirationRepository.getSummary(any(), listOf(2L, 3L)) } returns summary

            // When
            val result = adminProductExpirationService.getSummary(userId)

            // Then
            assertThat(result.totalCount).isEqualTo(10L)
        }
    }

    // ── resolveEmployeeScope edge cases ─────────────────────────────

    @Nested
    @DisplayName("resolveEmployeeScope - 엣지 케이스")
    inner class ResolveEmployeeScopeTests {

        @Test
        @DisplayName("LEADER orgName null - 본인 데이터만 반환")
        fun leader_nullOrgName_returnsSelfOnly() {
            // Given
            val userId = 5L
            val leader = createEmployee(id = userId, employeeCode = "E005", name = "NULL조장", role = UserRole.LEADER, orgName = null)
            every { employeeRepository.findById(userId) } returns Optional.of(leader)
            val pageable = PageRequest.of(0, 20)
            val page = PageImpl(emptyList<ProductExpiration>(), pageable, 0L)

            stubFindForAdmin(page, employeeIds = listOf(5L))

            // When
            val result = adminProductExpirationService.getList(
                userId = userId, fromDate = null, toDate = null,
                employeeKeyword = null, accountKeyword = null, status = null, pageable = pageable
            )

            // Then
            assertThat(result.totalElements).isEqualTo(0L)
        }
    }
}
