package com.otoki.internal.admin.controller

import com.otoki.internal.admin.dto.response.*
import com.otoki.internal.admin.security.AdminAuthorityFilter
import com.otoki.internal.admin.service.AdminProductService
import com.otoki.internal.common.security.GpsConsentFilter
import com.otoki.internal.common.security.JwtAuthenticationFilter
import com.otoki.internal.common.security.JwtTokenProvider
import com.otoki.internal.common.security.UserPrincipal
import com.otoki.internal.sap.entity.UserRole
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdminProductController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminProductController 테스트")
class AdminProductControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var adminProductService: AdminProductService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockitoBean
    private lateinit var adminAuthorityFilter: AdminAuthorityFilter

    @MockitoBean
    private lateinit var gpsConsentFilter: GpsConsentFilter

    @BeforeEach
    fun setUp() {
        val principal = UserPrincipal(userId = 1L, role = UserRole.ADMIN)
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
    }

    @Nested
    @DisplayName("GET /api/v1/admin/products - 제품 목록 조회")
    inner class GetProducts {

        @Test
        @DisplayName("성공 - 기본 조회")
        fun getProducts_success() {
            // Given
            val response = ProductListResponse(
                content = listOf(
                    ProductListItem(
                        productCode = "P001",
                        name = "진라면 매운맛",
                        category1 = "면류",
                        category2 = "라면",
                        category3 = "봉지면",
                        standardPrice = 850.0,
                        unit = "EA",
                        storageCondition = "실온",
                        productStatus = "판매중",
                        launchDate = "2020-01-15"
                    )
                ),
                page = 0,
                size = 20,
                totalElements = 1,
                totalPages = 1
            )
            whenever(adminProductService.getProducts(
                keyword = isNull(), category1 = isNull(), category2 = isNull(),
                category3 = isNull(), productStatus = isNull(),
                page = eq(0), size = eq(20)
            )).thenReturn(response)

            // When & Then
            mockMvc.perform(get("/api/v1/admin/products"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray)
                .andExpect(jsonPath("$.data.content[0].product_code").value("P001"))
                .andExpect(jsonPath("$.data.content[0].name").value("진라면 매운맛"))
                .andExpect(jsonPath("$.data.content[0].category1").value("면류"))
                .andExpect(jsonPath("$.data.content[0].standard_price").value(850.0))
                .andExpect(jsonPath("$.data.content[0].product_status").value("판매중"))
                .andExpect(jsonPath("$.data.content[0].launch_date").value("2020-01-15"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.total_elements").value(1))
                .andExpect(jsonPath("$.data.total_pages").value(1))
        }

        @Test
        @DisplayName("성공 - 키워드 + 카테고리 필터")
        fun getProducts_withFilters_success() {
            // Given
            val response = ProductListResponse(
                content = emptyList(),
                page = 0, size = 10, totalElements = 0, totalPages = 0
            )
            whenever(adminProductService.getProducts(
                keyword = eq("진라면"), category1 = eq("면류"), category2 = isNull(),
                category3 = isNull(), productStatus = isNull(),
                page = eq(0), size = eq(10)
            )).thenReturn(response)

            // When & Then
            mockMvc.perform(
                get("/api/v1/admin/products")
                    .param("keyword", "진라면")
                    .param("category1", "면류")
                    .param("size", "10")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isEmpty)
        }

        @Test
        @DisplayName("성공 - 빈 결과")
        fun getProducts_empty_success() {
            // Given
            val response = ProductListResponse(
                content = emptyList(),
                page = 0, size = 20, totalElements = 0, totalPages = 0
            )
            whenever(adminProductService.getProducts(
                keyword = isNull(), category1 = isNull(), category2 = isNull(),
                category3 = isNull(), productStatus = isNull(),
                page = eq(0), size = eq(20)
            )).thenReturn(response)

            // When & Then
            mockMvc.perform(get("/api/v1/admin/products"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.content").isEmpty)
                .andExpect(jsonPath("$.data.total_elements").value(0))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/products/categories - 카테고리 목록 조회")
    inner class GetCategories {

        @Test
        @DisplayName("성공 - 카테고리 트리 반환")
        fun getCategories_success() {
            // Given
            val response = listOf(
                CategoryTree(
                    category1 = "면류",
                    children = listOf(
                        Category2Node(category2 = "라면", children = listOf("봉지면", "컵라면"))
                    )
                )
            )
            whenever(adminProductService.getCategories()).thenReturn(response)

            // When & Then
            mockMvc.perform(get("/api/v1/admin/products/categories"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].category1").value("면류"))
                .andExpect(jsonPath("$.data[0].children[0].category2").value("라면"))
                .andExpect(jsonPath("$.data[0].children[0].children[0]").value("봉지면"))
                .andExpect(jsonPath("$.data[0].children[0].children[1]").value("컵라면"))
        }
    }
}
