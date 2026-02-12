package com.otoki.internal.repository

import com.otoki.internal.entity.EducationPostImage
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 교육 게시물 이미지 Repository
 */
interface EducationPostImageRepository : JpaRepository<EducationPostImage, Long> {

    /**
     * 게시물별 이미지 조회 (정렬 순서대로)
     */
    fun findByPostIdOrderBySortOrderAsc(postId: Long): List<EducationPostImage>
}
