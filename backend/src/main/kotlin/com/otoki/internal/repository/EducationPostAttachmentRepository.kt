package com.otoki.internal.repository

import com.otoki.internal.entity.EducationPostAttachment
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 교육 게시물 첨부파일 Repository
 */
interface EducationPostAttachmentRepository : JpaRepository<EducationPostAttachment, Long> {

    /**
     * 게시물별 첨부파일 조회
     */
    fun findByPostId(postId: Long): List<EducationPostAttachment>
}
