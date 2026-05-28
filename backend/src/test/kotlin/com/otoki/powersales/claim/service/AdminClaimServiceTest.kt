package com.otoki.powersales.claim.service

import com.otoki.powersales.claim.entity.*
import com.otoki.powersales.claim.exception.ClaimNotFoundException
import com.otoki.powersales.claim.repository.AdminClaimRepository
import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.claim.enums.ClaimDateType
import com.otoki.powersales.claim.enums.ClaimStatus
import com.otoki.powersales.claim.enums.ClaimType1
import com.otoki.powersales.claim.enums.ClaimType2
import com.otoki.powersales.common.entity.UploadFile
import com.otoki.powersales.common.repository.UploadFileRepository
import com.otoki.powersales.common.storage.PublicUrlResolver
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.product.entity.Product
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDate
import java.util.*
import java.math.BigDecimal

@DisplayName("AdminClaimService 테스트")
class AdminClaimServiceTest {

    private val adminClaimRepository: AdminClaimRepository = mockk()
    private val uploadFileRepository: UploadFileRepository = mockk()
    private val publicUrlResolver = PublicUrlResolver(prefix = "https://bucket.s3.ap-northeast-2.amazonaws.com/public")

    private val adminClaimService = AdminClaimService(
        adminClaimRepository,
        uploadFileRepository,
        publicUrlResolver,
    )

