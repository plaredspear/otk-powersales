package com.otoki.internal.education.service

import com.otoki.internal.education.entity.*
import com.otoki.internal.common.entity.*
import com.otoki.internal.sap.entity.*
import com.otoki.internal.education.exception.EducationPostNotFoundException
import com.otoki.internal.education.exception.InvalidEducationCategoryException
import com.otoki.internal.education.repository.EducationCodeRepository
import com.otoki.internal.education.repository.EducationPostAttachmentRepository
import com.otoki.internal.education.repository.EducationPostRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
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
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("EducationService 테스트")
class EducationServiceTest {

    @InjectMocks
    private lateinit var educationService: EducationService

    @Mock
    private lateinit var educationPostRepository: EducationPostRepository

    @Mock
    private lateinit var educationPostAttachmentRepository: EducationPostAttachmentRepository

    @Mock
    private lateinit var educationCodeRepository: EducationCodeRepository

    private lateinit var testPost: EducationPost

    @BeforeEach
    fun setUp() {
        testPost = EducationPost(
            eduId = "EDU001",
            eduTitle = "진짬뽕 시식 매뉴얼",
            eduContent = "진짬뽕 시식 방법을 안내합니다.",
            eduCode = "TASTING_MANUAL",
            empCode = "10000001",
            instDate = LocalDateTime.of(2020, 8, 10, 0, 0, 0)
        )
    }

    @Nested
    @DisplayName("getPosts - 게시물 목록 조회")
    inner class GetPostsTests {

        @Test
        @DisplayName("정상 조회 - 카테고리별 게시물 목록 반환")
        fun getPosts_success() {
            // Given
            val posts = listOf(testPost)
            val page = PageImpl(posts, PageRequest.of(0, 10), 1)

            whenever(educationCodeRepository.existsById("TASTING_MANUAL")).thenReturn(true)
            whenever(educationPostRepository.findByEduCodeOrderByInstDateDesc(any(), any()))
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
            assertThat(result.content[0].id).isEqualTo("EDU001")
            assertThat(result.content[0].title).isEqualTo("진짬뽕 시식 매뉴얼")
            assertThat(result.content[0].createdAt).isEqualTo("2020-08-10T00:00:00")
        }

        @Test
        @DisplayName("검색 조회 - 검색 키워드로 게시물 목록 반환")
        fun getPosts_withSearch() {
            // Given
            val posts = listOf(testPost)
            val page = PageImpl(posts, PageRequest.of(0, 10), 1)

            whenever(educationCodeRepository.existsById("TASTING_MANUAL")).thenReturn(true)
            whenever(educationPostRepository.findByEduCodeAndSearchWithPaging(any(), any(), any()))
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
        @DisplayName("유효하지 않은 카테고리 - InvalidEducationCategoryException")
        fun getPosts_invalidCategory() {
            // Given
            whenever(educationCodeRepository.existsById("INVALID_CATEGORY")).thenReturn(false)

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
        @DisplayName("빈 결과 - 조회 결과 없을 때 빈 리스트 반환")
        fun getPosts_emptyResult() {
            // Given
            val emptyPage = PageImpl<EducationPost>(emptyList(), PageRequest.of(0, 10), 0)

            whenever(educationCodeRepository.existsById("TASTING_MANUAL")).thenReturn(true)
            whenever(educationPostRepository.findByEduCodeOrderByInstDateDesc(any(), any()))
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
    }

    @Nested
    @DisplayName("getPostDetail - 게시물 상세 조회")
    inner class GetPostDetailTests {

        @Test
        @DisplayName("정상 조회 - 게시물 상세 + 첨부파일 반환")
        fun getPostDetail_success() {
            // Given
            val attachments = listOf(
                EducationPostAttachment(
                    eduId = "EDU001",
                    eduFileKey = "file-key-001",
                    eduFileType = "pdf",
                    eduFileOrgNm = "guide.pdf"
                )
            )

            val eduCode = EducationCode(
                eduCode = "TASTING_MANUAL",
                eduCodeNm = "시식 매뉴얼"
            )

            whenever(educationPostRepository.findById("EDU001"))
                .thenReturn(Optional.of(testPost))
            whenever(educationPostAttachmentRepository.findByEduId("EDU001"))
                .thenReturn(attachments)
            whenever(educationCodeRepository.findById("TASTING_MANUAL"))
                .thenReturn(Optional.of(eduCode))

            // When
            val result = educationService.getPostDetail("EDU001")

            // Then
            assertThat(result.id).isEqualTo("EDU001")
            assertThat(result.category).isEqualTo("TASTING_MANUAL")
            assertThat(result.categoryName).isEqualTo("시식 매뉴얼")
            assertThat(result.title).isEqualTo("진짬뽕 시식 매뉴얼")
            assertThat(result.content).isEqualTo("진짬뽕 시식 방법을 안내합니다.")
            assertThat(result.createdAt).isEqualTo("2020-08-10T00:00:00")
            assertThat(result.attachments).hasSize(1)
            assertThat(result.attachments[0].fileName).isEqualTo("guide.pdf")
        }

        @Test
        @DisplayName("게시물 미존재 - EducationPostNotFoundException")
        fun getPostDetail_notFound() {
            // Given
            whenever(educationPostRepository.findById("NONEXIST"))
                .thenReturn(Optional.empty())

            // When & Then
            assertThatThrownBy {
                educationService.getPostDetail("NONEXIST")
            }.isInstanceOf(EducationPostNotFoundException::class.java)
        }
    }
}
