package com.otoki.internal.notice.controller

import com.otoki.internal.sap.entity.UserRole
import com.otoki.internal.notice.dto.response.NoticeImageResponse
import com.otoki.internal.notice.dto.response.NoticePostDetailResponse
import com.otoki.internal.notice.dto.response.NoticePostListResponse
import com.otoki.internal.notice.dto.response.NoticePostSummaryResponse
import com.otoki.internal.notice.exception.InvalidNoticeCategoryException
import com.otoki.internal.notice.exception.InvalidNoticeIdException
import com.otoki.internal.notice.exception.NoticePostNotFoundException
import com.otoki.internal.common.security.GpsConsentFilter
import com.otoki.internal.common.security.JwtAuthenticationFilter
import com.otoki.internal.admin.security.AdminAuthorityFilter
import com.otoki.internal.common.security.JwtTokenProvider
import com.otoki.internal.common.security.UserPrincipal
import com.otoki.internal.notice.service.NoticeService
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
    @DisplayName("GET /api/v1/notices - 목록 조회")
    inner class GetPostsTests {

        @Test
        @DisplayName("성공 - 전체 목록 조회")
        fun getPosts_success() {
            // Given
            val response = NoticePostListResponse(
                content = listOf(
                    NoticePostSummaryResponse(id = 42L, category = "ALL", categoryName = "전체공지", title = "전체공지 제목", createdAt = "2026-02-28T10:30:00"),
                    NoticePostSummaryResponse(id = 41L, category = "BRANCH", categoryName = "지점공지", title = "지점공지 제목", createdAt = "2026-02-27T09:00:00")
                ),
                totalCount = 5,
                totalPages = 1,
                currentPage = 1,
                size = 10
            )
            whenever(noticeService.getPosts(eq(1L), eq(null), eq(null), eq(1), eq(10))).thenReturn(response)

            // When & Then
            mockMvc.perform(
                get("/api/v1/notices")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray)
                .andExpect(jsonPath("$.data.content[0].id").value(42))
                .andExpect(jsonPath("$.data.content[0].category").value("ALL"))
                .andExpect(jsonPath("$.data.content[0].category_name").value("전체공지"))
                .andExpect(jsonPath("$.data.content[0].title").value("전체공지 제목"))
                .andExpect(jsonPath("$.data.content[0].created_at").value("2026-02-28T10:30:00"))
                .andExpect(jsonPath("$.data.content[1].id").value(41))
                .andExpect(jsonPath("$.data.total_count").value(5))
                .andExpect(jsonPath("$.data.total_pages").value(1))
                .andExpect(jsonPath("$.data.current_page").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
        }

        @Test
        @DisplayName("성공 - 카테고리 + 검색 + 페이지네이션")
        fun getPosts_withParams() {
            // Given
            val response = NoticePostListResponse(
                content = listOf(
                    NoticePostSummaryResponse(id = 1L, category = "ALL", categoryName = "전체공지", title = "영업 목표", createdAt = "2026-02-28T10:30:00")
                ),
                totalCount = 1,
                totalPages = 1,
                currentPage = 1,
                size = 5
            )
            whenever(noticeService.getPosts(eq(1L), eq("COMPANY"), eq("영업"), eq(1), eq(5))).thenReturn(response)

            // When & Then
            mockMvc.perform(
                get("/api/v1/notices")
                    .param("category", "COMPANY")
                    .param("search", "영업")
                    .param("page", "1")
                    .param("size", "5")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.content").isArray)
                .andExpect(jsonPath("$.data.content[0].title").value("영업 목표"))
                .andExpect(jsonPath("$.data.total_count").value(1))
        }

        @Test
        @DisplayName("성공 - 빈 결과")
        fun getPosts_emptyResult() {
            // Given
            val response = NoticePostListResponse(
                content = emptyList(),
                totalCount = 0,
                totalPages = 0,
                currentPage = 1,
                size = 10
            )
            whenever(noticeService.getPosts(eq(1L), eq(null), eq(null), eq(1), eq(10))).thenReturn(response)

            // When & Then
            mockMvc.perform(
                get("/api/v1/notices")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.content").isEmpty)
                .andExpect(jsonPath("$.data.total_count").value(0))
        }

        @Test
        @DisplayName("실패 - 잘못된 카테고리 -> 400")
        fun getPosts_invalidCategory() {
            // Given
            whenever(noticeService.getPosts(eq(1L), eq("INVALID"), eq(null), eq(1), eq(10)))
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
    }

    @Nested
    @DisplayName("GET /api/v1/notices/{noticeId} - 상세 조회")
    inner class GetNoticeDetailTests {

        @Test
        @DisplayName("성공 - 공지사항 상세 조회")
        fun getNoticeDetail_success() {
            // Given
            val response = NoticePostDetailResponse(
                id = 42L,
                category = "ALL",
                categoryName = "전체공지",
                title = "테스트 공지",
                content = "본문 내용입니다.",
                branch = null,
                branchCode = null,
                createdAt = "2026-02-28T10:30:00",
                images = listOf(
                    NoticeImageResponse(id = 101L, url = "https://bucket.s3.ap-northeast-2.amazonaws.com/img.jpg", sortOrder = 0)
                )
            )
            whenever(noticeService.getNoticeDetail(42L)).thenReturn(response)

            // When & Then
            mockMvc.perform(
                get("/api/v1/notices/42")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(42))
                .andExpect(jsonPath("$.data.category").value("ALL"))
                .andExpect(jsonPath("$.data.category_name").value("전체공지"))
                .andExpect(jsonPath("$.data.title").value("테스트 공지"))
                .andExpect(jsonPath("$.data.content").value("본문 내용입니다."))
                .andExpect(jsonPath("$.data.created_at").value("2026-02-28T10:30:00"))
                .andExpect(jsonPath("$.data.images").isArray)
                .andExpect(jsonPath("$.data.images[0].id").value(101))
                .andExpect(jsonPath("$.data.images[0].url").value("https://bucket.s3.ap-northeast-2.amazonaws.com/img.jpg"))
                .andExpect(jsonPath("$.data.images[0].sort_order").value(0))
        }

        @Test
        @DisplayName("성공 - 이미지 없는 공지 조회")
        fun getNoticeDetail_noImages() {
            // Given
            val response = NoticePostDetailResponse(
                id = 10L,
                category = "BRANCH",
                categoryName = "지점공지",
                title = "지점 안내",
                content = "지점 공지 본문",
                branch = "서울1지점",
                branchCode = "B001",
                createdAt = "2026-01-01T00:00:00",
                images = emptyList()
            )
            whenever(noticeService.getNoticeDetail(10L)).thenReturn(response)

            // When & Then
            mockMvc.perform(
                get("/api/v1/notices/10")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.images").isEmpty)
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 공지 ID -> 404")
        fun getNoticeDetail_notFound() {
            // Given
            whenever(noticeService.getNoticeDetail(999L))
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

        @Test
        @DisplayName("실패 - noticeId 0 이하 -> 400")
        fun getNoticeDetail_invalidId() {
            // When & Then
            mockMvc.perform(
                get("/api/v1/notices/0")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
        }

        @Test
        @DisplayName("실패 - 음수 noticeId -> 400")
        fun getNoticeDetail_negativeId() {
            // When & Then
            mockMvc.perform(
                get("/api/v1/notices/-1")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
        }
    }
}
