package com.otoki.powersales.domain.support.education.service

import com.otoki.powersales.platform.common.service.FileStorageService
import com.otoki.powersales.domain.support.education.entity.EducationCode
import com.otoki.powersales.domain.support.education.entity.EducationPost
import com.otoki.powersales.domain.support.education.entity.EducationPostAttachment
import com.otoki.powersales.domain.support.education.exception.EducationPostNotFoundException
import com.otoki.powersales.domain.support.education.exception.FileLimitExceededException
import com.otoki.powersales.domain.support.education.exception.FileSizeExceededException
import com.otoki.powersales.domain.support.education.exception.InvalidEducationCategoryException
import com.otoki.powersales.domain.support.education.exception.InvalidEducationParameterException
import com.otoki.powersales.domain.support.education.exception.InvalidFileKeyException
import com.otoki.powersales.domain.support.education.repository.EducationCodeRepository
import com.otoki.powersales.domain.support.education.repository.EducationPostAttachmentRepository
import com.otoki.powersales.domain.support.education.repository.EducationPostRepository
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
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
import java.time.LocalDateTime
import java.util.Optional

@DisplayName("EducationService 테스트")
class EducationServiceTest {

    private val educationPostRepository: EducationPostRepository = mockk()
    private val educationPostAttachmentRepository: EducationPostAttachmentRepository = mockk()
    private val educationCodeRepository: EducationCodeRepository = mockk()
    private val fileStorageService: FileStorageService = mockk()
    private val employeeRepository: EmployeeRepository = mockk()

    private val educationService = EducationService(
        educationPostRepository,
        educationPostAttachmentRepository,
        educationCodeRepository,
        fileStorageService,
        employeeRepository,
    )

    private lateinit var testPost: EducationPost

    @BeforeEach
    fun setUp() {
        testPost = EducationPost(
            eduId = "EDU001",
            eduTitle = "진짬뽕 시식 매뉴얼",
            eduContent = "진짬뽕 시식 방법을 안내합니다.",
            eduCode = "TASTING_MANUAL",
            empCode = "10000001"
        ).apply {
            createdAt = LocalDateTime.of(2020, 8, 10, 0, 0, 0)
        }
    }

