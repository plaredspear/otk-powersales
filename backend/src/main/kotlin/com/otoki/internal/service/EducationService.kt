package com.otoki.internal.service

import com.otoki.internal.dto.response.*
import com.otoki.internal.entity.EducationCategory
import com.otoki.internal.exception.EducationPostNotFoundException
import com.otoki.internal.exception.InvalidEducationCategoryException
import com.otoki.internal.repository.EducationPostAttachmentRepository
// import com.otoki.internal.repository.EducationPostImageRepository  // Phase2: PG 대응 테이블 없음
import com.otoki.internal.repository.EducationPostRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.format.DateTimeFormatter

/**
 * 교육 비즈니스 로직 Service
 */
@Service
@Transactional(readOnly = true)
class EducationService(
    private val educationPostRepository: EducationPostRepository,
    // private val educationPostImageRepository: EducationPostImageRepository,  // Phase2: PG 대응 테이블 없음
    private val educationPostAttachmentRepository: EducationPostAttachmentRepository
) {

    companion object {
        private val DATE_TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    }

    /**
     * 교육 게시물 목록 조회
     *
     * @param category 카테고리 문자열 (EducationCategory enum name)
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
        // 1. 카테고리 enum 변환
        val categoryEnum = try {
            EducationCategory.fromString(category)
        } catch (e: IllegalArgumentException) {
            throw InvalidEducationCategoryException()
        }

        // 2. 페이지네이션 (page는 1부터, Spring Data는 0부터)
        val pageable = PageRequest.of(page - 1, size)

        // 3. 검색 키워드에 따라 다른 메서드 호출
        val postsPage = if (search.isNullOrBlank()) {
            educationPostRepository.findByCategoryAndIsActiveTrueOrderByCreatedAtDesc(
                categoryEnum,
                pageable
            )
        } else {
            educationPostRepository.findByCategoryAndSearchWithPaging(
                categoryEnum,
                search.take(100), // 최대 100자까지만 검색
                pageable
            )
        }

        // 4. Entity → DTO 변환
        val summaries = postsPage.content.map { post ->
            EducationPostSummaryResponse(
                id = post.id,
                title = post.title,
                createdAt = post.createdAt.format(DATE_TIME_FORMATTER)
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
     * @param postId 게시물 ID
     * @return 게시물 상세 + 이미지 + 첨부파일
     * @throws EducationPostNotFoundException 게시물을 찾을 수 없음
     */
    fun getPostDetail(postId: Long): EducationPostDetailResponse {
        // 1. 게시물 조회 (isActive=true만)
        val post = educationPostRepository.findByIdAndIsActiveTrue(postId)
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

        // 3. 첨부파일 목록 조회
        val attachments = educationPostAttachmentRepository.findByPostId(postId)
            .map { attachment ->
                EducationAttachmentResponse(
                    id = attachment.id,
                    fileName = attachment.fileName,
                    fileUrl = attachment.fileUrl,
                    fileSize = attachment.fileSize
                )
            }

        // 4. 응답 생성
        return EducationPostDetailResponse(
            id = post.id,
            category = post.category.name,
            categoryName = post.category.displayName,
            title = post.title,
            content = post.content,
            createdAt = post.createdAt.format(DATE_TIME_FORMATTER),
            images = images,
            attachments = attachments
        )
    }
}
