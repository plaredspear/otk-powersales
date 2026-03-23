package com.otoki.internal.entity

import com.otoki.internal.common.entity.BaseEntity
import com.otoki.internal.common.salesforce.HCColumn
import com.otoki.internal.common.salesforce.HCTable
import jakarta.persistence.*

/**
 * 사원 관리자 Entity (employee_admin 테이블, 구: employee_admin_mng)
 *
 * 단일 컬럼 String PK. 관리자 사번 존재 여부로 권한 체크.
 */
@Entity
@Table(name = "employee_admin")
@HCTable("employee_admin_mng")
class EmployeeAdmin(

    @Id
    @HCColumn("empcode__c")
    @Column(name = "emp_code", length = 40)
    val empCode: String
) : BaseEntity()
