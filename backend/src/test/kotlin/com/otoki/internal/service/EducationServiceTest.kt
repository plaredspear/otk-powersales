package com.otoki.internal.service

import com.otoki.internal.entity.*
import com.otoki.internal.exception.EducationPostNotFoundException
import com.otoki.internal.exception.InvalidEducationCategoryException
import com.otoki.internal.repository.EducationPostAttachmentRepository
import com.otoki.internal.repository.EducationPostImageRepository
import com.otoki.internal.repository.EducationPostRepository
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
@DisplayName("EducationService 테스트")
class EducationServiceTest {

    @InjectMocks
    private lateinit var educationService: EducationService

    @Mock
    private lateinit var educationPostRepository: EducationPostRepository

    @Mock
    private lateinit var educationPostImageRepository: EducationPostImageRepository

    @Mock
    private lateinit var educationPostAttachmentRepository: EducationPostAttachmentRepository

    private lateinit var testUser: User
    private lateinit var testPost: EducationPost

    @BeforeEach
    fun setUp() {
        testUser = User(
            id = 1L,
            employeeId = "10000001",
            password = "encoded",
            name = "관리자",
            department = "교육팀",
            branchName = "본사",
            role = UserRole.ADMIN,
            workerType = WorkerType.PATROL
        )

        testPost = EducationPost(
            id = 9L,
            category = EducationCategory.TASTING_MANUAL,
            title = "진짬뽕 시식 매뉴얼",
            content = "진짬뽕 시식 방법을 안내합니다.",
            createdBy = testUser,
            isActive = true,
            createdAt = LocalDateTime.of(2020, 8, 10, 0, 0, 0)
        )
    }

    @Test
    @DisplayName("카테고리별 게시물 목록을 조회할 수 있다")
    fun getPosts_Success() {
        // Given
        val posts = listOf(testPost)
        val page = PageImpl(posts, PageRequest.of(0, 10), 1)

        whenever(educationPostRepository.findByCategoryAndIsActiveTrueOrderByCreatedAtDesc(any(), any()))
            .thenReturn(page)

        // When
        val result = educationService.getPosts(
            category = "TASTING_MANUAL",
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
        assertThat(result.content[0].id).isEqualTo(9L)
        assertThat(result.content[0].title).isEqualTo("진짬뽕 시식 매뉴얼")
        assertThat(result.content[0].createdAt).isEqualTo("2020-08-10T00:00:00")
    }

    @Test
    @DisplayName("검색 키워드로 게시물을 조회할 수 있다")
    fun getPosts_WithSearch() {
        // Given
        val posts = listOf(testPost)
        val page = PageImpl(posts, PageRequest.of(0, 10), 1)

        whenever(educationPostRepository.findByCategoryAndSearchWithPaging(any(), any(), any()))
            .thenReturn(page)

        // When
        val result = educationService.getPosts(
            category = "TASTING_MANUAL",
            search = "시식",
            page = 1,
            size = 10
        )

        // Then
        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].title).isEqualTo("진짬뽕 시식 매뉴얼")
    }

    @Test
    @DisplayName("유효하지 않은 카테고리로 조회 시 예외가 발생한다")
    fun getPosts_InvalidCategory() {
        // When & Then
        assertThatThrownBy {
            educationService.getPosts(
                category = "INVALID_CATEGORY",
                search = null,
                page = 1,
                size = 10
            )
        }.isInstanceOf(InvalidEducationCategoryException::class.java)
    }

    @Test
    @DisplayName("조회 결과가 없을 때 빈 리스트를 반환한다")
    fun getPosts_EmptyResult() {
        // Given
        val emptyPage = PageImpl<EducationPost>(emptyList(), PageRequest.of(0, 10), 0)

        whenever(educationPostRepository.findByCategoryAndIsActiveTrueOrderByCreatedAtDesc(any(), any()))
            .thenReturn(emptyPage)

        // When
        val result = educationService.getPosts(
            category = "TASTING_MANUAL",
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
            EducationPostImage(
                id = 1L,
                post = testPost,
                url = "https://storage.example.com/img1.jpg",
                sortOrder = 1
            ),
            EducationPostImage(
                id = 2L,
                post = testPost,
                url = "https://storage.example.com/img2.jpg",
                sortOrder = 2
            )
        )

        val attachments = listOf(
            EducationPostAttachment(
                id = 1L,
                post = testPost,
                fileName = "guide.pdf",
                fileUrl = "https://storage.example.com/guide.pdf",
                fileSize = 2048576,
                contentType = "application/pdf"
            )
        )

        whenever(educationPostRepository.findByIdAndIsActiveTrue(9L))
            .thenReturn(testPost)
        whenever(educationPostImageRepository.findByPostIdOrderBySortOrderAsc(9L))
            .thenReturn(images)
        whenever(educationPostAttachmentRepository.findByPostId(9L))
            .thenReturn(attachments)

        // When
        val result = educationService.getPostDetail(9L)

        // Then
        assertThat(result.id).isEqualTo(9L)
        assertThat(result.category).isEqualTo("TASTING_MANUAL")
        assertThat(result.categoryName).isEqualTo("시식 매뉴얼")
        assertThat(result.title).isEqualTo("진짬뽕 시식 매뉴얼")
        assertThat(result.content).isEqualTo("진짬뽕 시식 방법을 안내합니다.")
        assertThat(result.createdAt).isEqualTo("2020-08-10T00:00:00")
        assertThat(result.images).hasSize(2)
        assertThat(result.images[0].sortOrder).isEqualTo(1)
        assertThat(result.images[1].sortOrder).isEqualTo(2)
        assertThat(result.attachments).hasSize(1)
        assertThat(result.attachments[0].fileName).isEqualTo("guide.pdf")
    }

    @Test
    @DisplayName("존재하지 않는 게시물 조회 시 예외가 발생한다")
    fun getPostDetail_NotFound() {
        // Given
        whenever(educationPostRepository.findByIdAndIsActiveTrue(999L))
            .thenReturn(null)

        // When & Then
        assertThatThrownBy {
            educationService.getPostDetail(999L)
        }.isInstanceOf(EducationPostNotFoundException::class.java)
    }

    @Test
    @DisplayName("비활성 게시물 조회 시 예외가 발생한다")
    fun getPostDetail_InactivePost() {
        // Given
        whenever(educationPostRepository.findByIdAndIsActiveTrue(9L))
            .thenReturn(null) // isActive=false인 경우 null 반환

        // When & Then
        assertThatThrownBy {
            educationService.getPostDetail(9L)
        }.isInstanceOf(EducationPostNotFoundException::class.java)
    }
}
