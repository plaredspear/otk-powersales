package com.otoki.internal.admin.controller

import com.otoki.internal.admin.security.AdminAuthorityFilter
import com.otoki.internal.sap.entity.UserRole
import com.otoki.internal.common.security.GpsConsentFilter
import com.otoki.internal.common.security.JwtAuthenticationFilter
import com.otoki.internal.common.security.JwtTokenProvider
import com.otoki.internal.common.security.UserPrincipal
import com.otoki.internal.education.dto.response.*
import com.otoki.internal.education.exception.EducationPostNotFoundException
import com.otoki.internal.education.exception.FileLimitExceededException
import com.otoki.internal.education.exception.InvalidEducationCategoryException
import com.otoki.internal.education.exception.InvalidEducationParameterException
import com.otoki.internal.education.service.EducationService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdminEducationController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminEducationController 테스트")
class AdminEducationControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var educationService: EducationService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockitoBean
    private lateinit var adminAuthorityFilter: AdminAuthorityFilter

    @MockitoBean
    private lateinit var gpsConsentFilter: GpsConsentFilter

    @BeforeEach
    fun setUp() {
        val principal = UserPrincipal(userId = 1L, role = UserRole.ADMIN)
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
    }

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
                        instDate = "2026-03-09T10:00:00",
                        attachmentCount = 3
                    )
                ),
                totalCount = 1,
                totalPages = 1,
                currentPage = 1,
                size = 10
            )

            whenever(educationService.getPostsForAdmin(isNull(), isNull(), eq(1), eq(10)))
                .thenReturn(response)

            mockMvc.perform(get("/api/v1/admin/education/posts"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].edu_id").value("edu20260309100000"))
                .andExpect(jsonPath("$.data.content[0].attachment_count").value(3))
                .andExpect(jsonPath("$.data.total_count").value(1))
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

            whenever(educationService.getPostsForAdmin(eq("c00001"), isNull(), eq(1), eq(10)))
                .thenReturn(response)

            mockMvc.perform(get("/api/v1/admin/education/posts").param("category", "c00001"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
        }

        @Test
        @DisplayName("실패 - 잘못된 카테고리")
        fun getPosts_invalidCategory() {
            whenever(educationService.getPostsForAdmin(eq("c99999"), isNull(), eq(1), eq(10)))
                .thenThrow(InvalidEducationCategoryException())

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
                createdAt = "2026-03-09T10:00:00",
                attachments = listOf(
                    EducationAttachmentResponse(
                        id = "abc123",
                        fileName = "교육자료.pdf",
                        fileUrl = "abc123",
                        fileSize = 0
                    )
                )
            )

            whenever(educationService.getPostDetail("edu20260309100000")).thenReturn(response)

            mockMvc.perform(get("/api/v1/admin/education/posts/edu20260309100000"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.id").value("edu20260309100000"))
                .andExpect(jsonPath("$.data.attachments[0].file_name").value("교육자료.pdf"))
        }

        @Test
        @DisplayName("실패 - 미존재 교육")
        fun getPostDetail_notFound() {
            whenever(educationService.getPostDetail("nonexistent"))
                .thenThrow(EducationPostNotFoundException())

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
                empCode = "12345678",
                instDate = "2026-03-09T10:00:00",
                updDate = null,
                attachments = emptyList()
            )

            whenever(educationService.createPost(eq(1L), eq("테스트 교육"), eq("내용"), eq("c00001"), isNull()))
                .thenReturn(response)

            mockMvc.perform(
                multipart("/api/v1/admin/education/posts")
                    .param("title", "테스트 교육")
                    .param("content", "내용")
                    .param("category", "c00001")
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.edu_id").value("edu20260309100000"))
                .andExpect(jsonPath("$.data.edu_title").value("테스트 교육"))
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
                empCode = "12345678",
                instDate = "2026-03-09T10:00:00",
                updDate = null,
                attachments = listOf(
                    AttachmentInfo(eduFileKey = "uuid.pdf", eduFileType = "f00003", eduFileOrgNm = "test.pdf")
                )
            )

            whenever(educationService.createPost(eq(1L), eq("테스트"), eq("내용"), eq("c00004"), any()))
                .thenReturn(response)

            mockMvc.perform(
                multipart("/api/v1/admin/education/posts")
                    .file(file)
                    .param("title", "테스트")
                    .param("content", "내용")
                    .param("category", "c00004")
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.data.attachments[0].edu_file_key").value("uuid.pdf"))
        }

        @Test
        @DisplayName("실패 - 빈 제목")
        fun createPost_emptyTitle() {
            whenever(educationService.createPost(eq(1L), eq(""), eq("내용"), eq("c00001"), isNull()))
                .thenThrow(InvalidEducationParameterException("제목은 1~150자여야 합니다"))

            mockMvc.perform(
                multipart("/api/v1/admin/education/posts")
                    .param("title", "")
                    .param("content", "내용")
                    .param("category", "c00001")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
        }

        @Test
        @DisplayName("실패 - 파일 수 초과")
        fun createPost_fileLimitExceeded() {
            whenever(educationService.createPost(eq(1L), eq("제목"), eq("내용"), eq("c00001"), isNull()))
                .thenThrow(FileLimitExceededException())

            mockMvc.perform(
                multipart("/api/v1/admin/education/posts")
                    .param("title", "제목")
                    .param("content", "내용")
                    .param("category", "c00001")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("FILE_LIMIT_EXCEEDED"))
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
                empCode = "12345678",
                instDate = "2026-03-09T10:00:00",
                updDate = "2026-03-09T14:00:00",
                attachments = emptyList()
            )

            whenever(educationService.updatePost(eq("edu001"), eq("수정됨"), eq("수정 내용"), eq("c00001"), isNull(), isNull()))
                .thenReturn(response)

            mockMvc.perform(
                multipart("/api/v1/admin/education/posts/edu001")
                    .with { it.method = "PUT"; it }
                    .param("title", "수정됨")
                    .param("content", "수정 내용")
                    .param("category", "c00001")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.edu_title").value("수정됨"))
                .andExpect(jsonPath("$.data.upd_date").value("2026-03-09T14:00:00"))
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/admin/education/posts/{postId} - 교육 삭제")
    inner class DeletePost {

        @Test
        @DisplayName("성공 - 교육 삭제")
        fun deletePost_success() {
            doNothing().whenever(educationService).deletePost("edu001")

            mockMvc.perform(delete("/api/v1/admin/education/posts/edu001"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
        }

        @Test
        @DisplayName("실패 - 미존재 교육")
        fun deletePost_notFound() {
            whenever(educationService.deletePost("nonexistent"))
                .thenThrow(EducationPostNotFoundException())

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

            whenever(educationService.getCategories()).thenReturn(categories)

            mockMvc.perform(get("/api/v1/admin/education/categories"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data[0].edu_code").value("c00001"))
                .andExpect(jsonPath("$.data[0].edu_code_nm").value("시식매뉴얼"))
                .andExpect(jsonPath("$.data[1].edu_code").value("c00002"))
        }
    }
}
