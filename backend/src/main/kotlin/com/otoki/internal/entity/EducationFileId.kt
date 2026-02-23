package com.otoki.internal.entity

import java.io.Serializable

/**
 * 교육 첨부파일 복합 키
 *
 * education_file_mng 테이블에 PK가 없으므로 (eduId, eduFileKey)를 복합 키로 사용.
 */
class EducationFileId(
    val eduId: String = "",
    val eduFileKey: String = ""
) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EducationFileId) return false
        return eduId == other.eduId && eduFileKey == other.eduFileKey
    }

    override fun hashCode(): Int {
        var result = eduId.hashCode()
        result = 31 * result + eduFileKey.hashCode()
        return result
    }
}
