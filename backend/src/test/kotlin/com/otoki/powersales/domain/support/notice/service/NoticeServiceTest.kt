package com.otoki.powersales.domain.support.notice.service

import com.otoki.powersales.platform.common.entity.UploadFile
import com.otoki.powersales.platform.common.repository.UploadFileRepository
import com.otoki.powersales.platform.common.service.FileStorageService
import com.otoki.powersales.platform.common.storage.StorageService
import com.otoki.powersales.platform.common.storage.UploadResult
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.domain.support.notice.dto.request.NoticeCreateRequest
import com.otoki.powersales.domain.support.notice.dto.request.NoticeUpdateRequest
import com.otoki.powersales.domain.support.notice.entity.Notice
import com.otoki.powersales.domain.support.notice.entity.NoticePushLog
import com.otoki.powersales.domain.support.notice.enums.NoticeCategory
import com.otoki.powersales.domain.support.notice.enums.NoticeScope
import com.otoki.powersales.domain.support.notice.enums.NoticeStatus
import com.otoki.powersales.domain.support.notice.exception.BranchRequiredException
import com.otoki.powersales.domain.support.notice.exception.NoticeNotPublishedException
import com.otoki.powersales.domain.support.notice.exception.NoticeScopeNotPushableException
import com.otoki.powersales.domain.support.notice.exception.NoticeCategoryNotPushableException
import com.otoki.powersales.domain.support.notice.repository.NoticePushLogRepository
import com.otoki.powersales.platform.push.sender.FcmSendResult
import com.otoki.powersales.platform.push.sender.FcmSender
import com.otoki.powersales.domain.support.notice.exception.InvalidImageIdException
import com.otoki.powersales.domain.support.notice.exception.BranchNoticeOnlyException
import com.otoki.powersales.domain.support.notice.exception.InvalidNoticeCategoryException
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.domain.support.notice.exception.InvalidNoticeIdException
import com.otoki.powersales.domain.support.notice.exception.NoticeVersionConflictException
import com.otoki.powersales.domain.support.notice.exception.NoticePostNotFoundException
import com.otoki.powersales.domain.support.notice.repository.NoticeRepository
import com.otoki.powersales.domain.org.organization.entity.Organization
import com.otoki.powersales.domain.org.organization.repository.OrganizationRepository
import com.otoki.powersales.domain.activity.schedule.service.WomenScheduleBranchResolver
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import com.otoki.powersales.domain.support.notice.exception.BranchNotAllowedException
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
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
    private val noticePushLogRepository: NoticePushLogRepository = mockk()
    private val uploadFileRepository: UploadFileRepository = mockk()
    private val employeeRepository: EmployeeRepository = mockk()
    private val organizationRepository: OrganizationRepository = mockk()
    private val fileStorageService: FileStorageService = mockk()
    private val storageService: StorageService = mockk()
    private val fcmSender: FcmSender = mockk()
    private val womenScheduleBranchResolver: WomenScheduleBranchResolver = mockk()

    private lateinit var noticeService: NoticeService

    @BeforeEach
    fun setUp() {
        noticeService = NoticeService(
            noticeRepository,
            noticePushLogRepository,
            uploadFileRepository,
            employeeRepository,
            fileStorageService,
            storageService,
            fcmSender,
            womenScheduleBranchResolver
        )
        // 공지 이미지는 private presigned 조회 — uniqueKey 를 받아 presigned 형태 URL 반환 stub.
        every { storageService.getPresignedUrl(any(), any()) } answers {
            "https://test-bucket.s3.ap-northeast-2.amazonaws.com/private/${firstArg<String>()}?X-Amz-Signature=test"
        }
        // 상세 조회 시 발송 이력 조회 — 기본 미발송 stub (개별 테스트에서 override 가능).
        every { noticePushLogRepository.countByNoticeId(any()) } returns 0L
        every { noticePushLogRepository.findFirstByNoticeIdOrderByCreatedAtDesc(any()) } returns null
        // 지점 스코프 화이트리스트 — 기본은 서울1지점(1101) 단일. 지점공지 저장/검증에 사용.
        // 개별 테스트에서 override 가능(전사 다중 지점, 빈 목록 등).
        every { womenScheduleBranchResolver.resolveBranches(any()) } returns listOf(
            BranchResponse(branchCode = "1101", branchName = "[수도권] 서울1지점")
        )
    }

    /** 테스트용 WebUserPrincipal 생성 — 지점공지 지점 스코프/role 검증에 필요한 필드만 채운다. */
    private fun principalOf(
        employeeId: Long = 1L,
        role: String? = null,
        costCenterCode: String? = "1101",
        profileName: String? = null,
        isSalesSupport: Boolean = false
    ): WebUserPrincipal = WebUserPrincipal(
        userId = 1L,
        usernameValue = "tester@otoki.com",
        employeeCode = "10000001",
        employeeId = employeeId,
        role = role,
        costCenterCode = costCenterCode,
        profileName = profileName,
        profileId = null,
        isSalesSupport = isSalesSupport,
        passwordChangeRequired = false,
        permissions = emptySet(),
        encodedPassword = "",
        grantedAuthorities = emptyList(),
        active = true
    )

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

            val result = noticeService.getNoticeDetail(42L, publishedOnly = false)

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

            val result = noticeService.getNoticeDetail(10L, publishedOnly = false)

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

            val result = noticeService.getNoticeDetail(10L, publishedOnly = false)

            assertThat(result.images).hasSize(2)
            assertThat(result.images[0].id).isEqualTo(1L)
            assertThat(result.images[1].id).isEqualTo(4L)
        }

        @Test
        @DisplayName("존재하지 않는 공지 ID -> NoticePostNotFoundException")
        fun getNoticeDetail_notFound() {
            every { noticeRepository.findById(999L) } returns Optional.empty()

            assertThatThrownBy { noticeService.getNoticeDetail(999L, publishedOnly = false) }
                .isInstanceOf(NoticePostNotFoundException::class.java)
        }

        @Test
        @DisplayName("삭제된 공지 조회 -> NoticePostNotFoundException")
        fun getNoticeDetail_deleted() {
            val notice = createNotice(id = 10L, isDeleted = true)

            every { noticeRepository.findById(10L) } returns Optional.of(notice)

            assertThatThrownBy { noticeService.getNoticeDetail(10L, publishedOnly = false) }
                .isInstanceOf(NoticePostNotFoundException::class.java)
        }

        @Test
        @DisplayName("카테고리 매핑 - COMPANY -> COMPANY/회사공지")
        fun getNoticeDetail_categoryMapping_company() {
            val notice = createNotice(id = 1L, category = NoticeCategory.COMPANY)
            every { noticeRepository.findById(1L) } returns Optional.of(notice)
            every { uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse("Notice", 1L) } returns emptyList()

            val result = noticeService.getNoticeDetail(1L, publishedOnly = false)

            assertThat(result.category).isEqualTo("COMPANY")
            assertThat(result.categoryName).isEqualTo("회사공지")
        }

        @Test
        @DisplayName("카테고리 매핑 - BRANCH -> BRANCH/지점공지")
        fun getNoticeDetail_categoryMapping_branch() {
            val notice = createNotice(id = 1L, category = NoticeCategory.BRANCH)
            every { noticeRepository.findById(1L) } returns Optional.of(notice)
            every { uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse("Notice", 1L) } returns emptyList()

            val result = noticeService.getNoticeDetail(1L, publishedOnly = false)

            assertThat(result.category).isEqualTo("BRANCH")
            assertThat(result.categoryName).isEqualTo("지점공지")
        }

        @Test
        @DisplayName("카테고리 매핑 - EDUCATION -> EDUCATION/교육")
        fun getNoticeDetail_categoryMapping_education() {
            val notice = createNotice(id = 1L, category = NoticeCategory.EDUCATION)
            every { noticeRepository.findById(1L) } returns Optional.of(notice)
            every { uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse("Notice", 1L) } returns emptyList()

            val result = noticeService.getNoticeDetail(1L, publishedOnly = false)

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

            val result = noticeService.getNoticeDetail(1L, publishedOnly = false)

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

            val result = noticeService.getNoticeDetail(1L, publishedOnly = false)

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

            val result = noticeService.getNoticeDetail(1L, publishedOnly = false)

            // 미적재 refid → placeholder 그대로 (깨진 이미지로 노출되되 본문 오염 없음)
            assertThat(result.content).isEqualTo("""<img src="notice-image://MISSING" data-refid="MISSING">""")
        }

        @Test
        @DisplayName("신규 인라인 이미지 - refid=upload_file.id 로 매칭하여 presigned 치환 + 첨부 목록에서 제외")
        fun getNoticeDetail_rewriteInlineImage_byId() {
            val notice = createNotice(
                id = 1L, category = NoticeCategory.COMPANY,
                contents = """<p>본문</p><img src="notice-image://777" data-refid="777">"""
            )
            every { noticeRepository.findById(1L) } returns Optional.of(notice)
            // 신규 업로드분: sfid 없음, upload_kbn=INLINE, refid 는 id(777) 로 매칭되어야 한다.
            every { uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse("Notice", 1L) } returns listOf(
                createUploadFile(id = 777L, sfid = null, uniqueKey = "uploads/notice/2026/06/26/inline.png", uploadKbn = "INLINE")
            )

            val result = noticeService.getNoticeDetail(1L, publishedOnly = false)

            // 본문은 id 기반 매칭으로 presigned 치환
            assertThat(result.content).contains("private/uploads/notice/2026/06/26/inline.png")
            assertThat(result.content).doesNotContain("notice-image://")
            // INLINE 은 하단 첨부 목록에서 제외
            assertThat(result.images).isEmpty()
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

            val result = noticeService.getNoticeDetail(1L, publishedOnly = false)

            assertThat(result.content).isEqualTo("<p>이미지 없는 본문</p>")
        }

        @Test
        @DisplayName("카테고리 null -> 빈 문자열 반환")
        fun getNoticeDetail_categoryNull() {
            val notice = createNotice(id = 1L, category = null)
            every { noticeRepository.findById(1L) } returns Optional.of(notice)
            every { uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse("Notice", 1L) } returns emptyList()

            val result = noticeService.getNoticeDetail(1L, publishedOnly = false)

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

        // 등록자(id=1L) 는 costCenterCode="1101" / orgName="작성자소속" 을 가진 조장 가정.
        @BeforeEach
        fun stubEmployeeReference() {
            val creator = Employee(
                id = 1L,
                employeeCode = "10000001",
                name = "작성자",
                orgName = "작성자소속",
                costCenterCode = "1101"
            )
            every { employeeRepository.findById(1L) } returns Optional.of(creator)
            // syncInlineImages cleanup 단계의 이 공지 소속 INLINE 조회 — 기본은 정리 대상 없음.
            every {
                uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse("Notice", any())
            } returns emptyList()
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

            val result = noticeService.createNotice(request, principalOf())

            assertThat(result.title).isEqualTo("테스트 공지")
            assertThat(result.category).isEqualTo("COMPANY")
            assertThat(result.categoryName).isEqualTo("회사공지")
            assertThat(result.branch).isNull()
            assertThat(result.branchCode).isNull()
        }

        @Test
        @DisplayName("붙여넣기 base64 인라인 이미지 - 저장 시 S3 업로드 + placeholder 로 정규화 (본문에 data URI 미잔존)")
        fun createNotice_normalizesInlineBase64Image() {
            // 1x1 PNG base64 (내용 무관, 디코드만 되면 됨)
            val b64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
            val request = NoticeCreateRequest(
                title = "공지",
                scope = "영업사원",
                category = "COMPANY",
                content = """<p>본문</p><img src="data:image/png;base64,$b64">"""
            )
            val savedNotice = slot<Notice>()
            every { noticeRepository.save(capture(savedNotice)) } answers { firstArg() }
            every { storageService.uploadPrivate("notice", any(), any(), "image/png") } returns
                UploadResult(key = "uploads/notice/2026/07/06/uuid.png", contentType = "image/png", originalName = "inline.png", sizeBytes = 68L)
            every { uploadFileRepository.save(any<UploadFile>()) } returns
                UploadFile(id = 555L, uniqueKey = "uploads/notice/2026/07/06/uuid.png", parentType = "Notice", parentId = 0L, uploadKbn = "INLINE")
            every { uploadFileRepository.findByIdInAndParentTypeAndIsDeletedFalse(any(), "Notice") } returns emptyList()

            noticeService.createNotice(request, principalOf())

            verify { storageService.uploadPrivate("notice", any(), any(), "image/png") }
            // 저장되는 본문(entity)에는 base64 대신 placeholder 만 남는다 → 조회 시 presigned 로 rewrite 되어 모바일 정상 렌더.
            assertThat(savedNotice.captured.contents).doesNotContain("data:image")
            assertThat(savedNotice.captured.contents).contains("notice-image://555")
            assertThat(savedNotice.captured.contents).contains("""data-refid="555"""")
        }

        @Test
        @DisplayName("지점공지 작성 시 선택한 지점코드를 권한 스코프 화이트리스트에서 매칭하여 저장")
        fun createNotice_branch_usesSelectedBranch() {
            val request = NoticeCreateRequest(
                title = "지점 공지",
                scope = "영업사원",
                category = "BRANCH",
                content = "<p>지점 내용</p>",
                // 작성자가 권한 스코프 안에서 고른 지점코드.
                branchCode = "1101"
            )
            every { noticeRepository.save(any<Notice>()) } answers { firstArg() }

            val result = noticeService.createNotice(request, principalOf())

            assertThat(result.category).isEqualTo("BRANCH")
            assertThat(result.categoryName).isEqualTo("지점공지")
            // 지점명은 resolver 화이트리스트에서 코드로 매칭한 값.
            assertThat(result.branch).isEqualTo("[수도권] 서울1지점")
            assertThat(result.branchCode).isEqualTo("1101")
        }

        @Test
        @DisplayName("지점공지 작성 시 선택 지점코드가 권한 스코프 밖이면 BranchNotAllowedException")
        fun createNotice_branch_outsideScopeRejected() {
            val request = NoticeCreateRequest(
                title = "지점 공지",
                scope = "영업사원",
                category = "BRANCH",
                content = "<p>지점 내용</p>",
                branchCode = "1101"
            )
            // 화이트리스트는 타지점(9999)만 — 요청한 1101 은 스코프 밖.
            every { womenScheduleBranchResolver.resolveBranches(any()) } returns listOf(
                BranchResponse(branchCode = "9999", branchName = "타지점")
            )

            assertThatThrownBy { noticeService.createNotice(request, principalOf()) }
                .isInstanceOf(BranchNotAllowedException::class.java)
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

            val result = noticeService.createNotice(request, principalOf())

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

            val result = noticeService.createNotice(request, principalOf())

            assertThat(result.branch).isNull()
            assertThat(result.branchCode).isNull()
        }

        @Test
        @DisplayName("잘못된 카테고리 -> InvalidNoticeCategoryException")
        fun createNotice_invalidCategory() {
            val request = NoticeCreateRequest(title = "공지", scope = "영업사원", category = "UNKNOWN", content = "내용")

            assertThatThrownBy { noticeService.createNotice(request, principalOf()) }
                .isInstanceOf(InvalidNoticeCategoryException::class.java)
        }

        @Test
        @DisplayName("지점공지인데 요청 지점코드 없음 -> BranchRequiredException")
        fun createNotice_branchRequiredWhenNoBranchCode() {
            val request = NoticeCreateRequest(
                title = "지점 공지",
                scope = "영업사원",
                category = "BRANCH",
                content = "<p>내용</p>"
                // branchCode 미지정 → 필수값 누락.
            )

            assertThatThrownBy { noticeService.createNotice(request, principalOf()) }
                .isInstanceOf(BranchRequiredException::class.java)
        }

        @Test
        @DisplayName("조장이 회사공지 작성 시도 -> BranchNoticeOnlyException")
        fun createNotice_leaderRejectsCompany() {
            val request = NoticeCreateRequest(title = "회사 공지", scope = "영업사원", category = "COMPANY", content = "내용")

            assertThatThrownBy { noticeService.createNotice(request, principalOf(role = AppAuthority.LEADER)) }
                .isInstanceOf(BranchNoticeOnlyException::class.java)
        }

        @Test
        @DisplayName("지점장이 교육 작성 시도 -> BranchNoticeOnlyException")
        fun createNotice_branchManagerRejectsEducation() {
            val request = NoticeCreateRequest(title = "교육 공지", scope = "영업사원", category = "EDUCATION", content = "내용")

            assertThatThrownBy { noticeService.createNotice(request, principalOf(role = AppAuthority.BRANCH_MANAGER)) }
                .isInstanceOf(BranchNoticeOnlyException::class.java)
        }

        @Test
        @DisplayName("지점장이 지점공지 작성 -> 정상 등록")
        fun createNotice_branchManagerBranchOk() {
            val request = NoticeCreateRequest(
                title = "지점 공지", scope = "영업사원", category = "BRANCH", content = "내용", branchCode = "1101"
            )
            every { noticeRepository.save(any<Notice>()) } answers { firstArg() }

            val result = noticeService.createNotice(request, principalOf(role = AppAuthority.BRANCH_MANAGER))

            assertThat(result.category).isEqualTo("BRANCH")
        }

        @Test
        @DisplayName("임시저장(publish=false) 작성 -> status=DRAFT 로 저장")
        fun createNotice_draftByDefault() {
            val request = NoticeCreateRequest(title = "임시", scope = "영업사원", category = "COMPANY", content = "내용", publish = false)
            val saved = slot<Notice>()
            every { noticeRepository.save(capture(saved)) } answers { firstArg() }

            noticeService.createNotice(request, principalOf())

            assertThat(saved.captured.status).isEqualTo(NoticeStatus.DRAFT)
        }

        @Test
        @DisplayName("발행(publish=true) 작성 -> status=PUBLISHED 로 저장")
        fun createNotice_publish() {
            val request = NoticeCreateRequest(title = "발행", scope = "영업사원", category = "COMPANY", content = "내용", publish = true)
            val saved = slot<Notice>()
            every { noticeRepository.save(capture(saved)) } answers { firstArg() }

            noticeService.createNotice(request, principalOf())

            assertThat(saved.captured.status).isEqualTo(NoticeStatus.PUBLISHED)
        }
    }

    @Nested
    @DisplayName("updateNotice - 공지사항 수정")
    inner class UpdateNoticeTests {

        // syncInlineImages cleanup 단계가 이 공지 소속 INLINE 조회를 수행한다 — 기본은 정리 대상 없음.
        @BeforeEach
        fun stubInlineCleanup() {
            every {
                uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse("Notice", any())
            } returns emptyList()
        }

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

            val result = noticeService.updateNotice(10L, request, principalOf())

            assertThat(result.title).isEqualTo("수정된 제목")
            assertThat(result.content).isEqualTo("<p>수정 내용</p>")
        }

        @Test
        @DisplayName("카테고리 변경 - COMPANY -> BRANCH 시 선택 지점코드를 권한 스코프에서 매칭하여 저장")
        fun updateNotice_changeCategoryToBranch_usesSelectedBranch() {
            val existing = createNotice(
                id = 10L,
                category = NoticeCategory.COMPANY,
                name = "전체 공지"
            )
            every { noticeRepository.findById(10L) } returns Optional.of(existing)
            every { noticeRepository.save(any<Notice>()) } answers { firstArg() }

            val request = NoticeUpdateRequest(
                title = "지점 공지로 변경",
                scope = "영업사원",
                category = "BRANCH",
                content = "<p>내용</p>",
                // 작성자가 권한 스코프 안에서 고른 지점코드.
                branchCode = "1101"
            )

            val result = noticeService.updateNotice(10L, request, principalOf())

            assertThat(result.category).isEqualTo("BRANCH")
            // 지점명은 resolver 화이트리스트에서 코드로 매칭한 값.
            assertThat(result.branch).isEqualTo("[수도권] 서울1지점")
            assertThat(result.branchCode).isEqualTo("1101")
        }

        @Test
        @DisplayName("BRANCH 로 수정 + 요청/기존 지점코드 모두 없음 -> BranchRequiredException")
        fun updateNotice_toBranchNoBranchCode_branchRequired() {
            val existing = createNotice(
                id = 11L,
                category = NoticeCategory.COMPANY,
                name = "레거시 공지",
                employee = null,
                branchCode = null
            )
            every { noticeRepository.findById(11L) } returns Optional.of(existing)

            val request = NoticeUpdateRequest(
                title = "지점 공지로 변경",
                scope = "영업사원",
                category = "BRANCH",
                content = "<p>내용</p>"
                // branchCode 미지정 + 기존 공지도 지점코드 없음.
            )

            assertThatThrownBy { noticeService.updateNotice(11L, request, principalOf()) }
                .isInstanceOf(BranchRequiredException::class.java)
        }

        @Test
        @DisplayName("0 이하 ID -> InvalidNoticeIdException")
        fun updateNotice_invalidId() {
            val request = NoticeUpdateRequest(title = "제목", scope = "영업사원", category = "COMPANY", content = "내용")

            assertThatThrownBy { noticeService.updateNotice(0L, request, principalOf()) }
                .isInstanceOf(InvalidNoticeIdException::class.java)
        }

        @Test
        @DisplayName("존재하지 않는 공지 -> NoticePostNotFoundException")
        fun updateNotice_notFound() {
            every { noticeRepository.findById(999L) } returns Optional.empty()
            val request = NoticeUpdateRequest(title = "제목", scope = "영업사원", category = "COMPANY", content = "내용")

            assertThatThrownBy { noticeService.updateNotice(999L, request, principalOf()) }
                .isInstanceOf(NoticePostNotFoundException::class.java)
        }

        @Test
        @DisplayName("삭제된 공지 수정 -> NoticePostNotFoundException")
        fun updateNotice_deleted() {
            val existing = createNotice(id = 10L, isDeleted = true)
            every { noticeRepository.findById(10L) } returns Optional.of(existing)
            val request = NoticeUpdateRequest(title = "제목", scope = "영업사원", category = "COMPANY", content = "내용")

            assertThatThrownBy { noticeService.updateNotice(10L, request, principalOf()) }
                .isInstanceOf(NoticePostNotFoundException::class.java)
        }

        @Test
        @DisplayName("조장이 회사공지로 수정 시도 -> BranchNoticeOnlyException")
        fun updateNotice_leaderRejectsCompany() {
            val existing = createNotice(id = 10L, category = NoticeCategory.BRANCH)
            every { noticeRepository.findById(10L) } returns Optional.of(existing)
            val request = NoticeUpdateRequest(title = "제목", scope = "영업사원", category = "COMPANY", content = "내용")

            assertThatThrownBy { noticeService.updateNotice(10L, request, principalOf(role = AppAuthority.LEADER)) }
                .isInstanceOf(BranchNoticeOnlyException::class.java)
        }

        @Test
        @DisplayName("발행된 공지 수정 시 임시저장(publish=false) -> status=DRAFT 로 되돌림")
        fun updateNotice_draftUnpublishesPublished() {
            val existing = createNotice(id = 10L, category = NoticeCategory.COMPANY, status = NoticeStatus.PUBLISHED)
            every { noticeRepository.findById(10L) } returns Optional.of(existing)
            every { noticeRepository.save(any<Notice>()) } answers { firstArg() }
            val request = NoticeUpdateRequest(title = "수정", scope = "영업사원", category = "COMPANY", content = "내용", publish = false)

            noticeService.updateNotice(10L, request, principalOf())

            assertThat(existing.status).isEqualTo(NoticeStatus.DRAFT)
        }

        @Test
        @DisplayName("수정 시 발행(publish=true) -> status=PUBLISHED")
        fun updateNotice_publish() {
            val existing = createNotice(id = 10L, category = NoticeCategory.COMPANY, status = NoticeStatus.DRAFT)
            every { noticeRepository.findById(10L) } returns Optional.of(existing)
            every { noticeRepository.save(any<Notice>()) } answers { firstArg() }
            val request = NoticeUpdateRequest(title = "수정", scope = "영업사원", category = "COMPANY", content = "내용", publish = true)

            noticeService.updateNotice(10L, request, principalOf())

            assertThat(existing.status).isEqualTo(NoticeStatus.PUBLISHED)
        }

        @Test
        @DisplayName("동시 편집 - 요청 version 이 현재 DB version 과 다르면 NoticeVersionConflictException (cleanup 미실행)")
        fun updateNotice_versionConflict() {
            // DB 의 현재 공지는 version=3 (다른 사용자가 먼저 저장해 올라간 상태).
            val existing = createNotice(id = 10L, category = NoticeCategory.COMPANY).apply { version = 3L }
            every { noticeRepository.findById(10L) } returns Optional.of(existing)

            // 이 사용자는 version=2 를 보고 있던 오래된 화면에서 저장 시도.
            val request = NoticeUpdateRequest(
                title = "수정", scope = "영업사원", category = "COMPANY", content = "내용", version = 2L
            )

            assertThatThrownBy { noticeService.updateNotice(10L, request, principalOf()) }
                .isInstanceOf(NoticeVersionConflictException::class.java)

            // 충돌 시 본문 변경도 cleanup(S3 delete)도 일어나지 않아야 한다.
            assertThat(existing.name).isEqualTo("테스트 공지")
            verify(exactly = 0) { storageService.deletePrivate(any()) }
        }

        @Test
        @DisplayName("동시 편집 - 요청 version 이 현재 DB version 과 같으면 정상 수정된다")
        fun updateNotice_versionMatch() {
            val existing = createNotice(id = 10L, category = NoticeCategory.COMPANY).apply { version = 5L }
            every { noticeRepository.findById(10L) } returns Optional.of(existing)
            every { noticeRepository.save(any<Notice>()) } answers { firstArg() }

            val request = NoticeUpdateRequest(
                title = "수정된 제목", scope = "영업사원", category = "COMPANY", content = "내용", version = 5L
            )

            val result = noticeService.updateNotice(10L, request, principalOf())

            assertThat(result.title).isEqualTo("수정된 제목")
        }

        @Test
        @DisplayName("동시 편집 - 요청 version 이 null(구 클라이언트)이면 선제 검사를 건너뛰고 정상 수정한다")
        fun updateNotice_versionNull_skipsCheck() {
            val existing = createNotice(id = 10L, category = NoticeCategory.COMPANY).apply { version = 7L }
            every { noticeRepository.findById(10L) } returns Optional.of(existing)
            every { noticeRepository.save(any<Notice>()) } answers { firstArg() }

            val request = NoticeUpdateRequest(
                title = "수정", scope = "영업사원", category = "COMPANY", content = "내용", version = null
            )

            val result = noticeService.updateNotice(10L, request, principalOf())

            assertThat(result.title).isEqualTo("수정")
        }

        @Test
        @DisplayName("수정 시 본문에 presigned URL(data-refid 소실)만 남아도 기존 인라인 이미지를 보존한다 (uniqueKey 매칭)")
        fun updateNotice_keepsInlineImageReferencedByPresignedUrl() {
            val existing = createNotice(id = 10L, category = NoticeCategory.COMPANY)
            every { noticeRepository.findById(10L) } returns Optional.of(existing)
            every { noticeRepository.save(any<Notice>()) } answers { firstArg() }

            val uniqueKey = "uploads/notice/2026/07/07/keep.png"
            val inlineFile = createUploadFile(
                id = 555L, uniqueKey = uniqueKey, parentId = 10L, uploadKbn = "INLINE"
            )
            // 이 공지 소속 INLINE 파일 1건 — 본문에서 참조 중.
            every {
                uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse("Notice", 10L)
            } returns listOf(inlineFile)

            // 수정 화면이 보내는 본문: 웹 에디터가 data-refid 를 버려 presigned src 만 남은 형태.
            // presigned URL path 에 uniqueKey 가 내재되어 있어 cleanup 이 보존해야 한다.
            val content =
                "<p>내용</p><img src=\"https://test-bucket.s3.ap-northeast-2.amazonaws.com/private/$uniqueKey?X-Amz-Signature=expired\">"
            val request = NoticeUpdateRequest(
                title = "수정", scope = "영업사원", category = "COMPANY", content = content, publish = false
            )

            noticeService.updateNotice(10L, request, principalOf())

            // 삭제되지 않아야 한다 — S3 delete 미호출 + soft-delete 미표기.
            verify(exactly = 0) { storageService.deletePrivate(uniqueKey) }
            assertThat(inlineFile.isDeleted).isNotEqualTo(true)
        }

        @Test
        @DisplayName("수정 시 본문에서 완전히 제거된 인라인 이미지는 정리(S3 delete + soft-delete)한다")
        fun updateNotice_deletesInlineImageRemovedFromBody() {
            val existing = createNotice(id = 10L, category = NoticeCategory.COMPANY)
            every { noticeRepository.findById(10L) } returns Optional.of(existing)
            every { noticeRepository.save(any<Notice>()) } answers { firstArg() }

            val removedKey = "uploads/notice/2026/07/07/removed.png"
            val removedFile = createUploadFile(
                id = 556L, uniqueKey = removedKey, parentId = 10L, uploadKbn = "INLINE"
            )
            every {
                uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse("Notice", 10L)
            } returns listOf(removedFile)
            every { storageService.deletePrivate(removedKey) } just Runs

            // 본문에 이 이미지(refid/uniqueKey) 참조가 전혀 없음 → 정리 대상.
            val request = NoticeUpdateRequest(
                title = "수정", scope = "영업사원", category = "COMPANY", content = "<p>이미지 제거됨</p>", publish = false
            )

            noticeService.updateNotice(10L, request, principalOf())

            verify(exactly = 1) { storageService.deletePrivate(removedKey) }
            assertThat(removedFile.isDeleted).isEqualTo(true)
        }
    }

    @Nested
    @DisplayName("발행 상태 필터 및 publish/unpublish")
    inner class PublishStatusTests {

        @Test
        @DisplayName("모바일 상세(publishedOnly=true) - DRAFT 공지 조회 시 NoticePostNotFoundException")
        fun mobileDetail_draftHidden() {
            val draft = createNotice(id = 30L, status = NoticeStatus.DRAFT)
            every { noticeRepository.findById(30L) } returns Optional.of(draft)

            assertThatThrownBy { noticeService.getNoticeDetail(30L, publishedOnly = true) }
                .isInstanceOf(NoticePostNotFoundException::class.java)
        }

        @Test
        @DisplayName("admin 상세(publishedOnly=false) - DRAFT 공지도 조회 성공")
        fun adminDetail_draftVisible() {
            val draft = createNotice(id = 31L, status = NoticeStatus.DRAFT)
            every { noticeRepository.findById(31L) } returns Optional.of(draft)
            every { uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse("Notice", 31L) } returns emptyList()

            val result = noticeService.getNoticeDetail(31L, publishedOnly = false)

            assertThat(result.status).isEqualTo("DRAFT")
            assertThat(result.statusName).isEqualTo("임시저장")
        }

        @Test
        @DisplayName("publishNotice - status=PUBLISHED 로 전환")
        fun publishNotice_success() {
            val draft = createNotice(id = 40L, status = NoticeStatus.DRAFT)
            every { noticeRepository.findById(40L) } returns Optional.of(draft)

            noticeService.publishNotice(40L)

            assertThat(draft.status).isEqualTo(NoticeStatus.PUBLISHED)
        }

        @Test
        @DisplayName("unpublishNotice - status=DRAFT 로 전환")
        fun unpublishNotice_success() {
            val published = createNotice(id = 41L, status = NoticeStatus.PUBLISHED)
            every { noticeRepository.findById(41L) } returns Optional.of(published)

            noticeService.unpublishNotice(41L)

            assertThat(published.status).isEqualTo(NoticeStatus.DRAFT)
        }

        @Test
        @DisplayName("publishNotice - 0 이하 ID -> InvalidNoticeIdException")
        fun publishNotice_invalidId() {
            assertThatThrownBy { noticeService.publishNotice(0L) }
                .isInstanceOf(InvalidNoticeIdException::class.java)
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
        @DisplayName("정상 조회 - 회사공지/지점공지만 노출(교육 제외) + 지점 목록은 권한 스코프 resolver 결과")
        fun getNoticeFormMeta_success() {
            // 지점 목록 산출(레벨별 지점명 조합/중복제거/불완전제외)은 WomenScheduleBranchResolver 책임 —
            // 여기서는 resolver 가 내려준 화이트리스트를 그대로 branches 로 노출하는지만 검증한다.
            val result = noticeService.getNoticeFormMeta(principalOf())

            // 교육(EDUCATION)은 별도 '교육' 메뉴에서 관리 → 공지사항 폼 카테고리에서 제외.
            assertThat(result.categories).hasSize(2)
            assertThat(result.categories[0].code).isEqualTo("COMPANY")
            assertThat(result.categories[0].name).isEqualTo("회사공지")
            assertThat(result.categories[1].code).isEqualTo("BRANCH")
            assertThat(result.categories[1].name).isEqualTo("지점공지")
            assertThat(result.categories.map { it.code }).doesNotContain("EDUCATION")

            // 기본 resolver stub = [수도권] 서울1지점(1101) 단일.
            assertThat(result.branches).hasSize(1)
            assertThat(result.branches[0].branchCode).isEqualTo("1101")
            assertThat(result.branches[0].branchName).isEqualTo("[수도권] 서울1지점")
        }

        @Test
        @DisplayName("전사 권한 - resolver 다중 지점을 그대로 노출")
        fun getNoticeFormMeta_multipleBranches() {
            every { womenScheduleBranchResolver.resolveBranches(any()) } returns listOf(
                BranchResponse(branchCode = "1101", branchName = "[수도권] 서울1지점"),
                BranchResponse(branchCode = "1102", branchName = "[수도권] 서울2지점")
            )

            val result = noticeService.getNoticeFormMeta(principalOf())

            assertThat(result.branches).hasSize(2)
            assertThat(result.branches[0].branchCode).isEqualTo("1101")
            assertThat(result.branches[1].branchCode).isEqualTo("1102")
        }

        @Test
        @DisplayName("resolver 빈 목록 - branches 빈 배열")
        fun getNoticeFormMeta_empty() {
            every { womenScheduleBranchResolver.resolveBranches(any()) } returns emptyList()

            val result = noticeService.getNoticeFormMeta(principalOf())

            assertThat(result.categories).hasSize(2)
            assertThat(result.branches).isEmpty()
        }

        @Test
        @DisplayName("조장 - 카테고리 지점공지만 노출")
        fun getNoticeFormMeta_leaderBranchOnly() {
            val result = noticeService.getNoticeFormMeta(principalOf(role = AppAuthority.LEADER))

            assertThat(result.categories).hasSize(1)
            assertThat(result.categories[0].code).isEqualTo("BRANCH")
            assertThat(result.categories[0].name).isEqualTo("지점공지")
        }

        @Test
        @DisplayName("지점장 - 카테고리 지점공지만 노출")
        fun getNoticeFormMeta_branchManagerBranchOnly() {
            val result = noticeService.getNoticeFormMeta(principalOf(role = AppAuthority.BRANCH_MANAGER))

            assertThat(result.categories).hasSize(1)
            assertThat(result.categories[0].code).isEqualTo("BRANCH")
        }

        @Test
        @DisplayName("여사원 - 회사공지/지점공지 노출 (교육 제외)")
        fun getNoticeFormMeta_womanExcludesEducation() {
            val result = noticeService.getNoticeFormMeta(principalOf(role = AppAuthority.WOMAN))

            assertThat(result.categories.map { it.code }).containsExactly("COMPANY", "BRANCH")
            assertThat(result.categories.map { it.code }).doesNotContain("EDUCATION")
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
    @DisplayName("uploadNoticeInlineImage - 본문 인라인 이미지 업로드 (신규)")
    inner class UploadNoticeInlineImageTests {

        private fun mockImage(name: String = "inline.png"): MultipartFile =
            MockMultipartFile("image", name, "image/png", ByteArray(1024))

        @Test
        @DisplayName("정상 업로드 - parent_id null + upload_kbn=INLINE 로 적재 + placeholder/previewUrl 반환")
        fun uploadInlineImage_success() {
            every { fileStorageService.uploadNoticeImage(any(), 0L) } returns "uploads/notice/2026/06/26/inline-uuid.png"
            every { uploadFileRepository.save(any<UploadFile>()) } answers {
                val arg = firstArg<UploadFile>()
                // 신규 INLINE 업로드는 parent_id 미정(null), upload_kbn=INLINE 이어야 한다.
                assertThat(arg.parentId).isNull()
                assertThat(arg.uploadKbn).isEqualTo("INLINE")
                UploadFile(
                    id = 777L,
                    name = arg.name,
                    uniqueKey = arg.uniqueKey,
                    parentType = arg.parentType,
                    parentId = arg.parentId,
                    uploadKbn = arg.uploadKbn,
                    isDeleted = arg.isDeleted
                )
            }

            val result = noticeService.uploadNoticeInlineImage(mockImage())

            assertThat(result.refid).isEqualTo("777")
            // placeholder 는 refid=id 로 생성, src 는 만료되는 previewUrl 이 아니라 placeholder 스킴이어야 한다.
            assertThat(result.placeholder).contains("""data-refid="777"""")
            assertThat(result.placeholder).contains("notice-image://777")
            assertThat(result.previewUrl).contains("inline-uuid.png")
        }
    }

    @Nested
    @DisplayName("본문 인라인 이미지 backfill (신규 업로드분 parent_id 채움)")
    inner class BackfillInlineImageTests {

        @BeforeEach
        fun stubEmployeeReference() {
            // createNotice 가 등록자 소속 지점 권위 적용을 위해 findById 로 조회한다 (회사공지는 지점 불요).
            every { employeeRepository.findById(1L) } returns Optional.of(
                Employee(id = 1L, employeeCode = "10000001", name = "작성자", orgName = "작성자소속", costCenterCode = "1101")
            )
            // syncInlineImages cleanup 단계가 이 공지 소속 INLINE 조회를 수행한다 — 기본은 빈 목록(정리 대상 없음).
            every {
                uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse("Notice", any())
            } returns emptyList()
        }

        @Test
        @DisplayName("create - 본문이 참조하는 INLINE 이미지의 parent_id 를 신규 noticeId 로 채운다")
        fun create_backfillsInlineParentId() {
            val request = NoticeCreateRequest(
                title = "이미지 공지",
                scope = "영업사원",
                category = "COMPANY",
                content = """<p>안내</p><img src="notice-image://777" data-refid="777">"""
            )
            every { noticeRepository.save(any<Notice>()) } answers {
                createNotice(id = 321L, contents = firstArg<Notice>().contents)
            }
            val inlineFile = createUploadFile(id = 777L, parentId = null, uploadKbn = "INLINE")
            every {
                uploadFileRepository.findByIdInAndParentTypeAndIsDeletedFalse(listOf(777L), "Notice")
            } returns listOf(inlineFile)

            noticeService.createNotice(request, principalOf())

            assertThat(inlineFile.parentId).isEqualTo(321L)
        }

        @Test
        @DisplayName("보안 - 이미 다른 공지에 소속된 이미지(parent_id != null)는 본문에 refid 를 심어도 탈취되지 않는다")
        fun create_doesNotStealOwnedImage() {
            val request = NoticeCreateRequest(
                title = "탈취 시도", scope = "영업사원", category = "COMPANY",
                content = """<img src="notice-image://999" data-refid="999">"""
            )
            every { noticeRepository.save(any<Notice>()) } answers {
                createNotice(id = 500L, contents = firstArg<Notice>().contents)
            }
            // 999 는 이미 공지 42 에 소속된 INLINE 이미지 — 재부모화 금지 대상.
            val ownedFile = createUploadFile(id = 999L, parentId = 42L, uploadKbn = "INLINE")
            every {
                uploadFileRepository.findByIdInAndParentTypeAndIsDeletedFalse(listOf(999L), "Notice")
            } returns listOf(ownedFile)

            noticeService.createNotice(request, principalOf())

            // parent_id 가 원래 공지(42)에서 바뀌지 않아야 한다.
            assertThat(ownedFile.parentId).isEqualTo(42L)
        }

        @Test
        @DisplayName("보안 - INLINE 이 아닌 첨부 이미지(parent_id=null)는 backfill 대상이 아니다")
        fun create_doesNotBackfillNonInline() {
            val request = NoticeCreateRequest(
                title = "공지", scope = "영업사원", category = "COMPANY",
                content = """<img src="notice-image://888" data-refid="888">"""
            )
            every { noticeRepository.save(any<Notice>()) } answers {
                createNotice(id = 600L, contents = firstArg<Notice>().contents)
            }
            // 888 은 parent_id=null 이지만 upload_kbn 이 INLINE 이 아님 → 대상 제외.
            val attachFile = createUploadFile(id = 888L, parentId = null, uploadKbn = null)
            every {
                uploadFileRepository.findByIdInAndParentTypeAndIsDeletedFalse(listOf(888L), "Notice")
            } returns listOf(attachFile)

            noticeService.createNotice(request, principalOf())

            assertThat(attachFile.parentId).isNull()
        }

        @Test
        @DisplayName("create - 본문에 인라인 placeholder 없으면 backfill 조회(findByIdIn)를 하지 않는다")
        fun create_noInline_skipsBackfill() {
            val request = NoticeCreateRequest(
                title = "텍스트 공지", scope = "영업사원", category = "COMPANY", content = "<p>이미지 없음</p>"
            )
            every { noticeRepository.save(any<Notice>()) } answers {
                createNotice(id = 1L, contents = firstArg<Notice>().contents)
            }

            noticeService.createNotice(request, principalOf())
            // backfill 조회(findByIdIn...) 스텁을 두지 않았으므로, 호출되면 MockKException 으로 실패한다 (= 미호출 검증).
            // cleanup 조회(findByParentTypeAndParentId...)는 @BeforeEach 에서 빈 목록 stub — 항상 수행되나 정리 대상 없음.
        }

        @Test
        @DisplayName("cleanup - 세션 업로드분 중 최종 본문에서 빠진 이미지는 S3+soft-delete 로 정리한다")
        fun create_cleansUpDroppedSessionImage() {
            // 세션에 700(본문 유지) + 701(삭제됨) 업로드. 최종 본문엔 700 만 남음.
            val request = NoticeCreateRequest(
                title = "공지", scope = "영업사원", category = "COMPANY",
                content = """<img src="notice-image://700" data-refid="700">""",
                sessionUploadedRefids = listOf("700", "701")
            )
            every { noticeRepository.save(any<Notice>()) } answers {
                createNotice(id = 800L, contents = firstArg<Notice>().contents)
            }
            val kept = createUploadFile(id = 700L, parentId = null, uploadKbn = "INLINE")
            val dropped = createUploadFile(id = 701L, uniqueKey = "uploads/notice/x/701.png", parentId = null, uploadKbn = "INLINE")
            every {
                uploadFileRepository.findByIdInAndParentTypeAndIsDeletedFalse(listOf(700L), "Notice")
            } returns listOf(kept)
            every {
                uploadFileRepository.findByIdInAndParentTypeAndIsDeletedFalse(listOf(700L, 701L), "Notice")
            } returns listOf(kept, dropped)
            every { storageService.deletePrivate(any()) } just Runs

            noticeService.createNotice(request, principalOf())

            // 700 은 본문 유지 → backfill 로 소속, 삭제 안 됨. 701 은 본문에서 빠짐 → 정리.
            assertThat(kept.parentId).isEqualTo(800L)
            assertThat(kept.isDeleted).isNotEqualTo(true)
            assertThat(dropped.isDeleted).isTrue()
            verify { storageService.deletePrivate("uploads/notice/x/701.png") }
        }

        @Test
        @DisplayName("cleanup 안전 - 세션 목록에 없고 이 공지 소속도 아닌 파일(타 세션 미저장분)은 건드리지 않는다")
        fun create_doesNotTouchForeignSessionImage() {
            // 세션 업로드는 700 뿐. 본문엔 아무 이미지 없음. 902(타 세션 parent_id=null)는 정리 대상 아님.
            val request = NoticeCreateRequest(
                title = "공지", scope = "영업사원", category = "COMPANY",
                content = "<p>본문 이미지 삭제됨</p>",
                sessionUploadedRefids = listOf("700")
            )
            every { noticeRepository.save(any<Notice>()) } answers {
                createNotice(id = 810L, contents = firstArg<Notice>().contents)
            }
            val myDropped = createUploadFile(id = 700L, uniqueKey = "uploads/notice/x/700.png", parentId = null, uploadKbn = "INLINE")
            every {
                uploadFileRepository.findByIdInAndParentTypeAndIsDeletedFalse(listOf(700L), "Notice")
            } returns listOf(myDropped)
            every { storageService.deletePrivate(any()) } just Runs

            noticeService.createNotice(request, principalOf())

            // 내 세션 700 은 본문에서 빠졌으니 정리, 타 세션 902 는 조회 자체를 안 하므로 간섭 없음.
            assertThat(myDropped.isDeleted).isTrue()
            verify(exactly = 1) { storageService.deletePrivate(any()) }
        }

        @Test
        @DisplayName("update cleanup - 수정 시 이 공지 소속(parent_id=noticeId) INLINE 중 본문에서 빠진 것을 정리한다")
        fun update_cleansUpDroppedOwnedImage() {
            val existing = createNotice(id = 50L, category = NoticeCategory.COMPANY, name = "원본")
            every { noticeRepository.findById(50L) } returns Optional.of(existing)
            every { noticeRepository.save(any<Notice>()) } answers { firstArg() }
            // 수정 후 본문엔 이미지 없음. 기존 소속 605(parent_id=50)는 본문에서 빠졌으므로 정리 대상.
            val ownedDropped = createUploadFile(id = 605L, uniqueKey = "uploads/notice/x/605.png", parentId = 50L, uploadKbn = "INLINE")
            every {
                uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse("Notice", 50L)
            } returns listOf(ownedDropped)
            every { storageService.deletePrivate(any()) } just Runs

            val request = NoticeUpdateRequest(
                title = "수정", scope = "영업사원", category = "COMPANY", content = "<p>이미지 제거</p>"
            )
            noticeService.updateNotice(50L, request, principalOf())

            assertThat(ownedDropped.isDeleted).isTrue()
            verify { storageService.deletePrivate("uploads/notice/x/605.png") }
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

    @Nested
    @DisplayName("sendPush - 공지 FCM push 즉시 발송")
    inner class SendPushTests {

        @Test
        @DisplayName("회사공지는 대상 토큰을 모아 딥링크 payload 와 함께 발송하고 이력을 저장한다")
        fun sendsCompanyNoticeToAllTokens() {
            val notice = createNotice(id = 10L, name = "전사 공지", category = NoticeCategory.COMPANY)
                .apply { scope = NoticeScope.FIELD_FEMALE_EMPLOYEE }
            every { noticeRepository.findById(10L) } returns Optional.of(notice)
            every { noticeRepository.findPushTargetTokens(NoticeCategory.COMPANY, null) } returns
                listOf("tok-a", "tok-b")
            every { fcmSender.sendNotificationToTokens(any(), any(), any(), any()) } returns
                FcmSendResult(successCount = 2, failureCount = 0)
            every { employeeRepository.findById(7L) } returns Optional.of(mockk(relaxed = true))
            val logSlot = slot<NoticePushLog>()
            every { noticePushLogRepository.save(capture(logSlot)) } answers { firstArg() }

            val result = noticeService.sendPush(10L, senderId = 7L)

            verify(exactly = 1) {
                fcmSender.sendNotificationToTokens(
                    listOf("tok-a", "tok-b"),
                    "공지사항",
                    "전사 공지",
                    mapOf("type" to "notice", "noticeId" to "10"),
                )
            }
            assertThat(result.targetCount).isEqualTo(2)
            assertThat(result.successCount).isEqualTo(2)
            assertThat(result.failureCount).isEqualTo(0)
            assertThat(logSlot.captured.noticeId).isEqualTo(10L)
            assertThat(logSlot.captured.targetCount).isEqualTo(2)
        }

        @Test
        @DisplayName("지점공지는 공지 지점코드로 대상 토큰을 조회한다")
        fun sendsBranchNoticeByBranchCode() {
            val notice = createNotice(
                id = 11L, name = "지점 공지", category = NoticeCategory.BRANCH, branchCode = "B100"
            ).apply { scope = NoticeScope.FIELD_FEMALE_EMPLOYEE }
            every { noticeRepository.findById(11L) } returns Optional.of(notice)
            every { noticeRepository.findPushTargetTokens(NoticeCategory.BRANCH, "B100") } returns listOf("tok-x")
            every { fcmSender.sendNotificationToTokens(any(), any(), any(), any()) } returns
                FcmSendResult(successCount = 1, failureCount = 0)
            every { employeeRepository.findById(any()) } returns Optional.of(mockk(relaxed = true))
            every { noticePushLogRepository.save(any()) } answers { firstArg() }

            val result = noticeService.sendPush(11L, senderId = 7L)

            verify(exactly = 1) { noticeRepository.findPushTargetTokens(NoticeCategory.BRANCH, "B100") }
            assertThat(result.targetCount).isEqualTo(1)
        }

        @Test
        @DisplayName("대상 토큰이 없으면 발송하지 않고 0 집계 이력을 저장한다")
        fun noopWhenNoTokens() {
            val notice = createNotice(id = 12L, category = NoticeCategory.COMPANY)
                .apply { scope = NoticeScope.FIELD_FEMALE_EMPLOYEE }
            every { noticeRepository.findById(12L) } returns Optional.of(notice)
            every { noticeRepository.findPushTargetTokens(NoticeCategory.COMPANY, null) } returns emptyList()
            every { employeeRepository.findById(any()) } returns Optional.of(mockk(relaxed = true))
            val logSlot = slot<NoticePushLog>()
            every { noticePushLogRepository.save(capture(logSlot)) } answers { firstArg() }

            val result = noticeService.sendPush(12L, senderId = 7L)

            verify(exactly = 0) { fcmSender.sendNotificationToTokens(any(), any(), any(), any()) }
            assertThat(result.targetCount).isEqualTo(0)
            assertThat(logSlot.captured.targetCount).isEqualTo(0)
        }

        @Test
        @DisplayName("임시저장(DRAFT) 공지는 발송할 수 없다")
        fun rejectsDraftNotice() {
            val notice = createNotice(id = 13L, status = NoticeStatus.DRAFT)
                .apply { scope = NoticeScope.FIELD_FEMALE_EMPLOYEE }
            every { noticeRepository.findById(13L) } returns Optional.of(notice)

            assertThatThrownBy { noticeService.sendPush(13L, senderId = 7L) }
                .isInstanceOf(NoticeNotPublishedException::class.java)
            verify(exactly = 0) { fcmSender.sendNotificationToTokens(any(), any(), any(), any()) }
        }

        @Test
        @DisplayName("영업사원 scope 공지는 모바일 미노출이므로 발송할 수 없다")
        fun rejectsSalesEmployeeScope() {
            val notice = createNotice(id = 14L, category = NoticeCategory.COMPANY)
                .apply { scope = NoticeScope.SALES_EMPLOYEE }
            every { noticeRepository.findById(14L) } returns Optional.of(notice)

            assertThatThrownBy { noticeService.sendPush(14L, senderId = 7L) }
                .isInstanceOf(NoticeScopeNotPushableException::class.java)
            verify(exactly = 0) { fcmSender.sendNotificationToTokens(any(), any(), any(), any()) }
        }

        @Test
        @DisplayName("교육 공지는 모바일 목록 미노출이므로 발송할 수 없다")
        fun rejectsEducationCategory() {
            val notice = createNotice(id = 15L, category = NoticeCategory.EDUCATION)
            every { noticeRepository.findById(15L) } returns Optional.of(notice)

            assertThatThrownBy { noticeService.sendPush(15L, senderId = 7L) }
                .isInstanceOf(NoticeCategoryNotPushableException::class.java)
            // 대상 선별 쿼리조차 호출하지 않고 조기 차단한다.
            verify(exactly = 0) { noticeRepository.findPushTargetTokens(any(), any()) }
            verify(exactly = 0) { fcmSender.sendNotificationToTokens(any(), any(), any(), any()) }
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
        branchCode: String? = null,
        employee: Employee? = null,
        isDeleted: Boolean? = false,
        status: NoticeStatus? = NoticeStatus.PUBLISHED,
        createdDate: LocalDateTime? = LocalDateTime.of(2026, 1, 1, 0, 0, 0)
    ): Notice = Notice(
        id = id,
        sfid = sfid,
        name = name,
        category = category,
        contents = contents,
        branch = branch,
        branchCode = branchCode,
        employee = employee,
        isDeleted = isDeleted,
        status = status
    ).apply {
        if (createdDate != null) createdAt = createdDate
    }

    private fun createUploadFile(
        id: Long = 1L,
        sfid: String? = null,
        uniqueKey: String? = "test/file.jpg",
        parentType: String = "Notice",
        parentId: Long? = null,
        uploadKbn: String? = null,
        createdDate: LocalDateTime? = LocalDateTime.of(2026, 1, 1, 0, 0, 0)
    ): UploadFile = UploadFile(
        id = id,
        sfid = sfid,
        uniqueKey = uniqueKey,
        parentType = parentType,
        parentId = parentId,
        uploadKbn = uploadKbn
    ).apply {
        if (createdDate != null) createdAt = createdDate
    }
}
