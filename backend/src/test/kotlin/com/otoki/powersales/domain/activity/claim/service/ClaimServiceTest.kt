package com.otoki.powersales.domain.activity.claim.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.domain.activity.claim.entity.Claim
import com.otoki.powersales.domain.activity.claim.enums.ClaimDateType
import com.otoki.powersales.domain.activity.claim.enums.ClaimStatus
import com.otoki.powersales.domain.activity.claim.enums.ClaimType1
import com.otoki.powersales.domain.activity.claim.enums.ClaimType2
import com.otoki.powersales.domain.activity.claim.exception.ClaimAccessDeniedException
import com.otoki.powersales.domain.activity.claim.exception.ClaimNotEditableException
import com.otoki.powersales.domain.activity.claim.exception.ClaimPhotoNotFoundException
import com.otoki.powersales.domain.activity.claim.repository.ClaimRepository
import com.otoki.powersales.platform.common.entity.UploadFile
import com.otoki.powersales.platform.common.repository.UploadFileRepository
import com.otoki.powersales.platform.common.storage.StorageService
import com.otoki.powersales.platform.common.storage.UploadFileParentTypes
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.foundation.product.entity.Product
import com.otoki.powersales.domain.foundation.product.repository.ProductRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.Optional
import java.math.BigDecimal

@DisplayName("ClaimService 테스트 — 수정/삭제 (UC-03/06/11)")
class ClaimServiceTest {

    private val claimRepository: ClaimRepository = mockk(relaxUnitFun = true)
    private val uploadFileRepository: UploadFileRepository = mockk(relaxUnitFun = true)
    private val accountRepository: AccountRepository = mockk()
    private val productRepository: ProductRepository = mockk()
    private val storageService: StorageService = mockk(relaxUnitFun = true)

    private val claimService = ClaimService(
        claimRepository,
        uploadFileRepository,
        accountRepository,
        productRepository,
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
        account = Account(id = 1, name = "테스트거래처", branchCode = "B001", externalKey = "SAP001")
        product = Product(id = 1L, name = "테스트제품", productCode = "P0001")
    }

    @Nested
    @DisplayName("updateClaim - 수정 (UC-03)")
    inner class UpdateClaimTests {

        private fun existingClaim(status: ClaimStatus = ClaimStatus.DRAFT) = Claim(
            id = claimId,
            employee = employee,
            account = account,
            product = product,
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

            verify { storageService.deletePrivate("uploads/claim/2026/01/01/x.jpg") }
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

            verify { storageService.deletePrivate("uploads/claim/2026/01/01/receipt.jpg") }
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
