package com.otoki.internal.repository

import com.otoki.internal.entity.EducationCategory
import com.otoki.internal.entity.EducationPost
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

/**
 * 교육 게시물 Repository
 */
interface EducationPostRepository : JpaRepository<EducationPost, Long> {

    /**
     * 카테고리별 활성 게시물 조회 (페이지네이션)
     * isActive가 true인 게시물만 조회하며, createdAt 내림차순 정렬
     */
    fun findByCategoryAndIsActiveTrueOrderByCreatedAtDesc(
        category: EducationCategory,
        pageable: Pageable
    ): Page<EducationPost>

    /**
     * 카테고리별 활성 게시물 검색 (타이틀 + 내용 LIKE 검색, 페이지네이션)
     * isActive가 true이고, title 또는 content에 검색 키워드가 포함된 게시물을 조회
     */
    @Query(
        """
        SELECT p FROM EducationPost p
        WHERE p.category = :category
          AND p.isActive = true
          AND (p.title LIKE %:search% OR p.content LIKE %:search%)
        ORDER BY p.createdAt DESC
        """
    )
    fun findByCategoryAndSearchWithPaging(
        category: EducationCategory,
        search: String,
        pageable: Pageable
    ): Page<EducationPost>

    /**
     * ID로 활성 게시물 조회
     */
    fun findByIdAndIsActiveTrue(id: Long): EducationPost?
}
