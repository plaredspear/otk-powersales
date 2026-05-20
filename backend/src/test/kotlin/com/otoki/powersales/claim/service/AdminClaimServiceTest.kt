package com.otoki.powersales.claim.service

import com.otoki.powersales.claim.entity.*
import com.otoki.powersales.claim.exception.ClaimNotFoundException
import com.otoki.powersales.claim.repository.AdminClaimPhotoRepository
import com.otoki.powersales.claim.repository.AdminClaimRepository
import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.claim.enums.ClaimDateType
import com.otoki.powersales.claim.enums.ClaimPhotoType
import com.otoki.powersales.claim.enums.ClaimStatus
import com.otoki.powersales.claim.enums.ClaimType1
import com.otoki.powersales.claim.enums.ClaimType2
import com.otoki.powersales.employee.entity.Employee
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
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.math.BigDecimal

@ExtendWith(MockitoExtension::class)
@DisplayName("AdminClaimService 테스트")
class AdminClaimServiceTest {

    @Mock
    private lateinit var adminClaimRepository: AdminClaimRepository

    @Mock
    private lateinit var adminClaimPhotoRepository: AdminClaimPhotoRepository

    @InjectMocks
    private lateinit var adminClaimService: AdminClaimService

    @Nested
    @DisplayName("getClaims - 클레임 목록 조회")
    inner class GetClaimsTests {

        @Test
        @DisplayName("기본 조회 - 필터 없이 호출 -> 페이지네이션된 목록 반환")
        fun getClaims_defaultQuery() {
            // Given
            val claim = createClaim()
            val page = PageImpl(listOf(claim), PageRequest.of(0, 20), 1)
            whenever(adminClaimRepository.findClaims(any(), any(), anyOrNull(), anyOrNull(), anyOrNull(), any())).thenReturn(page)

            // When
            val result = adminClaimService.getClaims(null, null, null, null, null, 0, 20)

            // Then
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
            // Given
            val claim = createClaim(status = ClaimStatus.SENT)
            val page = PageImpl(listOf(claim), PageRequest.of(0, 20), 1)
            whenever(adminClaimRepository.findClaims(any(), any(), anyOrNull(), anyOrNull(), anyOrNull(), any())).thenReturn(page)

            // When
            val result = adminClaimService.getClaims(
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31),
                "SENT", null, null, 0, 20
            )

            // Then
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].status).isEqualTo("SENT")
        }

        @Test
        @DisplayName("잘못된 상태값 - 유효하지 않은 상태 -> 상태 필터 null로 처리")
        fun getClaims_invalidStatus() {
            // Given
            val page = PageImpl(emptyList<Claim>(), PageRequest.of(0, 20), 0)
            whenever(adminClaimRepository.findClaims(any(), any(), anyOrNull(), anyOrNull(), anyOrNull(), any())).thenReturn(page)

            // When
            val result = adminClaimService.getClaims(null, null, "INVALID", null, null, 0, 20)

            // Then
            assertThat(result.content).isEmpty()
        }
    }

    @Nested
    @DisplayName("getClaimDetail - 클레임 상세 조회")
    inner class GetClaimDetailTests {

        @Test
        @DisplayName("정상 조회 - 사진 포함 클레임 -> 상세 정보 + 사진 목록 반환")
        fun getClaimDetail_withPhotos() {
            // Given
            val claim = createClaim()
            val photos = listOf(
                createClaimPhoto(id = 1L, claim = claim, photoType = ClaimPhotoType.DEFECT),
                createClaimPhoto(id = 2L, claim = claim, photoType = ClaimPhotoType.RECEIPT)
            )
            whenever(adminClaimRepository.findById(1L)).thenReturn(Optional.of(claim))
            whenever(adminClaimPhotoRepository.findByClaimId(1L)).thenReturn(photos)

            // When
            val result = adminClaimService.getClaimDetail(1L)

            // Then
            assertThat(result.claimId).isEqualTo(1L)
            assertThat(result.employeeName).isEqualTo("김영업")
            assertThat(result.defectDescription).isEqualTo("제품 개봉 시 이물질 발견")
            assertThat(result.photos).hasSize(2)
            assertThat(result.photos[0].photoType).isEqualTo("DEFECT")
            assertThat(result.photos[1].photoType).isEqualTo("RECEIPT")
        }

        @Test
        @DisplayName("사진 없는 클레임 조회 - 사진 없음 -> 빈 photos 배열 반환")
        fun getClaimDetail_noPhotos() {
            // Given
            val claim = createClaim()
            whenever(adminClaimRepository.findById(1L)).thenReturn(Optional.of(claim))
            whenever(adminClaimPhotoRepository.findByClaimId(1L)).thenReturn(emptyList())

            // When
            val result = adminClaimService.getClaimDetail(1L)

            // Then
            assertThat(result.claimId).isEqualTo(1L)
            assertThat(result.photos).isEmpty()
        }

        @Test
        @DisplayName("미존재 클레임 - 존재하지 않는 ID -> ClaimNotFoundException")
        fun getClaimDetail_notFound() {
            // Given
            whenever(adminClaimRepository.findById(999L)).thenReturn(Optional.empty())

            // Then
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
        id: Int = 1
    ): Account = Account(
        id = id
    )

    private fun createClaim(
        id: Long = 1L,
        employee: Employee = createEmployee(),
        account: Account = createAccount(),
        status: ClaimStatus = ClaimStatus.DRAFT
    ): Claim {
        return Claim(
            id = id,
            employee = employee,
            account = account,
            accountName = "홍길동 슈퍼",
            productCode = "12345678",
            productName = "오뚜기 진라면 순한맛",
            dateType = ClaimDateType.EXPIRY_DATE,
            date = LocalDate.of(2026, 6, 30),
            claimType1 = ClaimType1.C,
            claimType2 = ClaimType2.CA,
            defectDescription = "제품 개봉 시 이물질 발견",
            defectQuantity = BigDecimal.valueOf(5L),
            purchaseAmount = BigDecimal.valueOf(4500L),
            purchaseMethodName = "매장구매",
            requestTypeName = "교환",
            status = status
        ).apply {
            createdAt = java.time.LocalDateTime.of(2026, 4, 1, 9, 30, 0)
        }
    }

    private fun createClaimPhoto(
        id: Long = 1L,
        claim: Claim = createClaim(),
        photoType: ClaimPhotoType = ClaimPhotoType.DEFECT
    ): ClaimPhoto = ClaimPhoto(
        id = id,
        claim = claim,
        photoType = photoType,
        url = "https://cdn.example.com/claims/1/${photoType.name.lowercase()}_001.jpg",
        originalFileName = "${photoType.name.lowercase()}_001.jpg",
        fileSize = 1024L,
        contentType = "image/jpeg"
    )
}
