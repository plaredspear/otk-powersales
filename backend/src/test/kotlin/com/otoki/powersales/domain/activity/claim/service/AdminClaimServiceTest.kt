package com.otoki.powersales.domain.activity.claim.service

import com.otoki.powersales.domain.activity.claim.exception.ClaimNotFoundException
import com.otoki.powersales.domain.activity.claim.repository.AdminClaimRepository
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.activity.claim.enums.ClaimDateType
import com.otoki.powersales.domain.activity.claim.enums.ClaimStatus
import com.otoki.powersales.domain.activity.claim.enums.ClaimType1
import com.otoki.powersales.domain.activity.claim.enums.ClaimType2
import com.otoki.powersales.platform.common.entity.UploadFile
import com.otoki.powersales.platform.common.repository.UploadFileRepository
import com.otoki.powersales.platform.common.storage.StorageConstants
import com.otoki.powersales.platform.common.storage.StorageService
import com.otoki.powersales.domain.activity.claim.entity.Claim
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.foundation.product.entity.Product
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
import java.time.LocalDateTime

@DisplayName("AdminClaimService 테스트")
class AdminClaimServiceTest {

    private val adminClaimRepository: AdminClaimRepository = mockk()
    private val uploadFileRepository: UploadFileRepository = mockk()
    private val storageService: StorageService = mockk()

    private val adminClaimService = AdminClaimService(
        adminClaimRepository,
        uploadFileRepository,
        storageService,
    )

    init {
        // 클레임 이미지는 private/ presigned 조회. 테스트에선 key → 결정적 presigned URL 로 스텁
        // (실 presigned 는 서명 timestamp 가 비결정적이라 mock 으로 고정).
        every { storageService.getPresignedUrl(any(), StorageConstants.CLAIM_PRESIGN_TTL_SECONDS) } answers {
            "https://signed.example.com/private/${firstArg<String>()}?sig=test"
        }
    }

