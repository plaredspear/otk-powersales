package com.otoki.powersales.claim.service

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.account.repository.AccountRepository
import com.otoki.powersales.claim.dto.request.ClaimCreateRequest
import com.otoki.powersales.claim.entity.Claim
import com.otoki.powersales.claim.entity.sfpicklist.RequestType
import com.otoki.powersales.claim.enums.ClaimDateType
import com.otoki.powersales.claim.enums.ClaimStatus
import com.otoki.powersales.claim.enums.ClaimType1
import com.otoki.powersales.claim.enums.ClaimType2
import com.otoki.powersales.claim.exception.ClaimAccessDeniedException
import com.otoki.powersales.claim.exception.ClaimNotEditableException
import com.otoki.powersales.claim.exception.ClaimPhotoNotFoundException
import com.otoki.powersales.claim.exception.ClaimTypeHierarchyMismatchException
import com.otoki.powersales.claim.exception.InvalidClaimDateException
import com.otoki.powersales.claim.exception.InvalidClaimType1Exception
import com.otoki.powersales.claim.exception.RequestTypeMaxExceededException
import com.otoki.powersales.claim.repository.ClaimRepository
import com.otoki.powersales.common.entity.UploadFile
import com.otoki.powersales.common.repository.UploadFileRepository
import com.otoki.powersales.common.service.FileStorageService
import com.otoki.powersales.common.storage.StorageService
import com.otoki.powersales.common.storage.UploadFileParentTypes
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.product.entity.Product
import com.otoki.powersales.product.repository.ProductRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import java.time.LocalDate
import java.util.Optional
import java.math.BigDecimal

@DisplayName("ClaimService 테스트")
class ClaimServiceTest {

    private val claimRepository: ClaimRepository = mockk(relaxUnitFun = true)
    private val uploadFileRepository: UploadFileRepository = mockk(relaxUnitFun = true)
    private val employeeRepository: EmployeeRepository = mockk()
    private val accountRepository: AccountRepository = mockk()
    private val productRepository: ProductRepository = mockk()
    private val fileStorageService: FileStorageService = mockk(relaxUnitFun = true)
    private val storageService: StorageService = mockk(relaxUnitFun = true)

    private val claimService = ClaimService(
        claimRepository,
        uploadFileRepository,
        employeeRepository,
        accountRepository,
        productRepository,
        fileStorageService,
        storageService,
    )

    private val userId = 100L
    private val claimId = 999L

    private lateinit var employee: Employee
    private lateinit var account: Account
    private lateinit var product: Product

    @BeforeEach
    fun setup() {
        employee = Employee(
            id = userId,
            employeeCode = "EMP001",
            name = "홍길동",
            orgName = "FS사업부",
            costCenterCode = "CC100"
        )
        account = Account(id = 1, name = "테스트거래처", branchCode = "B001")
        product = Product(id = 1L, name = "테스트제품", productCode = "P0001")
    }

    private fun validRequest(
        claimType1: String = "A",
        claimType2: String = "AA",
        date: String = "2026-01-01",
        dateType: String = ClaimDateType.EXPIRY_DATE.name,
        purchaseAmount: BigDecimal? = null,
        purchaseMethodCode: String? = null,
        requestTypeCode: String? = null
    ) = ClaimCreateRequest(
        accountId = 1L,
        productCode = "P0001",
        dateType = dateType,
        date = date,
        claimType1 = claimType1,
        claimType2 = claimType2,
        defectDescription = "포장이 찢어져 있음",
        defectQuantity = BigDecimal.valueOf(2L),
        purchaseAmount = purchaseAmount,
        purchaseMethodCode = purchaseMethodCode,
        requestTypeCode = requestTypeCode
    )

    private fun mockPhoto(name: String) = MockMultipartFile(name, "$name.jpg", "image/jpeg", byteArrayOf(1, 2, 3))

