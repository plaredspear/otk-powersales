package com.otoki.internal.notice.service

import com.otoki.internal.notice.entity.Notice
import com.otoki.internal.notice.entity.UploadFile
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
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("NoticeService 테스트")
class NoticeServiceTest {

    @Mock
    private lateinit var noticeRepository: NoticeRepository

    @Mock
    private lateinit var uploadFileRepository: UploadFileRepository

    private lateinit var noticeService: NoticeService

    @BeforeEach
    fun setUp() {
        noticeService = NoticeService(noticeRepository, uploadFileRepository, "test-bucket")
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

    private fun createNotice(
        id: Long = 1L,
        sfid: String? = null,
        name: String? = "테스트 공지",
        category: String? = "ALL",
        contents: String? = "본문 내용",
        isDeleted: Boolean? = false,
        createdDate: LocalDateTime? = LocalDateTime.of(2026, 1, 1, 0, 0, 0)
    ): Notice = Notice(
        id = id,
        sfid = sfid,
        name = name,
        category = category,
        contents = contents,
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
