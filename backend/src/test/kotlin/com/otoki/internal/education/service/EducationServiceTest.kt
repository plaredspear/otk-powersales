package com.otoki.internal.education.service

import com.otoki.internal.common.service.FileStorageService
import com.otoki.internal.education.entity.*
import com.otoki.internal.common.entity.*
import com.otoki.internal.sap.entity.*
import com.otoki.internal.education.exception.*
import com.otoki.internal.education.repository.EducationCodeRepository
import com.otoki.internal.education.repository.EducationPostAttachmentRepository
import com.otoki.internal.education.repository.EducationPostRepository
import com.otoki.internal.sap.repository.UserRepository
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
import org.mockito.kotlin.*
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.mock.web.MockMultipartFile
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

    @Mock
    private lateinit var fileStorageService: FileStorageService

    @Mock
    private lateinit var userRepository: UserRepository

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

    @Nested
    @DisplayName("getPostsForAdmin - Admin 교육 목록 조회")
    inner class GetPostsForAdminTests {

        @Test
        @DisplayName("전체 카테고리 조회 - category null이면 전체 반환")
        fun getPostsForAdmin_allCategories() {
            val posts = listOf(testPost)
            val page = PageImpl(posts, PageRequest.of(0, 10), 1)
            val eduCode = EducationCode(eduCode = "TASTING_MANUAL", eduCodeNm = "시식 매뉴얼")

            whenever(educationPostRepository.findByOptionalEduCodeAndSearchWithPaging(isNull(), isNull(), any()))
                .thenReturn(page)
            whenever(educationPostAttachmentRepository.findByEduId("EDU001"))
                .thenReturn(emptyList())
            whenever(educationCodeRepository.findById("TASTING_MANUAL"))
                .thenReturn(Optional.of(eduCode))

            val result = educationService.getPostsForAdmin(null, null, 1, 10)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].eduId).isEqualTo("EDU001")
            assertThat(result.content[0].attachmentCount).isEqualTo(0)
        }

        @Test
        @DisplayName("카테고리 필터 조회 - 유효한 카테고리 지정")
        fun getPostsForAdmin_withCategory() {
            val posts = listOf(testPost)
            val page = PageImpl(posts, PageRequest.of(0, 10), 1)
            val eduCode = EducationCode(eduCode = "TASTING_MANUAL", eduCodeNm = "시식 매뉴얼")

            whenever(educationCodeRepository.existsById("TASTING_MANUAL")).thenReturn(true)
            whenever(educationPostRepository.findByOptionalEduCodeAndSearchWithPaging(eq("TASTING_MANUAL"), isNull(), any()))
                .thenReturn(page)
            whenever(educationPostAttachmentRepository.findByEduId("EDU001"))
                .thenReturn(listOf(
                    EducationPostAttachment(eduId = "EDU001", eduFileKey = "key1", eduFileType = "f00003", eduFileOrgNm = "doc.pdf")
                ))
            whenever(educationCodeRepository.findById("TASTING_MANUAL"))
                .thenReturn(Optional.of(eduCode))

            val result = educationService.getPostsForAdmin("TASTING_MANUAL", null, 1, 10)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].attachmentCount).isEqualTo(1)
        }

        @Test
        @DisplayName("유효하지 않은 카테고리 - InvalidEducationCategoryException")
        fun getPostsForAdmin_invalidCategory() {
            whenever(educationCodeRepository.existsById("INVALID")).thenReturn(false)

            assertThatThrownBy {
                educationService.getPostsForAdmin("INVALID", null, 1, 10)
            }.isInstanceOf(InvalidEducationCategoryException::class.java)
        }
    }

    @Nested
    @DisplayName("createPost - 교육 자료 작성")
    inner class CreatePostTests {

        private val testUser = User(id = 1L, employeeId = "12345678", name = "테스트")

        @Test
        @DisplayName("정상 작성 - 파일 없이 교육 자료 생성")
        fun createPost_success_noFiles() {
            whenever(educationCodeRepository.existsById("c00001")).thenReturn(true)
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(testUser))
            whenever(educationPostRepository.save(any<EducationPost>())).thenAnswer { it.getArgument<EducationPost>(0) }
            whenever(educationCodeRepository.findById("c00001"))
                .thenReturn(Optional.of(EducationCode(eduCode = "c00001", eduCodeNm = "시식매뉴얼")))

            val result = educationService.createPost(1L, "테스트 교육", "교육 내용", "c00001", null)

            assertThat(result.eduTitle).isEqualTo("테스트 교육")
            assertThat(result.eduContent).isEqualTo("교육 내용")
            assertThat(result.eduCode).isEqualTo("c00001")
            assertThat(result.eduCodeNm).isEqualTo("시식매뉴얼")
            assertThat(result.empCode).isEqualTo("12345678")
            assertThat(result.attachments).isEmpty()
        }

        @Test
        @DisplayName("정상 작성 - 파일 포함 교육 자료 생성")
        fun createPost_success_withFiles() {
            val file = MockMultipartFile("files", "test.pdf", "application/pdf", ByteArray(100))

            whenever(educationCodeRepository.existsById("c00004")).thenReturn(true)
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(testUser))
            whenever(educationPostRepository.save(any<EducationPost>())).thenAnswer { it.getArgument<EducationPost>(0) }
            whenever(fileStorageService.uploadEducationFile(any(), any())).thenReturn("uuid-file.pdf")
            whenever(educationPostAttachmentRepository.save(any<EducationPostAttachment>()))
                .thenAnswer { it.getArgument<EducationPostAttachment>(0) }
            whenever(educationCodeRepository.findById("c00004"))
                .thenReturn(Optional.of(EducationCode(eduCode = "c00004", eduCodeNm = "신제품소개")))

            val result = educationService.createPost(1L, "신제품 교육", "내용", "c00004", listOf(file))

            assertThat(result.attachments).hasSize(1)
            assertThat(result.attachments[0].eduFileKey).isEqualTo("uuid-file.pdf")
            assertThat(result.attachments[0].eduFileType).isEqualTo("f00003")
        }

        @Test
        @DisplayName("빈 제목 - InvalidEducationParameterException")
        fun createPost_emptyTitle() {
            assertThatThrownBy {
                educationService.createPost(1L, "", "내용", "c00001", null)
            }.isInstanceOf(InvalidEducationParameterException::class.java)
        }

        @Test
        @DisplayName("150자 초과 제목 - InvalidEducationParameterException")
        fun createPost_titleTooLong() {
            assertThatThrownBy {
                educationService.createPost(1L, "A".repeat(151), "내용", "c00001", null)
            }.isInstanceOf(InvalidEducationParameterException::class.java)
        }

        @Test
        @DisplayName("잘못된 카테고리 - InvalidEducationCategoryException")
        fun createPost_invalidCategory() {
            whenever(educationCodeRepository.existsById("c99999")).thenReturn(false)

            assertThatThrownBy {
                educationService.createPost(1L, "제목", "내용", "c99999", null)
            }.isInstanceOf(InvalidEducationCategoryException::class.java)
        }

        @Test
        @DisplayName("파일 수 초과 - FileLimitExceededException")
        fun createPost_fileLimitExceeded() {
            whenever(educationCodeRepository.existsById("c00001")).thenReturn(true)
            val files = (1..21).map { MockMultipartFile("files", "file$it.txt", "text/plain", ByteArray(10)) }

            assertThatThrownBy {
                educationService.createPost(1L, "제목", "내용", "c00001", files)
            }.isInstanceOf(FileLimitExceededException::class.java)
        }

        @Test
        @DisplayName("파일 크기 초과 - FileSizeExceededException")
        fun createPost_fileSizeExceeded() {
            whenever(educationCodeRepository.existsById("c00001")).thenReturn(true)
            val largeFile = MockMultipartFile("files", "big.pdf", "application/pdf", ByteArray(51 * 1024 * 1024))

            assertThatThrownBy {
                educationService.createPost(1L, "제목", "내용", "c00001", listOf(largeFile))
            }.isInstanceOf(FileSizeExceededException::class.java)
        }
    }

    @Nested
    @DisplayName("updatePost - 교육 자료 수정")
    inner class UpdatePostTests {

        @Test
        @DisplayName("정상 수정 - 기존 파일 유지 + 신규 파일 추가")
        fun updatePost_success() {
            val existingAttachment = EducationPostAttachment(
                eduId = "EDU001", eduFileKey = "existing-key", eduFileType = "f00003", eduFileOrgNm = "old.pdf"
            )

            whenever(educationPostRepository.findById("EDU001")).thenReturn(Optional.of(testPost))
            whenever(educationCodeRepository.existsById("TASTING_MANUAL")).thenReturn(true)
            whenever(educationPostAttachmentRepository.findByEduId("EDU001"))
                .thenReturn(listOf(existingAttachment))
                .thenReturn(listOf(existingAttachment)) // second call after save
            whenever(educationPostRepository.save(any<EducationPost>())).thenAnswer { it.getArgument<EducationPost>(0) }
            whenever(educationCodeRepository.findById("TASTING_MANUAL"))
                .thenReturn(Optional.of(EducationCode(eduCode = "TASTING_MANUAL", eduCodeNm = "시식 매뉴얼")))

            val result = educationService.updatePost(
                "EDU001", "수정된 제목", "수정된 내용", "TASTING_MANUAL", null, listOf("existing-key")
            )

            assertThat(result.eduTitle).isEqualTo("수정된 제목")
            assertThat(result.updDate).isNotNull()
        }

        @Test
        @DisplayName("미존재 교육 수정 - EducationPostNotFoundException")
        fun updatePost_notFound() {
            whenever(educationPostRepository.findById("NONEXIST")).thenReturn(Optional.empty())

            assertThatThrownBy {
                educationService.updatePost("NONEXIST", "제목", "내용", "c00001", null, null)
            }.isInstanceOf(EducationPostNotFoundException::class.java)
        }

        @Test
        @DisplayName("잘못된 keep_file_keys - InvalidFileKeyException")
        fun updatePost_invalidFileKey() {
            val existingAttachment = EducationPostAttachment(
                eduId = "EDU001", eduFileKey = "existing-key", eduFileType = "f00003", eduFileOrgNm = "old.pdf"
            )

            whenever(educationPostRepository.findById("EDU001")).thenReturn(Optional.of(testPost))
            whenever(educationCodeRepository.existsById("TASTING_MANUAL")).thenReturn(true)
            whenever(educationPostAttachmentRepository.findByEduId("EDU001"))
                .thenReturn(listOf(existingAttachment))

            assertThatThrownBy {
                educationService.updatePost("EDU001", "제목", "내용", "TASTING_MANUAL", null, listOf("wrong-key"))
            }.isInstanceOf(InvalidFileKeyException::class.java)
        }

        @Test
        @DisplayName("파일 수 합산 초과 - FileLimitExceededException")
        fun updatePost_combinedFileLimitExceeded() {
            val existingAttachments = (1..15).map {
                EducationPostAttachment(eduId = "EDU001", eduFileKey = "key$it", eduFileType = "f00003", eduFileOrgNm = "file$it.pdf")
            }
            val newFiles = (1..6).map { MockMultipartFile("files", "new$it.pdf", "application/pdf", ByteArray(10)) }

            whenever(educationPostRepository.findById("EDU001")).thenReturn(Optional.of(testPost))
            whenever(educationCodeRepository.existsById("TASTING_MANUAL")).thenReturn(true)
            whenever(educationPostAttachmentRepository.findByEduId("EDU001"))
                .thenReturn(existingAttachments)

            assertThatThrownBy {
                educationService.updatePost(
                    "EDU001", "제목", "내용", "TASTING_MANUAL", newFiles,
                    existingAttachments.map { it.eduFileKey }
                )
            }.isInstanceOf(FileLimitExceededException::class.java)
        }
    }

    @Nested
    @DisplayName("deletePost - 교육 자료 삭제")
    inner class DeletePostTests {

        @Test
        @DisplayName("정상 삭제 - 교육 자료 + 첨부파일 삭제")
        fun deletePost_success() {
            val attachments = listOf(
                EducationPostAttachment(eduId = "EDU001", eduFileKey = "key1", eduFileType = "f00003", eduFileOrgNm = "doc.pdf")
            )

            whenever(educationPostRepository.findById("EDU001")).thenReturn(Optional.of(testPost))
            whenever(educationPostAttachmentRepository.findByEduId("EDU001")).thenReturn(attachments)

            educationService.deletePost("EDU001")

            verify(fileStorageService).deleteEducationFile("EDU001", "key1")
            verify(educationPostAttachmentRepository).deleteAll(attachments)
            verify(educationPostRepository).delete(testPost)
        }

        @Test
        @DisplayName("미존재 교육 삭제 - EducationPostNotFoundException")
        fun deletePost_notFound() {
            whenever(educationPostRepository.findById("NONEXIST")).thenReturn(Optional.empty())

            assertThatThrownBy {
                educationService.deletePost("NONEXIST")
            }.isInstanceOf(EducationPostNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("getCategories - 카테고리 목록 조회")
    inner class GetCategoriesTests {

        @Test
        @DisplayName("정상 조회 - 카테고리 목록 반환")
        fun getCategories_success() {
            val categories = listOf(
                EducationCode(eduCode = "c00002", eduCodeNm = "CS/안전"),
                EducationCode(eduCode = "c00001", eduCodeNm = "시식매뉴얼")
            )
            whenever(educationCodeRepository.findAll()).thenReturn(categories)

            val result = educationService.getCategories()

            assertThat(result).hasSize(2)
            assertThat(result[0].eduCode).isEqualTo("c00001") // 오름차순 정렬
            assertThat(result[1].eduCode).isEqualTo("c00002")
        }
    }
}
