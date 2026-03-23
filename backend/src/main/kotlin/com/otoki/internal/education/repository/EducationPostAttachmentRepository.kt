package com.otoki.internal.education.repository

import com.otoki.internal.education.entity.EducationFileId
import com.otoki.internal.education.entity.EducationPostAttachment
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 교육 첨부파일 Repository
 */
interface EducationPostAttachmentRepository : JpaRepository<EducationPostAttachment, EducationFileId> {

    /**
     * 교육 게시글별 첨부파일 조회
     */
    fun findByEducationPostId(educationPostId: String): List<EducationPostAttachment>

    fun deleteByEducationPostId(educationPostId: String)
}
