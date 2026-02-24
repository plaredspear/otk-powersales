package com.otoki.internal.education.entity

import java.io.Serializable
import java.time.LocalDateTime

/**
 * 교육 조회 이력 복합 키
 *
 * education_member_history 테이블에 PK가 없으므로
 * (communityId, empCode, instDate)를 복합 키로 사용.
 */
class EducationViewHistoryId(
    val communityId: String = "",
    val empCode: String = "",
    val instDate: LocalDateTime? = null
) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EducationViewHistoryId) return false
        return communityId == other.communityId &&
            empCode == other.empCode &&
            instDate == other.instDate
    }

    override fun hashCode(): Int {
        var result = communityId.hashCode()
        result = 31 * result + empCode.hashCode()
        result = 31 * result + (instDate?.hashCode() ?: 0)
        return result
    }
}
