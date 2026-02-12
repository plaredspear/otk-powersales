package com.otoki.internal.controller

import com.otoki.internal.entity.UserRole
import com.otoki.internal.exception.InvalidNoticeCategoryException
import com.otoki.internal.exception.NoticePostNotFoundException
import com.otoki.internal.security.JwtAuthenticationFilter
import com.otoki.internal.security.JwtTokenProvider
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.NoticeService
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

@WebMvcTest(NoticeController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("NoticeController 테스트")
class NoticeControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var noticeService: NoticeService

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
    @DisplayName("유효하지 않은 category로 요청 시 400 Bad Request 반환")
    fun getPosts_InvalidCategory() {
        // Given
        whenever(noticeService.getPosts("INVALID", null, 1, 10))
            .thenThrow(InvalidNoticeCategoryException())

        // When & Then
        mockMvc.perform(
            get("/api/v1/notices")
                .param("category", "INVALID")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("INVALID_CATEGORY"))
    }

    @Test
    @DisplayName("존재하지 않는 공지사항 조회 시 404 Not Found 반환")
    fun getPostDetail_NotFound() {
        // Given
        whenever(noticeService.getPostDetail(999L))
            .thenThrow(NoticePostNotFoundException())

        // When & Then
        mockMvc.perform(
            get("/api/v1/notices/999")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("NOTICE_NOT_FOUND"))
    }
}
