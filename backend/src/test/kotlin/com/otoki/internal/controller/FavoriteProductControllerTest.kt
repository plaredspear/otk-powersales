package com.otoki.internal.controller

import com.otoki.internal.dto.response.FavoriteProductResponse
import com.otoki.internal.entity.UserRole
import com.otoki.internal.exception.AlreadyFavoritedException
import com.otoki.internal.exception.FavoriteNotFoundException
import com.otoki.internal.exception.ProductNotFoundException
import com.otoki.internal.security.GpsConsentFilter
import com.otoki.internal.security.JwtAuthenticationFilter
import com.otoki.internal.security.JwtTokenProvider
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.FavoriteProductService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(FavoriteProductController::class)
@AutoConfigureMockMvc(addFilters = false)
class FavoriteProductControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var favoriteProductService: FavoriteProductService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockitoBean
    private lateinit var gpsConsentFilter: GpsConsentFilter

    private val testUserId = 1L

    @BeforeEach
    fun setup() {
        val userPrincipal = UserPrincipal(
            userId = testUserId,
            role = UserRole.USER
        )
        val authentication = UsernamePasswordAuthenticationToken(
            userPrincipal,
            null,
            userPrincipal.authorities
        )
        SecurityContextHolder.getContext().authentication = authentication
    }

    @Test
    fun `GET favorites - returns page with items`() {
        // Given
        val favoriteResponses = listOf(
            FavoriteProductResponse(
                productCode = "PC001",
                productName = "오뚜기 카레",
                logisticsBarcode ="8801234567890",
                storageCondition ="냉장",
                category1 ="조미식품",
                category2 ="카레",
                addedAt = "2025-02-10T10:00:00"
            ),
            FavoriteProductResponse(
                productCode = "PC002",
                productName = "오뚜기 케첩",
                logisticsBarcode ="8801234567891",
                storageCondition ="상온",
                category1 ="조미식품",
                category2 ="소스",
                addedAt = "2025-02-10T09:00:00"
            )
        )
        val pageable = PageRequest.of(0, 20)
        val page = PageImpl(favoriteResponses, pageable, 2)

        whenever(favoriteProductService.getMyFavoriteProducts(eq(testUserId), eq(0), eq(20)))
            .thenReturn(page)

        // When & Then
        mockMvc.perform(
            get("/api/v1/me/favorite-products")
                .param("page", "0")
                .param("size", "20")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.content.length()").value(2))
            .andExpect(jsonPath("$.data.content[0].product_code").value("PC001"))
            .andExpect(jsonPath("$.data.content[0].product_name").value("오뚜기 카레"))
            .andExpect(jsonPath("$.data.content[0].logistics_barcode").value("8801234567890"))
            .andExpect(jsonPath("$.data.content[0].storage_condition").value("냉장"))
            .andExpect(jsonPath("$.data.content[0].category1").value("조미식품"))
            .andExpect(jsonPath("$.data.content[0].category2").value("카레"))
            .andExpect(jsonPath("$.data.content[0].added_at").exists())
            .andExpect(jsonPath("$.data.content[1].product_code").value("PC002"))
            .andExpect(jsonPath("$.data.content[1].product_name").value("오뚜기 케첩"))
            .andExpect(jsonPath("$.data.total_elements").value(2))
            .andExpect(jsonPath("$.data.total_pages").value(1))
            .andExpect(jsonPath("$.data.size").value(20))
            .andExpect(jsonPath("$.data.number").value(0))
    }

    @Test
    fun `GET favorites - returns empty page`() {
        // Given
        val pageable = PageRequest.of(0, 20)
        val emptyPage = PageImpl<FavoriteProductResponse>(emptyList(), pageable, 0)

        whenever(favoriteProductService.getMyFavoriteProducts(eq(testUserId), eq(0), eq(20)))
            .thenReturn(emptyPage)

        // When & Then
        mockMvc.perform(
            get("/api/v1/me/favorite-products")
                .param("page", "0")
                .param("size", "20")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray)
            .andExpect(jsonPath("$.data.content.length()").value(0))
            .andExpect(jsonPath("$.data.total_elements").value(0))
            .andExpect(jsonPath("$.data.total_pages").value(0))
    }

    @Test
    fun `POST add favorite - success`() {
        // Given
        val productCode = "PC001"

        // When & Then
        mockMvc.perform(
            post("/api/v1/me/favorite-products/{productCode}", productCode)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isEmpty)
    }

    @Test
    fun `POST add favorite - product not found`() {
        // Given
        val productCode = "INVALID_CODE"

        whenever(favoriteProductService.addFavoriteProduct(eq(testUserId), eq(productCode)))
            .thenThrow(ProductNotFoundException(productCode))

        // When & Then
        mockMvc.perform(
            post("/api/v1/me/favorite-products/{productCode}", productCode)
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("PRODUCT_NOT_FOUND"))
    }

    @Test
    fun `POST add favorite - already favorited`() {
        // Given
        val productCode = "PC001"

        whenever(favoriteProductService.addFavoriteProduct(eq(testUserId), eq(productCode)))
            .thenThrow(AlreadyFavoritedException())

        // When & Then
        mockMvc.perform(
            post("/api/v1/me/favorite-products/{productCode}", productCode)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("ALREADY_FAVORITED"))
    }

    @Test
    fun `DELETE remove favorite - success`() {
        // Given
        val productCode = "PC001"

        // When & Then
        mockMvc.perform(
            delete("/api/v1/me/favorite-products/{productCode}", productCode)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isEmpty)
    }

    @Test
    fun `DELETE remove favorite - not found`() {
        // Given
        val productCode = "PC001"

        whenever(favoriteProductService.removeFavoriteProduct(eq(testUserId), eq(productCode)))
            .thenThrow(FavoriteNotFoundException())

        // When & Then
        mockMvc.perform(
            delete("/api/v1/me/favorite-products/{productCode}", productCode)
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("FAVORITE_NOT_FOUND"))
    }
}
