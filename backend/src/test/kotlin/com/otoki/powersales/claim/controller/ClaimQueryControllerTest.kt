package com.otoki.powersales.claim.controller

import com.otoki.powersales.claim.dto.response.ClaimDetailResponse
import com.otoki.powersales.claim.dto.response.ClaimListItemResponse
import com.otoki.powersales.claim.dto.response.ClaimPhotoItem
import com.otoki.powersales.claim.exception.ClaimNotFoundException
import com.otoki.powersales.claim.exception.InvalidDateFormatException
import com.otoki.powersales.claim.exception.InvalidDateRangeException
import com.otoki.powersales.admin.security.AdminAuthorityFilter
import com.otoki.powersales.claim.service.ClaimQueryService
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.common.security.UserPrincipal
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.LocalDateTime

@WebMvcTest(ClaimQueryController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ClaimQueryController 테스트")
class ClaimQueryControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var claimQueryService: ClaimQueryService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var sapInboundAuditService: SapInboundAuditService

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockitoBean
    private lateinit var adminAuthorityFilter: AdminAuthorityFilter

    @BeforeEach
    fun setUp() {
        val principal = UserPrincipal(userId = 1L, role = UserRole.USER)
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
    }

    @Nested
    @DisplayName("GET /api/v1/mobile/claims - 클레임 목록 조회")
    inner class GetClaimsTests {

        @Test
        @DisplayName("성공 - 기본 파라미터 → 200 + 목록")
        fun getClaims_success() {
            val items = listOf(
                ClaimListItemResponse(
                    claimId = 1L,
                    accountName = "미광종합물류",
                    productName = "진라면(매운맛)멀티",
                    productCode = "12345678",
                    categoryName = "포장불량",
                    subcategoryName = "누수/누유",
                    defectQuantity = 3,
                    status = "SUBMITTED",
                    statusLabel = "접수",
                    createdAt = LocalDateTime.of(2026, 4, 8, 10, 30, 0)
                )
            )
            whenever(claimQueryService.getClaims(eq(1L), isNull(), isNull())).thenReturn(items)

            mockMvc.perform(get("/api/v1/mobile/claims"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data[0].claim_id").value(1))
                .andExpect(jsonPath("$.data[0].account_name").value("미광종합물류"))
                .andExpect(jsonPath("$.data[0].status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data[0].status_label").value("접수"))
        }

        @Test
        @DisplayName("성공 - 기간 지정 → 200")
        fun getClaims_withDates_success() {
            whenever(claimQueryService.getClaims(eq(1L), eq("2026-04-01"), eq("2026-04-08")))
                .thenReturn(emptyList())

            mockMvc.perform(
                get("/api/v1/mobile/claims")
                    .param("startDate", "2026-04-01")
                    .param("endDate", "2026-04-08")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data").isEmpty)
        }

        @Test
        @DisplayName("실패 - 잘못된 날짜 형식 → 400")
        fun getClaims_invalidDateFormat() {
            whenever(claimQueryService.getClaims(eq(1L), eq("20260401"), isNull()))
                .thenThrow(InvalidDateFormatException())

            mockMvc.perform(
                get("/api/v1/mobile/claims")
                    .param("startDate", "20260401")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("INVALID_DATE_FORMAT"))
        }

        @Test
        @DisplayName("실패 - 역전된 날짜 범위 → 400")
        fun getClaims_invalidDateRange() {
            whenever(claimQueryService.getClaims(eq(1L), eq("2026-04-10"), eq("2026-04-01")))
                .thenThrow(InvalidDateRangeException())

            mockMvc.perform(
                get("/api/v1/mobile/claims")
                    .param("startDate", "2026-04-10")
                    .param("endDate", "2026-04-01")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("INVALID_DATE_RANGE"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/mobile/claims/{claimId} - 클레임 상세 조회")
    inner class GetClaimDetailTests {

        @Test
        @DisplayName("성공 - 본인 클레임 상세 → 200")
        fun getClaimDetail_success() {
            val detail = ClaimDetailResponse(
                claimId = 1L,
                accountName = "미광종합물류",
                productName = "진라면(매운맛)멀티",
                productCode = "12345678",
                dateType = "EXPIRY_DATE",
                dateTypeLabel = "유통기한",
                date = LocalDate.of(2026, 8, 19),
                categoryName = "포장불량",
                subcategoryName = "누수/누유",
                defectDescription = "포장 파손으로 내용물 누유 확인",
                defectQuantity = 3,
                purchaseAmount = 3500,
                purchaseMethodName = "개인카드",
                requestTypeName = "교환",
                status = "SUBMITTED",
                statusLabel = "접수",
                createdAt = LocalDateTime.of(2026, 4, 8, 10, 30, 0),
                photos = listOf(
                    ClaimPhotoItem(
                        photoId = 1L,
                        photoType = "DEFECT",
                        photoTypeLabel = "불량 사진",
                        url = "https://cdn.example.com/claims/1/defect.jpg",
                        originalFileName = "IMG_001.jpg"
                    )
                )
            )
            whenever(claimQueryService.getClaimDetail(eq(1L), eq(1L))).thenReturn(detail)

            mockMvc.perform(get("/api/v1/mobile/claims/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.claim_id").value(1))
                .andExpect(jsonPath("$.data.date_type").value("EXPIRY_DATE"))
                .andExpect(jsonPath("$.data.date_type_label").value("유통기한"))
                .andExpect(jsonPath("$.data.photos[0].photo_type").value("DEFECT"))
                .andExpect(jsonPath("$.data.photos[0].photo_type_label").value("불량 사진"))
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 클레임 → 404")
        fun getClaimDetail_notFound() {
            whenever(claimQueryService.getClaimDetail(eq(1L), eq(999L)))
                .thenThrow(ClaimNotFoundException(999L))

            mockMvc.perform(get("/api/v1/mobile/claims/999"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("CLAIM_NOT_FOUND"))
        }

        @Test
        @DisplayName("실패 - 타인 클레임 → 404")
        fun getClaimDetail_notOwner() {
            whenever(claimQueryService.getClaimDetail(eq(1L), eq(2L)))
                .thenThrow(ClaimNotFoundException(2L))

            mockMvc.perform(get("/api/v1/mobile/claims/2"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("CLAIM_NOT_FOUND"))
        }
    }
}
