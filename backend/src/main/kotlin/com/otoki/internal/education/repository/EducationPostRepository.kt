package com.otoki.internal.education.repository

import com.otoki.internal.education.entity.EducationPost
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 교육 게시물 Repository
 */
interface EducationPostRepository : JpaRepository<EducationPost, String>, EducationPostRepositoryCustom {

    /**
     * 카테고리별 게시물 조회 (페이지네이션)
     * eduCode 기준, instDate 내림차순 정렬
     */
    fun findByEduCodeOrderByInstDateDesc(
        eduCode: String,
        pageable: Pageable
    ): Page<EducationPost>

    // --- 주석 처리: V1 스키마 변경으로 불필요 ---
    // fun findByCategoryAndIsActiveTrueOrderByCreatedAtDesc(category: EducationCategory, pageable: Pageable): Page<EducationPost>
    // fun findByCategoryAndSearchWithPaging(category: EducationCategory, search: String, pageable: Pageable): Page<EducationPost>
    // fun findByIdAndIsActiveTrue(id: Long): EducationPost?
}
