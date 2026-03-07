package com.otoki.internal.notice.service

import com.otoki.internal.sap.entity.User
import com.otoki.internal.sap.repository.UserRepository
import com.otoki.internal.notice.dto.request.NoticeCreateRequest
import com.otoki.internal.notice.dto.request.NoticeUpdateRequest
import com.otoki.internal.notice.entity.Notice
import com.otoki.internal.notice.entity.NoticeCategory
import com.otoki.internal.notice.entity.UploadFile
import com.otoki.internal.notice.exception.BranchRequiredException
import com.otoki.internal.notice.exception.InvalidNoticeCategoryException
import com.otoki.internal.notice.exception.InvalidNoticeIdException
import com.otoki.internal.notice.exception.NoticePostNotFoundException
import com.otoki.internal.notice.repository.NoticeRepository
import com.otoki.internal.notice.repository.UploadFileRepository
import com.otoki.internal.sap.entity.Org
import com.otoki.internal.sap.repository.OrgRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("NoticeService 테스트")
class NoticeServiceTest {

    @Mock
    private lateinit var noticeRepository: NoticeRepository

    @Mock
    private lateinit var uploadFileRepository: UploadFileRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var orgRepository: OrgRepository

    private lateinit var noticeService: NoticeService

    @BeforeEach
    fun setUp() {
        noticeService = NoticeService(noticeRepository, uploadFileRepository, userRepository, orgRepository, "test-bucket")
    }

