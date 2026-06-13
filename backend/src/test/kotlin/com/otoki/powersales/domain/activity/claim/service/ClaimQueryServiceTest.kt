package com.otoki.powersales.domain.activity.claim.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.domain.activity.claim.entity.Claim
import com.otoki.powersales.domain.activity.claim.enums.ClaimDateType
import com.otoki.powersales.domain.activity.claim.enums.ClaimStatus
import com.otoki.powersales.domain.activity.claim.enums.ClaimType1
import com.otoki.powersales.domain.activity.claim.enums.ClaimType2
import com.otoki.powersales.domain.activity.claim.exception.ClaimInvalidParameterException
import com.otoki.powersales.domain.activity.claim.exception.ClaimNotFoundException
import com.otoki.powersales.domain.activity.claim.exception.InvalidDateFormatException
import com.otoki.powersales.domain.activity.claim.exception.InvalidDateRangeException
import com.otoki.powersales.domain.activity.claim.repository.ClaimRepository
import com.otoki.powersales.common.entity.UploadFile
import com.otoki.powersales.common.repository.UploadFileRepository
import com.otoki.powersales.common.storage.StorageConstants
import com.otoki.powersales.common.storage.StorageService
import com.otoki.powersales.common.storage.UploadFileParentTypes
import com.otoki.powersales.domain.activity.claim.entity.sfpicklist.PurchaseMethod
import com.otoki.powersales.domain.activity.claim.entity.sfpicklist.RequestType
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.foundation.product.entity.Product
import io.mockk.every
import io.mockk.mockk
import org.springframework.data.repository.findByIdOrNull
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*
import java.math.BigDecimal
import java.time.LocalDateTime

@DisplayName("ClaimQueryService 테스트")
class ClaimQueryServiceTest {

    private val claimRepository: ClaimRepository = mockk()
    private val employeeRepository: EmployeeRepository = mockk()
    private val uploadFileRepository: UploadFileRepository = mockk()
    private val storageService: StorageService = mockk()

    private val claimQueryService = ClaimQueryService(
        claimRepository,
        employeeRepository,
        uploadFileRepository,
        storageService,
    )

