package com.otoki.internal.education.controller

import com.otoki.internal.education.dto.response.*
import com.otoki.internal.common.dto.response.*
import com.otoki.internal.sap.entity.UserRole
import com.otoki.internal.education.exception.EducationPostNotFoundException
import com.otoki.internal.education.exception.InvalidEducationCategoryException
import com.otoki.internal.common.security.GpsConsentFilter
import com.otoki.internal.common.security.JwtAuthenticationFilter
import com.otoki.internal.admin.security.AdminAuthorityFilter
import com.otoki.internal.common.security.JwtTokenProvider
import com.otoki.internal.common.security.UserPrincipal
import com.otoki.internal.education.service.EducationService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
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
    @MockitoBean private lateinit var adminAuthorityFilter: AdminAuthorityFilter

    @MockitoBean
    private lateinit var gpsConsentFilter: GpsConsentFilter

    private val testPrincipal = UserPrincipal(userId = 1L, role = UserRole.USER)

    @BeforeEach
    fun setUp() {
        val authentication = UsernamePasswordAuthenticationToken(
            testPrincipal, null, testPrincipal.authorities
        )
        SecurityContextHolder.getContext().authentication = authentication
    }

    @Nested
    @DisplayName("GET /api/v1/education/posts - 목록 조회")
    inner class GetPostsTests {

        @Test
        @DisplayName("category 파라미터 누락 시 400 Bad Request 반환")
        fun getPosts_missingCategory() {
            mockMvc.perform(
                get("/api/v1/education/posts")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
        }

        @Test
        @DisplayName("유효하지 않은 category로 요청 시 400 Bad Request 반환")
        fun getPosts_invalidCategory() {
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
    }

    @Nested
    @DisplayName("GET /api/v1/education/posts/{postId} - 상세 조회")
    inner class GetPostDetailTests {

        @Test
        @DisplayName("존재하지 않는 게시물 조회 시 404 Not Found 반환")
        fun getPostDetail_notFound() {
            whenever(educationService.getPostDetail("NONEXIST"))
                .thenThrow(EducationPostNotFoundException())

            mockMvc.perform(
                get("/api/v1/education/posts/NONEXIST")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("POST_NOT_FOUND"))
        }
    }
}
