package com.otoki.internal.repository

import com.otoki.internal.entity.EducationFileId
import com.otoki.internal.entity.EducationPostAttachment
import org.springframework.data.jpa.repository.JpaRepository

/**
 * 교육 첨부파일 Repository
 */
interface EducationPostAttachmentRepository : JpaRepository<EducationPostAttachment, EducationFileId> {

    /**
     * 교육 게시글별 첨부파일 조회
     */
    fun findByEduId(eduId: String): List<EducationPostAttachment>

    // --- 주석 처리: V1 스키마 변경으로 불필요 ---
    // fun findByPostId(postId: Long): List<EducationPostAttachment>
}
