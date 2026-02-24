package com.otoki.internal.entity

import java.io.Serializable
import java.time.LocalDateTime

/**
 * EmployeeLoginHistory 복합 키
 *
 * employee_his 테이블에 PK가 없으므로
 * (empCode, instDate)를 논리적 복합 키로 사용.
 */
class EmployeeLoginHistoryId(
    val empCode: String = "",
    val instDate: LocalDateTime? = null
) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EmployeeLoginHistoryId) return false
        return empCode == other.empCode &&
            instDate == other.instDate
    }

    override fun hashCode(): Int {
        var result = empCode.hashCode()
        result = 31 * result + (instDate?.hashCode() ?: 0)
        return result
    }
}
