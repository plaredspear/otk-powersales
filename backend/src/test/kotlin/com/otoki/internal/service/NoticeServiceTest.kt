package com.otoki.internal.service

import com.otoki.internal.entity.*
import com.otoki.internal.exception.InvalidNoticeCategoryException
import com.otoki.internal.exception.NoticePostNotFoundException
import com.otoki.internal.repository.NoticePostImageRepository
import com.otoki.internal.repository.NoticePostRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
@DisplayName("NoticeService 테스트")
class NoticeServiceTest {

    @InjectMocks
    private lateinit var noticeService: NoticeService

    @Mock
    private lateinit var noticePostRepository: NoticePostRepository

    @Mock
    private lateinit var noticePostImageRepository: NoticePostImageRepository

    private lateinit var testUser: User
    private lateinit var testPost: NoticePost

    @BeforeEach
    fun setUp() {
        testUser = User(
            id = 1L,
            employeeId = "10000001",
            password = "encoded",
            name = "관리자",
            orgName = "본사",
            appAuthority = "지점장"
        )

        testPost = NoticePost(
            id = 4L,
            category = NoticeCategory.COMPANY,
            title = "진라면 포장지 변경",
            content = "진라면 포장지 디자인이 변경됩니다.",
            createdBy = testUser,
            isActive = true,
            createdAt = LocalDateTime.of(2020, 8, 9, 0, 0, 0)
        )
    }

    @Test
    @DisplayName("전체 게시물 목록을 조회할 수 있다 (category=null)")
    fun getPosts_AllCategories() {
        // Given
        val posts = listOf(testPost)
        val page = PageImpl(posts, PageRequest.of(0, 10), 1)

        whenever(noticePostRepository.findByIsActiveTrueOrderByCreatedAtDesc(any()))
            .thenReturn(page)

        // When
        val result = noticeService.getPosts(
            category = null,
            search = null,
            page = 1,
            size = 10
        )

        // Then
        assertThat(result.content).hasSize(1)
        assertThat(result.totalCount).isEqualTo(1)
        assertThat(result.totalPages).isEqualTo(1)
        assertThat(result.currentPage).isEqualTo(1)
        assertThat(result.size).isEqualTo(10)
        assertThat(result.content[0].id).isEqualTo(4L)
        assertThat(result.content[0].category).isEqualTo("COMPANY")
        assertThat(result.content[0].categoryName).isEqualTo("회사공지")
        assertThat(result.content[0].title).isEqualTo("진라면 포장지 변경")
        assertThat(result.content[0].createdAt).isEqualTo("2020-08-09T00:00:00")
    }

