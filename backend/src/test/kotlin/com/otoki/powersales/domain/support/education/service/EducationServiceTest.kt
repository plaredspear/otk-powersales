package com.otoki.powersales.domain.support.education.service

import com.otoki.powersales.platform.common.entity.UploadFile
import com.otoki.powersales.platform.common.repository.UploadFileRepository
import com.otoki.powersales.platform.common.service.FileStorageService
import com.otoki.powersales.platform.common.storage.ExternalImageFetcher
import com.otoki.powersales.platform.common.storage.InlineImageService
import com.otoki.powersales.platform.common.storage.StorageService
import com.otoki.powersales.platform.common.storage.UploadFileParentTypes
import com.otoki.powersales.platform.common.storage.UploadResult
import com.otoki.powersales.domain.support.education.entity.EducationPost
import com.otoki.powersales.domain.support.education.entity.EducationPostAttachment
import com.otoki.powersales.domain.support.education.exception.EducationPostNotFoundException
import com.otoki.powersales.domain.support.education.exception.FileLimitExceededException
import com.otoki.powersales.domain.support.education.exception.FileSizeExceededException
import com.otoki.powersales.domain.support.education.exception.InvalidEducationCategoryException
import com.otoki.powersales.domain.support.education.exception.InvalidEducationParameterException
import com.otoki.powersales.domain.support.education.exception.InvalidFileKeyException
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
    private val fileStorageService: FileStorageService = mockk()
    private val employeeRepository: EmployeeRepository = mockk()
    private val uploadFileRepository: UploadFileRepository = mockk()
    private val storageService: StorageService = mockk()
    private val externalImageFetcher: ExternalImageFetcher = mockk()

    // 인라인 이미지 로직은 공용 InlineImageService 가 소유한다 — mock 이 아니라 실제 인스턴스를 주입해
    // 본문 정규화/정리 동작이 교육 경로에서도 그대로 도는지 검증한다.
    private val educationService = EducationService(
        educationPostRepository,
        educationPostAttachmentRepository,
        fileStorageService,
        employeeRepository,
        InlineImageService(uploadFileRepository, storageService, externalImageFetcher),
        uploadFileRepository,
        storageService,
    )

    private lateinit var testPost: EducationPost

    @BeforeEach
    fun setUp() {
        // 인라인 이미지 기본 stub — 기존 테스트 본문에는 <img> 가 없어 실제로는 대부분 진입하지 않지만,
        // 정규화/정리 경로가 조회를 시도할 때 빈 결과를 돌려주도록 둔다 (개별 테스트에서 override).
        every { uploadFileRepository.findByUniqueKeyInAndParentTypeAndIsDeletedFalse(any(), any()) } returns emptyList()
        every { uploadFileRepository.findByIdInAndParentTypeAndIsDeletedFalse(any(), any()) } returns emptyList()
        every { uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse(any(), any()) } returns emptyList()
        every { externalImageFetcher.fetch(any()) } returns null
        every { storageService.getPresignedUrl(any(), any()) } answers {
            "https://test-bucket.s3.ap-northeast-2.amazonaws.com/private/${firstArg<String>()}?X-Amz-Signature=test"
        }

        testPost = EducationPost(
            eduId = "EDU001",
            eduTitle = "진짬뽕 시식 매뉴얼",
            eduContent = "진짬뽕 시식 방법을 안내합니다.",
            eduCode = "c00001",
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

            every { educationPostRepository.findByEduCodeOrderByCreatedAtDesc(any(), any()) } returns page

            val result = educationService.getPosts(
                category = "c00001",
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

            every { educationPostRepository.findByEduCodeAndSearchWithPaging(any(), any(), any()) } returns page

            val result = educationService.getPosts(
                category = "c00001",
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

            every { educationPostRepository.findByEduCodeOrderByCreatedAtDesc(any(), any()) } returns emptyPage

            val result = educationService.getPosts(
                category = "c00001",
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

            every { educationPostRepository.findByEduId("EDU001") } returns testPost
            every { educationPostAttachmentRepository.findByEducationPost(testPost) } returns attachments
            every { fileStorageService.getEducationFileUrl("file-key-001") } returns "https://signed/guide.pdf"

            val result = educationService.getPostDetail("EDU001")

            assertThat(result.id).isEqualTo("EDU001")
            assertThat(result.category).isEqualTo("c00001")
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

            every {
                educationPostRepository.findByOptionalEduCodeAndSearchWithPaging(null, null, any())
            } returns page
            every { educationPostAttachmentRepository.findByEducationPost(testPost) } returns emptyList()

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

            every {
                educationPostRepository.findByOptionalEduCodeAndSearchWithPaging("c00001", null, any())
            } returns page
            every { educationPostAttachmentRepository.findByEducationPost(testPost) } returns listOf(
                EducationPostAttachment(educationPost = testPost, fileKey = "key1", fileType = "f00003", fileOriginalName = "doc.pdf")
            )

            val result = educationService.getPostsForAdmin("c00001", null, 1, 10)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].attachmentCount).isEqualTo(1)
        }

        @Test
        @DisplayName("유효하지 않은 카테고리 - InvalidEducationCategoryException")
        fun getPostsForAdmin_invalidCategory() {

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
            every { employeeRepository.findById(1L) } returns Optional.of(testEmployee)
            every { educationPostRepository.save(any<EducationPost>()) } answers { firstArg() }

            val result = educationService.createPost(1L, "테스트 교육", "교육 내용", "c00001", null)

            assertThat(result.eduTitle).isEqualTo("테스트 교육")
            assertThat(result.eduContent).isEqualTo("교육 내용")
            assertThat(result.eduCode).isEqualTo("c00001")
            assertThat(result.eduCodeNm).isEqualTo("시식 매뉴얼")
            assertThat(result.employeeId).isNotNull()
            assertThat(result.attachments).isEmpty()
        }

        @Test
        @DisplayName("정상 작성 - 파일 포함 교육 자료 생성")
        fun createPost_success_withFiles() {
            val file = MockMultipartFile("files", "test.pdf", "application/pdf", ByteArray(100))

            every { employeeRepository.findById(1L) } returns Optional.of(testEmployee)
            every { educationPostRepository.save(any<EducationPost>()) } answers { firstArg() }
            every { fileStorageService.uploadEducationFile(any(), any()) } returns "uuid-file.pdf"
            every { educationPostAttachmentRepository.save(any<EducationPostAttachment>()) } answers { firstArg() }

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

            assertThatThrownBy {
                educationService.createPost(1L, "제목", "내용", "c99999", null)
            }.isInstanceOf(InvalidEducationCategoryException::class.java)
        }

        @Test
        @DisplayName("파일 수 초과 - FileLimitExceededException")
        fun createPost_fileLimitExceeded() {
            val files = (1..21).map { MockMultipartFile("files", "file$it.txt", "text/plain", ByteArray(10)) }

            assertThatThrownBy {
                educationService.createPost(1L, "제목", "내용", "c00001", files)
            }.isInstanceOf(FileLimitExceededException::class.java)
        }

        @Test
        @DisplayName("파일 크기 초과 - FileSizeExceededException")
        fun createPost_fileSizeExceeded() {
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
            // 동일 인자에 대해 호출 횟수만큼 같은 결과 반환 — MockK 는 default 가 모든 호출에 동일 응답
            every { educationPostAttachmentRepository.findByEducationPost(any<EducationPost>()) } returns
                listOf(existingAttachment)
            every { educationPostRepository.save(any<EducationPost>()) } answers { firstArg() }

            val result = educationService.updatePost(
                "EDU001", "수정된 제목", "수정된 내용", "c00001", null, listOf("existing-key")
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
            every { educationPostAttachmentRepository.findByEducationPost(any<EducationPost>()) } returns
                listOf(existingAttachment)

            assertThatThrownBy {
                educationService.updatePost("EDU001", "제목", "내용", "c00001", null, listOf("wrong-key"))
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
            every { educationPostAttachmentRepository.findByEducationPost(any<EducationPost>()) } returns
                existingAttachments

            assertThatThrownBy {
                educationService.updatePost(
                    "EDU001", "제목", "내용", "c00001", newFiles,
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
            val result = educationService.getCategories()

            // enum 선언 순서 = 노출 순서 (어드민 선택 목록)
            assertThat(result.map { it.eduCode })
                .containsExactly("c00001", "c00002", "c00003", "c00005", "c00004")
            assertThat(result.map { it.eduCodeNm })
                .containsExactly("시식 매뉴얼", "안전교육", "영업 교육", "APP 매뉴얼", "설문조사")
        }
    }

    @Nested
    @DisplayName("본문 인라인 이미지 — 공지와 동일 파이프라인(InlineImageService) 적용")
    inner class InlineImageTests {

        private val testEmployee = Employee(id = 1L, employeeCode = "12345678", name = "테스트")

        private fun image(name: String = "inline.png"): MockMultipartFile =
            MockMultipartFile("image", name, "image/png", ByteArray(1024))

        private fun uploadResult(key: String) = UploadResult(
            key = key,
            contentType = "image/png",
            originalName = "inline.png",
            sizeBytes = 1024L,
        )

        @Test
        @DisplayName("업로드 - parent_id null + upload_kbn=INLINE 로 적재, 교육 전용 S3 세그먼트 사용")
        fun uploadInlineImage_success() {
            every {
                storageService.uploadPrivate(
                    domain = "education-inline", originalName = any(), bytes = any(), contentType = any(),
                )
            } returns uploadResult("uploads/education-inline/2026/08/31/x.png")
            every { uploadFileRepository.save(any<UploadFile>()) } answers {
                val arg = firstArg<UploadFile>()
                // 업로드 시점엔 글이 없을 수 있어(신규 작성) parent_id 는 비운다 — 저장 시 backfill 이 채운다.
                assertThat(arg.parentId).isNull()
                assertThat(arg.uploadKbn).isEqualTo("INLINE")
                // 첨부(EducationPostAttachment)와 섞이지 않도록 parent_type 으로 격리된다.
                assertThat(arg.parentType).isEqualTo(UploadFileParentTypes.EDUCATION_POST)
                UploadFile(
                    id = 555L,
                    name = arg.name,
                    uniqueKey = arg.uniqueKey,
                    parentType = arg.parentType,
                    parentId = arg.parentId,
                    uploadKbn = arg.uploadKbn,
                    isDeleted = arg.isDeleted,
                )
            }

            val result = educationService.uploadInlineImage(image())

            assertThat(result.refid).isEqualTo("555")
            // 본문에는 만료되는 presigned 가 아니라 placeholder 가 들어가야 한다.
            assertThat(result.placeholder).contains("""data-refid="555"""")
            assertThat(result.placeholder).contains("notice-image://555")
            assertThat(result.previewUrl).contains("uploads/education-inline/2026/08/31/x.png")
        }

        @Test
        @DisplayName("작성 - 본문의 만료 presigned URL 을 placeholder 로 되돌려 저장한다")
        fun createPost_normalizesPresignedToPlaceholder() {
            val uniqueKey = "uploads/education-inline/2026/08/31/x.png"
            val inlineFile = UploadFile(
                id = 555L,
                uniqueKey = uniqueKey,
                parentType = UploadFileParentTypes.EDUCATION_POST,
                parentId = null,
                uploadKbn = "INLINE",
                isDeleted = false,
            )
            every { employeeRepository.findById(1L) } returns Optional.of(testEmployee)
            every { educationPostRepository.save(any<EducationPost>()) } answers { firstArg() }
            every {
                uploadFileRepository.findByUniqueKeyInAndParentTypeAndIsDeletedFalse(
                    any(), UploadFileParentTypes.EDUCATION_POST,
                )
            } returns listOf(inlineFile)

            val body = """<p><img src="https://b.s3.amazonaws.com/private/$uniqueKey?X-Amz-Signature=z"></p>"""
            val result = educationService.createPost(1L, "교육", body, "c00001", null)

            // 만료 URL 이 그대로 저장되면 30분 뒤 이미지가 영구히 깨진다 (공지 2393 과 같은 사고).
            assertThat(result.eduContent).doesNotContain("X-Amz-Signature")
            assertThat(result.eduContent).contains("""data-refid="555"""")
        }

        @Test
        @DisplayName("작성 - 본문이 참조하는 임시 업로드분을 이 글로 backfill 한다")
        fun createPost_backfillsInlineImageParent() {
            val inlineFile = UploadFile(
                id = 555L,
                uniqueKey = "uploads/education-inline/2026/08/31/x.png",
                parentType = UploadFileParentTypes.EDUCATION_POST,
                parentId = null,
                uploadKbn = "INLINE",
                isDeleted = false,
            )
            every { employeeRepository.findById(1L) } returns Optional.of(testEmployee)
            every { educationPostRepository.save(any<EducationPost>()) } answers {
                // parent_id 로는 화면 식별자 eduId(String)가 아니라 PK id(Long)를 써야 한다.
                firstArg<EducationPost>().let { EducationPost(
                    id = 99L,
                    eduId = it.eduId,
                    eduTitle = it.eduTitle,
                    eduContent = it.eduContent,
                    eduCode = it.eduCode,
                    employee = it.employee,
                    empCode = it.empCode,
                ) }
            }
            every {
                uploadFileRepository.findByIdInAndParentTypeAndIsDeletedFalse(
                    listOf(555L), UploadFileParentTypes.EDUCATION_POST,
                )
            } returns listOf(inlineFile)

            val body = """<p><img src="notice-image://555" data-refid="555"></p>"""
            educationService.createPost(1L, "교육", body, "c00001", null)

            assertThat(inlineFile.parentId).isEqualTo(99L)
        }

        @Test
        @DisplayName("삭제 - 본문 인라인 이미지도 S3 삭제 + soft-delete 한다 (hard delete 라 고아 방지)")
        fun deletePost_cleansUpInlineImages() {
            val inlineFile = UploadFile(
                id = 555L,
                uniqueKey = "uploads/education-inline/2026/08/31/x.png",
                parentType = UploadFileParentTypes.EDUCATION_POST,
                parentId = 99L,
                uploadKbn = "INLINE",
                isDeleted = false,
            )
            every { educationPostRepository.findByEduId("EDU001") } returns testPost
            every { educationPostAttachmentRepository.findByEducationPost(testPost) } returns emptyList()
            every { educationPostAttachmentRepository.deleteAll(any()) } just Runs
            every { educationPostRepository.delete(testPost) } just Runs
            every {
                uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse(
                    UploadFileParentTypes.EDUCATION_POST, any(),
                )
            } returns listOf(inlineFile)
            every { storageService.deletePrivate(any()) } just Runs

            educationService.deletePost("EDU001")

            verify { storageService.deletePrivate("uploads/education-inline/2026/08/31/x.png") }
            assertThat(inlineFile.isDeleted).isTrue()
        }
    }
}
