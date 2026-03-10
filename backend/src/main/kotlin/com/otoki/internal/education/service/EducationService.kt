package com.otoki.internal.education.service

import com.otoki.internal.education.dto.response.*
import com.otoki.internal.common.dto.response.*
import com.otoki.internal.common.service.FileStorageService
import com.otoki.internal.education.entity.EducationPost
import com.otoki.internal.education.entity.EducationPostAttachment
import com.otoki.internal.education.exception.*
import com.otoki.internal.education.repository.EducationPostAttachmentRepository
// import com.otoki.internal.education.repository.EducationPostImageRepository  // Phase2: PG 대응 테이블 없음
import com.otoki.internal.education.repository.EducationCodeRepository
import com.otoki.internal.education.repository.EducationPostRepository
import com.otoki.internal.sap.repository.UserRepository
import org.springframework.data.domain.PageRequest
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
    private val educationCodeRepository: EducationCodeRepository,
    private val fileStorageService: FileStorageService,
    private val userRepository: UserRepository
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
        // 1. 카테고리 유효성 검증 (education_code_mng 테이블 참조)
        if (!educationCodeRepository.existsById(category)) {
            throw InvalidEducationCategoryException()
        }

        // 2. 페이지네이션 (page는 1부터, Spring Data는 0부터)
        val pageable = PageRequest.of(page - 1, size)

        // 3. 검색 키워드에 따라 다른 메서드 호출
        val postsPage = if (search.isNullOrBlank()) {
            educationPostRepository.findByEduCodeOrderByInstDateDesc(
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
                id = post.eduId,
                title = post.eduTitle ?: "",
                createdAt = post.instDate?.format(DATE_TIME_FORMATTER) ?: ""
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
        val post = educationPostRepository.findById(postId)
            .orElseThrow { EducationPostNotFoundException() }

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

        // 3. 첨부파일 목록 조회
        val attachments = educationPostAttachmentRepository.findByEduId(postId)
            .map { attachment ->
                EducationAttachmentResponse(
                    id = attachment.eduFileKey,
                    fileName = attachment.eduFileOrgNm ?: "",
                    fileUrl = attachment.eduFileKey,
                    fileSize = 0
                )
            }

        // 4. 카테고리명 조회
        val categoryName = post.eduCode?.let { code ->
            educationCodeRepository.findById(code).orElse(null)?.eduCodeNm
        } ?: ""

        // 5. 응답 생성
        return EducationPostDetailResponse(
            id = post.eduId,
            category = post.eduCode ?: "",
            categoryName = categoryName,
            title = post.eduTitle ?: "",
            content = post.eduContent ?: "",
            createdAt = post.instDate?.format(DATE_TIME_FORMATTER) ?: "",
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
        if (!category.isNullOrBlank() && !educationCodeRepository.existsById(category)) {
            throw InvalidEducationCategoryException()
        }

        val pageable = PageRequest.of(page - 1, size)
        val postsPage = educationPostRepository.findByOptionalEduCodeAndSearchWithPaging(
            category, search, pageable
        )

        val attachmentCounts = postsPage.content.associate { post ->
            post.eduId to educationPostAttachmentRepository.findByEduId(post.eduId).size
        }

        val summaries = postsPage.content.map { post ->
            val categoryName = post.eduCode?.let { code ->
                educationCodeRepository.findById(code).orElse(null)?.eduCodeNm
            } ?: ""

            AdminEducationPostSummary(
                eduId = post.eduId,
                eduTitle = post.eduTitle ?: "",
                eduCode = post.eduCode ?: "",
                eduCodeNm = categoryName,
                instDate = post.instDate?.format(DATE_TIME_FORMATTER) ?: "",
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
        files: List<MultipartFile>?
    ): EducationMutationResponse {
        validatePostInput(title, content, category)
        validateFiles(files)

        val user = userRepository.findById(userId)
            .orElseThrow { InvalidEducationParameterException("사용자를 찾을 수 없습니다") }

        val now = LocalDateTime.now()
        val eduId = "edu" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))

        val post = EducationPost(
            eduId = eduId,
            eduTitle = title,
            eduContent = content,
            eduCode = category,
            empCode = user.employeeId,
            instDate = now
        )
        educationPostRepository.save(post)

        val attachments = saveAttachments(eduId, files)
        val categoryName = educationCodeRepository.findById(category).orElse(null)?.eduCodeNm ?: ""

        return toMutationResponse(post, categoryName, attachments)
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
        keepFileKeys: List<String>?
    ): EducationMutationResponse {
        val post = educationPostRepository.findById(postId)
            .orElseThrow { EducationPostNotFoundException() }

        validatePostInput(title, content, category)

        val existingAttachments = educationPostAttachmentRepository.findByEduId(postId)
        val keysToKeep = keepFileKeys ?: emptyList()

        // keep_file_keys 유효성 검증
        val existingKeys = existingAttachments.map { it.eduFileKey }.toSet()
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
        existingAttachments.filter { it.eduFileKey !in keysToKeep }.forEach { attachment ->
            fileStorageService.deleteEducationFile(postId, attachment.eduFileKey)
            educationPostAttachmentRepository.delete(attachment)
        }

        // 신규 파일 저장
        saveAttachments(postId, files)

        // 엔티티 업데이트 (val 필드이므로 새 인스턴스 생성 후 merge)
        val updated = EducationPost(
            eduId = post.eduId,
            eduTitle = title,
            eduContent = content,
            eduCode = category,
            empCode = post.empCode,
            instDate = post.instDate,
            updDate = LocalDateTime.now()
        )
        educationPostRepository.save(updated)

        val allAttachments = educationPostAttachmentRepository.findByEduId(postId)
        val categoryName = educationCodeRepository.findById(category).orElse(null)?.eduCodeNm ?: ""

        return toMutationResponse(updated, categoryName, allAttachments)
    }

    /**
     * 교육 자료 삭제
     */
    @Transactional
    fun deletePost(postId: String) {
        val post = educationPostRepository.findById(postId)
            .orElseThrow { EducationPostNotFoundException() }

        val attachments = educationPostAttachmentRepository.findByEduId(postId)
        attachments.forEach { attachment ->
            fileStorageService.deleteEducationFile(postId, attachment.eduFileKey)
        }
        educationPostAttachmentRepository.deleteAll(attachments)
        educationPostRepository.delete(post)
    }

    /**
     * 카테고리 목록 조회
     */
    fun getCategories(): List<EducationCategoryResponse> {
        return educationCodeRepository.findAll()
            .sortedBy { it.eduCode }
            .map { EducationCategoryResponse(eduCode = it.eduCode, eduCodeNm = it.eduCodeNm ?: "") }
    }

    // --- Private helpers ---

    private fun validatePostInput(title: String, content: String, category: String) {
        if (title.isBlank() || title.length > 150) {
            throw InvalidEducationParameterException("제목은 1~150자여야 합니다")
        }
        if (content.isBlank()) {
            throw InvalidEducationParameterException("본문은 필수입니다")
        }
        if (!educationCodeRepository.existsById(category)) {
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
        eduId: String,
        files: List<MultipartFile>?
    ): List<EducationPostAttachment> {
        if (files.isNullOrEmpty()) return emptyList()

        return files.map { file ->
            val fileKey = fileStorageService.uploadEducationFile(file, eduId)
            val originalName = file.originalFilename ?: "unknown"
            val fileType = determineFileType(originalName)

            val attachment = EducationPostAttachment(
                eduId = eduId,
                eduFileKey = fileKey,
                eduFileType = fileType,
                eduFileOrgNm = originalName
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
            eduId = post.eduId,
            eduTitle = post.eduTitle ?: "",
            eduContent = post.eduContent ?: "",
            eduCode = post.eduCode ?: "",
            eduCodeNm = categoryName,
            empCode = post.empCode ?: "",
            instDate = post.instDate?.format(DATE_TIME_FORMATTER) ?: "",
            updDate = post.updDate?.format(DATE_TIME_FORMATTER),
            attachments = attachments.map {
                AttachmentInfo(
                    eduFileKey = it.eduFileKey,
                    eduFileType = it.eduFileType ?: "",
                    eduFileOrgNm = it.eduFileOrgNm ?: ""
                )
            }
        )
    }

    companion object {
        private val DATE_TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        private const val MAX_ATTACHMENTS = 20
        private const val MAX_FILE_SIZE = 50 * 1024 * 1024L // 50MB
    }
}
