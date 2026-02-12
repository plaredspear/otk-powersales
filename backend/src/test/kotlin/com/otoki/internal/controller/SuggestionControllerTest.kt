package com.otoki.internal.controller

import com.otoki.internal.dto.response.SuggestionCreateResponse
import com.otoki.internal.entity.UserRole
import com.otoki.internal.security.JwtAuthenticationFilter
import com.otoki.internal.security.JwtTokenProvider
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.SuggestionService
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(SuggestionController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SuggestionController 테스트")
class SuggestionControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var suggestionService: SuggestionService

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
    @DisplayName("제안 등록 성공 - 신제품, 사진 없음")
    fun createSuggestion_newProduct_withoutPhotos_success() {
        // Given
        val response = SuggestionCreateResponse(
            id = 1L,
            category = "NEW_PRODUCT",
            categoryName = "신제품 제안",
            productCode = null,
            productName = null,
            title = "저당 라면 시리즈 출시 제안",
            createdAt = "2026-02-11T11:00:00"
        )

        whenever(suggestionService.createSuggestion(any(), any(), any()))
            .thenReturn(response)

        // When & Then
        mockMvc.perform(
            multipart("/api/v1/suggestions")
                .param("category", "NEW_PRODUCT")
                .param("title", "저당 라면 시리즈 출시 제안")
                .param("content", "건강을 생각하는 소비자들을 위한 저당 라면을 제안합니다.")
                .contentType(MediaType.MULTIPART_FORM_DATA)
        )
            .andExpect(status().isCreated)
    }

    @Test
    @DisplayName("제안 등록 성공 - 신제품, 사진 2장 포함")
    fun createSuggestion_newProduct_withPhotos_success() {
        // Given
        val photo1 = MockMultipartFile(
            "photos",
            "photo1.jpg",
            "image/jpeg",
            ByteArray(100)
        )
        val photo2 = MockMultipartFile(
            "photos",
            "photo2.jpg",
            "image/jpeg",
            ByteArray(100)
        )

        val response = SuggestionCreateResponse(
            id = 1L,
            category = "NEW_PRODUCT",
            categoryName = "신제품 제안",
            productCode = null,
            productName = null,
            title = "저당 라면 시리즈",
            createdAt = "2026-02-11T11:00:00"
        )

        whenever(suggestionService.createSuggestion(any(), any(), any()))
            .thenReturn(response)

        // When & Then
        mockMvc.perform(
            multipart("/api/v1/suggestions")
                .file(photo1)
                .file(photo2)
                .param("category", "NEW_PRODUCT")
                .param("title", "저당 라면 시리즈")
                .param("content", "제안 내용입니다.")
                .contentType(MediaType.MULTIPART_FORM_DATA)
        )
            .andExpect(status().isCreated)
    }

    @Test
    @DisplayName("제안 등록 성공 - 기존제품 개선")
    fun createSuggestion_existingProduct_success() {
        // Given
        val response = SuggestionCreateResponse(
            id = 1L,
            category = "EXISTING_PRODUCT",
            categoryName = "기존제품 상품가치향상",
            productCode = "PROD001",
            productName = "진라면",
            title = "진라면 용기 개선 제안",
            createdAt = "2026-02-11T11:00:00"
        )

        whenever(suggestionService.createSuggestion(any(), any(), any()))
            .thenReturn(response)

        // When & Then
        mockMvc.perform(
            multipart("/api/v1/suggestions")
                .param("category", "EXISTING_PRODUCT")
                .param("productCode", "PROD001")
                .param("title", "진라면 용기 개선 제안")
                .param("content", "용기를 더 견고하게 만들 것을 제안합니다.")
                .contentType(MediaType.MULTIPART_FORM_DATA)
        )
            .andExpect(status().isCreated)
    }

    @Test
    @DisplayName("제안 등록 실패 - 제목 누락")
    fun createSuggestion_missingTitle_fail() {
        // When & Then
        mockMvc.perform(
            multipart("/api/v1/suggestions")
                .param("category", "NEW_PRODUCT")
                .param("content", "내용")
                .contentType(MediaType.MULTIPART_FORM_DATA)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("제안 등록 실패 - 내용 누락")
    fun createSuggestion_missingContent_fail() {
        // When & Then
        mockMvc.perform(
            multipart("/api/v1/suggestions")
                .param("category", "NEW_PRODUCT")
                .param("title", "제목")
                .contentType(MediaType.MULTIPART_FORM_DATA)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("제안 등록 실패 - 분류 누락")
    fun createSuggestion_missingCategory_fail() {
        // When & Then
        mockMvc.perform(
            multipart("/api/v1/suggestions")
                .param("title", "제목")
                .param("content", "내용")
                .contentType(MediaType.MULTIPART_FORM_DATA)
        )
            .andExpect(status().isBadRequest)
    }
}
