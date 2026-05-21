package com.otoki.powersales.admin.controller

import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.common.test.AdminControllerTestSupport
import com.otoki.powersales.notice.dto.request.NoticeCreateRequest
import com.otoki.powersales.notice.dto.request.NoticeUpdateRequest
import com.otoki.powersales.notice.dto.response.BranchOption
import com.otoki.powersales.notice.dto.response.CategoryOption
import com.otoki.powersales.notice.dto.response.NoticeFormMetaResponse
import com.otoki.powersales.notice.dto.response.NoticeMutationResponse
import com.otoki.powersales.notice.dto.response.NoticePostDetailResponse
import com.otoki.powersales.notice.dto.response.NoticeImageResponse
import com.otoki.powersales.notice.dto.response.NoticePostListResponse
import com.otoki.powersales.notice.dto.response.NoticePostSummaryResponse
import com.otoki.powersales.notice.exception.BranchRequiredException
import com.otoki.powersales.notice.exception.InvalidImageIdException
import com.otoki.powersales.notice.exception.InvalidNoticeCategoryException
import com.otoki.powersales.notice.exception.NoticePostNotFoundException
import org.springframework.mock.web.MockMultipartFile
import com.otoki.powersales.notice.service.NoticeService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import com.ninjasquad.springmockk.MockkBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdminNoticeController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminNoticeController 테스트")
class AdminNoticeControllerTest : AdminControllerTestSupport() {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var noticeService: NoticeService