    private fun stubCreateDeps() {
        every { employeeRepository.findById(userId) } returns Optional.of(employee)
        every { accountRepository.findById(1) } returns Optional.of(account)
        every { productRepository.findByProductCode("P0001") } returns product
        every { claimRepository.save(any<Claim>()) } answers { firstArg() }
        every { fileStorageService.uploadClaimPhoto(any(), any(), any(), any()) } returns
            "uploads/claim/2026/01/01/uuid.jpg"
        every { uploadFileRepository.save(any<UploadFile>()) } answers { firstArg() }
    }

    @Nested
    @DisplayName("createClaim - 등록 (UC-02/UC-10)")
    inner class CreateClaimTests {

        @Test
        @DisplayName("정상 요청 - 클레임 + 사진 2장 저장")
        fun createsClaimWithRequiredPhotos() {
            stubCreateDeps()

            val result = claimService.createClaim(
                userId, validRequest(),
                defectPhoto = mockPhoto("defectPhoto"),
                labelPhoto = mockPhoto("labelPhoto"),
                receiptPhoto = null
            )

            assertThat(result.accountName).isEqualTo("테스트거래처")
            assertThat(result.productCode).isEqualTo("P0001")
            verify(exactly = 2) { uploadFileRepository.save(any<UploadFile>()) }
        }

        @Test
        @DisplayName("ClaimType2.parent != ClaimType1 - hierarchy mismatch 예외")
        fun rejectsHierarchyMismatch() {
            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { accountRepository.findById(1) } returns Optional.of(account)
            every { productRepository.findByProductCode("P0001") } returns product

            assertThatThrownBy {
                claimService.createClaim(
                    userId, validRequest(claimType1 = "A", claimType2 = "BA"),
                    mockPhoto("d"), mockPhoto("l"), null
                )
            }.isInstanceOf(ClaimTypeHierarchyMismatchException::class.java)
        }

        @Test
        @DisplayName("ClaimType1 유효값 외 - InvalidClaimType1Exception")
        fun rejectsInvalidClaimType1() {
            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { accountRepository.findById(1) } returns Optional.of(account)
            every { productRepository.findByProductCode("P0001") } returns product

            assertThatThrownBy {
                claimService.createClaim(
                    userId, validRequest(claimType1 = "Z"), mockPhoto("d"), mockPhoto("l"), null
                )
            }.isInstanceOf(InvalidClaimType1Exception::class.java)
        }

        @Test
        @DisplayName("제조일자 미래 - InvalidClaimDateException")
        fun rejectsFutureManufactureDate() {
            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { accountRepository.findById(1) } returns Optional.of(account)
            every { productRepository.findByProductCode("P0001") } returns product
            val futureDate = LocalDate.now().plus(1, java.time.temporal.ChronoUnit.DAYS).toString()

            assertThatThrownBy {
                claimService.createClaim(
                    userId,
                    validRequest(dateType = ClaimDateType.MANUFACTURE_DATE.name, date = futureDate),
                    mockPhoto("d"), mockPhoto("l"), null
                )
            }.isInstanceOf(InvalidClaimDateException::class.java)
                .hasMessageContaining("제조일자")
        }

        @Test
        @DisplayName("유통기한 미래 - 허용")
        fun allowsFutureExpiryDate() {
            stubCreateDeps()
            val futureDate = LocalDate.now().plus(30, java.time.temporal.ChronoUnit.DAYS).toString()

            claimService.createClaim(
                userId,
                validRequest(dateType = ClaimDateType.EXPIRY_DATE.name, date = futureDate),
                mockPhoto("d"), mockPhoto("l"), null
            )

            verify { claimRepository.save(any<Claim>()) }
        }

        @Test
        @DisplayName("요청사항 4개 초과 - RequestTypeMaxExceededException")
        fun rejectsMoreThanFourRequestTypes() {
            every { employeeRepository.findById(userId) } returns Optional.of(employee)
            every { accountRepository.findById(1) } returns Optional.of(account)
            every { productRepository.findByProductCode("P0001") } returns product
            val joined = listOf(
                RequestType.OPINION_REPORT.displayName,
                RequestType.CONSULTATION.displayName,
                RequestType.URGENT_FS.displayName,
                RequestType.SALE_CANCEL_NEEDED.displayName,
                RequestType.PRODUCTION_SCHEDULE_ADJUSTMENT.displayName
            ).joinToString(";")

            assertThatThrownBy {
                claimService.createClaim(
                    userId, validRequest(requestTypeCode = joined),
                    mockPhoto("d"), mockPhoto("l"), null
                )
            }.isInstanceOf(RequestTypeMaxExceededException::class.java)
        }

        @Test
        @DisplayName("Employee.costCenterCode 우선 - claim.costCenterCode 자동 채움")
        fun fillsCostCenterCodeFromEmployee() {
            stubCreateDeps()
            val claimSlot = slot<Claim>()
            every { claimRepository.save(capture(claimSlot)) } answers { firstArg() }

            claimService.createClaim(userId, validRequest(), mockPhoto("d"), mockPhoto("l"), null)

            assertThat(claimSlot.captured.costCenterCode).isEqualTo("CC100")
            assertThat(claimSlot.captured.division).isEqualTo("FS사업부")
        }
    }

