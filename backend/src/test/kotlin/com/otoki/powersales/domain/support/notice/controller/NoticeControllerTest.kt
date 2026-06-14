package com.otoki.powersales.domain.support.notice.controller

import com.ninjasquad.springmockk.MockkBean
import com.otoki.powersales.platform.common.test.MobileControllerTestSupport
import com.otoki.powersales.domain.support.notice.dto.response.NoticeImageResponse
import com.otoki.powersales.domain.support.notice.dto.response.NoticePostDetailResponse
import com.otoki.powersales.domain.support.notice.dto.response.NoticePostListResponse
import com.otoki.powersales.domain.support.notice.dto.response.NoticePostSummaryResponse
import com.otoki.powersales.domain.support.notice.exception.InvalidNoticeCategoryException
import com.otoki.powersales.domain.support.notice.exception.NoticePostNotFoundException
import com.otoki.powersales.domain.support.notice.service.NoticeService
import io.mockk.every
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(NoticeController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("NoticeController 테스트")
class NoticeControllerTest : MobileControllerTestSupport() {

    @MockkBean private lateinit var noticeService: NoticeService

    @Nested
    @DisplayName("GET /api/v1/mobile/notices - 목록 조회")
    inner class GetPostsTests {

        @Test
        @DisplayName("성공 - 전체 목록 조회")
        fun getPosts_success() {
            val response = NoticePostListResponse(
                content = listOf(
                    NoticePostSummaryResponse(id = 42L, category = "COMPANY", categoryName = "회사공지", scope = "영업사원", title = "전체공지 제목", branch = null, department = "판매전략실", authorName = "판매전략실", createdAt = LocalDateTime.parse("2026-02-28T10:30:00")),
                    NoticePostSummaryResponse(id = 41L, category = "BRANCH", categoryName = "지점공지", scope = "영업사원", title = "지점공지 제목", branch = "[제1사업부] 1영업부-강서1지점", department = "강서1지점", authorName = "홍길동", createdAt = LocalDateTime.parse("2026-02-27T09:00:00"))
                ),
                totalCount = 5,
                totalPages = 1,
                currentPage = 1,
                size = 10
            )
            every { noticeService.getPosts(1L, null, null, 1, 10) } returns response

            mockMvc.perform(
                get("/api/v1/mobile/notices").contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.totalCount").value(5))
        }

        @Test
        @DisplayName("성공 - 카테고리 + 검색 + 페이지네이션")
        fun getPosts_withParams() {
            val response = NoticePostListResponse(
                content = listOf(
                    NoticePostSummaryResponse(id = 1L, category = "COMPANY", categoryName = "회사공지", scope = "영업사원", title = "영업 목표", branch = null, department = "판매전략실", authorName = "판매전략실", createdAt = LocalDateTime.parse("2026-02-28T10:30:00"))
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
                .andExpect(jsonPath("$.data.content[0].title").value("영업 목표"))
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
                get("/api/v1/mobile/notices").contentType(MediaType.APPLICATION_JSON)
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
                .andExpect(jsonPath("$.error.code").value("INVALID_CATEGORY"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/mobile/notices/{noticeId} - 상세 조회")
    inner class GetNoticeDetailTests {

        @Test
        @DisplayName("성공 - 공지사항 상세 조회 (이미지 보유 분기)")
        fun getNoticeDetail_success() {
            // 분기 명세: images 배열 보유 시 url + sortOrder 매핑
            val response = NoticePostDetailResponse(
                id = 42L,
                scope = "영업사원",
                category = "COMPANY",
                categoryName = "회사공지",
                title = "테스트 공지",
                content = "본문 내용입니다.",
                branch = null,
                branchCode = null,
                createdAt = LocalDateTime.parse("2026-02-28T10:30:00"),
                images = listOf(
                    NoticeImageResponse(id = 101L, url = "https://bucket.s3.ap-northeast-2.amazonaws.com/img.jpg", sortOrder = 0)
                )
            )
            every { noticeService.getNoticeDetail(42L) } returns response

            mockMvc.perform(
                get("/api/v1/mobile/notices/42").contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.id").value(42))
                .andExpect(jsonPath("$.data.images[0].url").value("https://bucket.s3.ap-northeast-2.amazonaws.com/img.jpg"))
        }

        @Test
        @DisplayName("성공 - 이미지 없는 공지 조회 (images 빈 배열 분기)")
        fun getNoticeDetail_noImages() {
            val response = NoticePostDetailResponse(
                id = 10L,
                scope = "현장여사원",
                category = "BRANCH",
                categoryName = "지점공지",
                title = "지점 안내",
                content = "지점 공지 본문",
                branch = "서울1지점",
                branchCode = "B001",
                createdAt = LocalDateTime.parse("2026-01-01T00:00:00"),
                images = emptyList()
            )
            every { noticeService.getNoticeDetail(10L) } returns response

            mockMvc.perform(
                get("/api/v1/mobile/notices/10").contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.images").isEmpty)
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 공지 ID -> 404")
        fun getNoticeDetail_notFound() {
            every { noticeService.getNoticeDetail(999L) } throws NoticePostNotFoundException()

            mockMvc.perform(
                get("/api/v1/mobile/notices/999").contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("NOTICE_NOT_FOUND"))
        }

        @ParameterizedTest(name = "noticeId = {0} -> 400 INVALID_PARAMETER")
        @ValueSource(strings = ["0", "-1"])
        @DisplayName("실패 - 잘못된 noticeId (0 이하/음수) -> 400")
        fun getNoticeDetail_invalidIds(invalidId: String) {
            mockMvc.perform(
                get("/api/v1/mobile/notices/$invalidId").contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
        }
    }
}
