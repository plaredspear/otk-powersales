package com.otoki.internal.common.entity

import jakarta.persistence.*
import java.io.Serializable
import java.time.LocalDateTime

/**
 * 로그인 이력 복합 키
 *
 * employee_his 테이블에 PK가 없으므로 employeeId + loginAt을 복합 키로 사용.
 * INSERT 전용 테이블이라 유일성 충돌 가능성은 실질적으로 없다.
 */
class LoginHistoryId(
    val employeeId: String = "",
    val loginAt: LocalDateTime = LocalDateTime.MIN
) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LoginHistoryId) return false
        return employeeId == other.employeeId && loginAt == other.loginAt
    }

    override fun hashCode(): Int {
        var result = employeeId.hashCode()
        result = 31 * result + loginAt.hashCode()
        return result
    }
}

/**
 * 로그인 이력 Entity
 *
 * 레거시 employee_his 테이블에 매핑. 쓰기 전용(write-only).
 * 로그인 성공 시 사번과 로그인 시각을 INSERT한다.
 */
@Entity
@Table(name = "employee_his")
@IdClass(LoginHistoryId::class)
class LoginHistory(

    @Id
    @Column(name = "empcode__c", length = 80)
    val employeeId: String,

    @Id
    @Column(name = "inst_date")
    val loginAt: LocalDateTime = LocalDateTime.now()
)