    @Nested
    @DisplayName("updateClaim - 수정 (UC-03)")
    inner class UpdateClaimTests {

        private fun existingClaim(status: ClaimStatus = ClaimStatus.DRAFT) = Claim(
            id = claimId,
            employee = employee,
            account = account,
            accountName = account.name,
            productCode = "P0001",
            productName = "테스트제품",
            dateType = ClaimDateType.EXPIRY_DATE,
            date = LocalDate.now(),
            claimType1 = ClaimType1.A,
            claimType2 = ClaimType2.AA,
            defectDescription = "원본 설명",
            defectQuantity = BigDecimal.valueOf(1L),
            status = status
        )

        @Test
        @DisplayName("DRAFT 상태 + 본인 작성 - 정상 수정")
        fun updatesDraftClaim() {
            val existing = existingClaim()
            every { claimRepository.findById(claimId) } returns Optional.of(existing)

            val result = claimService.updateClaim(
                userId, claimId,
                ClaimUpdateRequest(defectDescription = "수정된 설명", defectQuantity = BigDecimal.valueOf(5L))
            )

            assertThat(existing.defectDescription).isEqualTo("수정된 설명")
            assertThat(existing.defectQuantity).isEqualByComparingTo(BigDecimal.valueOf(5L))
            assertThat(result.id).isEqualTo(claimId)
        }

        @Test
        @DisplayName("SENT 상태 - ClaimNotEditableException")
        fun rejectsNonDraftStatus() {
            val existing = existingClaim(status = ClaimStatus.SENT)
            every { claimRepository.findById(claimId) } returns Optional.of(existing)

            assertThatThrownBy {
                claimService.updateClaim(userId, claimId, ClaimUpdateRequest(defectDescription = "x"))
            }.isInstanceOf(ClaimNotEditableException::class.java)
        }

        @Test
        @DisplayName("다른 사원의 클레임 - ClaimAccessDeniedException")
        fun rejectsOtherEmployeesClaim() {
            val other = Employee(id = userId + 1, employeeCode = "EMP999", name = "타인")
            val foreign = Claim(
                id = claimId,
                employee = other,
                account = account,
                date = LocalDate.now(),
                claimType1 = ClaimType1.A,
                claimType2 = ClaimType2.AA,
                defectDescription = "x",
                defectQuantity = BigDecimal.valueOf(1L),
                status = ClaimStatus.DRAFT
            )
            every { claimRepository.findById(claimId) } returns Optional.of(foreign)

            assertThatThrownBy {
                claimService.updateClaim(userId, claimId, ClaimUpdateRequest(defectDescription = "y"))
            }.isInstanceOf(ClaimAccessDeniedException::class.java)
        }
    }