    @Nested
    @DisplayName("getPosts - 게시물 목록 조회")
    inner class GetPostsTests {

        @Test
        @DisplayName("정상 조회 - 카테고리별 게시물 목록 반환")
        fun getPosts_success() {
            val posts = listOf(testPost)
            val page = PageImpl(posts, PageRequest.of(0, 10), 1)

            every { educationCodeRepository.existsByEduCode("TASTING_MANUAL") } returns true
            every { educationPostRepository.findByEduCodeOrderByCreatedAtDesc(any(), any()) } returns page

            val result = educationService.getPosts(
                category = "TASTING_MANUAL",
                search = null,
                page = 1,
                size = 10
            )

            assertThat(result.content).hasSize(1)
            assertThat(result.totalCount).isEqualTo(1)
            assertThat(result.totalPages).isEqualTo(1)
            assertThat(result.currentPage).isEqualTo(1)
            assertThat(result.size).isEqualTo(10)
            assertThat(result.content[0].id).isEqualTo("EDU001")
            assertThat(result.content[0].title).isEqualTo("진짬뽕 시식 매뉴얼")
            assertThat(result.content[0].createdAt).isEqualTo(LocalDateTime.parse("2020-08-10T00:00:00"))
        }

        @Test
        @DisplayName("검색 조회 - 검색 키워드로 게시물 목록 반환")
        fun getPosts_withSearch() {
            val posts = listOf(testPost)
            val page = PageImpl(posts, PageRequest.of(0, 10), 1)

            every { educationCodeRepository.existsByEduCode("TASTING_MANUAL") } returns true
            every { educationPostRepository.findByEduCodeAndSearchWithPaging(any(), any(), any()) } returns page

            val result = educationService.getPosts(
                category = "TASTING_MANUAL",
                search = "시식",
                page = 1,
                size = 10
            )

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].title).isEqualTo("진짬뽕 시식 매뉴얼")
        }

        @Test
        @DisplayName("유효하지 않은 카테고리 - InvalidEducationCategoryException")
        fun getPosts_invalidCategory() {
            every { educationCodeRepository.existsByEduCode("INVALID_CATEGORY") } returns false

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
            val emptyPage = PageImpl<EducationPost>(emptyList(), PageRequest.of(0, 10), 0)

            every { educationCodeRepository.existsByEduCode("TASTING_MANUAL") } returns true
            every { educationPostRepository.findByEduCodeOrderByCreatedAtDesc(any(), any()) } returns emptyPage

            val result = educationService.getPosts(
                category = "TASTING_MANUAL",
                search = null,
                page = 1,
                size = 10
            )

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
            val attachments = listOf(
                EducationPostAttachment(
                    educationPost = testPost,
                    fileKey = "file-key-001",
                    fileType = "pdf",
                    fileOriginalName = "guide.pdf"
                )
            )

            val eduCode = EducationCode(
                eduCode = "TASTING_MANUAL",
                eduCodeNm = "시식 매뉴얼"
            )

            every { educationPostRepository.findByEduId("EDU001") } returns testPost
            every { educationPostAttachmentRepository.findByEducationPost(testPost) } returns attachments
            every { educationCodeRepository.findByEduCode("TASTING_MANUAL") } returns eduCode
            every { fileStorageService.getEducationFileUrl("file-key-001") } returns "https://signed/guide.pdf"

            val result = educationService.getPostDetail("EDU001")

            assertThat(result.id).isEqualTo("EDU001")
            assertThat(result.category).isEqualTo("TASTING_MANUAL")
            assertThat(result.categoryName).isEqualTo("시식 매뉴얼")
            assertThat(result.title).isEqualTo("진짬뽕 시식 매뉴얼")
            assertThat(result.content).isEqualTo("진짬뽕 시식 방법을 안내합니다.")
            assertThat(result.createdAt).isEqualTo(LocalDateTime.parse("2020-08-10T00:00:00"))
            assertThat(result.attachments).hasSize(1)
            assertThat(result.attachments[0].fileName).isEqualTo("guide.pdf")
            assertThat(result.attachments[0].fileType).isEqualTo("pdf")
            assertThat(result.attachments[0].fileUrl).isEqualTo("https://signed/guide.pdf")
        }

        @Test
        @DisplayName("게시물 미존재 - EducationPostNotFoundException")
        fun getPostDetail_notFound() {
            every { educationPostRepository.findByEduId("NONEXIST") } returns null

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

            every {
                educationPostRepository.findByOptionalEduCodeAndSearchWithPaging(null, null, any())
            } returns page
            every { educationPostAttachmentRepository.findByEducationPost(testPost) } returns emptyList()
            every { educationCodeRepository.findByEduCode("TASTING_MANUAL") } returns eduCode

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

            every { educationCodeRepository.existsByEduCode("TASTING_MANUAL") } returns true
            every {
                educationPostRepository.findByOptionalEduCodeAndSearchWithPaging("TASTING_MANUAL", null, any())
            } returns page
            every { educationPostAttachmentRepository.findByEducationPost(testPost) } returns listOf(
                EducationPostAttachment(educationPost = testPost, fileKey = "key1", fileType = "f00003", fileOriginalName = "doc.pdf")
            )
            every { educationCodeRepository.findByEduCode("TASTING_MANUAL") } returns eduCode

            val result = educationService.getPostsForAdmin("TASTING_MANUAL", null, 1, 10)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].attachmentCount).isEqualTo(1)
        }

        @Test
        @DisplayName("유효하지 않은 카테고리 - InvalidEducationCategoryException")
        fun getPostsForAdmin_invalidCategory() {
            every { educationCodeRepository.existsByEduCode("INVALID") } returns false

            assertThatThrownBy {
                educationService.getPostsForAdmin("INVALID", null, 1, 10)
            }.isInstanceOf(InvalidEducationCategoryException::class.java)
        }
    }

    @Nested
    @DisplayName("createPost - 교육 자료 작성")
    inner class CreatePostTests {

        private val testEmployee = Employee(id = 1L, employeeCode = "12345678", name = "테스트")

        @Test
        @DisplayName("정상 작성 - 파일 없이 교육 자료 생성")
        fun createPost_success_noFiles() {
            every { educationCodeRepository.existsByEduCode("c00001") } returns true
            every { employeeRepository.findById(1L) } returns Optional.of(testEmployee)
            every { educationPostRepository.save(any<EducationPost>()) } answers { firstArg() }
            every { educationCodeRepository.findByEduCode("c00001") } returns
                EducationCode(eduCode = "c00001", eduCodeNm = "시식매뉴얼")

            val result = educationService.createPost(1L, "테스트 교육", "교육 내용", "c00001", null)

            assertThat(result.eduTitle).isEqualTo("테스트 교육")
            assertThat(result.eduContent).isEqualTo("교육 내용")
            assertThat(result.eduCode).isEqualTo("c00001")
            assertThat(result.eduCodeNm).isEqualTo("시식매뉴얼")
            assertThat(result.employeeId).isNotNull()
            assertThat(result.attachments).isEmpty()
        }

        @Test
        @DisplayName("정상 작성 - 파일 포함 교육 자료 생성")
        fun createPost_success_withFiles() {
            val file = MockMultipartFile("files", "test.pdf", "application/pdf", ByteArray(100))

            every { educationCodeRepository.existsByEduCode("c00004") } returns true
            every { employeeRepository.findById(1L) } returns Optional.of(testEmployee)
            every { educationPostRepository.save(any<EducationPost>()) } answers { firstArg() }
            every { fileStorageService.uploadEducationFile(any(), any()) } returns "uuid-file.pdf"
            every { educationPostAttachmentRepository.save(any<EducationPostAttachment>()) } answers { firstArg() }
            every { educationCodeRepository.findByEduCode("c00004") } returns
                EducationCode(eduCode = "c00004", eduCodeNm = "신제품소개")

            val result = educationService.createPost(1L, "신제품 교육", "내용", "c00004", listOf(file))

            assertThat(result.attachments).hasSize(1)
            assertThat(result.attachments[0].fileKey).isEqualTo("uuid-file.pdf")
            assertThat(result.attachments[0].fileType).isEqualTo("f00003")
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
            every { educationCodeRepository.existsByEduCode("c99999") } returns false

            assertThatThrownBy {
                educationService.createPost(1L, "제목", "내용", "c99999", null)
            }.isInstanceOf(InvalidEducationCategoryException::class.java)
        }

        @Test
        @DisplayName("파일 수 초과 - FileLimitExceededException")
        fun createPost_fileLimitExceeded() {
            every { educationCodeRepository.existsByEduCode("c00001") } returns true
            val files = (1..21).map { MockMultipartFile("files", "file$it.txt", "text/plain", ByteArray(10)) }

            assertThatThrownBy {
                educationService.createPost(1L, "제목", "내용", "c00001", files)
            }.isInstanceOf(FileLimitExceededException::class.java)
        }

        @Test
        @DisplayName("파일 크기 초과 - FileSizeExceededException")
        fun createPost_fileSizeExceeded() {
            every { educationCodeRepository.existsByEduCode("c00001") } returns true
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
                educationPost = testPost, fileKey = "existing-key", fileType = "f00003", fileOriginalName = "old.pdf"
            )

            every { educationPostRepository.findByEduId("EDU001") } returns testPost
            every { educationCodeRepository.existsByEduCode("TASTING_MANUAL") } returns true
            // 동일 인자에 대해 호출 횟수만큼 같은 결과 반환 — MockK 는 default 가 모든 호출에 동일 응답
            every { educationPostAttachmentRepository.findByEducationPost(any<EducationPost>()) } returns
                listOf(existingAttachment)
            every { educationPostRepository.save(any<EducationPost>()) } answers { firstArg() }
            every { educationCodeRepository.findByEduCode("TASTING_MANUAL") } returns
                EducationCode(eduCode = "TASTING_MANUAL", eduCodeNm = "시식 매뉴얼")

            val result = educationService.updatePost(
                "EDU001", "수정된 제목", "수정된 내용", "TASTING_MANUAL", null, listOf("existing-key")
            )

            assertThat(result.eduTitle).isEqualTo("수정된 제목")
            assertThat(result.updDate).isNotNull()
        }

        @Test
        @DisplayName("미존재 교육 수정 - EducationPostNotFoundException")
        fun updatePost_notFound() {
            every { educationPostRepository.findByEduId("NONEXIST") } returns null

            assertThatThrownBy {
                educationService.updatePost("NONEXIST", "제목", "내용", "c00001", null, null)
            }.isInstanceOf(EducationPostNotFoundException::class.java)
        }

        @Test
        @DisplayName("잘못된 keep_file_keys - InvalidFileKeyException")
        fun updatePost_invalidFileKey() {
            val existingAttachment = EducationPostAttachment(
                educationPost = testPost, fileKey = "existing-key", fileType = "f00003", fileOriginalName = "old.pdf"
            )

            every { educationPostRepository.findByEduId("EDU001") } returns testPost
            every { educationCodeRepository.existsByEduCode("TASTING_MANUAL") } returns true
            every { educationPostAttachmentRepository.findByEducationPost(any<EducationPost>()) } returns
                listOf(existingAttachment)

            assertThatThrownBy {
                educationService.updatePost("EDU001", "제목", "내용", "TASTING_MANUAL", null, listOf("wrong-key"))
            }.isInstanceOf(InvalidFileKeyException::class.java)
        }

        @Test
        @DisplayName("파일 수 합산 초과 - FileLimitExceededException")
        fun updatePost_combinedFileLimitExceeded() {
            val existingAttachments = (1..15).map {
                EducationPostAttachment(educationPost = testPost, fileKey = "key$it", fileType = "f00003", fileOriginalName = "file$it.pdf")
            }
            val newFiles = (1..6).map { MockMultipartFile("files", "new$it.pdf", "application/pdf", ByteArray(10)) }

            every { educationPostRepository.findByEduId("EDU001") } returns testPost
            every { educationCodeRepository.existsByEduCode("TASTING_MANUAL") } returns true
            every { educationPostAttachmentRepository.findByEducationPost(any<EducationPost>()) } returns
                existingAttachments

            assertThatThrownBy {
                educationService.updatePost(
                    "EDU001", "제목", "내용", "TASTING_MANUAL", newFiles,
                    existingAttachments.map { it.fileKey }
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
                EducationPostAttachment(educationPost = testPost, fileKey = "key1", fileType = "f00003", fileOriginalName = "doc.pdf")
            )

            every { educationPostRepository.findByEduId("EDU001") } returns testPost
            every { educationPostAttachmentRepository.findByEducationPost(testPost) } returns attachments
            every { fileStorageService.deleteEducationFile(any(), any()) } just Runs
            every { educationPostAttachmentRepository.deleteAll(attachments) } just Runs
            every { educationPostRepository.delete(testPost) } just Runs

            educationService.deletePost("EDU001")

            verify { fileStorageService.deleteEducationFile("EDU001", "key1") }
            verify { educationPostAttachmentRepository.deleteAll(attachments) }
            verify { educationPostRepository.delete(testPost) }
        }

        @Test
        @DisplayName("미존재 교육 삭제 - EducationPostNotFoundException")
        fun deletePost_notFound() {
            every { educationPostRepository.findByEduId("NONEXIST") } returns null

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
            every { educationCodeRepository.findAll() } returns categories

            val result = educationService.getCategories()

            assertThat(result).hasSize(2)
            assertThat(result[0].eduCode).isEqualTo("c00001") // 오름차순 정렬
            assertThat(result[1].eduCode).isEqualTo("c00002")
        }
    }
}
