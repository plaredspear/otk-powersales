package com.otoki.powersales.domain.support.notice.service

import com.otoki.powersales.platform.common.entity.UploadFile
import com.otoki.powersales.platform.common.repository.UploadFileRepository
import com.otoki.powersales.platform.common.service.FileStorageService
import com.otoki.powersales.platform.common.storage.StorageService
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.support.notice.dto.request.NoticeCreateRequest
import com.otoki.powersales.domain.support.notice.dto.request.NoticeUpdateRequest
import com.otoki.powersales.domain.support.notice.entity.Notice
import com.otoki.powersales.domain.support.notice.enums.NoticeCategory
import com.otoki.powersales.domain.support.notice.exception.BranchRequiredException
import com.otoki.powersales.domain.support.notice.exception.InvalidImageIdException
import com.otoki.powersales.domain.support.notice.exception.InvalidNoticeCategoryException
import com.otoki.powersales.domain.support.notice.exception.InvalidNoticeIdException
import com.otoki.powersales.domain.support.notice.exception.NoticePostNotFoundException
import com.otoki.powersales.domain.support.notice.repository.NoticeRepository
import com.otoki.powersales.organization.entity.Organization
import com.otoki.powersales.organization.repository.OrganizationRepository
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime
import java.util.Optional

@DisplayName("NoticeService 테스트")
class NoticeServiceTest {

    private val noticeRepository: NoticeRepository = mockk()
    private val uploadFileRepository: UploadFileRepository = mockk()
    private val employeeRepository: EmployeeRepository = mockk()
    private val organizationRepository: OrganizationRepository = mockk()
    private val fileStorageService: FileStorageService = mockk()
    private val storageService: StorageService = mockk()

    private lateinit var noticeService: NoticeService

    @BeforeEach
    fun setUp() {
        noticeService = NoticeService(
            noticeRepository,
            uploadFileRepository,
            employeeRepository,
            organizationRepository,
            fileStorageService,
            storageService
        )
        // 공지 이미지는 private presigned 조회 — uniqueKey 를 받아 presigned 형태 URL 반환 stub.
        every { storageService.getPresignedUrl(any(), any()) } answers {
            "https://test-bucket.s3.ap-northeast-2.amazonaws.com/private/${firstArg<String>()}?X-Amz-Signature=test"
        }
    }