    @Nested
    @DisplayName("getNoticeDetail - 공지사항 상세 조회")
    inner class GetNoticeDetailTests {

        @Test
        @DisplayName("정상 조회 - 이미지 포함 공지 -> 상세 정보 반환")
        fun getNoticeDetail_withImages() {
            // Given
            val notice = createNotice(
                id = 42L,
                sfid = "a0B5g000001XXXAAA",
                name = "2026년 상반기 영업 목표 안내",
                category = NoticeCategory.COMPANY,
                contents = "상반기 영업 목표가 확정되었습니다.",
                createdDate = LocalDateTime.of(2026, 2, 28, 10, 30, 0)
            )
            val files = listOf(
                createUploadFile(id = 101L, uniqueKey = "notices/img1.jpg", createdDate = LocalDateTime.of(2026, 2, 28, 10, 30, 0)),
                createUploadFile(id = 102L, uniqueKey = "notices/img2.jpg", createdDate = LocalDateTime.of(2026, 2, 28, 11, 0, 0))
            )

            whenever(noticeRepository.findById(42L)).thenReturn(Optional.of(notice))
            whenever(uploadFileRepository.findByRecordIdAndIsDeletedFalse("a0B5g000001XXXAAA")).thenReturn(files)

            // When
            val result = noticeService.getNoticeDetail(42L)

            // Then
            assertThat(result.id).isEqualTo(42L)
            assertThat(result.category).isEqualTo("COMPANY")
            assertThat(result.categoryName).isEqualTo("회사공지")
            assertThat(result.title).isEqualTo("2026년 상반기 영업 목표 안내")
            assertThat(result.content).isEqualTo("상반기 영업 목표가 확정되었습니다.")
            assertThat(result.createdAt).isEqualTo("2026-02-28T10:30:00")
            assertThat(result.images).hasSize(2)
            assertThat(result.images[0].id).isEqualTo(101L)
            assertThat(result.images[0].url).isEqualTo("https://test-bucket.s3.ap-northeast-2.amazonaws.com/notices/img1.jpg")
            assertThat(result.images[0].sortOrder).isEqualTo(0)
            assertThat(result.images[1].sortOrder).isEqualTo(1)
        }

        @Test
        @DisplayName("이미지 없음 - sfid null -> images 빈 배열 반환")
        fun getNoticeDetail_noSfid() {
            // Given
            val notice = createNotice(id = 10L, sfid = null, name = "공지", category = NoticeCategory.BRANCH)

            whenever(noticeRepository.findById(10L)).thenReturn(Optional.of(notice))

            // When
            val result = noticeService.getNoticeDetail(10L)

            // Then
            assertThat(result.id).isEqualTo(10L)
            assertThat(result.category).isEqualTo("BRANCH")
            assertThat(result.categoryName).isEqualTo("지점공지")
            assertThat(result.images).isEmpty()
        }

        @Test
        @DisplayName("이미지 없음 - sfid에 매칭 파일 없음 -> images 빈 배열 반환")
        fun getNoticeDetail_noMatchingFiles() {
            // Given
            val notice = createNotice(id = 10L, sfid = "some-sfid", name = "공지", category = NoticeCategory.COMPANY)

            whenever(noticeRepository.findById(10L)).thenReturn(Optional.of(notice))
            whenever(uploadFileRepository.findByRecordIdAndIsDeletedFalse("some-sfid")).thenReturn(emptyList())

            // When
            val result = noticeService.getNoticeDetail(10L)

            // Then
            assertThat(result.images).isEmpty()
        }

        @Test
        @DisplayName("이미지 URL 무효 - uniqueKey null/빈 문자열인 이미지 제외")
        fun getNoticeDetail_filterInvalidImages() {
            // Given
            val notice = createNotice(id = 10L, sfid = "sfid-1", category = NoticeCategory.COMPANY)
            val files = listOf(
                createUploadFile(id = 1L, uniqueKey = "valid/img.jpg", createdDate = LocalDateTime.of(2026, 1, 1, 0, 0)),
                createUploadFile(id = 2L, uniqueKey = null, createdDate = LocalDateTime.of(2026, 1, 2, 0, 0)),
                createUploadFile(id = 3L, uniqueKey = "", createdDate = LocalDateTime.of(2026, 1, 3, 0, 0)),
                createUploadFile(id = 4L, uniqueKey = "valid/img2.jpg", createdDate = LocalDateTime.of(2026, 1, 4, 0, 0))
            )

            whenever(noticeRepository.findById(10L)).thenReturn(Optional.of(notice))
            whenever(uploadFileRepository.findByRecordIdAndIsDeletedFalse("sfid-1")).thenReturn(files)

            // When
            val result = noticeService.getNoticeDetail(10L)

            // Then
            assertThat(result.images).hasSize(2)
            assertThat(result.images[0].id).isEqualTo(1L)
            assertThat(result.images[1].id).isEqualTo(4L)
        }

        @Test
        @DisplayName("존재하지 않는 공지 ID -> NoticePostNotFoundException")
        fun getNoticeDetail_notFound() {
            // Given
            whenever(noticeRepository.findById(999L)).thenReturn(Optional.empty())

            // When & Then
            assertThatThrownBy { noticeService.getNoticeDetail(999L) }
                .isInstanceOf(NoticePostNotFoundException::class.java)
        }

        @Test
        @DisplayName("삭제된 공지 조회 -> NoticePostNotFoundException")
        fun getNoticeDetail_deleted() {
            // Given
            val notice = createNotice(id = 10L, isDeleted = true)

            whenever(noticeRepository.findById(10L)).thenReturn(Optional.of(notice))

            // When & Then
            assertThatThrownBy { noticeService.getNoticeDetail(10L) }
                .isInstanceOf(NoticePostNotFoundException::class.java)
        }

        @Test
        @DisplayName("카테고리 매핑 - COMPANY -> COMPANY/회사공지")
        fun getNoticeDetail_categoryMapping_company() {
            // Given
            val notice = createNotice(id = 1L, category = NoticeCategory.COMPANY)
            whenever(noticeRepository.findById(1L)).thenReturn(Optional.of(notice))

            // When
            val result = noticeService.getNoticeDetail(1L)

            // Then
            assertThat(result.category).isEqualTo("COMPANY")
            assertThat(result.categoryName).isEqualTo("회사공지")
        }

        @Test
        @DisplayName("카테고리 매핑 - BRANCH -> BRANCH/지점공지")
        fun getNoticeDetail_categoryMapping_branch() {
            // Given
            val notice = createNotice(id = 1L, category = NoticeCategory.BRANCH)
            whenever(noticeRepository.findById(1L)).thenReturn(Optional.of(notice))

            // When
            val result = noticeService.getNoticeDetail(1L)

            // Then
            assertThat(result.category).isEqualTo("BRANCH")
            assertThat(result.categoryName).isEqualTo("지점공지")
        }

        @Test
        @DisplayName("카테고리 매핑 - EDUCATION -> EDUCATION/교육")
        fun getNoticeDetail_categoryMapping_education() {
            // Given
            val notice = createNotice(id = 1L, category = NoticeCategory.EDUCATION)
            whenever(noticeRepository.findById(1L)).thenReturn(Optional.of(notice))

            // When
            val result = noticeService.getNoticeDetail(1L)

            // Then
            assertThat(result.category).isEqualTo("EDUCATION")
            assertThat(result.categoryName).isEqualTo("교육")
        }

        @Test
        @DisplayName("카테고리 null -> 빈 문자열 반환")
        fun getNoticeDetail_categoryNull() {
            // Given
            val notice = createNotice(id = 1L, category = null)
            whenever(noticeRepository.findById(1L)).thenReturn(Optional.of(notice))

            // When
            val result = noticeService.getNoticeDetail(1L)

            // Then
            assertThat(result.category).isEqualTo("")
            assertThat(result.categoryName).isEqualTo("")
        }
    }