    @Nested
    @DisplayName("deleteClaim - 삭제 (UC-11)")
    inner class DeleteClaimTests {

        @Test
        @DisplayName("DRAFT 상태 - 클레임 삭제 + 사진 S3 삭제 + soft-delete")
        fun deletesDraftClaimAndPhotos() {
            val claim = Claim(
                id = claimId, employee = employee, account = account,
                date = LocalDate.now(), claimType1 = ClaimType1.A, claimType2 = ClaimType2.AA,
                defectDescription = "x", defectQuantity = BigDecimal.valueOf(1L), status = ClaimStatus.DRAFT
            )
            val photo = UploadFile(
                id = 1L,
                name = "x.jpg",
                uniqueKey = "uploads/claim/2026/01/01/x.jpg",
                parentType = UploadFileParentTypes.CLAIM,
                parentId = claimId,
                isDeleted = false
            )
            every { claimRepository.findById(claimId) } returns Optional.of(claim)
            every {
                uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse(
                    UploadFileParentTypes.CLAIM, claimId
                )
            } returns listOf(photo)

            claimService.deleteClaim(userId, claimId)

            verify { storageService.delete("uploads/claim/2026/01/01/x.jpg") }
            verify { claimRepository.delete(claim) }
            assertThat(photo.isDeleted).isTrue
        }

        @Test
        @DisplayName("SENT 상태 - 삭제 차단")
        fun rejectsNonDraftDelete() {
            val claim = Claim(
                id = claimId, employee = employee, account = account,
                date = LocalDate.now(), claimType1 = ClaimType1.A, claimType2 = ClaimType2.AA,
                defectDescription = "x", defectQuantity = BigDecimal.valueOf(1L), status = ClaimStatus.SENT
            )
            every { claimRepository.findById(claimId) } returns Optional.of(claim)

            assertThatThrownBy { claimService.deleteClaim(userId, claimId) }
                .isInstanceOf(ClaimNotEditableException::class.java)
            verify(exactly = 0) { claimRepository.delete(any<Claim>()) }
        }
    }

    @Nested
    @DisplayName("deletePhoto - 사진 삭제 (UC-06)")
    inner class DeletePhotoTests {

        @Test
        @DisplayName("DRAFT 상태 - 사진 S3 삭제 + soft-delete")
        fun deletesPhotoOnDraft() {
            val claim = Claim(
                id = claimId, employee = employee, account = account,
                date = LocalDate.now(), claimType1 = ClaimType1.A, claimType2 = ClaimType2.AA,
                defectDescription = "x", defectQuantity = BigDecimal.valueOf(1L), status = ClaimStatus.DRAFT
            )
            val photo = UploadFile(
                id = 7L,
                name = "r.jpg",
                uniqueKey = "uploads/claim/2026/01/01/receipt.jpg",
                parentType = UploadFileParentTypes.CLAIM,
                parentId = claimId,
                isDeleted = false
            )
            every { claimRepository.findById(claimId) } returns Optional.of(claim)
            every {
                uploadFileRepository.findByIdAndParentTypeAndParentIdAndIsDeletedFalse(
                    7L, UploadFileParentTypes.CLAIM, claimId
                )
            } returns photo

            claimService.deletePhoto(userId, claimId, 7L)

            verify { storageService.delete("uploads/claim/2026/01/01/receipt.jpg") }
            assertThat(photo.isDeleted).isTrue
        }

        @Test
        @DisplayName("존재하지 않는 photoId - ClaimPhotoNotFoundException")
        fun rejectsMissingPhoto() {
            val claim = Claim(
                id = claimId, employee = employee, account = account,
                date = LocalDate.now(), claimType1 = ClaimType1.A, claimType2 = ClaimType2.AA,
                defectDescription = "x", defectQuantity = BigDecimal.valueOf(1L), status = ClaimStatus.DRAFT
            )
            every { claimRepository.findById(claimId) } returns Optional.of(claim)
            every {
                uploadFileRepository.findByIdAndParentTypeAndParentIdAndIsDeletedFalse(
                    99L, UploadFileParentTypes.CLAIM, claimId
                )
            } returns null

            assertThatThrownBy { claimService.deletePhoto(userId, claimId, 99L) }
                .isInstanceOf(ClaimPhotoNotFoundException::class.java)
        }
    }
}
