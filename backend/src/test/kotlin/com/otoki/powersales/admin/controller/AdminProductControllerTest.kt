package com.otoki.powersales.admin.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.otoki.powersales.product.dto.response.ProductListResponse
import com.otoki.powersales.product.dto.response.ProductListItem
import com.otoki.powersales.product.dto.response.CategoryTree
import com.otoki.powersales.product.dto.response.Category2Node
import com.otoki.powersales.product.dto.response.ProductDetail
import com.otoki.powersales.product.dto.response.InventorySearchResponse
import com.otoki.powersales.product.dto.response.InventorySearchResultItem
import com.otoki.powersales.product.dto.request.InventorySearchRequest
import com.otoki.powersales.product.dto.request.ProductExportRequest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import java.time.LocalDate
import com.otoki.powersales.product.service.AdminProductExportService
import com.otoki.powersales.product.service.AdminProductInventoryService
import com.otoki.powersales.product.service.AdminProductService
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.user.entity.ProfileType
import com.otoki.powersales.auth.entity.UserRole
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
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

@WebMvcTest(AdminProductController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminProductController 테스트")
class AdminProductControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var adminProductService: AdminProductService

    @MockitoBean
    private lateinit var adminProductInventoryService: AdminProductInventoryService

    @MockitoBean
    private lateinit var adminProductExportService: AdminProductExportService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var sapInboundAuditService: SapInboundAuditService

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter


    @MockitoBean
    private lateinit var gpsConsentFilter: GpsConsentFilter

    @BeforeEach
    fun setUp() {
        val principal = WebUserPrincipal(
            userId = 100L,
            usernameValue = "test@otokims.co.kr",
            employeeCode = "S001",
            employeeId = 1L,
            role = UserRole.BRANCH_MANAGER,
            costCenterCode = null,
            profileType = ProfileType.STAFF,
            isSalesSupport = false,
            passwordChangeRequired = false,
            permissions = emptySet(),
            encodedPassword = "",
            grantedAuthorities = emptyList(),
            active = true
        )
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
                        id = 1L,
                        productCode = "P001",
                        name = "진라면 매운맛",
                        category1 = "면류",
                        category2 = "라면",
                        category3 = "봉지면",
                        standardUnitPrice = java.math.BigDecimal("850.00"),
                        unit = "EA",
                        storageCondition = "실온",
                        productStatus = "판매중",
                        launchDate = "2020-01-15",
                        superTax = java.math.BigDecimal("85"),
                        shelfLife = "12",
                        shelfLifeUnit = "월",
                        tasteGift = "1",
                        lastModifiedAt = "2026-01-01T00:00:00"
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
                .andExpect(jsonPath("$.data.content[0].productCode").value("P001"))
                .andExpect(jsonPath("$.data.content[0].name").value("진라면 매운맛"))
                .andExpect(jsonPath("$.data.content[0].category1").value("면류"))
                .andExpect(jsonPath("$.data.content[0].standardUnitPrice").value(850.0))
                .andExpect(jsonPath("$.data.content[0].productStatus").value("판매중"))
                .andExpect(jsonPath("$.data.content[0].launchDate").value("2020-01-15"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
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
                .andExpect(jsonPath("$.data.totalElements").value(0))
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

    @Nested
    @DisplayName("GET /api/v1/admin/products/{productCode} - 제품 상세 조회 (UC-02)")
    inner class GetProductDetail {

        @Test
        @DisplayName("성공 - 단건 상세 응답")
        fun getProductDetail_success() {
            // Given
            val detail = ProductDetail(
                id = 1L,
                productCode = "P001",
                name = "꿀배청 680G",
                barcode = null, logisticsBarcode = null,
                category1 = "음료", category2 = "건강", category3 = "전통차",
                categoryCode1 = null, categoryCode2 = null, categoryCode3 = null,
                unit = "EA", orderingUnit = null,
                conversionQuantity = null, boxReceivingQuantity = null,
                standardUnitPrice = java.math.BigDecimal("5000"),
                superTax = null, launchDate = "2020-01-15",
                storageCondition = "실온", productStatus = "-", productType = null,
                shelfLife = "12", shelfLifeUnit = "월",
                tasteGift = null, productFeatures = null, sellingPoint = null,
                purpose = null, targetAccountType = null,
                allergen = null, crossContamination = null,
                imgRefPathFront = null, imgRefPathBack = null,
                pallet = null, manufacture = null, manufactureDetail = null,
                claimManagement = null,
                createdAt = "2026-01-01T00:00:00",
                lastModifiedAt = "2026-05-01T00:00:00"
            )
            whenever(adminProductService.getProductDetail(eq("P001"))).thenReturn(detail)

            // When & Then
            mockMvc.perform(get("/api/v1/admin/products/P001"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productCode").value("P001"))
                .andExpect(jsonPath("$.data.name").value("꿀배청 680G"))
                .andExpect(jsonPath("$.data.shelfLife").value("12"))
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/products/inventory-search - 재고조회 (UC-03/04)")
    inner class SearchInventory {

        private val objectMapper = ObjectMapper().registerModule(JavaTimeModule())

        @Test
        @DisplayName("성공 - SAP 응답 매핑")
        fun searchInventory_success() {
            // Given
            val request = InventorySearchRequest(
                accountId = 1,
                productCodes = listOf("P001", "P002"),
                deliveryRequestDate = LocalDate.now().plus(1, java.time.temporal.ChronoUnit.DAYS)
            )
            val response = InventorySearchResponse(
                results = listOf(
                    InventorySearchResultItem(
                        productCode = "P001", productName = "꿀배청", unit = "EA",
                        conversionQuantity = 1, supplyLimitQuantity = 100,
                        unitPrice = java.math.BigDecimal("5000"), message = null
                    ),
                    InventorySearchResultItem(
                        productCode = "P002", productName = "카레", unit = "EA",
                        conversionQuantity = 1, supplyLimitQuantity = 200,
                        unitPrice = java.math.BigDecimal("3000"), message = null
                    )
                )
            )
            whenever(adminProductInventoryService.searchInventory(any())).thenReturn(response)

            // When & Then
            mockMvc.perform(
                post("/api/v1/admin/products/inventory-search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.results.length()").value(2))
                .andExpect(jsonPath("$.data.results[0].productCode").value("P001"))
                .andExpect(jsonPath("$.data.results[1].unitPrice").value(3000))
        }

        @Test
        @DisplayName("실패 - 제품 0건 -> 400 (validation)")
        fun searchInventory_emptyProducts() {
            val body = """{"accountId":1,"productCodes":[],"deliveryRequestDate":"${LocalDate.now().plus(1, java.time.temporal.ChronoUnit.DAYS)}"}"""
            mockMvc.perform(
                post("/api/v1/admin/products/inventory-search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
            )
                .andExpect(status().isBadRequest)
        }

        @Test
        @DisplayName("실패 - 제품 51건 -> 400 (validation)")
        fun searchInventory_tooManyProducts() {
            val codes = (1..51).map { "\"P$it\"" }.joinToString(",")
            val body = """{"accountId":1,"productCodes":[$codes],"deliveryRequestDate":"${LocalDate.now().plus(1, java.time.temporal.ChronoUnit.DAYS)}"}"""
            mockMvc.perform(
                post("/api/v1/admin/products/inventory-search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
            )
                .andExpect(status().isBadRequest)
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/products/export-excel - 엑셀 다운로드 (UC-05)")
    inner class ExportExcel {

        private val objectMapper = ObjectMapper()

        @Test
        @DisplayName("성공 - .xlsx 바이트 응답 + Content-Disposition")
        fun exportExcel_success() {
            // Given
            val xlsxBytes = byteArrayOf(0x50, 0x4B, 0x03, 0x04) // PK header (dummy)
            whenever(adminProductExportService.exportSelectedProducts(eq(listOf("P001", "P002"))))
                .thenReturn(xlsxBytes)
            val request = ProductExportRequest(productCodes = listOf("P001", "P002"))

            // When & Then
            mockMvc.perform(
                post("/api/v1/admin/products/export-excel")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(content().bytes(xlsxBytes))
        }

        @Test
        @DisplayName("실패 - 빈 productCodes -> 400")
        fun exportExcel_empty() {
            val body = """{"productCodes":[]}"""
            mockMvc.perform(
                post("/api/v1/admin/products/export-excel")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
            )
                .andExpect(status().isBadRequest)
        }
    }
}
