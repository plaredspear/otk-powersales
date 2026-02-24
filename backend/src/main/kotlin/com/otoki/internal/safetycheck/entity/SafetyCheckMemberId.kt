package com.otoki.internal.safetycheck.entity

import java.io.Serializable
import java.time.LocalDate

/**
 * SafetyCheckSubmission 복합 키
 *
 * safetycheck__workschedule__member 테이블에 단일 PK가 없으므로
 * (masterId, employeeId, workingDate)를 복합 키로 사용.
 */
class SafetyCheckMemberId(
    val masterId: String = "",
    val employeeId: String = "",
    val workingDate: LocalDate? = null
) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SafetyCheckMemberId) return false
        return masterId == other.masterId &&
            employeeId == other.employeeId &&
            workingDate == other.workingDate
    }

    override fun hashCode(): Int {
        var result = masterId.hashCode()
        result = 31 * result + employeeId.hashCode()
        result = 31 * result + (workingDate?.hashCode() ?: 0)
        return result
    }
}
