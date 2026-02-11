package com.otoki.internal.controller

import com.otoki.internal.dto.response.*
import com.otoki.internal.entity.UserRole
import com.otoki.internal.security.JwtAuthenticationFilter
import com.otoki.internal.security.JwtTokenProvider
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.ClaimService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(ClaimController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ClaimController 테스트")
class ClaimControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var claimService: ClaimService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    private val testPrincipal = UserPrincipal(userId = 1L, role = UserRole.USER)

    @BeforeEach
    fun setUp() {
        val authentication = UsernamePasswordAuthenticationToken(
            testPrincipal, null, testPrincipal.authorities
        )
        SecurityContextHolder.getContext().authentication = authentication
    }

    @Test
    @DisplayName("클레임 등록 성공 - 기본 정보만")
    fun createClaim_basicInfo_success() {
        // Given
        val defectPhoto = MockMultipartFile(
            "defectPhoto",
            "defect.jpg",
            "image/jpeg",
            ByteArray(100)
        )
        val labelPhoto = MockMultipartFile(
            "labelPhoto",
            "label.jpg",
            "image/jpeg",
            ByteArray(100)
        )

        val response = ClaimCreateResponse(
            id = 1L,
            storeName = "테스트 거래처",
            storeId = 1L,
            productName = "테스트 제품",
            productCode = "PROD001",
            createdAt = "2026-02-11T10:00:00"
        )

        whenever(claimService.createClaim(any(), any(), any(), any(), any()))
            .thenReturn(response)

        // When & Then
        mockMvc.perform(
            multipart("/api/v1/claims")
                .file(defectPhoto)
                .file(labelPhoto)
                .param("storeId", "1")
                .param("productCode", "PROD001")
                .param("dateType", "EXPIRY_DATE")
                .param("date", "2026-12-31")
                .param("categoryId", "1")
                .param("subcategoryId", "1")
                .param("defectDescription", "불량입니다")
                .param("defectQuantity", "1")
                .contentType(MediaType.MULTIPART_FORM_DATA)
        )
            .andExpect(status().isCreated)
    }

    @Test
    @DisplayName("클레임 등록 성공 - 구매 정보 포함")
    fun createClaim_withPurchaseInfo_success() {
        // Given
        val defectPhoto = MockMultipartFile(
            "defectPhoto",
            "defect.jpg",
            "image/jpeg",
            ByteArray(100)
        )
        val labelPhoto = MockMultipartFile(
            "labelPhoto",
            "label.jpg",
            "image/jpeg",
            ByteArray(100)
        )
        val receiptPhoto = MockMultipartFile(
            "receiptPhoto",
            "receipt.jpg",
            "image/jpeg",
            ByteArray(100)
        )

        val response = ClaimCreateResponse(
            id = 1L,
            storeName = "테스트 거래처",
            storeId = 1L,
            productName = "테스트 제품",
            productCode = "PROD001",
            createdAt = "2026-02-11T10:00:00"
        )

        whenever(claimService.createClaim(any(), any(), any(), any(), any()))
            .thenReturn(response)

        // When & Then
        mockMvc.perform(
            multipart("/api/v1/claims")
                .file(defectPhoto)
                .file(labelPhoto)
                .file(receiptPhoto)
                .param("storeId", "1")
                .param("productCode", "PROD001")
                .param("dateType", "EXPIRY_DATE")
                .param("date", "2026-12-31")
                .param("categoryId", "1")
                .param("subcategoryId", "1")
                .param("defectDescription", "불량입니다")
                .param("defectQuantity", "1")
                .param("purchaseAmount", "10000")
                .param("purchaseMethodCode", "PM01")
                .param("requestTypeCode", "RT01")
                .contentType(MediaType.MULTIPART_FORM_DATA)
        )
            .andExpect(status().isCreated)
    }

    @Test
    @DisplayName("폼 초기화 데이터 조회 성공")
    fun getFormData_success() {
        // Given
        val categories = listOf(
            ClaimCategoryWithSubcategoriesResponse(
                id = 1L,
                name = "이물",
                subcategories = listOf(
                    ClaimSubcategoryResponse(id = 1L, name = "벌레"),
                    ClaimSubcategoryResponse(id = 2L, name = "금속")
                )
            ),
            ClaimCategoryWithSubcategoriesResponse(
                id = 2L,
                name = "변질/변패",
                subcategories = listOf(
                    ClaimSubcategoryResponse(id = 3L, name = "곰팡이")
                )
            )
        )

        val purchaseMethods = listOf(
            PurchaseMethodResponse(code = "PM01", name = "대형마트"),
            PurchaseMethodResponse(code = "PM02", name = "편의점")
        )

        val requestTypes = listOf(
            ClaimRequestTypeResponse(code = "RT01", name = "교환"),
            ClaimRequestTypeResponse(code = "RT02", name = "환불")
        )

        val response = ClaimFormDataResponse(
            categories = categories,
            purchaseMethods = purchaseMethods,
            requestTypes = requestTypes
        )

        whenever(claimService.getFormData()).thenReturn(response)

        // When & Then
        mockMvc.perform(
            get("/api/v1/claims/form-data")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
    }
}
