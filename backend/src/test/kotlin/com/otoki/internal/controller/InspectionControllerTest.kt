package com.otoki.internal.controller

import com.otoki.internal.dto.response.*
import com.otoki.internal.entity.UserRole
import com.otoki.internal.security.JwtAuthenticationFilter
import com.otoki.internal.security.JwtTokenProvider
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.InspectionService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(InspectionController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("InspectionController 테스트")
class InspectionControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var inspectionService: InspectionService

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
    @DisplayName("현장 점검 목록 조회 성공")
    fun getInspectionList_success() {
        // Given
        val items = listOf(
            InspectionListItemResponse(
                id = 1L,
                category = "OWN",
                storeName = "테스트 거래처",
                storeId = 100L,
                inspectionDate = "2020-08-18",
                fieldType = "본매대",
                fieldTypeCode = "FT01"
            )
        )
        val response = InspectionListResponse(totalCount = 1, items = items)

        whenever(inspectionService.getInspectionList(any(), any(), any(), any(), any()))
            .thenReturn(response)

        // When & Then
        mockMvc.perform(
            get("/api/v1/inspections")
                .param("fromDate", "2020-08-01")
                .param("toDate", "2020-08-31")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
    }

    @Test
    @DisplayName("현장 점검 상세 조회 성공")
    fun getInspectionDetail_success() {
        // Given
        val response = InspectionDetailResponse(
            id = 1L,
            category = "OWN",
            storeName = "테스트 거래처",
            storeId = 100L,
            themeName = "테스트 테마",
            themeId = 10L,
            inspectionDate = "2020-08-18",
            fieldType = "본매대",
            fieldTypeCode = "FT01",
            description = "설명",
            productCode = "12345678",
            productName = "테스트 제품",
            competitorName = null,
            competitorActivity = null,
            competitorTasting = null,
            competitorProductName = null,
            competitorProductPrice = null,
            competitorSalesQuantity = null,
            photos = emptyList(),
            createdAt = "2020-08-18T10:00:00"
        )

        whenever(inspectionService.getInspectionDetail(any(), any()))
            .thenReturn(response)

        // When & Then
        mockMvc.perform(
            get("/api/v1/inspections/1")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
    }

    @Test
    @DisplayName("테마 목록 조회 성공")
    fun getThemes_success() {
        // Given
        val themes = listOf(
            ThemeResponse(
                id = 10L,
                name = "테스트 테마",
                startDate = "2020-08-01",
                endDate = "2020-08-31"
            )
        )

        whenever(inspectionService.getThemes()).thenReturn(themes)

        // When & Then
        mockMvc.perform(
            get("/api/v1/inspections/themes")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
    }

    @Test
    @DisplayName("현장 유형 목록 조회 성공")
    fun getFieldTypes_success() {
        // Given
        val fieldTypes = listOf(
            FieldTypeResponse(code = "FT01", name = "본매대"),
            FieldTypeResponse(code = "FT02", name = "시식")
        )

        whenever(inspectionService.getFieldTypes()).thenReturn(fieldTypes)

        // When & Then
        mockMvc.perform(
            get("/api/v1/inspections/field-types")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
    }
}
