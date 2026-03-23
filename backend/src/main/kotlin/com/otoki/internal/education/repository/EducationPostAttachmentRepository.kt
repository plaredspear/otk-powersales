package com.otoki.internal.education.repository

import com.otoki.internal.education.entity.EducationPost
import com.otoki.internal.education.entity.EducationPostAttachment
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 교육 첨부파일 Repository
 */
interface EducationPostAttachmentRepository : JpaRepository<EducationPostAttachment, Long> {

    /**
     * 교육 게시글별 첨부파일 조회
     */
    fun findByEducationPost(educationPost: EducationPost): List<EducationPostAttachment>

    fun deleteByEducationPost(educationPost: EducationPost)
}
