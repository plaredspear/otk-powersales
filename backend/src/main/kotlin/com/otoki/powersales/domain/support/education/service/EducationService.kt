package com.otoki.powersales.domain.support.education.service

import com.otoki.powersales.platform.common.service.FileStorageService
import com.otoki.powersales.platform.common.util.TimeZones
import com.otoki.powersales.domain.support.education.dto.response.AdminEducationListResponse
import com.otoki.powersales.domain.support.education.dto.response.AdminEducationPostSummary
import com.otoki.powersales.domain.support.education.dto.response.AttachmentInfo
import com.otoki.powersales.domain.support.education.dto.response.EducationAttachmentResponse
import com.otoki.powersales.domain.support.education.dto.response.EducationCategoryResponse
import com.otoki.powersales.domain.support.education.dto.response.EducationMutationResponse
import com.otoki.powersales.domain.support.education.dto.response.EducationPostDetailResponse
import com.otoki.powersales.domain.support.education.dto.response.EducationPostListResponse
import com.otoki.powersales.domain.support.education.dto.response.EducationPostSummaryResponse
import com.otoki.powersales.domain.support.education.entity.EducationPost
import com.otoki.powersales.domain.support.education.entity.EducationPostAttachment
import com.otoki.powersales.domain.support.education.enums.EducationCategoryCode
import com.otoki.powersales.domain.support.education.exception.EducationPostNotFoundException
import com.otoki.powersales.domain.support.education.exception.FileLimitExceededException
import com.otoki.powersales.domain.support.education.exception.FileSizeExceededException
import com.otoki.powersales.domain.support.education.exception.InvalidEducationCategoryException
import com.otoki.powersales.domain.support.education.exception.InvalidEducationParameterException
import com.otoki.powersales.domain.support.education.exception.InvalidFileKeyException
import com.otoki.powersales.domain.support.education.repository.EducationPostAttachmentRepository
import com.otoki.powersales.domain.support.education.repository.EducationPostRepository
// import com.otoki.powersales.education.repository.EducationPostImageRepository  // Phase2: PG 대응 테이블 없음
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import org.springframework.data.domain.PageRequest
import com.otoki.powersales.platform.common.repository.UploadFileRepository
import com.otoki.powersales.platform.common.storage.InlineImageDomain
import com.otoki.powersales.platform.common.storage.InlineImageService
import com.otoki.powersales.platform.common.storage.InlineImageUploadResult
import com.otoki.powersales.platform.common.storage.StorageService
import com.otoki.powersales.platform.common.storage.UploadFileParentTypes
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 교육 비즈니스 로직 Service
 */
