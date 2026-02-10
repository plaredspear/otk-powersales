package com.otoki.internal.controller

import com.otoki.internal.dto.response.ProductDto
import com.otoki.internal.entity.UserRole
import com.otoki.internal.exception.InvalidSearchParameterException
import com.otoki.internal.exception.InvalidSearchTypeException
import com.otoki.internal.security.JwtAuthenticationFilter
import com.otoki.internal.security.JwtTokenProvider
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.ProductService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(ProductController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ProductController 테스트")
class ProductControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var productService: ProductService

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

    // ========== 성공 케이스 ==========

    @Nested
    @DisplayName("검색 성공 케이스")
    inner class SuccessCases {

        @Test
        @DisplayName("텍스트 검색 성공 - 200 OK, 제품 목록 반환")
        fun searchProducts_textSearch_returnsOk() {
            // Given
            val products = listOf(
                createProductDto("18110014", "열라면_용기105G", "18110014", "8801045570716"),
                createProductDto("18110007", "열라면_용기115G", "18110007", "8801045570723")
            )
            val page = PageImpl(products, PageRequest.of(0, 20), 2)
            whenever(productService.searchProducts(eq("열라면"), eq("text"), eq(0), eq(20)))
                .thenReturn(page)

            // When & Then
            mockMvc.perform(
                get("/api/v1/products/search")
                    .param("query", "열라면")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray)
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].product_id").value("18110014"))
                .andExpect(jsonPath("$.data.content[0].product_name").value("열라면_용기105G"))
                .andExpect(jsonPath("$.data.content[0].product_code").value("18110014"))
                .andExpect(jsonPath("$.data.content[0].barcode").value("8801045570716"))
                .andExpect(jsonPath("$.data.total_elements").value(2))
                .andExpect(jsonPath("$.data.total_pages").value(1))
                .andExpect(jsonPath("$.data.number").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.last").value(true))
        }

        @Test
        @DisplayName("바코드 검색 성공 - 200 OK")
        fun searchProducts_barcodeSearch_returnsOk() {
            // Given
            val products = listOf(
                createProductDto("18110014", "열라면_용기105G", "18110014", "8801045570716")
            )
            val page = PageImpl(products, PageRequest.of(0, 20), 1)
            whenever(productService.searchProducts(eq("8801045570716"), eq("barcode"), eq(0), eq(20)))
                .thenReturn(page)

            // When & Then
            mockMvc.perform(
                get("/api/v1/products/search")
                    .param("query", "8801045570716")
                    .param("type", "barcode")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].barcode").value("8801045570716"))
        }

        @Test
        @DisplayName("빈 결과 - 200 OK, 빈 목록")
        fun searchProducts_noResults_returnsEmptyList() {
            // Given
            val page = PageImpl(emptyList<ProductDto>(), PageRequest.of(0, 20), 0)
            whenever(productService.searchProducts(eq("존재하지않는제품"), eq("text"), eq(0), eq(20)))
                .thenReturn(page)

            // When & Then
            mockMvc.perform(
                get("/api/v1/products/search")
                    .param("query", "존재하지않는제품")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isEmpty)
                .andExpect(jsonPath("$.data.total_elements").value(0))
        }

        @Test
        @DisplayName("페이지네이션 파라미터 전달 성공")
        fun searchProducts_withPagination_passesParams() {
            // Given
            val products = listOf(
                createProductDto("18130001", "참깨라면_봉지115G", "18130001", "8801045572001")
            )
            val page = PageImpl(products, PageRequest.of(1, 5), 6)
            whenever(productService.searchProducts(eq("라면"), eq("text"), eq(1), eq(5)))
                .thenReturn(page)

            // When & Then
            mockMvc.perform(
                get("/api/v1/products/search")
                    .param("query", "라면")
                    .param("page", "1")
                    .param("size", "5")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.number").value(1))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.total_elements").value(6))
                .andExpect(jsonPath("$.data.total_pages").value(2))
        }
    }

    // ========== 에러 케이스 ==========

    @Nested
    @DisplayName("검색 에러 케이스")
    inner class ErrorCases {

        @Test
        @DisplayName("검색어 1자 - 400 INVALID_PARAMETER")
        fun searchProducts_shortQuery_returnsBadRequest() {
            // Given
            whenever(productService.searchProducts(eq("열"), eq("text"), any(), any()))
                .thenThrow(InvalidSearchParameterException("검색어는 2자 이상이어야 합니다"))

            // When & Then
            mockMvc.perform(
                get("/api/v1/products/search")
                    .param("query", "열")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.error.message").value("검색어는 2자 이상이어야 합니다"))
        }

        @Test
        @DisplayName("잘못된 검색 유형 - 400 INVALID_SEARCH_TYPE")
        fun searchProducts_invalidType_returnsBadRequest() {
            // Given
            whenever(productService.searchProducts(eq("열라면"), eq("invalid"), any(), any()))
                .thenThrow(InvalidSearchTypeException())

            // When & Then
            mockMvc.perform(
                get("/api/v1/products/search")
                    .param("query", "열라면")
                    .param("type", "invalid")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_SEARCH_TYPE"))
        }

        @Test
        @DisplayName("바코드 형식 오류 - 400 INVALID_PARAMETER")
        fun searchProducts_invalidBarcode_returnsBadRequest() {
            // Given
            whenever(productService.searchProducts(eq("abc"), eq("barcode"), any(), any()))
                .thenThrow(InvalidSearchParameterException("유효하지 않은 바코드 형식입니다"))

            // When & Then
            mockMvc.perform(
                get("/api/v1/products/search")
                    .param("query", "abc")
                    .param("type", "barcode")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
        }

        @Test
        @DisplayName("query 파라미터 누락 - 400 에러")
        fun searchProducts_missingQuery_returnsBadRequest() {
            // When & Then
            mockMvc.perform(
                get("/api/v1/products/search")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
        }
    }

    // ========== 헬퍼 메서드 ==========

    private fun createProductDto(
        productId: String,
        productName: String,
        productCode: String,
        barcode: String
    ): ProductDto {
        return ProductDto(
            productId = productId,
            productName = productName,
            productCode = productCode,
            barcode = barcode,
            storageType = "상온",
            shelfLife = "7개월",
            categoryMid = "라면",
            categorySub = "용기면"
        )
    }
}