    @Nested
    @DisplayName("getClaims - 클레임 목록 조회")
    inner class GetClaimsTests {

        @Test
        @DisplayName("기본 조회 - 필터 없이 호출 -> 페이지네이션된 목록 반환")
        fun getClaims_defaultQuery() {
            val claim = createClaim()
            val page = PageImpl(listOf(claim), PageRequest.of(0, 20), 1)
            every { adminClaimRepository.findClaims(any(), any(), any(), any(), any(), any()) } returns page
            every {
                uploadFileRepository.findByParentTypeAndParentIdInAndIsDeletedFalse(any(), any())
            } returns emptyList()

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
            every {
                uploadFileRepository.findByParentTypeAndParentIdInAndIsDeletedFalse(any(), any())
            } returns emptyList()

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

        @Test
        @DisplayName("대표 이미지 - 불량(CLAIM_DEFECT) 사진 우선 (생성시각이 늦어도 우선)")
        fun getClaims_representativeImage_defectFirst() {
            val claim = createClaim(id = 1L)
            val page = PageImpl(listOf(claim), PageRequest.of(0, 20), 1)
            every { adminClaimRepository.findClaims(any(), any(), any(), any(), any(), any()) } returns page
            every {
                uploadFileRepository.findByParentTypeAndParentIdInAndIsDeletedFalse(any(), any())
            } returns listOf(
                // 영수증(먼저 생성) + 불량(나중 생성) -> 시각이 늦어도 불량이 대표
                createUploadFile(
                    id = 11L, uniqueKey = "receipt_001.jpg", uploadKbn = "receipt",
                    createdAt = LocalDateTime.of(2026, 4, 1, 8, 0, 0)
                ),
                createUploadFile(
                    id = 12L, uniqueKey = "claim_001.jpg", uploadKbn = "claim",
                    createdAt = LocalDateTime.of(2026, 4, 1, 9, 0, 0)
                )
            )

            val result = adminClaimService.getClaims(null, null, null, null, null, 0, 20)

            assertThat(result.content[0].representativeImageUrl)
                .isEqualTo("https://signed.example.com/private/claim_001.jpg?sig=test")
        }

        @Test
        @DisplayName("대표 이미지 - 불량 사진 없으면 첫 사진(createdAt 최소) fallback")
        fun getClaims_representativeImage_firstPhotoFallback() {
            val claim = createClaim(id = 1L)
            val page = PageImpl(listOf(claim), PageRequest.of(0, 20), 1)
            every { adminClaimRepository.findClaims(any(), any(), any(), any(), any(), any()) } returns page
            every {
                uploadFileRepository.findByParentTypeAndParentIdInAndIsDeletedFalse(any(), any())
            } returns listOf(
                createUploadFile(
                    id = 11L, uniqueKey = "receipt_001.jpg", uploadKbn = "receipt",
                    createdAt = LocalDateTime.of(2026, 4, 1, 9, 0, 0)
                ),
                createUploadFile(
                    id = 12L, uniqueKey = "part_001.jpg", uploadKbn = "part",
                    createdAt = LocalDateTime.of(2026, 4, 1, 8, 0, 0)
                )
            )

            val result = adminClaimService.getClaims(null, null, null, null, null, 0, 20)

            // 불량 없음 -> createdAt 최소인 part_001 선택
            assertThat(result.content[0].representativeImageUrl)
                .isEqualTo("https://signed.example.com/private/part_001.jpg?sig=test")
        }

        @Test
        @DisplayName("대표 이미지 - 첨부 사진 0건이면 null")
        fun getClaims_representativeImage_noPhotos() {
            val claim = createClaim(id = 1L)
            val page = PageImpl(listOf(claim), PageRequest.of(0, 20), 1)
            every { adminClaimRepository.findClaims(any(), any(), any(), any(), any(), any()) } returns page
            every {
                uploadFileRepository.findByParentTypeAndParentIdInAndIsDeletedFalse(any(), any())
            } returns emptyList()

            val result = adminClaimService.getClaims(null, null, null, null, null, 0, 20)

            assertThat(result.content[0].representativeImageUrl).isNull()
        }

        @Test
        @DisplayName("대표 이미지 - 불량이지만 uniqueKey 없으면 후보 제외, 유효한 다른 사진 선택")
        fun getClaims_representativeImage_defectWithoutUniqueKey() {
            val claim = createClaim(id = 1L)
            val page = PageImpl(listOf(claim), PageRequest.of(0, 20), 1)
            every { adminClaimRepository.findClaims(any(), any(), any(), any(), any(), any()) } returns page
            every {
                uploadFileRepository.findByParentTypeAndParentIdInAndIsDeletedFalse(any(), any())
            } returns listOf(
                createUploadFile(id = 11L, uniqueKey = null, uploadKbn = "claim"),
                createUploadFile(id = 12L, uniqueKey = "part_001.jpg", uploadKbn = "part")
            )

            val result = adminClaimService.getClaims(null, null, null, null, null, 0, 20)

            assertThat(result.content[0].representativeImageUrl)
                .isEqualTo("https://signed.example.com/private/part_001.jpg?sig=test")
        }

        @Test
        @DisplayName("대표 이미지 - 다중 claim 매핑 정확성 + 배치 조회 1회 호출")
        fun getClaims_representativeImage_multiClaimMapping() {
            val claim1 = createClaim(id = 1L)
            val claim2 = createClaim(id = 2L)
            val page = PageImpl(listOf(claim1, claim2), PageRequest.of(0, 20), 2)
            every { adminClaimRepository.findClaims(any(), any(), any(), any(), any(), any()) } returns page
            every {
                uploadFileRepository.findByParentTypeAndParentIdInAndIsDeletedFalse(any(), any())
            } returns listOf(
                // 두 claim 의 파일이 섞여 반환됨
                createUploadFile(id = 11L, uniqueKey = "c1.jpg", uploadKbn = "claim", parentId = 1L),
                createUploadFile(id = 21L, uniqueKey = "c2.jpg", uploadKbn = "claim", parentId = 2L)
            )

            val result = adminClaimService.getClaims(null, null, null, null, null, 0, 20)

            val byId = result.content.associateBy { it.claimId }
            assertThat(byId[1L]?.representativeImageUrl)
                .isEqualTo("https://signed.example.com/private/c1.jpg?sig=test")
            assertThat(byId[2L]?.representativeImageUrl)
                .isEqualTo("https://signed.example.com/private/c2.jpg?sig=test")
            verify(exactly = 1) {
                uploadFileRepository.findByParentTypeAndParentIdInAndIsDeletedFalse(any(), any())
            }
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
                uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse("Claim", 1L)
            } returns files

            val result = adminClaimService.getClaimDetail(1L)

            assertThat(result.claimId).isEqualTo(1L)
            assertThat(result.photos).hasSize(2)
            assertThat(result.photos[0].photoId).isEqualTo(11L)
            assertThat(result.photos[0].url).isEqualTo("https://signed.example.com/private/26may2026claim_001jpg?sig=test")
            assertThat(result.photos[0].originalFileName).isEqualTo("claim_001.jpg")
            assertThat(result.photos[0].photoType).isNull()
            assertThat(result.photos[1].url).isEqualTo("https://signed.example.com/private/26may2026part_001jpg?sig=test")
        }

        @Test
        @DisplayName("UploadFile 매칭 0건 - 빈 photos 반환")
        fun getClaimDetail_noMatchingUploadFiles() {
            val claim = createClaim(id = 1L)
            every { adminClaimRepository.findById(1L) } returns Optional.of(claim)
            every {
                uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse("Claim", 1L)
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
                uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse("Claim", 1L)
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
        id: Long = 1L,
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
            createdAt = LocalDateTime.of(2026, 4, 1, 9, 30, 0)
        }
    }

    private fun createUploadFile(
        id: Long,
        uniqueKey: String?,
        name: String? = "file.jpg",
        uploadKbn: String? = null,
        parentId: Long = 1L,
        createdAt: LocalDateTime = LocalDateTime.of(2026, 4, 1, 9, 30, 0)
    ): UploadFile = UploadFile(
        id = id,
        sfid = "a0O%012d".format(id),
        name = name,
        uniqueKey = uniqueKey,
        recordSfid = "a012x00000ABCDE",
        parentType = "Claim",
        parentId = parentId,
        uploadKbn = uploadKbn
    ).apply {
        this.createdAt = createdAt
    }
}