    @Nested
    @DisplayName("getPosts - 공지사항 목록 조회")
    inner class GetPostsTests {

        private val userId = 1L
        private val testUser = User(id = userId, employeeId = "20030117", name = "테스트사원", orgName = "테스트지점")

        @BeforeEach
        fun setUpUser() {
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(testUser))
        }

        @Test
        @DisplayName("전체 목록 조회 - category null -> COMPANY + 사용자 지점 BRANCH 공지 반환")
        fun getPosts_allCategories() {
            // Given
            val notices = listOf(
                createNotice(id = 1L, category = NoticeCategory.COMPANY, name = "전체공지 제목"),
                createNotice(id = 2L, category = NoticeCategory.BRANCH, name = "지점공지 제목", branch = "테스트지점")
            )
            val page = PageImpl(notices, PageRequest.of(0, 10), 2)
            whenever(noticeRepository.findNotices(eq(null), eq(null), eq("테스트지점"), any())).thenReturn(page)

            // When
            val result = noticeService.getPosts(userId, null, null, 1, 10)

            // Then
            assertThat(result.content).hasSize(2)
            assertThat(result.totalCount).isEqualTo(2)
            assertThat(result.totalPages).isEqualTo(1)
            assertThat(result.currentPage).isEqualTo(1)
            assertThat(result.size).isEqualTo(10)
        }

