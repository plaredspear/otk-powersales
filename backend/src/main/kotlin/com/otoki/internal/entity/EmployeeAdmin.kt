package com.otoki.internal.entity

import jakarta.persistence.*

/**
 * 사원 관리자 Entity (employee_admin_mng 테이블)
 *
 * 단일 컬럼 String PK. 관리자 사번 존재 여부로 권한 체크.
 */
@Entity
@Table(name = "employee_admin_mng")
class EmployeeAdmin(

    @Id
    @Column(name = "empcode__c", length = 40)
    val empCode: String
)