    @Test
    @DisplayName("분류별 게시물 목록을 조회할 수 있다")
    fun getPosts_ByCategory() {
        // Given
        val posts = listOf(testPost)
        val page = PageImpl(posts, PageRequest.of(0, 10), 1)

        whenever(noticePostRepository.findByCategoryAndIsActiveTrueOrderByCreatedAtDesc(any(), any()))
            .thenReturn(page)

        // When
        val result = noticeService.getPosts(
            category = "COMPANY",
            search = null,
            page = 1,
            size = 10
        )

        // Then
        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].category).isEqualTo("COMPANY")
    }

    @Test
    @DisplayName("전체 검색(제목+내용)으로 게시물을 조회할 수 있다")
    fun getPosts_AllWithSearch() {
        // Given
        val posts = listOf(testPost)
        val page = PageImpl(posts, PageRequest.of(0, 10), 1)

        whenever(noticePostRepository.findBySearchWithPaging(any(), any()))
            .thenReturn(page)

        // When
        val result = noticeService.getPosts(
            category = null,
            search = "포장지",
            page = 1,
            size = 10
        )

        // Then
        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].title).isEqualTo("진라면 포장지 변경")
    }

    @Test
    @DisplayName("분류별 검색(제목+내용)으로 게시물을 조회할 수 있다")
    fun getPosts_ByCategoryWithSearch() {
        // Given
        val posts = listOf(testPost)
        val page = PageImpl(posts, PageRequest.of(0, 10), 1)

        whenever(noticePostRepository.findByCategoryAndSearchWithPaging(any(), any(), any()))
            .thenReturn(page)

        // When
        val result = noticeService.getPosts(
            category = "COMPANY",
            search = "포장지",
            page = 1,
            size = 10
        )

        // Then
        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].category).isEqualTo("COMPANY")
        assertThat(result.content[0].title).isEqualTo("진라면 포장지 변경")
    }

    @Test
    @DisplayName("유효하지 않은 카테고리로 조회 시 예외가 발생한다")
    fun getPosts_InvalidCategory() {
        // When & Then
        assertThatThrownBy {
            noticeService.getPosts(
                category = "INVALID_CATEGORY",
                search = null,
                page = 1,
                size = 10
            )
        }.isInstanceOf(InvalidNoticeCategoryException::class.java)
    }

    @Test
    @DisplayName("조회 결과가 없을 때 빈 리스트를 반환한다")
    fun getPosts_EmptyResult() {
        // Given
        val emptyPage = PageImpl<NoticePost>(emptyList(), PageRequest.of(0, 10), 0)

        whenever(noticePostRepository.findByIsActiveTrueOrderByCreatedAtDesc(any()))
            .thenReturn(emptyPage)

        // When
        val result = noticeService.getPosts(
            category = null,
            search = null,
            page = 1,
            size = 10
        )

        // Then
        assertThat(result.content).isEmpty()
        assertThat(result.totalCount).isEqualTo(0)
    }

    @Test
    @DisplayName("게시물 상세를 조회할 수 있다")
    fun getPostDetail_Success() {
        // Given
        val images = listOf(
            NoticePostImage(
                id = 1L,
                post = testPost,
                url = "https://storage.example.com/notices/4/img1.jpg",
                sortOrder = 1
            ),
            NoticePostImage(
                id = 2L,
                post = testPost,
                url = "https://storage.example.com/notices/4/img2.jpg",
                sortOrder = 2
            )
        )

        whenever(noticePostRepository.findByIdAndIsActiveTrue(4L))
            .thenReturn(testPost)
        whenever(noticePostImageRepository.findByPostIdOrderBySortOrderAsc(4L))
            .thenReturn(images)

        // When
        val result = noticeService.getPostDetail(4L)

        // Then
        assertThat(result.id).isEqualTo(4L)
        assertThat(result.category).isEqualTo("COMPANY")
        assertThat(result.categoryName).isEqualTo("회사공지")
        assertThat(result.title).isEqualTo("진라면 포장지 변경")
        assertThat(result.content).isEqualTo("진라면 포장지 디자인이 변경됩니다.")
        assertThat(result.createdAt).isEqualTo("2020-08-09T00:00:00")
        assertThat(result.images).hasSize(2)
        assertThat(result.images[0].sortOrder).isEqualTo(1)
        assertThat(result.images[1].sortOrder).isEqualTo(2)
    }

    @Test
    @DisplayName("이미지가 없는 게시물도 조회할 수 있다")
    fun getPostDetail_NoImages() {
        // Given
        whenever(noticePostRepository.findByIdAndIsActiveTrue(4L))
            .thenReturn(testPost)
        whenever(noticePostImageRepository.findByPostIdOrderBySortOrderAsc(4L))
            .thenReturn(emptyList())

        // When
        val result = noticeService.getPostDetail(4L)

        // Then
        assertThat(result.id).isEqualTo(4L)
        assertThat(result.images).isEmpty()
    }

    @Test
    @DisplayName("존재하지 않는 게시물 조회 시 예외가 발생한다")
    fun getPostDetail_NotFound() {
        // Given
        whenever(noticePostRepository.findByIdAndIsActiveTrue(999L))
            .thenReturn(null)

        // When & Then
        assertThatThrownBy {
            noticeService.getPostDetail(999L)
        }.isInstanceOf(NoticePostNotFoundException::class.java)
    }

    @Test
    @DisplayName("비활성 게시물 조회 시 예외가 발생한다")
    fun getPostDetail_InactivePost() {
        // Given
        whenever(noticePostRepository.findByIdAndIsActiveTrue(4L))
            .thenReturn(null) // isActive=false인 경우 null 반환

        // When & Then
        assertThatThrownBy {
            noticeService.getPostDetail(4L)
        }.isInstanceOf(NoticePostNotFoundException::class.java)
    }
}