@Service
@Transactional(readOnly = true)
class EducationService(
    private val educationPostRepository: EducationPostRepository,
    // private val educationPostImageRepository: EducationPostImageRepository,  // Phase2: PG 대응 테이블 없음
    private val educationPostAttachmentRepository: EducationPostAttachmentRepository,
    private val fileStorageService: FileStorageService,
    private val employeeRepository: EmployeeRepository,
    // 본문 인라인 이미지(공지와 공용). 첨부파일(education_post_attachment)과는 완전히 별개 저장 경로다.
    private val inlineImageService: InlineImageService,
    private val uploadFileRepository: UploadFileRepository,
    private val storageService: StorageService
) {

    /**
     * 교육 게시물 목록 조회
     *
     * @param category 카테고리 문자열 (edu_code 값)
     * @param search 검색 키워드 (nullable, 제목+내용 LIKE 검색)
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지 크기
     * @return 게시물 목록 + 페이지네이션 정보
     * @throws InvalidEducationCategoryException 유효하지 않은 카테고리
     */
    fun getPosts(
        category: String,
        search: String?,
        page: Int = 1,
        size: Int = 10
    ): EducationPostListResponse {
        // 1. 카테고리 유효성 검증 (EducationCategoryCode enum 기준)
        if (!EducationCategoryCode.exists(category)) {
            throw InvalidEducationCategoryException()
        }

        // 2. 페이지네이션 (page는 1부터, Spring Data는 0부터)
        val pageable = PageRequest.of(page - 1, size)

        // 3. 검색 키워드에 따라 다른 메서드 호출
        val postsPage = if (search.isNullOrBlank()) {
            educationPostRepository.findByEduCodeOrderByCreatedAtDesc(
                category,
                pageable
            )
        } else {
            educationPostRepository.findByEduCodeAndSearchWithPaging(
                category,
                search.take(100),
                pageable
            )
        }

        // 4. Entity → DTO 변환
        val summaries = postsPage.content.map { post ->
            EducationPostSummaryResponse(
                id = post.eduId ?: "",
                title = post.eduTitle ?: "",
                createdAt = post.createdAt
            )
        }

        return EducationPostListResponse(
            content = summaries,
            totalCount = postsPage.totalElements,
            totalPages = postsPage.totalPages,
            currentPage = page,
            size = size
        )
    }

    /**
     * 교육 게시물 상세 조회
     *
     * @param postId 게시물 ID (edu_id)
     * @return 게시물 상세 + 첨부파일
     * @throws EducationPostNotFoundException 게시물을 찾을 수 없음
     */
    fun getPostDetail(postId: String): EducationPostDetailResponse {
        // 1. 게시물 조회
        val post = educationPostRepository.findByEduId(postId)
            ?: throw EducationPostNotFoundException()

        // Phase2: EducationPostImage PG 대응 테이블 없음 - 주석 처리
        // val images = educationPostImageRepository.findByPostIdOrderBySortOrderAsc(postId)
        //     .map { image ->
        //         EducationImageResponse(
        //             id = image.id,
        //             url = image.url,
        //             sortOrder = image.sortOrder
        //         )
        //     }
        val images = emptyList<Any>()

        // 3. 첨부파일 목록 조회 (fileType 으로 이미지/동영상/문서 분기 + presigned URL)
        val attachments = educationPostAttachmentRepository.findByEducationPost(post)
            .map { attachment ->
                EducationAttachmentResponse(
                    id = attachment.fileKey,
                    fileName = attachment.fileOriginalName ?: "",
                    fileUrl = fileStorageService.getEducationFileUrl(attachment.fileKey),
                    fileType = attachment.fileType ?: "",
                    fileSize = 0
                )
            }

        // 4. 카테고리명 조회
        val categoryName = EducationCategoryCode.displayNameOf(post.eduCode)

        // 5. 본문 인라인 이미지 placeholder → presigned URL rewrite.
        //    DB 에는 만료 없는 placeholder 만 저장하고 조회 시점에 발급한다 (web/mobile 공통).
        //    upload_file 조회는 지연 평가 — 본문에 인라인 이미지가 없으면(레거시 이관분 대부분) 쿼리가 나가지 않는다.
        val content = inlineImageService.rewriteInlineImages(
            InlineImageDomain.EDUCATION,
            post.eduContent ?: "",
        ) { inlineUploadFilesOf(post) }

        // 6. 응답 생성
        return EducationPostDetailResponse(
            id = post.eduId ?: "",
            category = post.eduCode ?: "",
            categoryName = categoryName,
            title = post.eduTitle ?: "",
            content = content,
            createdAt = post.createdAt,
            images = images,
            attachments = attachments
        )
    }

    /**
     * Admin 교육 목록 조회 (category 선택적)
     */
    fun getPostsForAdmin(
        category: String?,
        search: String?,
        page: Int = 1,
        size: Int = 10
    ): AdminEducationListResponse {
        if (!category.isNullOrBlank() && !EducationCategoryCode.exists(category)) {
            throw InvalidEducationCategoryException()
        }

        val pageable = PageRequest.of(page - 1, size)
        val postsPage = educationPostRepository.findByOptionalEduCodeAndSearchWithPaging(
            category, search, pageable
        )

        val attachmentCounts = postsPage.content.associate { post ->
            post.eduId to educationPostAttachmentRepository.findByEducationPost(post).size
        }

        val summaries = postsPage.content.map { post ->
            val categoryName = EducationCategoryCode.displayNameOf(post.eduCode)

            AdminEducationPostSummary(
                eduId = post.eduId ?: "",
                eduTitle = post.eduTitle ?: "",
                eduCode = post.eduCode ?: "",
                eduCodeNm = categoryName,
                instDate = post.createdAt,
                attachmentCount = attachmentCounts[post.eduId] ?: 0
            )
        }

        return AdminEducationListResponse(
            content = summaries,
            totalCount = postsPage.totalElements,
            totalPages = postsPage.totalPages,
            currentPage = page,
            size = size
        )
    }

    /**
     * 교육 자료 작성
     */
    @Transactional
    fun createPost(
        userId: Long,
        title: String,
        content: String,
        category: String,
        files: List<MultipartFile>?,
        sessionUploadedRefids: List<String>? = null
    ): EducationMutationResponse {
        validatePostInput(title, content, category)
        validateFiles(files)

        val employee = employeeRepository.findById(userId)
            .orElseThrow { InvalidEducationParameterException("사용자를 찾을 수 없습니다") }

        val now = LocalDateTime.now()
        val eduId = "edu" + EDU_ID_FORMATTER.format(now)

        val post = EducationPost(
            eduId = eduId,
            eduTitle = title,
            eduContent = content,
            eduCode = category,
            employee = employee,
            empCode = employee.employeeCode
        )
        val savedPost = educationPostRepository.save(post)

        // 본문 인라인 이미지 정규화는 save 이후에만 가능하다 — placeholder 의 parent_id 로 쓸 PK 가 그때 정해진다.
        // (업로드 시점엔 글이 없어 parent_id=null 로 떠 있다가 syncInlineImages 의 backfill 이 연결한다.)
        savedPost.eduContent = inlineImageService.normalizeContent(
            InlineImageDomain.EDUCATION, savedPost.id, savedPost.eduContent,
        )
        inlineImageService.syncInlineImages(
            InlineImageDomain.EDUCATION, savedPost.id, savedPost.eduContent, sessionUploadedRefids,
        )

        val attachments = saveAttachments(savedPost, files)
        val categoryName = EducationCategoryCode.displayNameOf(category)

        return toMutationResponse(savedPost, categoryName, attachments)
    }

    /**
     * 교육 자료 수정
     */
    @Transactional
    fun updatePost(
        postId: String,
        title: String,
        content: String,
        category: String,
        files: List<MultipartFile>?,
        keepFileKeys: List<String>?,
        sessionUploadedRefids: List<String>? = null
    ): EducationMutationResponse {
        val post = educationPostRepository.findByEduId(postId)
            ?: throw EducationPostNotFoundException()

        validatePostInput(title, content, category)

        val existingAttachments = educationPostAttachmentRepository.findByEducationPost(post)
        val keysToKeep = keepFileKeys ?: emptyList()

        // keep_file_keys 유효성 검증
        val existingKeys = existingAttachments.map { it.fileKey }.toSet()
        keysToKeep.forEach { key ->
            if (key !in existingKeys) {
                throw InvalidFileKeyException()
            }
        }

        val keptCount = keysToKeep.size
        val newFileCount = files?.size ?: 0
        if (keptCount + newFileCount > MAX_ATTACHMENTS) {
            throw FileLimitExceededException()
        }

        validateFileSizes(files)

        // 유지하지 않는 파일 삭제
        existingAttachments.filter { it.fileKey !in keysToKeep }.forEach { attachment ->
            fileStorageService.deleteEducationFile(postId, attachment.fileKey)
            educationPostAttachmentRepository.delete(attachment)
        }

        // 신규 파일 저장
        saveAttachments(post, files)

        // 본문 인라인 이미지 정규화 — 클라이언트가 placeholder 복원에 실패해도 만료 presigned URL 이
        // DB 에 남지 않도록 서버가 저장 시점에 한 번 더 되돌린다.
        val normalizedContent = inlineImageService.normalizeContent(
            InlineImageDomain.EDUCATION, post.id, content,
        )

        // 엔티티 업데이트 (대부분 val 필드이므로 새 인스턴스 생성 후 merge)
        val updated = EducationPost(
            id = post.id,
            eduId = post.eduId,
            eduTitle = title,
            eduContent = normalizedContent,
            eduCode = category,
            employee = post.employee,
            empCode = post.empCode
        ).apply {
            createdAt = post.createdAt
            updatedAt = LocalDateTime.now()
        }
        educationPostRepository.save(updated)

        // 본문에서 빠진 인라인 이미지 정리 + 이번 세션 업로드분 소속 연결.
        inlineImageService.syncInlineImages(
            InlineImageDomain.EDUCATION, post.id, normalizedContent, sessionUploadedRefids,
        )

        val allAttachments = educationPostAttachmentRepository.findByEducationPost(updated)
        val categoryName = EducationCategoryCode.displayNameOf(category)

        return toMutationResponse(updated, categoryName, allAttachments)
    }

    /**
     * 교육 자료 삭제.
     *
     * 첨부파일과 본문 인라인 이미지는 저장 경로가 다르므로 각각 정리한다. 인라인 이미지를 여기서 지우는
     * 이유는 교육 게시물이 **hard delete** 이기 때문 — 부모 row 가 사라지면 upload_file 이 영원히 고아로
     * 남아 S3 비용만 쌓인다. (공지는 soft delete 라 본문이 남아 있어 이 단계가 없다.)
     */
    @Transactional
    fun deletePost(postId: String) {
        val post = educationPostRepository.findByEduId(postId)
            ?: throw EducationPostNotFoundException()

        val attachments = educationPostAttachmentRepository.findByEducationPost(post)
        attachments.forEach { attachment ->
            fileStorageService.deleteEducationFile(postId, attachment.fileKey)
        }
        educationPostAttachmentRepository.deleteAll(attachments)

        inlineUploadFilesOf(post).forEach { file ->
            file.uniqueKey?.takeIf { it.isNotBlank() }?.let { storageService.deletePrivate(it) }
            file.isDeleted = true
        }

        educationPostRepository.delete(post)
    }

    /**
     * 교육 본문 인라인 이미지 업로드 (작성/수정 화면 Quill 툴바/드래그앤드롭/붙여넣기).
     *
     * 첨부파일([uploadEducationFile]) 과 저장 경로가 다르다 — 첨부는 file_key(30자 평면) 를 쓰는
     * education_post_attachment, 인라인은 unique_key(500자) 를 쓰는 upload_file. 두 목록은 서로 섞이지 않는다.
     * 업로드 시점에 parent_id 를 채우지 않는 이유는 [InlineImageService.uploadInlineImage] 참조.
     */
    @Transactional
    fun uploadInlineImage(file: MultipartFile): InlineImageUploadResult {
        if (file.isEmpty) {
            throw InvalidEducationParameterException("빈 파일은 업로드할 수 없습니다")
        }
        return inlineImageService.uploadInlineImage(InlineImageDomain.EDUCATION, file)
    }

    /**
     * 카테고리 목록 조회 (enum 선언 순서 = 노출 순서)
     */
    fun getCategories(): List<EducationCategoryResponse> {
        return EducationCategoryCode.entries.map {
            EducationCategoryResponse(eduCode = it.code, eduCodeNm = it.displayName)
        }
    }

    // --- Private helpers ---

    /**
     * 이 게시물에 소속된 본문 인라인 이미지 upload_file 목록.
     *
     * parent_id 는 화면 식별자 eduId(String)가 아니라 **PK id(Long)** 다 — upload_file.parent_id 가 bigint 라
     * eduId 를 넣을 수 없다. 조회/정리 양쪽이 같은 기준을 쓰도록 여기 한 곳으로 모은다.
     */
    private fun inlineUploadFilesOf(post: EducationPost) =
        uploadFileRepository.findByParentTypeAndParentIdAndIsDeletedFalse(
            UploadFileParentTypes.EDUCATION_POST, post.id,
        )

    private fun validatePostInput(title: String, content: String, category: String) {
        if (title.isBlank() || title.length > 150) {
            throw InvalidEducationParameterException("제목은 1~150자여야 합니다")
        }
        if (content.isBlank()) {
            throw InvalidEducationParameterException("본문은 필수입니다")
        }
        if (!EducationCategoryCode.exists(category)) {
            throw InvalidEducationCategoryException()
        }
    }

    private fun validateFiles(files: List<MultipartFile>?) {
        if (files == null) return
        if (files.size > MAX_ATTACHMENTS) {
            throw FileLimitExceededException()
        }
        validateFileSizes(files)
    }

    private fun validateFileSizes(files: List<MultipartFile>?) {
        files?.forEach { file ->
            if (file.size > MAX_FILE_SIZE) {
                throw FileSizeExceededException()
            }
        }
    }

    private fun saveAttachments(
        post: EducationPost,
        files: List<MultipartFile>?
    ): List<EducationPostAttachment> {
        if (files.isNullOrEmpty()) return emptyList()

        return files.map { file ->
            val fileKey = fileStorageService.uploadEducationFile(file, post.eduId ?: "")
            val originalName = file.originalFilename ?: "unknown"
            val fileType = determineFileType(originalName)

            val attachment = EducationPostAttachment(
                educationPost = post,
                fileKey = fileKey,
                fileType = fileType,
                fileOriginalName = originalName
            )
            educationPostAttachmentRepository.save(attachment)
        }
    }

    private fun determineFileType(filename: String): String {
        val ext = filename.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "jpg", "jpeg", "png", "gif" -> "f00001"
            "mp4", "avi", "wmv", "mkv", "mov", "m4v" -> "f00002"
            "pdf", "docx", "txt", "hwp", "pptx", "xlsx" -> "f00003"
            else -> "f00004"
        }
    }

    private fun toMutationResponse(
        post: EducationPost,
        categoryName: String,
        attachments: List<EducationPostAttachment>
    ): EducationMutationResponse {
        return EducationMutationResponse(
            eduId = post.eduId ?: "",
            eduTitle = post.eduTitle ?: "",
            eduContent = post.eduContent ?: "",
            eduCode = post.eduCode ?: "",
            eduCodeNm = categoryName,
            employeeId = post.employee?.id,
            instDate = post.createdAt,
            updDate = post.updatedAt,
            attachments = attachments.map {
                AttachmentInfo(
                    fileKey = it.fileKey,
                    fileType = it.fileType ?: "",
                    fileOriginalName = it.fileOriginalName ?: ""
                )
            }
        )
    }

    companion object {
        private val EDU_ID_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .withZone(TimeZones.SEOUL_ZONE)
        private const val MAX_ATTACHMENTS = 20
        private const val MAX_FILE_SIZE = 50 * 1024 * 1024L // 50MB
    }
}
