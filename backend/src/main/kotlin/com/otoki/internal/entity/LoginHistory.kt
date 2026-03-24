package com.otoki.internal.entity

import com.otoki.internal.common.salesforce.HCColumn
import com.otoki.internal.common.salesforce.HCTable
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 로그인 이력 Entity (login_history 테이블)
 *
 * 원본 테이블에 PK가 없으므로 @IdClass 전략으로
 * (empCode, instDate) 논리적 복합 키를 구성.
 * EmployeeInfo.empCode 논리적 참조 (외래 키 제약 없음).
 */
@Entity
@Table(name = "login_history")
@IdClass(LoginHistoryId::class)
@HCTable("employee_his")
class LoginHistory(

    @Id
    @HCColumn("empcode__c")
    @Column(name = "employee_code", nullable = false, length = 80)
    val empCode: String,

    @Id
    @HCColumn("inst_date")
    @Column(name = "login_at", nullable = false)
    val instDate: LocalDateTime = LocalDateTime.now()
)
