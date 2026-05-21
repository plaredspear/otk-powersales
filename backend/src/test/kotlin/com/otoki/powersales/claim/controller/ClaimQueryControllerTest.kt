package com.otoki.powersales.claim.controller

import com.otoki.powersales.claim.dto.response.ClaimDetailResponse
import com.otoki.powersales.claim.dto.response.ClaimListItemResponse
import com.otoki.powersales.claim.dto.response.ClaimPhotoItem
import com.otoki.powersales.claim.exception.ClaimNotFoundException
import com.otoki.powersales.claim.exception.InvalidDateFormatException
import com.otoki.powersales.claim.exception.InvalidDateRangeException
import com.otoki.powersales.claim.service.ClaimQueryService
import com.otoki.powersales.auth.entity.UserRoleEnum
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.common.security.UserPrincipal
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.math.BigDecimal

@WebMvcTest(ClaimQueryController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ClaimQueryController 테스트")
class ClaimQueryControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var claimQueryService: ClaimQueryService

    @MockkBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockkBean
    private lateinit var sapInboundAuditService: SapInboundAuditService

    @MockkBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter


    @BeforeEach
    fun setUp() {
        val principal = UserPrincipal(userId = 1L, role = UserRoleEnum.WOMAN)
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
                    categoryValue = "A",
                    categoryLabel = "포장불량",
                    subcategoryValue = "AB",
                    subcategoryLabel = "누수/누유",
                    defectQuantity = BigDecimal.valueOf(3L),
                    status = "DRAFT",
                    statusLabel = "임시저장",
                    createdAt = java.time.LocalDateTime.of(2026, 4, 8, 10, 30, 0)
                )
            )
            every { claimQueryService.getClaims(1L, null, null) } returns items

            mockMvc.perform(get("/api/v1/mobile/claims"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data[0].claimId").value(1))
                .andExpect(jsonPath("$.data[0].accountName").value("미광종합물류"))
                .andExpect(jsonPath("$.data[0].status").value("DRAFT"))
                .andExpect(jsonPath("$.data[0].statusLabel").value("임시저장"))
        }

        @Test
        @DisplayName("성공 - 기간 지정 → 200")
        fun getClaims_withDates_success() {
            every { claimQueryService.getClaims(1L, "2026-04-01", "2026-04-08") } returns emptyList()

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

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.otoki.powersales.claim.controller.ClaimQueryControllerTest#getClaimsExceptions")
        @DisplayName("실패 - 날짜 검증 예외 → 400 ErrorCode 매핑")
        fun getClaims_exceptions(
            @Suppress("UNUSED_PARAMETER") name: String,
            startDate: String,
            endDate: String?,
            exception: Throwable,
            expectedCode: String,
        ) {
            every { claimQueryService.getClaims(1L, startDate, endDate) } throws exception

            val builder = get("/api/v1/mobile/claims").param("startDate", startDate)
            if (endDate != null) builder.param("endDate", endDate)

            mockMvc.perform(builder)
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value(expectedCode))
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
                categoryValue = "A",
                categoryLabel = "포장불량",
                subcategoryValue = "AB",
                subcategoryLabel = "누수/누유",
                defectDescription = "포장 파손으로 내용물 누유 확인",
                defectQuantity = BigDecimal.valueOf(3L),
                purchaseAmount = BigDecimal.valueOf(3500L),
                purchaseMethodName = "개인카드",
                requestTypeName = "교환",
                status = "DRAFT",
                statusLabel = "임시저장",
                createdAt = java.time.LocalDateTime.of(2026, 4, 8, 10, 30, 0),
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
            every { claimQueryService.getClaimDetail(1L, 1L) } returns detail

            mockMvc.perform(get("/api/v1/mobile/claims/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.claimId").value(1))
                .andExpect(jsonPath("$.data.dateType").value("EXPIRY_DATE"))
                .andExpect(jsonPath("$.data.dateTypeLabel").value("유통기한"))
                .andExpect(jsonPath("$.data.photos[0].photoType").value("DEFECT"))
                .andExpect(jsonPath("$.data.photos[0].photoTypeLabel").value("불량 사진"))
        }

        // notFound (999L) / notOwner (2L) 둘 다 동일 ClaimNotFoundException → CLAIM_NOT_FOUND 매핑.
        // 비즈니스 의미: service 의 "본인 클레임 확인" 정책으로 타인 클레임 접근도 NotFound 로 통합 응답 (정보 노출 방지).
        @ParameterizedTest(name = "{0}")
        @org.junit.jupiter.params.provider.ValueSource(longs = [999L, 2L])
        @DisplayName("실패 - 미존재/타인 클레임 → 404 CLAIM_NOT_FOUND (둘 다 동일 응답 - 정보 노출 방지)")
        fun getClaimDetail_notFoundCases(claimId: Long) {
            every { claimQueryService.getClaimDetail(1L, claimId) } throws ClaimNotFoundException(claimId)

            mockMvc.perform(get("/api/v1/mobile/claims/$claimId"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("CLAIM_NOT_FOUND"))
        }
    }

    companion object {
        @JvmStatic
        fun getClaimsExceptions(): List<Arguments> = listOf(
            Arguments.of(
                "invalidDateFormat -> INVALID_DATE_FORMAT",
                "20260401",
                null,
                InvalidDateFormatException(),
                "INVALID_DATE_FORMAT",
            ),
            Arguments.of(
                "invalidDateRange -> INVALID_DATE_RANGE",
                "2026-04-10",
                "2026-04-01",
                InvalidDateRangeException(),
                "INVALID_DATE_RANGE",
            ),
        )
    }
}
