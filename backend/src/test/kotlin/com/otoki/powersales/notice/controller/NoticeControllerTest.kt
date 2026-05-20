package com.otoki.powersales.notice.controller

import com.ninjasquad.springmockk.MockkBean
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.common.security.UserPrincipal
import com.otoki.powersales.notice.dto.response.NoticeImageResponse
import com.otoki.powersales.notice.dto.response.NoticePostDetailResponse
import com.otoki.powersales.notice.dto.response.NoticePostListResponse
import com.otoki.powersales.notice.dto.response.NoticePostSummaryResponse
import com.otoki.powersales.notice.exception.InvalidNoticeCategoryException
import com.otoki.powersales.notice.exception.NoticePostNotFoundException
import com.otoki.powersales.notice.service.NoticeService
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
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

    @MockkBean
    private lateinit var noticeService: NoticeService

    @MockkBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockkBean
    private lateinit var sapInboundAuditService: SapInboundAuditService

    @MockkBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockkBean
    private lateinit var gpsConsentFilter: GpsConsentFilter

    private val testPrincipal = UserPrincipal(userId = 1L, role = UserRole.WOMAN)

    @BeforeEach
    fun setUp() {
        val authentication = UsernamePasswordAuthenticationToken(
            testPrincipal, null, testPrincipal.authorities
        )
        SecurityContextHolder.getContext().authentication = authentication
    }

    @Nested
    @DisplayName("GET /api/v1/mobile/notices - 목록 조회")
    inner class GetPostsTests {

        @Test
        @DisplayName("성공 - 전체 목록 조회")
        fun getPosts_success() {
            val response = NoticePostListResponse(
                content = listOf(
                    NoticePostSummaryResponse(id = 42L, category = "COMPANY", categoryName = "회사공지", title = "전체공지 제목", createdAt = java.time.LocalDateTime.parse("2026-02-28T10:30:00")),
                    NoticePostSummaryResponse(id = 41L, category = "BRANCH", categoryName = "지점공지", title = "지점공지 제목", createdAt = java.time.LocalDateTime.parse("2026-02-27T09:00:00"))
                ),
                totalCount = 5,
                totalPages = 1,
                currentPage = 1,
                size = 10
            )
            every { noticeService.getPosts(1L, null, null, 1, 10) } returns response

            mockMvc.perform(
                get("/api/v1/mobile/notices")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray)
                .andExpect(jsonPath("$.data.content[0].id").value(42))
                .andExpect(jsonPath("$.data.content[0].category").value("COMPANY"))
                .andExpect(jsonPath("$.data.content[0].categoryName").value("회사공지"))
                .andExpect(jsonPath("$.data.content[0].title").value("전체공지 제목"))
                .andExpect(jsonPath("$.data.content[0].createdAt").value("2026-02-28T10:30:00"))
                .andExpect(jsonPath("$.data.content[1].id").value(41))
                .andExpect(jsonPath("$.data.totalCount").value(5))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.currentPage").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
        }

        @Test
        @DisplayName("성공 - 카테고리 + 검색 + 페이지네이션")
        fun getPosts_withParams() {
            val response = NoticePostListResponse(
                content = listOf(
                    NoticePostSummaryResponse(id = 1L, category = "COMPANY", categoryName = "회사공지", title = "영업 목표", createdAt = java.time.LocalDateTime.parse("2026-02-28T10:30:00"))
                ),
                totalCount = 1,
                totalPages = 1,
                currentPage = 1,
                size = 5
            )
            every { noticeService.getPosts(1L, "COMPANY", "영업", 1, 5) } returns response

            mockMvc.perform(
                get("/api/v1/mobile/notices")
                    .param("category", "COMPANY")
                    .param("search", "영업")
                    .param("page", "1")
                    .param("size", "5")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.content").isArray)
                .andExpect(jsonPath("$.data.content[0].title").value("영업 목표"))
                .andExpect(jsonPath("$.data.totalCount").value(1))
        }

        @Test
        @DisplayName("성공 - 빈 결과")
        fun getPosts_emptyResult() {
            val response = NoticePostListResponse(
                content = emptyList(),
                totalCount = 0,
                totalPages = 0,
                currentPage = 1,
                size = 10
            )
            every { noticeService.getPosts(1L, null, null, 1, 10) } returns response

            mockMvc.perform(
                get("/api/v1/mobile/notices")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.content").isEmpty)
                .andExpect(jsonPath("$.data.totalCount").value(0))
        }

        @Test
        @DisplayName("실패 - 잘못된 카테고리 -> 400")
        fun getPosts_invalidCategory() {
            every {
                noticeService.getPosts(1L, "INVALID", null, 1, 10)
            } throws InvalidNoticeCategoryException()

            mockMvc.perform(
                get("/api/v1/mobile/notices")
                    .param("category", "INVALID")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_CATEGORY"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/mobile/notices/{noticeId} - 상세 조회")
    inner class GetNoticeDetailTests {

        @Test
        @DisplayName("성공 - 공지사항 상세 조회")
        fun getNoticeDetail_success() {
            val response = NoticePostDetailResponse(
                id = 42L,
                category = "COMPANY",
                categoryName = "회사공지",
                title = "테스트 공지",
                content = "본문 내용입니다.",
                branch = null,
                branchCode = null,
                createdAt = java.time.LocalDateTime.parse("2026-02-28T10:30:00"),
                images = listOf(
                    NoticeImageResponse(id = 101L, url = "https://bucket.s3.ap-northeast-2.amazonaws.com/img.jpg", sortOrder = 0)
                )
            )
            every { noticeService.getNoticeDetail(42L) } returns response

            mockMvc.perform(
                get("/api/v1/mobile/notices/42")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(42))
                .andExpect(jsonPath("$.data.category").value("COMPANY"))
                .andExpect(jsonPath("$.data.categoryName").value("회사공지"))
                .andExpect(jsonPath("$.data.title").value("테스트 공지"))
                .andExpect(jsonPath("$.data.content").value("본문 내용입니다."))
                .andExpect(jsonPath("$.data.createdAt").value("2026-02-28T10:30:00"))
                .andExpect(jsonPath("$.data.images").isArray)
                .andExpect(jsonPath("$.data.images[0].id").value(101))
                .andExpect(jsonPath("$.data.images[0].url").value("https://bucket.s3.ap-northeast-2.amazonaws.com/img.jpg"))
                .andExpect(jsonPath("$.data.images[0].sortOrder").value(0))
        }

        @Test
        @DisplayName("성공 - 이미지 없는 공지 조회")
        fun getNoticeDetail_noImages() {
            val response = NoticePostDetailResponse(
                id = 10L,
                category = "BRANCH",
                categoryName = "지점공지",
                title = "지점 안내",
                content = "지점 공지 본문",
                branch = "서울1지점",
                branchCode = "B001",
                createdAt = java.time.LocalDateTime.parse("2026-01-01T00:00:00"),
                images = emptyList()
            )
            every { noticeService.getNoticeDetail(10L) } returns response

            mockMvc.perform(
                get("/api/v1/mobile/notices/10")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.images").isEmpty)
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 공지 ID -> 404")
        fun getNoticeDetail_notFound() {
            every { noticeService.getNoticeDetail(999L) } throws NoticePostNotFoundException()

            mockMvc.perform(
                get("/api/v1/mobile/notices/999")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("NOTICE_NOT_FOUND"))
        }

        @Test
        @DisplayName("실패 - noticeId 0 이하 -> 400")
        fun getNoticeDetail_invalidId() {
            mockMvc.perform(
                get("/api/v1/mobile/notices/0")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
        }

        @Test
        @DisplayName("실패 - 음수 noticeId -> 400")
        fun getNoticeDetail_negativeId() {
            mockMvc.perform(
                get("/api/v1/mobile/notices/-1")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
        }
    }
}