    @Nested
    @DisplayName("getNoticeDetail - 공지사항 상세 조회")
    inner class GetNoticeDetailTests {

        @Test
        @DisplayName("정상 조회 - 이미지 포함 공지 -> 상세 정보 반환")
        fun getNoticeDetail_withImages() {
            val notice = createNotice(
                id = 42L,
                name = "2026년 상반기 영업 목표 안내",
                category = NoticeCategory.COMPANY,
                contents = "상반기 영업 목표가 확정되었습니다.",
                createdDate = LocalDateTime.of(2026, 2, 28, 10, 30, 0)
            )
            val files = listOf(
                createUploadFile(id = 101L, uniqueKey = "notices/img1.jpg", createdDate = LocalDateTime.of(2026, 2, 28, 10, 30, 0)),
                createUploadFile(id = 102L, uniqueKey = "notices/img2.jpg", createdDate = LocalDateTime.of(2026, 2, 28, 11, 0, 0))
            )

            every { noticeRepository.findById(42L) } returns Optional.of(notice)
            every { uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse("Notice", 42L) } returns files

            val result = noticeService.getNoticeDetail(42L)

            assertThat(result.id).isEqualTo(42L)
            assertThat(result.category).isEqualTo("COMPANY")
            assertThat(result.categoryName).isEqualTo("회사공지")
            assertThat(result.title).isEqualTo("2026년 상반기 영업 목표 안내")
            assertThat(result.content).isEqualTo("상반기 영업 목표가 확정되었습니다.")
            assertThat(result.createdAt).isEqualTo(LocalDateTime.parse("2026-02-28T10:30:00"))
            assertThat(result.images).hasSize(2)
            assertThat(result.images[0].id).isEqualTo(101L)
            assertThat(result.images[0].url)
                .isEqualTo("https://test-bucket.s3.ap-northeast-2.amazonaws.com/private/notices/img1.jpg?X-Amz-Signature=test")
            assertThat(result.images[0].sortOrder).isEqualTo(0)
            assertThat(result.images[1].sortOrder).isEqualTo(1)
        }

        @Test
        @DisplayName("이미지 없음 - 매칭 파일 없음 -> images 빈 배열 반환")
        fun getNoticeDetail_noMatchingFiles() {
            val notice = createNotice(id = 10L, name = "공지", category = NoticeCategory.COMPANY)

            every { noticeRepository.findById(10L) } returns Optional.of(notice)
            every { uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse("Notice", 10L) } returns emptyList()

            val result = noticeService.getNoticeDetail(10L)

            assertThat(result.images).isEmpty()
        }

        @Test
        @DisplayName("이미지 URL 무효 - uniqueKey null/빈 문자열인 이미지 제외")
        fun getNoticeDetail_filterInvalidImages() {
            val notice = createNotice(id = 10L, category = NoticeCategory.COMPANY)
            val files = listOf(
                createUploadFile(id = 1L, uniqueKey = "valid/img.jpg", createdDate = LocalDateTime.of(2026, 1, 1, 0, 0)),
                createUploadFile(id = 2L, uniqueKey = null, createdDate = LocalDateTime.of(2026, 1, 2, 0, 0)),
                createUploadFile(id = 3L, uniqueKey = "", createdDate = LocalDateTime.of(2026, 1, 3, 0, 0)),
                createUploadFile(id = 4L, uniqueKey = "valid/img2.jpg", createdDate = LocalDateTime.of(2026, 1, 4, 0, 0))
            )

            every { noticeRepository.findById(10L) } returns Optional.of(notice)
            every { uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse("Notice", 10L) } returns files

            val result = noticeService.getNoticeDetail(10L)

            assertThat(result.images).hasSize(2)
            assertThat(result.images[0].id).isEqualTo(1L)
            assertThat(result.images[1].id).isEqualTo(4L)
        }

        @Test
        @DisplayName("존재하지 않는 공지 ID -> NoticePostNotFoundException")
        fun getNoticeDetail_notFound() {
            every { noticeRepository.findById(999L) } returns Optional.empty()

            assertThatThrownBy { noticeService.getNoticeDetail(999L) }
                .isInstanceOf(NoticePostNotFoundException::class.java)
        }

        @Test
        @DisplayName("삭제된 공지 조회 -> NoticePostNotFoundException")
        fun getNoticeDetail_deleted() {
            val notice = createNotice(id = 10L, isDeleted = true)

            every { noticeRepository.findById(10L) } returns Optional.of(notice)

            assertThatThrownBy { noticeService.getNoticeDetail(10L) }
                .isInstanceOf(NoticePostNotFoundException::class.java)
        }

        @Test
        @DisplayName("카테고리 매핑 - COMPANY -> COMPANY/회사공지")
        fun getNoticeDetail_categoryMapping_company() {
            val notice = createNotice(id = 1L, category = NoticeCategory.COMPANY)
            every { noticeRepository.findById(1L) } returns Optional.of(notice)
            every { uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse("Notice", 1L) } returns emptyList()

            val result = noticeService.getNoticeDetail(1L)

            assertThat(result.category).isEqualTo("COMPANY")
            assertThat(result.categoryName).isEqualTo("회사공지")
        }

        @Test
        @DisplayName("카테고리 매핑 - BRANCH -> BRANCH/지점공지")
        fun getNoticeDetail_categoryMapping_branch() {
            val notice = createNotice(id = 1L, category = NoticeCategory.BRANCH)
            every { noticeRepository.findById(1L) } returns Optional.of(notice)
            every { uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse("Notice", 1L) } returns emptyList()

            val result = noticeService.getNoticeDetail(1L)

            assertThat(result.category).isEqualTo("BRANCH")
            assertThat(result.categoryName).isEqualTo("지점공지")
        }

        @Test
        @DisplayName("카테고리 매핑 - EDUCATION -> EDUCATION/교육")
        fun getNoticeDetail_categoryMapping_education() {
            val notice = createNotice(id = 1L, category = NoticeCategory.EDUCATION)
            every { noticeRepository.findById(1L) } returns Optional.of(notice)
            every { uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse("Notice", 1L) } returns emptyList()

            val result = noticeService.getNoticeDetail(1L)

            assertThat(result.category).isEqualTo("EDUCATION")
            assertThat(result.categoryName).isEqualTo("교육")
        }

        @Test
        @DisplayName("본문 placeholder 이미지 - data-refid 의 src 를 presigned 로 치환 + data-refid 보존")
        fun getNoticeDetail_rewriteInlineImage() {
            val notice = createNotice(
                id = 1L, category = NoticeCategory.COMPANY,
                contents = """<p>안내</p><img src="notice-image://0EM001" data-refid="0EM001" alt="신년사.png">"""
            )
            every { noticeRepository.findById(1L) } returns Optional.of(notice)
            every { uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse("Notice", 1L) } returns listOf(
                createUploadFile(id = 1L, sfid = "0EM001", uniqueKey = "uploads/notice/migrated/0EM001.png")
            )

            val result = noticeService.getNoticeDetail(1L)

            // src 는 presigned 로 교체, data-refid 는 보존 (mobile cacheKey 용), placeholder/rtaImage 잔존 없음
            assertThat(result.content).contains("""src="https://test-bucket.s3.ap-northeast-2.amazonaws.com/private/uploads/notice/migrated/0EM001.png?X-Amz-Signature=test"""")
            assertThat(result.content).contains("""data-refid="0EM001"""")
            assertThat(result.content).doesNotContain("notice-image://")
        }

        @Test
        @DisplayName("본문 이미지 다수 - 각 refid 별 올바른 uniqueKey 의 presigned 로 치환")
        fun getNoticeDetail_rewriteMultipleImages() {
            val notice = createNotice(
                id = 1L, category = NoticeCategory.COMPANY,
                contents = """<img src="notice-image://A" data-refid="A"><p>중간</p><img src="notice-image://B" data-refid="B">"""
            )
            every { noticeRepository.findById(1L) } returns Optional.of(notice)
            every { uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse("Notice", 1L) } returns listOf(
                createUploadFile(id = 1L, sfid = "A", uniqueKey = "uploads/notice/migrated/A.png"),
                createUploadFile(id = 2L, sfid = "B", uniqueKey = "uploads/notice/migrated/B.jpg")
            )

            val result = noticeService.getNoticeDetail(1L)

            assertThat(result.content).contains("private/uploads/notice/migrated/A.png")
            assertThat(result.content).contains("private/uploads/notice/migrated/B.jpg")
            assertThat(result.content).doesNotContain("notice-image://")
        }

        @Test
        @DisplayName("미적재 refid - 매칭 uploadFile 없으면 placeholder 유지 (치환 안 함)")
        fun getNoticeDetail_rewriteMissingRefid() {
            val notice = createNotice(
                id = 1L, category = NoticeCategory.COMPANY,
                contents = """<img src="notice-image://MISSING" data-refid="MISSING">"""
            )
            every { noticeRepository.findById(1L) } returns Optional.of(notice)
            every { uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse("Notice", 1L) } returns emptyList()

            val result = noticeService.getNoticeDetail(1L)

            // 미적재 refid → placeholder 그대로 (깨진 이미지로 노출되되 본문 오염 없음)
            assertThat(result.content).isEqualTo("""<img src="notice-image://MISSING" data-refid="MISSING">""")
        }

        @Test
        @DisplayName("data-refid 없는 본문 - 무변경 (빠른 경로)")
        fun getNoticeDetail_rewriteNoPlaceholder() {
            val notice = createNotice(
                id = 1L, category = NoticeCategory.COMPANY,
                contents = "<p>이미지 없는 본문</p>"
            )
            every { noticeRepository.findById(1L) } returns Optional.of(notice)
            every { uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse("Notice", 1L) } returns emptyList()

            val result = noticeService.getNoticeDetail(1L)

            assertThat(result.content).isEqualTo("<p>이미지 없는 본문</p>")
        }

        @Test
        @DisplayName("카테고리 null -> 빈 문자열 반환")
        fun getNoticeDetail_categoryNull() {
            val notice = createNotice(id = 1L, category = null)
            every { noticeRepository.findById(1L) } returns Optional.of(notice)
            every { uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse("Notice", 1L) } returns emptyList()

            val result = noticeService.getNoticeDetail(1L)

            assertThat(result.category).isEqualTo("")
            assertThat(result.categoryName).isEqualTo("")
        }
    }

    @Nested
    @DisplayName("getPosts - 공지사항 목록 조회")
    inner class GetPostsTests {

        private val userId = 1L
        // 지점공지 필터는 지점코드(costCenterCode) 기준 — 레거시 jeejumcode = costcentercode 정합
        private val testEmployee = Employee(id = userId, employeeCode = "20030117", name = "테스트사원", orgName = "테스트지점", costCenterCode = "테스트지점")

        @BeforeEach
        fun setUpUser() {
            every { employeeRepository.findById(userId) } returns Optional.of(testEmployee)
        }

        @Test
        @DisplayName("전체 목록 조회 - category null -> COMPANY + 사용자 지점 BRANCH 공지 반환")
        fun getPosts_allCategories() {
            val notices = listOf(
                createNotice(id = 1L, category = NoticeCategory.COMPANY, name = "전체공지 제목"),
                createNotice(id = 2L, category = NoticeCategory.BRANCH, name = "지점공지 제목", branch = "테스트지점")
            )
            val page = PageImpl(notices, PageRequest.of(0, 10), 2)
            every { noticeRepository.findNotices(null, null, "테스트지점", any()) } returns page

            val result = noticeService.getPosts(userId, null, null, 1, 10)

            assertThat(result.content).hasSize(2)
            assertThat(result.totalCount).isEqualTo(2)
            assertThat(result.totalPages).isEqualTo(1)
            assertThat(result.currentPage).isEqualTo(1)
            assertThat(result.size).isEqualTo(10)
        }

        @Test
        @DisplayName("회사공지만 조회 - category=COMPANY -> COMPANY 공지만 반환")
        fun getPosts_companyOnly() {
            val notices = listOf(createNotice(id = 1L, category = NoticeCategory.COMPANY, name = "전체공지"))
            val page = PageImpl(notices, PageRequest.of(0, 10), 1)
            every { noticeRepository.findNotices(NoticeCategory.COMPANY, null, "테스트지점", any()) } returns page

            val result = noticeService.getPosts(userId, "COMPANY", null, 1, 10)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].category).isEqualTo("COMPANY")
            assertThat(result.content[0].categoryName).isEqualTo("회사공지")
        }

        @Test
        @DisplayName("지점공지만 조회 - category=BRANCH -> 사용자 지점 BRANCH 공지만 반환")
        fun getPosts_branchOnly() {
            val notices = listOf(createNotice(id = 2L, category = NoticeCategory.BRANCH, name = "지점공지", branch = "테스트지점"))
            val page = PageImpl(notices, PageRequest.of(0, 10), 1)
            every { noticeRepository.findNotices(NoticeCategory.BRANCH, null, "테스트지점", any()) } returns page

            val result = noticeService.getPosts(userId, "BRANCH", null, 1, 10)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].category).isEqualTo("BRANCH")
            assertThat(result.content[0].categoryName).isEqualTo("지점공지")
        }

        @Test
        @DisplayName("검색 조회 - search 키워드로 필터링")
        fun getPosts_withSearch() {
            val notices = listOf(createNotice(id = 1L, category = NoticeCategory.COMPANY, name = "영업 목표 안내"))
            val page = PageImpl(notices, PageRequest.of(0, 10), 1)
            every { noticeRepository.findNotices(null, "영업", "테스트지점", any()) } returns page

            val result = noticeService.getPosts(userId, null, "영업", 1, 10)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].title).isEqualTo("영업 목표 안내")
        }

        @Test
        @DisplayName("페이지네이션 - page=2, size=2 -> 올바른 페이지 정보 반환")
        fun getPosts_pagination() {
            val notices = listOf(createNotice(id = 3L, category = NoticeCategory.COMPANY))
            val page = PageImpl(notices, PageRequest.of(1, 2), 5)
            every { noticeRepository.findNotices(null, null, "테스트지점", any()) } returns page

            val result = noticeService.getPosts(userId, null, null, 2, 2)

            assertThat(result.currentPage).isEqualTo(2)
            assertThat(result.size).isEqualTo(2)
            assertThat(result.totalCount).isEqualTo(5)
            assertThat(result.totalPages).isEqualTo(3)
        }

        @Test
        @DisplayName("빈 결과 - 매칭 없는 검색어 -> 빈 배열, totalCount=0")
        fun getPosts_emptyResult() {
            val page = PageImpl<Notice>(emptyList(), PageRequest.of(0, 10), 0)
            every { noticeRepository.findNotices(null, "없는검색어", "테스트지점", any()) } returns page

            val result = noticeService.getPosts(userId, null, "없는검색어", 1, 10)

            assertThat(result.content).isEmpty()
            assertThat(result.totalCount).isEqualTo(0)
        }

        @Test
        @DisplayName("잘못된 카테고리 - INVALID -> InvalidNoticeCategoryException")
        fun getPosts_invalidCategory() {
            assertThatThrownBy { noticeService.getPosts(userId, "INVALID", null, 1, 10) }
                .isInstanceOf(InvalidNoticeCategoryException::class.java)
        }

        @Test
        @DisplayName("검색 키워드 100자 제한 - 101자 -> 100자로 잘림")
        fun getPosts_searchTruncated() {
            val longSearch = "가".repeat(101)
            val truncated = "가".repeat(100)
            val page = PageImpl<Notice>(emptyList(), PageRequest.of(0, 10), 0)
            every { noticeRepository.findNotices(null, truncated, "테스트지점", any()) } returns page

            val result = noticeService.getPosts(userId, null, longSearch, 1, 10)

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
            every { noticeRepository.findAllNotices(null, null, any()) } returns page

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

        @BeforeEach
        fun stubEmployeeReference() {
            every { employeeRepository.getReferenceById(1L) } returns
                Employee(id = 1L, employeeCode = "10000001", name = "작성자")
        }

        @Test
        @DisplayName("회사공지 작성 성공 - category=COMPANY -> DB에 회사공지로 저장")
        fun createNotice_company_success() {
            val request = NoticeCreateRequest(
                title = "테스트 공지",
                scope = "영업사원",
                category = "COMPANY",
                content = "<p>내용</p>"
            )
            every { noticeRepository.save(any<Notice>()) } answers { firstArg() }

            val result = noticeService.createNotice(request, 1L)

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
                scope = "영업사원",
                category = "BRANCH",
                content = "<p>지점 내용</p>",
                branch = "서울1지점",
                branchCode = "1101"
            )
            every { noticeRepository.save(any<Notice>()) } answers { firstArg() }

            val result = noticeService.createNotice(request, 1L)

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
                scope = "영업사원",
                category = "EDUCATION",
                content = "<p>교육 내용</p>"
            )
            every { noticeRepository.save(any<Notice>()) } answers { firstArg() }

            val result = noticeService.createNotice(request, 1L)

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
                scope = "영업사원",
                category = "COMPANY",
                content = "<p>내용</p>",
                branch = "잘못된지점",
                branchCode = "9999"
            )
            every { noticeRepository.save(any<Notice>()) } answers { firstArg() }

            val result = noticeService.createNotice(request, 1L)

            assertThat(result.branch).isNull()
            assertThat(result.branchCode).isNull()
        }

        @Test
        @DisplayName("잘못된 카테고리 -> InvalidNoticeCategoryException")
        fun createNotice_invalidCategory() {
            val request = NoticeCreateRequest(title = "공지", scope = "영업사원", category = "UNKNOWN", content = "내용")

            assertThatThrownBy { noticeService.createNotice(request, 1L) }
                .isInstanceOf(InvalidNoticeCategoryException::class.java)
        }

        @Test
        @DisplayName("지점공지 branch 누락 -> BranchRequiredException")
        fun createNotice_branchRequired() {
            val request = NoticeCreateRequest(
                title = "지점 공지",
                scope = "영업사원",
                category = "BRANCH",
                content = "<p>내용</p>",
                branch = null,
                branchCode = null
            )

            assertThatThrownBy { noticeService.createNotice(request, 1L) }
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
            every { noticeRepository.findById(10L) } returns Optional.of(existing)
            every { noticeRepository.save(any<Notice>()) } answers { firstArg() }

            val request = NoticeUpdateRequest(
                title = "수정된 제목",
                scope = "영업사원",
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
            every { noticeRepository.findById(10L) } returns Optional.of(existing)
            every { noticeRepository.save(any<Notice>()) } answers { firstArg() }

            val request = NoticeUpdateRequest(
                title = "지점 공지로 변경",
                scope = "영업사원",
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
            val request = NoticeUpdateRequest(title = "제목", scope = "영업사원", category = "COMPANY", content = "내용")

            assertThatThrownBy { noticeService.updateNotice(0L, request) }
                .isInstanceOf(InvalidNoticeIdException::class.java)
        }

        @Test
        @DisplayName("존재하지 않는 공지 -> NoticePostNotFoundException")
        fun updateNotice_notFound() {
            every { noticeRepository.findById(999L) } returns Optional.empty()
            val request = NoticeUpdateRequest(title = "제목", scope = "영업사원", category = "COMPANY", content = "내용")

            assertThatThrownBy { noticeService.updateNotice(999L, request) }
                .isInstanceOf(NoticePostNotFoundException::class.java)
        }

        @Test
        @DisplayName("삭제된 공지 수정 -> NoticePostNotFoundException")
        fun updateNotice_deleted() {
            val existing = createNotice(id = 10L, isDeleted = true)
            every { noticeRepository.findById(10L) } returns Optional.of(existing)
            val request = NoticeUpdateRequest(title = "제목", scope = "영업사원", category = "COMPANY", content = "내용")

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
            every { noticeRepository.findById(10L) } returns Optional.of(existing)
            every { noticeRepository.save(any<Notice>()) } answers { firstArg() }

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
            every { noticeRepository.findById(999L) } returns Optional.empty()

            assertThatThrownBy { noticeService.deleteNotice(999L) }
                .isInstanceOf(NoticePostNotFoundException::class.java)
        }

        @Test
        @DisplayName("이미 삭제된 공지 -> NoticePostNotFoundException")
        fun deleteNotice_alreadyDeleted() {
            val existing = createNotice(id = 10L, isDeleted = true)
            every { noticeRepository.findById(10L) } returns Optional.of(existing)

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
            every { organizationRepository.findAll() } returns orgs

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
            every { organizationRepository.findAll() } returns orgs

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
            every { organizationRepository.findAll() } returns orgs

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
            every { organizationRepository.findAll() } returns orgs

            val result = noticeService.getNoticeFormMeta()

            assertThat(result.branches).hasSize(1)
        }

        @Test
        @DisplayName("빈 org 테이블 - branches 빈 배열")
        fun getNoticeFormMeta_empty() {
            every { organizationRepository.findAll() } returns emptyList()

            val result = noticeService.getNoticeFormMeta()

            assertThat(result.categories).hasSize(3)
            assertThat(result.branches).isEmpty()
        }
    }

    @Nested
    @DisplayName("uploadNoticeImage - 첨부 이미지 업로드")
    inner class UploadNoticeImageTests {

        private fun mockImage(name: String = "photo.png", contentType: String = "image/png", bytes: ByteArray = ByteArray(2048)): MultipartFile =
            MockMultipartFile("image", name, contentType, bytes)

        @Test
        @DisplayName("정상 업로드 - 활성 공지 + 유효 파일 -> NoticeImageResponse 반환 + UploadFile 적재")
        fun uploadNoticeImage_success() {
            val notice = createNotice(id = 100L, category = NoticeCategory.COMPANY)
            every { noticeRepository.findById(100L) } returns Optional.of(notice)
            every { fileStorageService.uploadNoticeImage(any(), 100L) } returns "uploads/notice/2026/05/11/abc-uuid.png"
            every { uploadFileRepository.save(any<UploadFile>()) } answers {
                val arg = firstArg<UploadFile>()
                UploadFile(
                    id = 555L,
                    name = arg.name,
                    uniqueKey = arg.uniqueKey,
                    fileSize = arg.fileSize,
                    parentType = arg.parentType,
                    parentId = arg.parentId,
                    isDeleted = arg.isDeleted
                )
            }

            val result = noticeService.uploadNoticeImage(100L, mockImage())

            assertThat(result.id).isEqualTo(555L)
            assertThat(result.url)
                .isEqualTo("https://test-bucket.s3.ap-northeast-2.amazonaws.com/private/uploads/notice/2026/05/11/abc-uuid.png?X-Amz-Signature=test")
            assertThat(result.sortOrder).isEqualTo(0)
        }

        @Test
        @DisplayName("0 이하 noticeId -> InvalidNoticeIdException")
        fun uploadNoticeImage_invalidId() {
            assertThatThrownBy { noticeService.uploadNoticeImage(0L, mockImage()) }
                .isInstanceOf(InvalidNoticeIdException::class.java)
        }

        @Test
        @DisplayName("존재하지 않는 공지 -> NoticePostNotFoundException")
        fun uploadNoticeImage_noticeNotFound() {
            every { noticeRepository.findById(999L) } returns Optional.empty()

            assertThatThrownBy { noticeService.uploadNoticeImage(999L, mockImage()) }
                .isInstanceOf(NoticePostNotFoundException::class.java)
        }

        @Test
        @DisplayName("삭제된 공지 -> NoticePostNotFoundException")
        fun uploadNoticeImage_deletedNotice() {
            val notice = createNotice(id = 50L, isDeleted = true)
            every { noticeRepository.findById(50L) } returns Optional.of(notice)

            assertThatThrownBy { noticeService.uploadNoticeImage(50L, mockImage()) }
                .isInstanceOf(NoticePostNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("deleteNoticeImage - 첨부 이미지 삭제")
    inner class DeleteNoticeImageTests {

        @Test
        @DisplayName("정상 삭제 - parent 일치 -> S3 DELETE + soft-delete")
        fun deleteNoticeImage_success() {
            val uploadFile = createUploadFile(id = 200L, uniqueKey = "uploads/notice/2026/05/11/abc.png", parentId = 42L)
            every {
                uploadFileRepository.findByIdAndParentTypeAndParentIdAndIsDeletedFalse(200L, "Notice", 42L)
            } returns uploadFile
            every { storageService.deletePrivate(any()) } just Runs

            noticeService.deleteNoticeImage(42L, 200L)

            assertThat(uploadFile.isDeleted).isTrue()
        }

        @Test
        @DisplayName("0 이하 noticeId -> InvalidNoticeIdException")
        fun deleteNoticeImage_invalidNoticeId() {
            assertThatThrownBy { noticeService.deleteNoticeImage(0L, 1L) }
                .isInstanceOf(InvalidNoticeIdException::class.java)
        }

        @Test
        @DisplayName("0 이하 imageId -> InvalidImageIdException")
        fun deleteNoticeImage_invalidImageId() {
            assertThatThrownBy { noticeService.deleteNoticeImage(1L, 0L) }
                .isInstanceOf(InvalidImageIdException::class.java)
        }

        @Test
        @DisplayName("imageId 미존재 또는 parent 불일치 -> InvalidImageIdException")
        fun deleteNoticeImage_notFoundOrMismatch() {
            every {
                uploadFileRepository.findByIdAndParentTypeAndParentIdAndIsDeletedFalse(999L, "Notice", 42L)
            } returns null

            assertThatThrownBy { noticeService.deleteNoticeImage(42L, 999L) }
                .isInstanceOf(InvalidImageIdException::class.java)
        }

        @Test
        @DisplayName("uniqueKey 빈값 - S3 DELETE 호출 생략 + soft-delete만 수행")
        fun deleteNoticeImage_blankUniqueKey() {
            val uploadFile = createUploadFile(id = 200L, uniqueKey = "", parentId = 42L)
            every {
                uploadFileRepository.findByIdAndParentTypeAndParentIdAndIsDeletedFalse(200L, "Notice", 42L)
            } returns uploadFile

            noticeService.deleteNoticeImage(42L, 200L)

            assertThat(uploadFile.isDeleted).isTrue()
        }
    }

    private fun createOrg(
        orgNameLevel3: String? = null,
        orgNameLevel4: String? = null,
        orgNameLevel5: String? = null,
        costCenterLevel4: String? = null,
        costCenterLevel5: String? = null
    ): Organization = Organization(
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
        isDeleted = isDeleted
    ).apply {
        if (createdDate != null) createdAt = createdDate
    }

    private fun createUploadFile(
        id: Long = 1L,
        sfid: String? = null,
        uniqueKey: String? = "test/file.jpg",
        parentType: String = "Notice",
        parentId: Long? = null,
        createdDate: LocalDateTime? = LocalDateTime.of(2026, 1, 1, 0, 0, 0)
    ): UploadFile = UploadFile(
        id = id,
        sfid = sfid,
        uniqueKey = uniqueKey,
        parentType = parentType,
        parentId = parentId
    ).apply {
        if (createdDate != null) createdAt = createdDate
    }
}