    @Nested
    @DisplayName("GET /api/v1/admin/notices - Admin 목록 조회")
    inner class GetPosts {

        @Test
        @DisplayName("성공 - 전체 목록 조회")
        fun getPosts_success() {
            val response = NoticePostListResponse(
                content = listOf(
                    NoticePostSummaryResponse(1L, "COMPANY", "회사공지", "테스트 공지", java.time.LocalDateTime.parse("2026-03-04T10:00:00"))
                ),
                totalCount = 1,
                totalPages = 1,
                currentPage = 1,
                size = 10
            )
            every { noticeService.getPostsForAdmin(null, null, eq(1), eq(10)) } returns response

            mockMvc.perform(get("/api/v1/admin/notices"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].title").value("테스트 공지"))
                .andExpect(jsonPath("$.data.totalCount").value(1))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/notices/form-meta - 폼 메타데이터 조회")
    inner class GetNoticeFormMeta {

        @Test
        @DisplayName("성공 - 카테고리 + 지점 목록 반환")
        fun getNoticeFormMeta_success() {
            val response = NoticeFormMetaResponse(
                categories = listOf(
                    CategoryOption("COMPANY", "회사공지"),
                    CategoryOption("BRANCH", "지점공지"),
                    CategoryOption("EDUCATION", "교육")
                ),
                branches = listOf(
                    BranchOption("1101", "[제1사업부] 1영업부-서울1지점"),
                    BranchOption("1102", "[제1사업부] 1영업부-서울2지점")
                )
            )
            every { noticeService.getNoticeFormMeta() } returns response

            mockMvc.perform(get("/api/v1/admin/notices/form-meta"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.categories").isArray)
                .andExpect(jsonPath("$.data.categories[0].code").value("COMPANY"))
                .andExpect(jsonPath("$.data.categories[0].name").value("회사공지"))
                .andExpect(jsonPath("$.data.branches").isArray)
                .andExpect(jsonPath("$.data.branches[0].branchCode").value("1101"))
                .andExpect(jsonPath("$.data.branches[0].branchName").value("[제1사업부] 1영업부-서울1지점"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/notices/{noticeId} - Admin 상세 조회")
    inner class GetNoticeDetail {

        @Test
        @DisplayName("성공 - 공지 상세 반환")
        fun getNoticeDetail_success() {
            val response = NoticePostDetailResponse(
                id = 1L,
                category = "COMPANY",
                categoryName = "회사공지",
                title = "공지 제목",
                content = "<p>본문</p>",
                branch = null,
                branchCode = null,
                createdAt = java.time.LocalDateTime.parse("2026-03-04T10:00:00"),
                images = emptyList()
            )
            every { noticeService.getNoticeDetail(1L) } returns response

            mockMvc.perform(get("/api/v1/admin/notices/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.title").value("공지 제목"))
        }

        @Test
        @DisplayName("실패 - 음수 ID")
        fun getNoticeDetail_invalidId() {
            mockMvc.perform(get("/api/v1/admin/notices/-1"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/notices - 공지 작성")
    inner class CreateNotice {

        @Test
        @DisplayName("성공 - 회사공지 작성")
        fun createNotice_success() {
            val mutationResponse = NoticeMutationResponse(
                id = 100L,
                category = "COMPANY",
                categoryName = "회사공지",
                title = "새 공지",
                content = "<p>내용</p>",
                branch = null,
                branchCode = null,
                createdAt = java.time.LocalDateTime.parse("2026-03-04T10:00:00")
            )
            every { noticeService.createNotice(any(), eq(1L)) } returns mutationResponse

            val request = NoticeCreateRequest(
                title = "새 공지",
                category = "COMPANY",
                content = "<p>내용</p>"
            )

            mockMvc.perform(
                post("/api/v1/admin/notices")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(100))
                .andExpect(jsonPath("$.data.category").value("COMPANY"))
                .andExpect(jsonPath("$.data.categoryName").value("회사공지"))
        }

        @Test
        @DisplayName("실패 - 빈 제목")
        fun createNotice_emptyTitle() {
            val json = """{"title": "", "category": "COMPANY", "content": "<p>내용</p>"}"""

            mockMvc.perform(
                post("/api/v1/admin/notices")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
                .andExpect(status().isBadRequest)
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.otoki.powersales.admin.controller.AdminNoticeControllerTest#createNoticeExceptions")
        @DisplayName("실패 - 예외 → ErrorCode 매핑")
        fun createNotice_exceptions(
            @Suppress("UNUSED_PARAMETER") name: String,
            category: String,
            exception: Throwable,
            expectedCode: String
        ) {
            every { noticeService.createNotice(any(), eq(1L)) } throws exception

            val request = NoticeCreateRequest(
                title = "공지",
                category = category,
                content = "<p>내용</p>"
            )

            mockMvc.perform(
                post("/api/v1/admin/notices")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value(expectedCode))
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/notices/{noticeId} - 공지 수정")
    inner class UpdateNotice {

        @Test
        @DisplayName("성공 - 공지 수정")
        fun updateNotice_success() {
            val mutationResponse = NoticeMutationResponse(
                id = 10L,
                category = "COMPANY",
                categoryName = "회사공지",
                title = "수정된 제목",
                content = "<p>수정 내용</p>",
                branch = null,
                branchCode = null,
                createdAt = java.time.LocalDateTime.parse("2026-03-04T10:00:00")
            )
            every { noticeService.updateNotice(eq(10L), any()) } returns mutationResponse

            val request = NoticeUpdateRequest(
                title = "수정된 제목",
                category = "COMPANY",
                content = "<p>수정 내용</p>"
            )

            mockMvc.perform(
                put("/api/v1/admin/notices/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.title").value("수정된 제목"))
        }

        @Test
        @DisplayName("실패 - 미존재 공지")
        fun updateNotice_notFound() {
            every { noticeService.updateNotice(eq(999L), any()) } throws NoticePostNotFoundException()

            val request = NoticeUpdateRequest(
                title = "제목",
                category = "COMPANY",
                content = "내용"
            )

            mockMvc.perform(
                put("/api/v1/admin/notices/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("NOTICE_NOT_FOUND"))
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/admin/notices/{noticeId} - 공지 삭제")
    inner class DeleteNotice {

        @Test
        @DisplayName("성공 - 공지 삭제")
        fun deleteNotice_success() {
            every { noticeService.deleteNotice(any()) } just Runs

            mockMvc.perform(delete("/api/v1/admin/notices/10"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty)
        }

        @Test
        @DisplayName("실패 - 미존재 공지")
        fun deleteNotice_notFound() {
            every { noticeService.deleteNotice(999L) } throws NoticePostNotFoundException()

            mockMvc.perform(delete("/api/v1/admin/notices/999"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("NOTICE_NOT_FOUND"))
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/notices/{noticeId}/images - 첨부 업로드")
    inner class UploadNoticeImage {

        @Test
        @DisplayName("성공 - multipart 파일 업로드 + 201 + NoticeImageResponse 반환")
        fun uploadNoticeImage_success() {
            val response = NoticeImageResponse(
                id = 555L,
                url = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/uploads/notice/2026/05/11/abc.png",
                sortOrder = 0
            )
            every { noticeService.uploadNoticeImage(eq(42L), any()) } returns response

            val file = MockMultipartFile("image", "photo.png", "image/png", ByteArray(2048))

            mockMvc.perform(
                multipart("/api/v1/admin/notices/42/images").file(file)
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(555))
                .andExpect(jsonPath("$.data.url").value("https://test-bucket.s3.ap-northeast-2.amazonaws.com/uploads/notice/2026/05/11/abc.png"))
                .andExpect(jsonPath("$.data.sortOrder").value(0))
        }

        @Test
        @DisplayName("실패 - 미존재 공지")
        fun uploadNoticeImage_noticeNotFound() {
            every { noticeService.uploadNoticeImage(eq(999L), any()) } throws NoticePostNotFoundException()

            val file = MockMultipartFile("image", "photo.png", "image/png", ByteArray(100))

            mockMvc.perform(multipart("/api/v1/admin/notices/999/images").file(file))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("NOTICE_NOT_FOUND"))
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/admin/notices/{noticeId}/images/{imageId} - 첨부 삭제")
    inner class DeleteNoticeImage {

        @Test
        @DisplayName("성공 - 첨부 삭제 + 200 + 메시지 반환")
        fun deleteNoticeImage_success() {
            every { noticeService.deleteNoticeImage(any(), any()) } just Runs

            mockMvc.perform(delete("/api/v1/admin/notices/42/images/200"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("첨부 이미지가 삭제되었습니다"))
        }

        @Test
        @DisplayName("실패 - imageId 미존재 또는 parent 불일치")
        fun deleteNoticeImage_invalidImageId() {
            every { noticeService.deleteNoticeImage(42L, 999L) } throws InvalidImageIdException()

            mockMvc.perform(delete("/api/v1/admin/notices/42/images/999"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("INVALID_IMAGE_ID"))
        }
    }

    companion object {
        @JvmStatic
        fun createNoticeExceptions(): List<Arguments> = listOf(
            Arguments.of("branchRequired -> BRANCH_REQUIRED", "BRANCH", BranchRequiredException(), "BRANCH_REQUIRED"),
            Arguments.of("invalidCategory -> INVALID_CATEGORY", "UNKNOWN", InvalidNoticeCategoryException(), "INVALID_CATEGORY"),
        )
    }
}
