package com.otoki.internal.notice.service

import com.otoki.internal.common.entity.User
import com.otoki.internal.common.repository.UserRepository
import com.otoki.internal.notice.entity.Notice
import com.otoki.internal.notice.entity.UploadFile
import com.otoki.internal.notice.exception.InvalidNoticeCategoryException
import com.otoki.internal.notice.exception.NoticePostNotFoundException
import com.otoki.internal.notice.repository.NoticeRepository
import com.otoki.internal.notice.repository.UploadFileRepository
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

    private lateinit var noticeService: NoticeService

    @BeforeEach
    fun setUp() {
        noticeService = NoticeService(noticeRepository, uploadFileRepository, userRepository, "test-bucket")
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
                category = "ALL",
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
            assertThat(result.category).isEqualTo("ALL")
            assertThat(result.categoryName).isEqualTo("전체공지")
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
            val notice = createNotice(id = 10L, sfid = null, name = "공지", category = "BRANCH")

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
            val notice = createNotice(id = 10L, sfid = "some-sfid", name = "공지", category = "ALL")

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
            val notice = createNotice(id = 10L, sfid = "sfid-1", category = "ALL")
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
        @DisplayName("카테고리 매핑 - 한글 '회사공지' -> ALL/전체공지")
        fun getNoticeDetail_categoryMapping_korean() {
            // Given
            val notice = createNotice(id = 1L, category = "회사공지")
            whenever(noticeRepository.findById(1L)).thenReturn(Optional.of(notice))

            // When
            val result = noticeService.getNoticeDetail(1L)

            // Then
            assertThat(result.category).isEqualTo("ALL")
            assertThat(result.categoryName).isEqualTo("전체공지")
        }

        @Test
        @DisplayName("카테고리 매핑 - 한글 '영업부/지점공지' -> BRANCH/지점공지")
        fun getNoticeDetail_categoryMapping_branch() {
            // Given
            val notice = createNotice(id = 1L, category = "영업부/지점공지")
            whenever(noticeRepository.findById(1L)).thenReturn(Optional.of(notice))

            // When
            val result = noticeService.getNoticeDetail(1L)

            // Then
            assertThat(result.category).isEqualTo("BRANCH")
            assertThat(result.categoryName).isEqualTo("지점공지")
        }

        @Test
        @DisplayName("카테고리 매핑 - 미등록 값 -> 원본 값 유지")
        fun getNoticeDetail_categoryMapping_unknown() {
            // Given
            val notice = createNotice(id = 1L, category = "특별공지")
            whenever(noticeRepository.findById(1L)).thenReturn(Optional.of(notice))

            // When
            val result = noticeService.getNoticeDetail(1L)

            // Then
            assertThat(result.category).isEqualTo("특별공지")
            assertThat(result.categoryName).isEqualTo("특별공지")
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
        @DisplayName("전체 목록 조회 - category null -> ALL + 사용자 지점 BRANCH 공지 반환")
        fun getPosts_allCategories() {
            // Given
            val notices = listOf(
                createNotice(id = 1L, category = "ALL", name = "전체공지 제목"),
                createNotice(id = 2L, category = "BRANCH", name = "지점공지 제목", branch = "테스트지점")
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
        @DisplayName("회사공지만 조회 - category=COMPANY -> ALL 공지만 반환")
        fun getPosts_companyOnly() {
            // Given
            val notices = listOf(createNotice(id = 1L, category = "ALL", name = "전체공지"))
            val page = PageImpl(notices, PageRequest.of(0, 10), 1)
            whenever(noticeRepository.findNotices(eq("COMPANY"), eq(null), eq("테스트지점"), any())).thenReturn(page)

            // When
            val result = noticeService.getPosts(userId, "COMPANY", null, 1, 10)

            // Then
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].category).isEqualTo("ALL")
            assertThat(result.content[0].categoryName).isEqualTo("전체공지")
        }

        @Test
        @DisplayName("지점공지만 조회 - category=BRANCH -> 사용자 지점 BRANCH 공지만 반환")
        fun getPosts_branchOnly() {
            // Given
            val notices = listOf(createNotice(id = 2L, category = "BRANCH", name = "지점공지", branch = "테스트지점"))
            val page = PageImpl(notices, PageRequest.of(0, 10), 1)
            whenever(noticeRepository.findNotices(eq("BRANCH"), eq(null), eq("테스트지점"), any())).thenReturn(page)

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
            val notices = listOf(createNotice(id = 1L, category = "ALL", name = "영업 목표 안내"))
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
            val notices = listOf(createNotice(id = 3L, category = "ALL"))
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
        @DisplayName("카테고리 매핑 - DB '회사공지' -> 응답 ALL/전체공지")
        fun getPosts_categoryMapping_korean() {
            // Given
            val notices = listOf(createNotice(id = 1L, category = "회사공지", name = "공지"))
            val page = PageImpl(notices, PageRequest.of(0, 10), 1)
            whenever(noticeRepository.findNotices(eq(null), eq(null), eq("테스트지점"), any())).thenReturn(page)

            // When
            val result = noticeService.getPosts(userId, null, null, 1, 10)

            // Then
            assertThat(result.content[0].category).isEqualTo("ALL")
            assertThat(result.content[0].categoryName).isEqualTo("전체공지")
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

    private fun createNotice(
        id: Long = 1L,
        sfid: String? = null,
        name: String? = "테스트 공지",
        category: String? = "ALL",
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
