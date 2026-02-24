package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 사원 로그인 이력 Entity (employee_his 테이블)
 *
 * 원본 테이블에 PK가 없으므로 @IdClass 전략으로
 * (empCode, instDate) 논리적 복합 키를 구성.
 * EmployeeMng.empCode 논리적 참조 (외래 키 제약 없음).
 */
@Entity
@Table(name = "employee_his")
@IdClass(EmployeeLoginHistoryId::class)
class EmployeeLoginHistory(

    @Id
    @Column(name = "empcode__c", nullable = false, length = 80)
    val empCode: String,

    @Id
    @Column(name = "inst_date", nullable = false)
    val instDate: LocalDateTime
)
