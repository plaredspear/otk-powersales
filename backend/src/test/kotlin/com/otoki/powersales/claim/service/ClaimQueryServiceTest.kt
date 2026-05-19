package com.otoki.powersales.claim.service

import com.otoki.powersales.claim.entity.*
import com.otoki.powersales.claim.exception.ClaimNotFoundException
import com.otoki.powersales.claim.exception.InvalidDateFormatException
import com.otoki.powersales.claim.exception.InvalidDateRangeException
import com.otoki.powersales.claim.repository.ClaimPhotoRepository
import com.otoki.powersales.claim.repository.ClaimRepository
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
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("ClaimQueryService 테스트")
class ClaimQueryServiceTest {

    @Mock
    private lateinit var claimRepository: ClaimRepository

    @Mock
    private lateinit var claimPhotoRepository: ClaimPhotoRepository

    @InjectMocks
    private lateinit var claimQueryService: ClaimQueryService

    @Nested
    @DisplayName("getClaims - 클레임 목록 조회")
    inner class GetClaimsTests {

        @Test
        @DisplayName("정상 조회 - 기본 파라미터(최근 7일) → 목록 반환")
        fun getClaims_defaultDates_success() {
            val claim = createClaim()
            whenever(claimRepository.findByEmployeeIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                eq(1L), any(), any()
            )).thenReturn(listOf(claim))

            val result = claimQueryService.getClaims(1L, null, null)

            assertThat(result).hasSize(1)
            assertThat(result[0].claimId).isEqualTo(1L)
            assertThat(result[0].status).isEqualTo("DRAFT")
            assertThat(result[0].statusLabel).isEqualTo("임시저장")
        }

        @Test
        @DisplayName("정상 조회 - 기간 지정 → 목록 반환")
        fun getClaims_withDates_success() {
            whenever(claimRepository.findByEmployeeIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                eq(1L), any(), any()
            )).thenReturn(emptyList())

            val result = claimQueryService.getClaims(1L, "2026-04-01", "2026-04-08")

            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("빈 결과 - 해당 기간에 클레임 없음 → 빈 리스트")
        fun getClaims_empty() {
            whenever(claimRepository.findByEmployeeIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                eq(1L), any(), any()
            )).thenReturn(emptyList())

            val result = claimQueryService.getClaims(1L, null, null)

            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("잘못된 날짜 형식 - 20260401 → InvalidDateFormatException")
        fun getClaims_invalidDateFormat() {
            assertThatThrownBy { claimQueryService.getClaims(1L, "20260401", null) }
                .isInstanceOf(InvalidDateFormatException::class.java)
        }

        @Test
        @DisplayName("역전된 날짜 범위 - start > end → InvalidDateRangeException")
        fun getClaims_invalidDateRange() {
            assertThatThrownBy { claimQueryService.getClaims(1L, "2026-04-10", "2026-04-01") }
                .isInstanceOf(InvalidDateRangeException::class.java)
        }
    }

    @Nested
    @DisplayName("getClaimDetail - 클레임 상세 조회")
    inner class GetClaimDetailTests {

        @Test
        @DisplayName("정상 조회 - 본인 클레임 → 상세 + 사진 반환")
        fun getClaimDetail_success() {
            val claim = createClaim()
            val photo = createClaimPhoto(claim)
            whenever(claimRepository.findById(1L)).thenReturn(Optional.of(claim))
            whenever(claimPhotoRepository.findByClaimId(1L)).thenReturn(listOf(photo))

            val result = claimQueryService.getClaimDetail(1L, 1L)

            assertThat(result.claimId).isEqualTo(1L)
            assertThat(result.status).isEqualTo("DRAFT")
            assertThat(result.statusLabel).isEqualTo("임시저장")
            assertThat(result.dateType).isEqualTo("EXPIRY_DATE")
            assertThat(result.dateTypeLabel).isEqualTo("유통기한")
            assertThat(result.photos).hasSize(1)
            assertThat(result.photos[0].photoType).isEqualTo("DEFECT")
            assertThat(result.photos[0].photoTypeLabel).isEqualTo("불량 사진")
        }

        @Test
        @DisplayName("사진 없음 - 사진 없는 클레임 → photos 빈 리스트")
        fun getClaimDetail_noPhotos() {
            val claim = createClaim()
            whenever(claimRepository.findById(1L)).thenReturn(Optional.of(claim))
            whenever(claimPhotoRepository.findByClaimId(1L)).thenReturn(emptyList())

            val result = claimQueryService.getClaimDetail(1L, 1L)

            assertThat(result.photos).isEmpty()
        }

        @Test
        @DisplayName("구매정보 없음 - purchaseAmount null → null 반환")
        fun getClaimDetail_noPurchaseInfo() {
            val claim = createClaim(purchaseAmount = null, purchaseMethodName = null, requestTypeName = null)
            whenever(claimRepository.findById(1L)).thenReturn(Optional.of(claim))
            whenever(claimPhotoRepository.findByClaimId(1L)).thenReturn(emptyList())

            val result = claimQueryService.getClaimDetail(1L, 1L)

            assertThat(result.purchaseAmount).isNull()
            assertThat(result.purchaseMethodName).isNull()
            assertThat(result.requestTypeName).isNull()
        }

        @Test
        @DisplayName("존재하지 않는 클레임 - claimId 없음 → ClaimNotFoundException")
        fun getClaimDetail_notFound() {
            whenever(claimRepository.findById(999L)).thenReturn(Optional.empty())

            assertThatThrownBy { claimQueryService.getClaimDetail(1L, 999L) }
                .isInstanceOf(ClaimNotFoundException::class.java)
        }

        @Test
        @DisplayName("타인 클레임 조회 - 다른 사원의 클레임 → ClaimNotFoundException")
        fun getClaimDetail_notOwner() {
            val claim = createClaim(employeeId = 2L)
            whenever(claimRepository.findById(1L)).thenReturn(Optional.of(claim))

            assertThatThrownBy { claimQueryService.getClaimDetail(1L, 1L) }
                .isInstanceOf(ClaimNotFoundException::class.java)
        }
    }

    private fun createEmployee(id: Long = 1L): Employee {
        return Employee(
            id = id,
            employeeCode = "20030117",
            name = "테스트사원"
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
        purchaseAmount: Long? = 3500L,
        purchaseMethodName: String? = "개인카드",
        requestTypeName: String? = "교환"
    ): Claim {
        return Claim(
            id = id,
            employee = createEmployee(employeeId),
            account = createAccount(),
            accountName = "미광종합물류",
            productCode = "12345678",
            productName = "진라면(매운맛)멀티",
            dateType = ClaimDateType.EXPIRY_DATE,
            date = LocalDate.of(2026, 8, 19),
            claimType1 = ClaimType1.A,
            claimType2 = ClaimType2.AB,
            defectDescription = "포장 파손으로 내용물 누유 확인",
            defectQuantity = 3L,
            purchaseAmount = purchaseAmount,
            purchaseMethodName = purchaseMethodName,
            requestTypeName = requestTypeName,
            status = ClaimStatus.DRAFT
        ).apply {
            createdAt = java.time.LocalDateTime.of(2026, 4, 8, 10, 30, 0)
        }
    }

    private fun createClaimPhoto(claim: Claim): ClaimPhoto {
        return ClaimPhoto(
            id = 1L,
            claim = claim,
            photoType = ClaimPhotoType.DEFECT,
            url = "https://cdn.example.com/claims/1/defect.jpg",
            originalFileName = "IMG_001.jpg",
            fileSize = 1024L,
            contentType = "image/jpeg"
        )
    }
}
