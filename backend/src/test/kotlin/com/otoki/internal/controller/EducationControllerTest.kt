package com.otoki.internal.controller

import com.otoki.internal.dto.response.*
import com.otoki.internal.entity.UserRole
import com.otoki.internal.exception.EducationPostNotFoundException
import com.otoki.internal.exception.InvalidEducationCategoryException
import com.otoki.internal.security.JwtAuthenticationFilter
import com.otoki.internal.security.JwtTokenProvider
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.EducationService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
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

@WebMvcTest(EducationController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("EducationController 테스트")
class EducationControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var educationService: EducationService

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
    @DisplayName("category 파라미터 누락 시 400 Bad Request 반환")
    fun getPosts_MissingCategory() {
        mockMvc.perform(
            get("/api/v1/education/posts")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("유효하지 않은 category로 요청 시 400 Bad Request 반환")
    fun getPosts_InvalidCategory() {
        whenever(educationService.getPosts("INVALID", null, 1, 10))
            .thenThrow(InvalidEducationCategoryException())

        mockMvc.perform(
            get("/api/v1/education/posts")
                .param("category", "INVALID")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("INVALID_CATEGORY"))
    }

    @Test
    @DisplayName("존재하지 않는 게시물 조회 시 404 Not Found 반환")
    fun getPostDetail_NotFound() {
        whenever(educationService.getPostDetail(999L))
            .thenThrow(EducationPostNotFoundException())

        mockMvc.perform(
            get("/api/v1/education/posts/999")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("POST_NOT_FOUND"))
    }
}
