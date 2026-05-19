package com.otoki.powersales.admin.controller

import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.user.entity.ProfileType
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdminNoticeController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminNoticeController 테스트")
class AdminNoticeControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var noticeService: NoticeService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var sapInboundAuditService: SapInboundAuditService

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter


    @MockitoBean
    private lateinit var gpsConsentFilter: GpsConsentFilter

    @BeforeEach
    fun setUp() {
        val principal = WebUserPrincipal(
            userId = 100L,
            usernameValue = "test@otokims.co.kr",
            employeeCode = "S001",
            employeeId = 1L,
            role = UserRole.BRANCH_MANAGER,
            costCenterCode = null,
            profileType = ProfileType.STAFF,
            isSalesSupport = false,
            passwordChangeRequired = false,
            permissions = emptySet(),
            encodedPassword = "",
            grantedAuthorities = emptyList(),
            active = true
        )
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
    }

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
            whenever(noticeService.getPostsForAdmin(eq(null), eq(null), eq(1), eq(10))).thenReturn(response)

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
            whenever(noticeService.getNoticeFormMeta()).thenReturn(response)

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
            whenever(noticeService.getNoticeDetail(1L)).thenReturn(response)

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
            whenever(noticeService.createNotice(any(), eq(1L))).thenReturn(mutationResponse)

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

        @Test
        @DisplayName("실패 - 지점 누락")
        fun createNotice_branchRequired() {
            whenever(noticeService.createNotice(any(), eq(1L))).thenThrow(BranchRequiredException())

            val request = NoticeCreateRequest(
                title = "지점 공지",
                category = "BRANCH",
                content = "<p>내용</p>"
            )

            mockMvc.perform(
                post("/api/v1/admin/notices")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("BRANCH_REQUIRED"))
        }

        @Test
        @DisplayName("실패 - 잘못된 카테고리")
        fun createNotice_invalidCategory() {
            whenever(noticeService.createNotice(any(), eq(1L))).thenThrow(InvalidNoticeCategoryException())

            val request = NoticeCreateRequest(
                title = "공지",
                category = "UNKNOWN",
                content = "<p>내용</p>"
            )

            mockMvc.perform(
                post("/api/v1/admin/notices")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("INVALID_CATEGORY"))
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
            whenever(noticeService.updateNotice(eq(10L), any())).thenReturn(mutationResponse)

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
            whenever(noticeService.updateNotice(eq(999L), any())).thenThrow(NoticePostNotFoundException())

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
            mockMvc.perform(delete("/api/v1/admin/notices/10"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty)
        }

        @Test
        @DisplayName("실패 - 미존재 공지")
        fun deleteNotice_notFound() {
            whenever(noticeService.deleteNotice(999L)).thenThrow(NoticePostNotFoundException())

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
            whenever(noticeService.uploadNoticeImage(eq(42L), any())).thenReturn(response)

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
            whenever(noticeService.uploadNoticeImage(eq(999L), any())).thenThrow(NoticePostNotFoundException())

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
            mockMvc.perform(delete("/api/v1/admin/notices/42/images/200"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("첨부 이미지가 삭제되었습니다"))
        }

        @Test
        @DisplayName("실패 - imageId 미존재 또는 parent 불일치")
        fun deleteNoticeImage_invalidImageId() {
            whenever(noticeService.deleteNoticeImage(42L, 999L)).thenThrow(InvalidImageIdException())

            mockMvc.perform(delete("/api/v1/admin/notices/42/images/999"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("INVALID_IMAGE_ID"))
        }
    }
}