    @Nested
    @DisplayName("getClaims - 클레임 목록 조회")
    inner class GetClaimsTests {

        @Test
        @DisplayName("기본 조회 - 필터 없이 호출 -> 페이지네이션된 목록 반환")
        fun getClaims_defaultQuery() {
            val claim = createClaim()
            val page = PageImpl(listOf(claim), PageRequest.of(0, 20), 1)
            every { adminClaimRepository.findClaims(any(), any(), any(), any(), any(), any()) } returns page

            val result = adminClaimService.getClaims(null, null, null, null, null, 0, 20)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].claimId).isEqualTo(1L)
            assertThat(result.content[0].employeeName).isEqualTo("김영업")
            assertThat(result.content[0].employeeCode).isEqualTo("10023456")
            assertThat(result.content[0].storeName).isEqualTo("홍길동 슈퍼")
            assertThat(result.content[0].status).isEqualTo("DRAFT")
            assertThat(result.totalElements).isEqualTo(1L)
            assertThat(result.page).isEqualTo(0)
            assertThat(result.size).isEqualTo(20)
        }

        @Test
        @DisplayName("상태 필터 - 유효한 상태값으로 필터링 -> 필터 적용된 결과 반환")
        fun getClaims_withStatusFilter() {
            val claim = createClaim(status = ClaimStatus.SENT)
            val page = PageImpl(listOf(claim), PageRequest.of(0, 20), 1)
            every { adminClaimRepository.findClaims(any(), any(), any(), any(), any(), any()) } returns page

            val result = adminClaimService.getClaims(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31),
                "SENT", null, null, 0, 20
            )

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].status).isEqualTo("SENT")
        }

        @Test
        @DisplayName("잘못된 상태값 - 유효하지 않은 상태 -> 상태 필터 null로 처리")
        fun getClaims_invalidStatus() {
            val page = PageImpl(emptyList<Claim>(), PageRequest.of(0, 20), 0)
            every { adminClaimRepository.findClaims(any(), any(), any(), any(), any(), any()) } returns page

            val result = adminClaimService.getClaims(null, null, "INVALID", null, null, 0, 20)

            assertThat(result.content).isEmpty()
        }
    }

    @Nested
    @DisplayName("getClaimDetail - 클레임 상세 조회")
    inner class GetClaimDetailTests {

        @Test
        @DisplayName("UploadFile 매칭 - (parent_type, parent_id) FK 로 조회된 첨부파일들이 응답에 포함됨")
        fun getClaimDetail_withUploadFiles() {
            val claim = createClaim(id = 1L)
            val files = listOf(
                createUploadFile(id = 11L, uniqueKey = "26may2026claim_001jpg", name = "claim_001.jpg"),
                createUploadFile(id = 12L, uniqueKey = "26may2026part_001jpg", name = "part_001.jpg")
            )
            every { adminClaimRepository.findById(1L) } returns Optional.of(claim)
            every {
                uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse("DKRetail__Claim__c", 1L)
            } returns files

            val result = adminClaimService.getClaimDetail(1L)

            assertThat(result.claimId).isEqualTo(1L)
            assertThat(result.photos).hasSize(2)
            assertThat(result.photos[0].photoId).isEqualTo(11L)
            assertThat(result.photos[0].url).isEqualTo("https://bucket.s3.ap-northeast-2.amazonaws.com/public/26may2026claim_001jpg")
            assertThat(result.photos[0].originalFileName).isEqualTo("claim_001.jpg")
            assertThat(result.photos[0].photoType).isNull()
            assertThat(result.photos[1].url).isEqualTo("https://bucket.s3.ap-northeast-2.amazonaws.com/public/26may2026part_001jpg")
        }

        @Test
        @DisplayName("UploadFile 매칭 0건 - 빈 photos 반환")
        fun getClaimDetail_noMatchingUploadFiles() {
            val claim = createClaim(id = 1L)
            every { adminClaimRepository.findById(1L) } returns Optional.of(claim)
            every {
                uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse("DKRetail__Claim__c", 1L)
            } returns emptyList()

            val result = adminClaimService.getClaimDetail(1L)

            assertThat(result.photos).isEmpty()
        }

        @Test
        @DisplayName("uniqueKey 부재 UploadFile - 응답에서 제외")
        fun getClaimDetail_uploadFileWithoutUniqueKey() {
            val claim = createClaim(id = 1L)
            val files = listOf(
                createUploadFile(id = 11L, uniqueKey = "valid_key.jpg"),
                createUploadFile(id = 12L, uniqueKey = null),
                createUploadFile(id = 13L, uniqueKey = "")
            )
            every { adminClaimRepository.findById(1L) } returns Optional.of(claim)
            every {
                uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse("DKRetail__Claim__c", 1L)
            } returns files

            val result = adminClaimService.getClaimDetail(1L)

            assertThat(result.photos).hasSize(1)
            assertThat(result.photos[0].photoId).isEqualTo(11L)
        }

        @Test
        @DisplayName("미존재 클레임 - 존재하지 않는 ID -> ClaimNotFoundException")
        fun getClaimDetail_notFound() {
            every { adminClaimRepository.findById(999L) } returns Optional.empty()

            assertThatThrownBy { adminClaimService.getClaimDetail(999L) }
                .isInstanceOf(ClaimNotFoundException::class.java)
        }
    }

    // -- Helpers --

    private fun createEmployee(
        id: Long = 1L,
        employeeCode: String = "10023456",
        name: String = "김영업"
    ): Employee = Employee(
        id = id,
        employeeCode = employeeCode,
        name = name
    )

    private fun createAccount(
        id: Int = 1,
        name: String = "홍길동 슈퍼"
    ): Account = Account(
        id = id,
        name = name
    )

    private fun createProduct(
        id: Long = 1L,
        productCode: String = "12345678",
        name: String = "오뚜기 진라면 순한맛"
    ): Product = Product(
        id = id,
        productCode = productCode,
        name = name
    )

    private fun createClaim(
        id: Long = 1L,
        employee: Employee = createEmployee(),
        account: Account = createAccount(),
        product: Product = createProduct(),
        status: ClaimStatus = ClaimStatus.DRAFT,
        sfid: String? = null
    ): Claim {
        return Claim(
            id = id,
            sfid = sfid,
            employee = employee,
            account = account,
            product = product,
            dateType = ClaimDateType.EXPIRY_DATE,
            date = LocalDate.of(2026, 6, 30),
            claimType1 = ClaimType1.C,
            claimType2 = ClaimType2.CA,
            defectDescription = "제품 개봉 시 이물질 발견",
            defectQuantity = BigDecimal.valueOf(5L),
            purchaseAmount = BigDecimal.valueOf(4500L),
            status = status
        ).apply {
            createdAt = java.time.LocalDateTime.of(2026, 4, 1, 9, 30, 0)
        }
    }

    private fun createUploadFile(
        id: Long,
        uniqueKey: String?,
        name: String? = "file.jpg"
    ): UploadFile = UploadFile(
        id = id,
        sfid = "a0O%012d".format(id),
        name = name,
        uniqueKey = uniqueKey,
        recordId = "a012x00000ABCDE",
        parentType = "DKRetail__Claim__c",
        parentId = 1L
    )
}
