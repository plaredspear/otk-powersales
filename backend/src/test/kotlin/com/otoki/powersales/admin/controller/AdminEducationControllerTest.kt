package com.otoki.powersales.admin.controller

import com.otoki.powersales.common.test.AdminControllerTestSupport
import com.otoki.powersales.domain.support.education.exception.EducationPostNotFoundException
import com.otoki.powersales.domain.support.education.exception.FileLimitExceededException
import com.otoki.powersales.domain.support.education.exception.InvalidEducationCategoryException
import com.otoki.powersales.domain.support.education.exception.InvalidEducationParameterException
import com.otoki.powersales.domain.support.education.service.EducationService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import io.mockk.every
import io.mockk.just
import io.mockk.Runs
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.mock.web.MockMultipartFile
import com.ninjasquad.springmockk.MockkBean
import com.otoki.powersales.domain.support.education.dto.response.AdminEducationListResponse
import com.otoki.powersales.domain.support.education.dto.response.AdminEducationPostSummary
import com.otoki.powersales.domain.support.education.dto.response.AttachmentInfo
import com.otoki.powersales.domain.support.education.dto.response.EducationAttachmentResponse
import com.otoki.powersales.domain.support.education.dto.response.EducationCategoryResponse
import com.otoki.powersales.domain.support.education.dto.response.EducationMutationResponse
import com.otoki.powersales.domain.support.education.dto.response.EducationPostDetailResponse
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(AdminEducationController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminEducationController 테스트")
class AdminEducationControllerTest : AdminControllerTestSupport() {

    @MockkBean
    private lateinit var educationService: EducationService

    @Nested
    @DisplayName("GET /api/v1/admin/education/posts - Admin 목록 조회")
    inner class GetPosts {

        @Test
        @DisplayName("성공 - 전체 카테고리 목록 조회")
        fun getPosts_success() {
            val response = AdminEducationListResponse(
                content = listOf(
                    AdminEducationPostSummary(
                        eduId = "edu20260309100000",
                        eduTitle = "3월 신제품 교육",
                        eduCode = "c00004",
                        eduCodeNm = "신제품소개",
                        instDate = LocalDateTime.parse("2026-03-09T10:00:00"),
                        attachmentCount = 3
                    )
                ),
                totalCount = 1,
                totalPages = 1,
                currentPage = 1,
                size = 10
            )

            every { educationService.getPostsForAdmin(null, null, eq(1), eq(10)) } returns response

            mockMvc.perform(get("/api/v1/admin/education/posts"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].eduId").value("edu20260309100000"))
                .andExpect(jsonPath("$.data.content[0].attachmentCount").value(3))
                .andExpect(jsonPath("$.data.totalCount").value(1))
        }

        @Test
        @DisplayName("성공 - 카테고리 필터 목록 조회")
        fun getPosts_withCategory() {
            val response = AdminEducationListResponse(
                content = emptyList(),
                totalCount = 0,
                totalPages = 0,
                currentPage = 1,
                size = 10
            )

            every { educationService.getPostsForAdmin(eq("c00001"), null, eq(1), eq(10)) } returns response

            mockMvc.perform(get("/api/v1/admin/education/posts").param("category", "c00001"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
        }

        @Test
        @DisplayName("실패 - 잘못된 카테고리")
        fun getPosts_invalidCategory() {
            every { educationService.getPostsForAdmin(eq("c99999"), null, eq(1), eq(10)) } throws InvalidEducationCategoryException()

            mockMvc.perform(get("/api/v1/admin/education/posts").param("category", "c99999"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("INVALID_CATEGORY"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/education/posts/{postId} - Admin 상세 조회")
    inner class GetPostDetail {

        @Test
        @DisplayName("성공 - 교육 상세 조회")
        fun getPostDetail_success() {
            val response = EducationPostDetailResponse(
                id = "edu20260309100000",
                category = "c00004",
                categoryName = "신제품소개",
                title = "3월 신제품 교육",
                content = "<p>내용</p>",
                createdAt = LocalDateTime.parse("2026-03-09T10:00:00"),
                attachments = listOf(
                    EducationAttachmentResponse(
                        id = "abc123",
                        fileName = "교육자료.pdf",
                        fileUrl = "abc123",
                        fileSize = 0
                    )
                )
            )

            every { educationService.getPostDetail("edu20260309100000") } returns response

            mockMvc.perform(get("/api/v1/admin/education/posts/edu20260309100000"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.id").value("edu20260309100000"))
                .andExpect(jsonPath("$.data.attachments[0].fileName").value("교육자료.pdf"))
        }

        @Test
        @DisplayName("실패 - 미존재 교육")
        fun getPostDetail_notFound() {
            every { educationService.getPostDetail("nonexistent") } throws EducationPostNotFoundException()

            mockMvc.perform(get("/api/v1/admin/education/posts/nonexistent"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("POST_NOT_FOUND"))
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/education/posts - 교육 작성")
    inner class CreatePost {

        @Test
        @DisplayName("성공 - 파일 없이 교육 작성")
        fun createPost_success() {
            val response = EducationMutationResponse(
                eduId = "edu20260309100000",
                eduTitle = "테스트 교육",
                eduContent = "내용",
                eduCode = "c00001",
                eduCodeNm = "시식매뉴얼",
                employeeId = 1L,
                instDate = LocalDateTime.parse("2026-03-09T10:00:00"),
                updDate = null,
                attachments = emptyList()
            )

            every { educationService.createPost(eq(1L), eq("테스트 교육"), eq("내용"), eq("c00001"), null) } returns response

            mockMvc.perform(
                multipart("/api/v1/admin/education/posts")
                    .param("title", "테스트 교육")
                    .param("content", "내용")
                    .param("category", "c00001")
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eduId").value("edu20260309100000"))
                .andExpect(jsonPath("$.data.eduTitle").value("테스트 교육"))
        }

        @Test
        @DisplayName("성공 - 파일 포함 교육 작성")
        fun createPost_withFiles() {
            val file = MockMultipartFile("files", "test.pdf", "application/pdf", ByteArray(100))
            val response = EducationMutationResponse(
                eduId = "edu20260309100000",
                eduTitle = "테스트",
                eduContent = "내용",
                eduCode = "c00004",
                eduCodeNm = "신제품소개",
                employeeId = 1L,
                instDate = LocalDateTime.parse("2026-03-09T10:00:00"),
                updDate = null,
                attachments = listOf(
                    AttachmentInfo(fileKey = "uuid.pdf", fileType = "f00003", fileOriginalName = "test.pdf")
                )
            )

            every { educationService.createPost(eq(1L), eq("테스트"), eq("내용"), eq("c00004"), any()) } returns response

            mockMvc.perform(
                multipart("/api/v1/admin/education/posts")
                    .file(file)
                    .param("title", "테스트")
                    .param("content", "내용")
                    .param("category", "c00004")
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.data.attachments[0].fileKey").value("uuid.pdf"))
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.otoki.powersales.admin.controller.AdminEducationControllerTest#createPostExceptions")
        @DisplayName("실패 - 예외 → ErrorCode 매핑")
        fun createPost_exceptions(
            @Suppress("UNUSED_PARAMETER") name: String,
            title: String,
            exception: Throwable,
            expectedCode: String
        ) {
            every { educationService.createPost(eq(1L), eq(title), eq("내용"), eq("c00001"), null) } throws exception

            mockMvc.perform(
                multipart("/api/v1/admin/education/posts")
                    .param("title", title)
                    .param("content", "내용")
                    .param("category", "c00001")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value(expectedCode))
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/education/posts/{postId} - 교육 수정")
    inner class UpdatePost {

        @Test
        @DisplayName("성공 - 교육 수정")
        fun updatePost_success() {
            val response = EducationMutationResponse(
                eduId = "edu001",
                eduTitle = "수정됨",
                eduContent = "수정 내용",
                eduCode = "c00001",
                eduCodeNm = "시식매뉴얼",
                employeeId = 1L,
                instDate = LocalDateTime.parse("2026-03-09T10:00:00"),
                updDate = LocalDateTime.parse("2026-03-09T14:00:00"),
                attachments = emptyList()
            )

            every { educationService.updatePost(eq("edu001"), eq("수정됨"), eq("수정 내용"), eq("c00001"), null, null) } returns response

            mockMvc.perform(
                multipart("/api/v1/admin/education/posts/edu001")
                    .with { it.method = "PUT"; it }
                    .param("title", "수정됨")
                    .param("content", "수정 내용")
                    .param("category", "c00001")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.eduTitle").value("수정됨"))
                .andExpect(jsonPath("$.data.updDate").value("2026-03-09T14:00:00"))
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/admin/education/posts/{postId} - 교육 삭제")
    inner class DeletePost {

        @Test
        @DisplayName("성공 - 교육 삭제")
        fun deletePost_success() {
            every { educationService.deletePost("edu001") } just Runs

            mockMvc.perform(delete("/api/v1/admin/education/posts/edu001"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
        }

        @Test
        @DisplayName("실패 - 미존재 교육")
        fun deletePost_notFound() {
            every { educationService.deletePost("nonexistent") } throws EducationPostNotFoundException()

            mockMvc.perform(delete("/api/v1/admin/education/posts/nonexistent"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("POST_NOT_FOUND"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/education/categories - 카테고리 조회")
    inner class GetCategories {

        @Test
        @DisplayName("성공 - 카테고리 목록 반환")
        fun getCategories_success() {
            val categories = listOf(
                EducationCategoryResponse(eduCode = "c00001", eduCodeNm = "시식매뉴얼"),
                EducationCategoryResponse(eduCode = "c00002", eduCodeNm = "CS/안전")
            )

            every { educationService.getCategories() } returns categories

            mockMvc.perform(get("/api/v1/admin/education/categories"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data[0].eduCode").value("c00001"))
                .andExpect(jsonPath("$.data[0].eduCodeNm").value("시식매뉴얼"))
                .andExpect(jsonPath("$.data[1].eduCode").value("c00002"))
        }
    }

    companion object {
        @JvmStatic
        fun createPostExceptions(): List<Arguments> = listOf(
            Arguments.of(
                "emptyTitle -> INVALID_PARAMETER",
                "",
                InvalidEducationParameterException("제목은 1~150자여야 합니다"),
                "INVALID_PARAMETER",
            ),
            Arguments.of("fileLimitExceeded -> FILE_LIMIT_EXCEEDED", "제목", FileLimitExceededException(), "FILE_LIMIT_EXCEEDED"),
        )
    }
}