    @Nested
    @DisplayName("getClaims - 클레임 목록 조회")
    inner class GetClaimsTests {

        @Test
        @DisplayName("여사원 - 본인 등록분만 반환 (employee_id 분기)")
        fun getClaims_woman_ownOnly() {
            val claim = createClaim()
            every { employeeRepository.findByIdOrNull(1L) } returns createEmployee(role = AppAuthority.WOMAN, costCenterCode = "CC01")
            every { claimRepository.findOwnClaims(1L, any(), any(), any()) } returns listOf(claim)

            val result = claimQueryService.getClaims(1L, null, null, null)

            assertThat(result).hasSize(1)
            assertThat(result[0].claimId).isEqualTo(1L)
            assertThat(result[0].status).isEqualTo("DRAFT")
            assertThat(result[0].statusLabel).isEqualTo("임시저장")
        }

        @Test
        @DisplayName("조장 - 같은 원가센터 전체 반환 (cost_center_code 분기) — SF 동등")
        fun getClaims_leader_sameCostCenter() {
            val claim = createClaim()
            every { employeeRepository.findByIdOrNull(1L) } returns createEmployee(role = AppAuthority.LEADER, costCenterCode = "CC01")
            every { claimRepository.findCostCenterClaims("CC01", any(), any(), any()) } returns listOf(claim)

            val result = claimQueryService.getClaims(1L, null, null, null)

            assertThat(result).hasSize(1)
        }

        @Test
        @DisplayName("지점장 - 같은 원가센터 전체 반환 (조장과 동일 분기)")
        fun getClaims_branchManager_sameCostCenter() {
            every { employeeRepository.findByIdOrNull(1L) } returns createEmployee(role = AppAuthority.BRANCH_MANAGER, costCenterCode = "CC01")
            every { claimRepository.findCostCenterClaims("CC01", any(), any(), any()) } returns emptyList()

            val result = claimQueryService.getClaims(1L, null, null, null)

            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("조장이지만 원가센터 미설정 - 본인 등록분으로 fallback (전사 노출 방지)")
        fun getClaims_leader_noCostCenter_fallbackToOwn() {
            every { employeeRepository.findByIdOrNull(1L) } returns createEmployee(role = AppAuthority.LEADER, costCenterCode = null)
            every { claimRepository.findOwnClaims(1L, any(), any(), any()) } returns emptyList()

            val result = claimQueryService.getClaims(1L, null, null, null)

            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("정상 조회 - 기간 지정 → 목록 반환")
        fun getClaims_withDates_success() {
            every { employeeRepository.findByIdOrNull(1L) } returns createEmployee(role = AppAuthority.WOMAN, costCenterCode = "CC01")
            every { claimRepository.findOwnClaims(1L, any(), any(), any()) } returns emptyList()

            val result = claimQueryService.getClaims(1L, "2026-04-01", "2026-04-08", null)

            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("빈 결과 - 해당 기간에 클레임 없음 → 빈 리스트")
        fun getClaims_empty() {
            every { employeeRepository.findByIdOrNull(1L) } returns createEmployee(role = AppAuthority.WOMAN, costCenterCode = "CC01")
            every { claimRepository.findOwnClaims(1L, any(), any(), any()) } returns emptyList()

            val result = claimQueryService.getClaims(1L, null, null, null)

            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("잘못된 날짜 형식 - 20260401 → InvalidDateFormatException")
        fun getClaims_invalidDateFormat() {
            assertThatThrownBy { claimQueryService.getClaims(1L, "20260401", null, null) }
                .isInstanceOf(InvalidDateFormatException::class.java)
        }

        @Test
        @DisplayName("역전된 날짜 범위 - start > end → InvalidDateRangeException")
        fun getClaims_invalidDateRange() {
            assertThatThrownBy { claimQueryService.getClaims(1L, "2026-04-10", "2026-04-01", null) }
                .isInstanceOf(InvalidDateRangeException::class.java)
        }

        @Test
        @DisplayName("조회 기간 7일 초과 - 8일 범위 → ClaimInvalidParameterException")
        fun getClaims_dateRangeExceedsMax() {
            assertThatThrownBy { claimQueryService.getClaims(1L, "2026-04-01", "2026-04-09", null) }
                .isInstanceOf(ClaimInvalidParameterException::class.java)
        }
    }

    @Nested
    @DisplayName("getClaimDetail - 클레임 상세 조회")
    inner class GetClaimDetailTests {

        @Test
        @DisplayName("정상 조회 - 본인 클레임 → 상세 + 사진 반환")
        fun getClaimDetail_success() {
            val claim = createClaim()
            val photo = createUploadFile(claim.id)
            every { claimRepository.findById(1L) } returns Optional.of(claim)
            every { employeeRepository.findByIdOrNull(1L) } returns createEmployee(role = AppAuthority.WOMAN, costCenterCode = "CC01")
            every {
                uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse(
                    UploadFileParentTypes.CLAIM, 1L
                )
            } returns listOf(photo)
            every {
                storageService.getPresignedUrl(
                    "uploads/claim/2026/01/01/x.jpg", StorageConstants.CLAIM_PRESIGN_TTL_SECONDS
                )
            } returns "https://s3.example.com/private/uploads/claim/2026/01/01/x.jpg?X-Amz-Signature=abc"

            val result = claimQueryService.getClaimDetail(1L, 1L)

            assertThat(result.claimId).isEqualTo(1L)
            assertThat(result.status).isEqualTo("DRAFT")
            assertThat(result.statusLabel).isEqualTo("임시저장")
            assertThat(result.dateType).isEqualTo("EXPIRY_DATE")
            assertThat(result.dateTypeLabel).isEqualTo("유통기한")
            assertThat(result.photos).hasSize(1)
            assertThat(result.photos[0].url).isEqualTo("https://s3.example.com/private/uploads/claim/2026/01/01/x.jpg?X-Amz-Signature=abc")
            assertThat(result.photos[0].originalFileName).isEqualTo("IMG_001.jpg")
        }

        @Test
        @DisplayName("사진 없음 - 사진 없는 클레임 → photos 빈 리스트")
        fun getClaimDetail_noPhotos() {
            val claim = createClaim()
            every { claimRepository.findById(1L) } returns Optional.of(claim)
            every { employeeRepository.findByIdOrNull(1L) } returns createEmployee(role = AppAuthority.WOMAN, costCenterCode = "CC01")
            every {
                uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse(
                    UploadFileParentTypes.CLAIM, 1L
                )
            } returns emptyList()

            val result = claimQueryService.getClaimDetail(1L, 1L)

            assertThat(result.photos).isEmpty()
        }

        @Test
        @DisplayName("구매정보 없음 - purchaseAmount null → null 반환")
        fun getClaimDetail_noPurchaseInfo() {
            val claim = createClaim(purchaseAmount = null, purchaseMethod = null, requestTypes = emptySet())
            every { claimRepository.findById(1L) } returns Optional.of(claim)
            every { employeeRepository.findByIdOrNull(1L) } returns createEmployee(role = AppAuthority.WOMAN, costCenterCode = "CC01")
            every {
                uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse(
                    UploadFileParentTypes.CLAIM, 1L
                )
            } returns emptyList()

            val result = claimQueryService.getClaimDetail(1L, 1L)

            assertThat(result.purchaseAmount).isNull()
            assertThat(result.purchaseMethodName).isNull()
            assertThat(result.requestTypeName).isNull()
        }

        @Test
        @DisplayName("존재하지 않는 클레임 - claimId 없음 → ClaimNotFoundException")
        fun getClaimDetail_notFound() {
            every { claimRepository.findById(999L) } returns Optional.empty()

            assertThatThrownBy { claimQueryService.getClaimDetail(1L, 999L) }
                .isInstanceOf(ClaimNotFoundException::class.java)
        }

        @Test
        @DisplayName("타인 클레임 조회 - 여사원이 다른 사원의 클레임 → ClaimNotFoundException")
        fun getClaimDetail_woman_notOwner() {
            val claim = createClaim(employeeId = 2L, costCenterCode = "CC01")
            every { claimRepository.findById(1L) } returns Optional.of(claim)
            every { employeeRepository.findByIdOrNull(1L) } returns createEmployee(role = AppAuthority.WOMAN, costCenterCode = "CC01")

            assertThatThrownBy { claimQueryService.getClaimDetail(1L, 1L) }
                .isInstanceOf(ClaimNotFoundException::class.java)
        }

        @Test
        @DisplayName("조장이 같은 원가센터 여사원 클레임 조회 → 정상 반환 (SF 동등)")
        fun getClaimDetail_leader_sameCostCenter_success() {
            val claim = createClaim(employeeId = 2L, costCenterCode = "CC01")
            every { claimRepository.findById(1L) } returns Optional.of(claim)
            every { employeeRepository.findByIdOrNull(1L) } returns createEmployee(role = AppAuthority.LEADER, costCenterCode = "CC01")
            every {
                uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse(
                    UploadFileParentTypes.CLAIM, 1L
                )
            } returns emptyList()

            val result = claimQueryService.getClaimDetail(1L, 1L)

            assertThat(result.claimId).isEqualTo(1L)
        }

        @Test
        @DisplayName("조장이 다른 원가센터 클레임 조회 → ClaimNotFoundException")
        fun getClaimDetail_leader_differentCostCenter_denied() {
            val claim = createClaim(employeeId = 2L, costCenterCode = "CC99")
            every { claimRepository.findById(1L) } returns Optional.of(claim)
            every { employeeRepository.findByIdOrNull(1L) } returns createEmployee(role = AppAuthority.LEADER, costCenterCode = "CC01")

            assertThatThrownBy { claimQueryService.getClaimDetail(1L, 1L) }
                .isInstanceOf(ClaimNotFoundException::class.java)
        }
    }

    private fun createEmployee(
        id: Long = 1L,
        role: String? = AppAuthority.WOMAN,
        costCenterCode: String? = "CC01"
    ): Employee {
        return Employee(
            id = id,
            employeeCode = "20030117",
            name = "테스트사원",
            role = role,
            costCenterCode = costCenterCode
        )
    }

    private fun createAccount(): Account {
        return Account(
            id = 1,
            name = "미광종합물류"
        )
    }

    private fun createClaim(
        id: Long = 1L,
        employeeId: Long = 1L,
        purchaseAmount: BigDecimal? = BigDecimal.valueOf(3500L),
        purchaseMethod: PurchaseMethod? = PurchaseMethod.PERSONAL_CARD,
        requestTypes: Set<RequestType> = setOf(RequestType.OPINION_REPORT),
        costCenterCode: String? = "CC01"
    ): Claim {
        return Claim(
            id = id,
            employee = createEmployee(employeeId),
            account = createAccount(),
            product = createProduct(),
            dateType = ClaimDateType.EXPIRY_DATE,
            date = LocalDate.of(2026, 8, 19),
            claimType1 = ClaimType1.A,
            claimType2 = ClaimType2.AB,
            defectDescription = "포장 파손으로 내용물 누유 확인",
            defectQuantity = BigDecimal.valueOf(3L),
            purchaseAmount = purchaseAmount,
            purchaseMethodCode = purchaseMethod,
            requestTypeCode = requestTypes,
            status = ClaimStatus.DRAFT,
            costCenterCode = costCenterCode
        ).apply {
            createdAt = LocalDateTime.of(2026, 4, 8, 10, 30, 0)
        }
    }

    private fun createProduct(): Product = Product(
        id = 1L,
        productCode = "12345678",
        name = "진라면(매운맛)멀티"
    )

    private fun createUploadFile(claimId: Long): UploadFile {
        return UploadFile(
            id = 1L,
            name = "IMG_001.jpg",
            uniqueKey = "uploads/claim/2026/01/01/x.jpg",
            fileSize = "1024",
            parentType = UploadFileParentTypes.CLAIM,
            parentId = claimId,
            isDeleted = false
        )
    }
}
