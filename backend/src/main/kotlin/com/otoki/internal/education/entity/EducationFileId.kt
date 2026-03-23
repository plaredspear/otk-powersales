package com.otoki.internal.education.entity

import java.io.Serializable

/**
 * 교육 첨부파일 복합 키
 *
 * education_post_attachment 테이블에 PK가 없으므로 (educationPostId, fileKey)를 복합 키로 사용.
 */
class EducationFileId(
    val educationPostId: String = "",
    val fileKey: String = ""
) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EducationFileId) return false
        return educationPostId == other.educationPostId && fileKey == other.fileKey
    }

    override fun hashCode(): Int {
        var result = educationPostId.hashCode()
        result = 31 * result + fileKey.hashCode()
        return result
    }
}