        @Test
        @DisplayName("회사공지만 조회 - category=COMPANY -> COMPANY 공지만 반환")
        fun getPosts_companyOnly() {
            // Given
            val notices = listOf(createNotice(id = 1L, category = NoticeCategory.COMPANY, name = "전체공지"))
            val page = PageImpl(notices, PageRequest.of(0, 10), 1)
            whenever(noticeRepository.findNotices(eq(NoticeCategory.COMPANY), eq(null), eq("테스트지점"), any())).thenReturn(page)

            // When
            val result = noticeService.getPosts(userId, "COMPANY", null, 1, 10)

            // Then
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].category).isEqualTo("COMPANY")
            assertThat(result.content[0].categoryName).isEqualTo("회사공지")
        }

        @Test
        @DisplayName("지점공지만 조회 - category=BRANCH -> 사용자 지점 BRANCH 공지만 반환")
        fun getPosts_branchOnly() {
            // Given
            val notices = listOf(createNotice(id = 2L, category = NoticeCategory.BRANCH, name = "지점공지", branch = "테스트지점"))
            val page = PageImpl(notices, PageRequest.of(0, 10), 1)
            whenever(noticeRepository.findNotices(eq(NoticeCategory.BRANCH), eq(null), eq("테스트지점"), any())).thenReturn(page)

            // When
            val result = noticeService.getPosts(userId, "BRANCH", null, 1, 10)

            // Then
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].category).isEqualTo("BRANCH")
            assertThat(result.content[0].categoryName).isEqualTo("지점공지")
        }

        @Test
        @DisplayName("검색 조회 - search 키워드로 필터링")
        fun getPosts_withSearch() {
            // Given
            val notices = listOf(createNotice(id = 1L, category = NoticeCategory.COMPANY, name = "영업 목표 안내"))
            val page = PageImpl(notices, PageRequest.of(0, 10), 1)
            whenever(noticeRepository.findNotices(eq(null), eq("영업"), eq("테스트지점"), any())).thenReturn(page)

            // When
            val result = noticeService.getPosts(userId, null, "영업", 1, 10)

            // Then
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].title).isEqualTo("영업 목표 안내")
        }

        @Test
        @DisplayName("페이지네이션 - page=2, size=2 -> 올바른 페이지 정보 반환")
        fun getPosts_pagination() {
            // Given
            val notices = listOf(createNotice(id = 3L, category = NoticeCategory.COMPANY))
            val page = PageImpl(notices, PageRequest.of(1, 2), 5)
            whenever(noticeRepository.findNotices(eq(null), eq(null), eq("테스트지점"), any())).thenReturn(page)

            // When
            val result = noticeService.getPosts(userId, null, null, 2, 2)

            // Then
            assertThat(result.currentPage).isEqualTo(2)
            assertThat(result.size).isEqualTo(2)
            assertThat(result.totalCount).isEqualTo(5)
            assertThat(result.totalPages).isEqualTo(3)
        }

        @Test
        @DisplayName("빈 결과 - 매칭 없는 검색어 -> 빈 배열, totalCount=0")
        fun getPosts_emptyResult() {
            // Given
            val page = PageImpl<Notice>(emptyList(), PageRequest.of(0, 10), 0)
            whenever(noticeRepository.findNotices(eq(null), eq("없는검색어"), eq("테스트지점"), any())).thenReturn(page)

            // When
            val result = noticeService.getPosts(userId, null, "없는검색어", 1, 10)

            // Then
            assertThat(result.content).isEmpty()
            assertThat(result.totalCount).isEqualTo(0)
        }

        @Test
        @DisplayName("잘못된 카테고리 - INVALID -> InvalidNoticeCategoryException")
        fun getPosts_invalidCategory() {
            // When & Then
            assertThatThrownBy { noticeService.getPosts(userId, "INVALID", null, 1, 10) }
                .isInstanceOf(InvalidNoticeCategoryException::class.java)
        }

        @Test
        @DisplayName("검색 키워드 100자 제한 - 101자 -> 100자로 잘림")
        fun getPosts_searchTruncated() {
            // Given
            val longSearch = "가".repeat(101)
            val truncated = "가".repeat(100)
            val page = PageImpl<Notice>(emptyList(), PageRequest.of(0, 10), 0)
            whenever(noticeRepository.findNotices(eq(null), eq(truncated), eq("테스트지점"), any())).thenReturn(page)

            // When
            val result = noticeService.getPosts(userId, null, longSearch, 1, 10)

            // Then
            assertThat(result.content).isEmpty()
        }
    }

    @Nested
    @DisplayName("getPostsForAdmin - Admin 공지사항 목록 조회")
    inner class GetPostsForAdminTests {

        @Test
        @DisplayName("전체 목록 조회 - category null -> 모든 카테고리 반환")
        fun getPostsForAdmin_allCategories() {
            val notices = listOf(
                createNotice(id = 1L, category = NoticeCategory.COMPANY, name = "전체공지"),
                createNotice(id = 2L, category = NoticeCategory.BRANCH, name = "지점공지", branch = "서울지점")
            )
            val page = PageImpl(notices, PageRequest.of(0, 10), 2)
            whenever(noticeRepository.findAllNotices(eq(null), eq(null), any())).thenReturn(page)

            val result = noticeService.getPostsForAdmin(null, null, 1, 10)

            assertThat(result.content).hasSize(2)
            assertThat(result.totalCount).isEqualTo(2)
        }

        @Test
        @DisplayName("잘못된 카테고리 - INVALID -> InvalidNoticeCategoryException")
        fun getPostsForAdmin_invalidCategory() {
            assertThatThrownBy { noticeService.getPostsForAdmin("INVALID", null, 1, 10) }
                .isInstanceOf(InvalidNoticeCategoryException::class.java)
        }
    }

    @Nested
    @DisplayName("createNotice - 공지사항 작성")
    inner class CreateNoticeTests {

        @Test
        @DisplayName("회사공지 작성 성공 - category=COMPANY -> DB에 회사공지로 저장")
        fun createNotice_company_success() {
            val request = NoticeCreateRequest(
                title = "테스트 공지",
                category = "COMPANY",
                content = "<p>내용</p>"
            )
            whenever(noticeRepository.save(any<Notice>())).thenAnswer { it.getArgument<Notice>(0) }

            val result = noticeService.createNotice(request)

            assertThat(result.title).isEqualTo("테스트 공지")
            assertThat(result.category).isEqualTo("COMPANY")
            assertThat(result.categoryName).isEqualTo("회사공지")
            assertThat(result.branch).isNull()
            assertThat(result.branchCode).isNull()
        }

        @Test
        @DisplayName("지점공지 작성 성공 - category=BRANCH + branch/branchCode 포함")
        fun createNotice_branch_success() {
            val request = NoticeCreateRequest(
                title = "지점 공지",
                category = "BRANCH",
                content = "<p>지점 내용</p>",
                branch = "서울1지점",
                branchCode = "1101"
            )
            whenever(noticeRepository.save(any<Notice>())).thenAnswer { it.getArgument<Notice>(0) }

            val result = noticeService.createNotice(request)

            assertThat(result.category).isEqualTo("BRANCH")
            assertThat(result.categoryName).isEqualTo("지점공지")
            assertThat(result.branch).isEqualTo("서울1지점")
            assertThat(result.branchCode).isEqualTo("1101")
        }

        @Test
        @DisplayName("교육 공지 작성 성공 - category=EDUCATION")
        fun createNotice_education_success() {
            val request = NoticeCreateRequest(
                title = "교육 공지",
                category = "EDUCATION",
                content = "<p>교육 내용</p>"
            )
            whenever(noticeRepository.save(any<Notice>())).thenAnswer { it.getArgument<Notice>(0) }

            val result = noticeService.createNotice(request)

            assertThat(result.category).isEqualTo("EDUCATION")
            assertThat(result.categoryName).isEqualTo("교육")
            assertThat(result.branch).isNull()
            assertThat(result.branchCode).isNull()
        }

        @Test
        @DisplayName("회사공지 작성 시 branch 무시 - category=COMPANY + branch 전달 -> branch null")
        fun createNotice_company_ignoresBranch() {
            val request = NoticeCreateRequest(
                title = "전체 공지",
                category = "COMPANY",
                content = "<p>내용</p>",
                branch = "잘못된지점",
                branchCode = "9999"
            )
            whenever(noticeRepository.save(any<Notice>())).thenAnswer { it.getArgument<Notice>(0) }

            val result = noticeService.createNotice(request)

            assertThat(result.branch).isNull()
            assertThat(result.branchCode).isNull()
        }

        @Test
        @DisplayName("잘못된 카테고리 -> InvalidNoticeCategoryException")
        fun createNotice_invalidCategory() {
            val request = NoticeCreateRequest(title = "공지", category = "UNKNOWN", content = "내용")

            assertThatThrownBy { noticeService.createNotice(request) }
                .isInstanceOf(InvalidNoticeCategoryException::class.java)
        }

        @Test
        @DisplayName("지점공지 branch 누락 -> BranchRequiredException")
        fun createNotice_branchRequired() {
            val request = NoticeCreateRequest(
                title = "지점 공지",
                category = "BRANCH",
                content = "<p>내용</p>",
                branch = null,
                branchCode = null
            )

            assertThatThrownBy { noticeService.createNotice(request) }
                .isInstanceOf(BranchRequiredException::class.java)
        }
    }

    @Nested
    @DisplayName("updateNotice - 공지사항 수정")
    inner class UpdateNoticeTests {

        @Test
        @DisplayName("수정 성공 - 제목/내용 변경")
        fun updateNotice_success() {
            val existing = createNotice(id = 10L, category = NoticeCategory.COMPANY, name = "원래 제목")
            whenever(noticeRepository.findById(10L)).thenReturn(Optional.of(existing))
            whenever(noticeRepository.save(any<Notice>())).thenAnswer { it.getArgument<Notice>(0) }

            val request = NoticeUpdateRequest(
                title = "수정된 제목",
                category = "COMPANY",
                content = "<p>수정 내용</p>"
            )

            val result = noticeService.updateNotice(10L, request)

            assertThat(result.title).isEqualTo("수정된 제목")
            assertThat(result.content).isEqualTo("<p>수정 내용</p>")
        }

        @Test
        @DisplayName("카테고리 변경 - COMPANY -> BRANCH + branch/branchCode 포함")
        fun updateNotice_changeCategoryToBranch() {
            val existing = createNotice(id = 10L, category = NoticeCategory.COMPANY, name = "전체 공지")
            whenever(noticeRepository.findById(10L)).thenReturn(Optional.of(existing))
            whenever(noticeRepository.save(any<Notice>())).thenAnswer { it.getArgument<Notice>(0) }

            val request = NoticeUpdateRequest(
                title = "지점 공지로 변경",
                category = "BRANCH",
                content = "<p>내용</p>",
                branch = "서울1지점",
                branchCode = "1101"
            )

            val result = noticeService.updateNotice(10L, request)

            assertThat(result.category).isEqualTo("BRANCH")
            assertThat(result.branch).isEqualTo("서울1지점")
            assertThat(result.branchCode).isEqualTo("1101")
        }

        @Test
        @DisplayName("0 이하 ID -> InvalidNoticeIdException")
        fun updateNotice_invalidId() {
            val request = NoticeUpdateRequest(title = "제목", category = "COMPANY", content = "내용")

            assertThatThrownBy { noticeService.updateNotice(0L, request) }
                .isInstanceOf(InvalidNoticeIdException::class.java)
        }

        @Test
        @DisplayName("존재하지 않는 공지 -> NoticePostNotFoundException")
        fun updateNotice_notFound() {
            whenever(noticeRepository.findById(999L)).thenReturn(Optional.empty())
            val request = NoticeUpdateRequest(title = "제목", category = "COMPANY", content = "내용")

            assertThatThrownBy { noticeService.updateNotice(999L, request) }
                .isInstanceOf(NoticePostNotFoundException::class.java)
        }

        @Test
        @DisplayName("삭제된 공지 수정 -> NoticePostNotFoundException")
        fun updateNotice_deleted() {
            val existing = createNotice(id = 10L, isDeleted = true)
            whenever(noticeRepository.findById(10L)).thenReturn(Optional.of(existing))
            val request = NoticeUpdateRequest(title = "제목", category = "COMPANY", content = "내용")

            assertThatThrownBy { noticeService.updateNotice(10L, request) }
                .isInstanceOf(NoticePostNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("deleteNotice - 공지사항 삭제")
    inner class DeleteNoticeTests {

        @Test
        @DisplayName("삭제 성공 - isDeleted=true 설정")
        fun deleteNotice_success() {
            val existing = createNotice(id = 10L)
            whenever(noticeRepository.findById(10L)).thenReturn(Optional.of(existing))
            whenever(noticeRepository.save(any<Notice>())).thenAnswer { it.getArgument<Notice>(0) }

            noticeService.deleteNotice(10L)

            assertThat(existing.isDeleted).isTrue()
        }

        @Test
        @DisplayName("0 이하 ID -> InvalidNoticeIdException")
        fun deleteNotice_invalidId() {
            assertThatThrownBy { noticeService.deleteNotice(-1L) }
                .isInstanceOf(InvalidNoticeIdException::class.java)
        }

        @Test
        @DisplayName("존재하지 않는 공지 -> NoticePostNotFoundException")
        fun deleteNotice_notFound() {
            whenever(noticeRepository.findById(999L)).thenReturn(Optional.empty())

            assertThatThrownBy { noticeService.deleteNotice(999L) }
                .isInstanceOf(NoticePostNotFoundException::class.java)
        }

        @Test
        @DisplayName("이미 삭제된 공지 -> NoticePostNotFoundException")
        fun deleteNotice_alreadyDeleted() {
            val existing = createNotice(id = 10L, isDeleted = true)
            whenever(noticeRepository.findById(10L)).thenReturn(Optional.of(existing))

            assertThatThrownBy { noticeService.deleteNotice(10L) }
                .isInstanceOf(NoticePostNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("getNoticeFormMeta - 폼 메타데이터 조회")
    inner class GetNoticeFormMetaTests {

        @Test
        @DisplayName("정상 조회 - 카테고리 3개 + 지점 목록 반환")
        fun getNoticeFormMeta_success() {
            val orgs = listOf(
                createOrg(orgNameLevel3 = "제1사업부", orgNameLevel4 = "1영업부", orgNameLevel5 = "서울1지점", costCenterLevel4 = "1100", costCenterLevel5 = "1101"),
                createOrg(orgNameLevel3 = "제1사업부", orgNameLevel4 = "1영업부", orgNameLevel5 = "서울2지점", costCenterLevel4 = "1100", costCenterLevel5 = "1102")
            )
            whenever(orgRepository.findAll()).thenReturn(orgs)

            val result = noticeService.getNoticeFormMeta()

            assertThat(result.categories).hasSize(3)
            assertThat(result.categories[0].code).isEqualTo("COMPANY")
            assertThat(result.categories[0].name).isEqualTo("회사공지")
            assertThat(result.categories[1].code).isEqualTo("BRANCH")
            assertThat(result.categories[1].name).isEqualTo("지점공지")
            assertThat(result.categories[2].code).isEqualTo("EDUCATION")
            assertThat(result.categories[2].name).isEqualTo("교육")

            assertThat(result.branches).hasSize(2)
            assertThat(result.branches[0].branchCode).isEqualTo("1101")
            assertThat(result.branches[0].branchName).isEqualTo("[제1사업부] 1영업부-서울1지점")
            assertThat(result.branches[1].branchCode).isEqualTo("1102")
            assertThat(result.branches[1].branchName).isEqualTo("[제1사업부] 1영업부-서울2지점")
        }

        @Test
        @DisplayName("5레벨 없는 조직 - orgNameLevel5 null -> '[사업부] 영업부' 형식")
        fun getNoticeFormMeta_noLevel5() {
            val orgs = listOf(
                createOrg(orgNameLevel3 = "e-Biz", orgNameLevel4 = "영업지원", orgNameLevel5 = null, costCenterLevel4 = "2100", costCenterLevel5 = null)
            )
            whenever(orgRepository.findAll()).thenReturn(orgs)

            val result = noticeService.getNoticeFormMeta()

            assertThat(result.branches).hasSize(1)
            assertThat(result.branches[0].branchName).isEqualTo("[e-Biz] 영업지원")
            assertThat(result.branches[0].branchCode).isEqualTo("2100")
        }

        @Test
        @DisplayName("불완전한 조직 제외 - orgNameLevel3 null -> 제외")
        fun getNoticeFormMeta_filterIncomplete() {
            val orgs = listOf(
                createOrg(orgNameLevel3 = null, orgNameLevel4 = "영업부", orgNameLevel5 = "지점"),
                createOrg(orgNameLevel3 = "사업부", orgNameLevel4 = null, orgNameLevel5 = "지점"),
                createOrg(orgNameLevel3 = "사업부", orgNameLevel4 = "영업부", orgNameLevel5 = "지점", costCenterLevel5 = "3001")
            )
            whenever(orgRepository.findAll()).thenReturn(orgs)

            val result = noticeService.getNoticeFormMeta()

            assertThat(result.branches).hasSize(1)
            assertThat(result.branches[0].branchCode).isEqualTo("3001")
        }

        @Test
        @DisplayName("중복 제거 - 동일 branchCode -> 하나만 반환")
        fun getNoticeFormMeta_dedup() {
            val orgs = listOf(
                createOrg(orgNameLevel3 = "사업부", orgNameLevel4 = "영업부", orgNameLevel5 = "지점A", costCenterLevel5 = "1001"),
                createOrg(orgNameLevel3 = "사업부", orgNameLevel4 = "영업부", orgNameLevel5 = "지점A", costCenterLevel5 = "1001")
            )
            whenever(orgRepository.findAll()).thenReturn(orgs)

            val result = noticeService.getNoticeFormMeta()

            assertThat(result.branches).hasSize(1)
        }

        @Test
        @DisplayName("빈 org 테이블 - branches 빈 배열")
        fun getNoticeFormMeta_empty() {
            whenever(orgRepository.findAll()).thenReturn(emptyList())

            val result = noticeService.getNoticeFormMeta()

            assertThat(result.categories).hasSize(3)
            assertThat(result.branches).isEmpty()
        }
    }

    private fun createOrg(
        orgNameLevel3: String? = null,
        orgNameLevel4: String? = null,
        orgNameLevel5: String? = null,
        costCenterLevel4: String? = null,
        costCenterLevel5: String? = null
    ): Org = Org(
        orgNameLevel3 = orgNameLevel3,
        orgNameLevel4 = orgNameLevel4,
        orgNameLevel5 = orgNameLevel5,
        costCenterLevel4 = costCenterLevel4,
        costCenterLevel5 = costCenterLevel5
    )

    private fun createNotice(
        id: Long = 1L,
        sfid: String? = null,
        name: String? = "테스트 공지",
        category: NoticeCategory? = NoticeCategory.COMPANY,
        contents: String? = "본문 내용",
        branch: String? = null,
        isDeleted: Boolean? = false,
        createdDate: LocalDateTime? = LocalDateTime.of(2026, 1, 1, 0, 0, 0)
    ): Notice = Notice(
        id = id,
        sfid = sfid,
        name = name,
        category = category,
        contents = contents,
        branch = branch,
        isDeleted = isDeleted,
        createdDate = createdDate
    )

    private fun createUploadFile(
        id: Long = 1L,
        uniqueKey: String? = "test/file.jpg",
        createdDate: LocalDateTime? = LocalDateTime.of(2026, 1, 1, 0, 0, 0)
    ): UploadFile = UploadFile(
        id = id,
        uniqueKey = uniqueKey,
        createdDate = createdDate
    )
}
