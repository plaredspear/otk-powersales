/*
package com.otoki.internal.service

import com.otoki.internal.dto.response.*
import com.otoki.internal.entity.NoticeCategory
import com.otoki.internal.exception.InvalidNoticeCategoryException
import com.otoki.internal.exception.NoticePostNotFoundException
import com.otoki.internal.repository.NoticePostImageRepository
import com.otoki.internal.repository.NoticePostRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.format.DateTimeFormatter

/ **
 * 공지사항 비즈니스 로직 Service
 * /
@Service
@Transactional(readOnly = true)
class NoticeService(
    private val noticePostRepository: NoticePostRepository,
    private val noticePostImageRepository: NoticePostImageRepository
) {

    companion object {
        private val DATE_TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    }

    / **
     * 공지사항 게시물 목록 조회
     *
     * @param category 카테고리 문자열 (nullable, null이면 전체 조회)
     * @param search 검색 키워드 (nullable, 제목+내용 LIKE 검색)
     * @param page 페이지 번호 (1부터 시작)
     * @param size 페이지 크기
     * @return 게시물 목록 + 페이지네이션 정보
     * @throws InvalidNoticeCategoryException 유효하지 않은 카테고리
     * /
    fun getPosts(
        category: String?,
        search: String?,
        page: Int = 1,
        size: Int = 10
    ): NoticePostListResponse {
        // 1. 카테고리 enum 변환 (null이면 전체 조회)
        val categoryEnum = category?.let {
            try {
                NoticeCategory.fromString(it)
            } catch (e: IllegalArgumentException) {
                throw InvalidNoticeCategoryException()
            }
        }

        // 2. 페이지네이션 (page는 1부터, Spring Data는 0부터)
        val pageable = PageRequest.of(page - 1, size)

        // 3. 카테고리 및 검색 키워드에 따라 다른 메서드 호출
        val postsPage = when {
            // 전체 + 검색
            categoryEnum == null && !search.isNullOrBlank() -> {
                noticePostRepository.findBySearchWithPaging(
                    search.take(100), // 최대 100자까지만 검색
                    pageable
                )
            }
            // 분류별 + 검색
            categoryEnum != null && !search.isNullOrBlank() -> {
                noticePostRepository.findByCategoryAndSearchWithPaging(
                    categoryEnum,
                    search.take(100),
                    pageable
                )
            }
            // 분류별 (검색 없음)
            categoryEnum != null -> {
                noticePostRepository.findByCategoryAndIsActiveTrueOrderByCreatedAtDesc(
                    categoryEnum,
                    pageable
                )
            }
            // 전체 (검색 없음)
            else -> {
                noticePostRepository.findByIsActiveTrueOrderByCreatedAtDesc(pageable)
            }
        }

        // 4. Entity → DTO 변환
        val summaries = postsPage.content.map { post ->
            NoticePostSummaryResponse(
                id = post.id,
                category = post.category.name,
                categoryName = post.category.displayName,
                title = post.title,
                createdAt = post.createdAt.format(DATE_TIME_FORMATTER)
            )
        }

        return NoticePostListResponse(
            content = summaries,
            totalCount = postsPage.totalElements,
            totalPages = postsPage.totalPages,
            currentPage = page,
            size = size
        )
    }

    / **
     * 공지사항 게시물 상세 조회
     *
     * @param postId 게시물 ID
     * @return 게시물 상세 + 이미지
     * @throws NoticePostNotFoundException 게시물을 찾을 수 없음
     * /
    fun getPostDetail(postId: Long): NoticePostDetailResponse {
        // 1. 게시물 조회 (isActive=true만)
        val post = noticePostRepository.findByIdAndIsActiveTrue(postId)
            ?: throw NoticePostNotFoundException()

        // 2. 이미지 목록 조회 (sortOrder 오름차순)
        val images = noticePostImageRepository.findByPostIdOrderBySortOrderAsc(postId)
            .map { image ->
                NoticeImageResponse(
                    id = image.id,
                    url = image.url,
                    sortOrder = image.sortOrder
                )
            }

        // 3. 응답 생성
        return NoticePostDetailResponse(
            id = post.id,
            category = post.category.name,
            categoryName = post.category.displayName,
            title = post.title,
            content = post.content,
            createdAt = post.createdAt.format(DATE_TIME_FORMATTER),
            images = images
        )
    }
}
*/
