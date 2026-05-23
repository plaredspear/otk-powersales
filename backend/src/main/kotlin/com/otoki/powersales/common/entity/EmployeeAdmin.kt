package com.otoki.powersales.common.entity

import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HerokuOnly
import jakarta.persistence.*

/**
 * 사원 관리자 Entity (employee_admin 테이블, 구: employee_admin_mng)
 *
 * 단일 컬럼 String PK. 관리자 사번 존재 여부로 권한 체크.
 */
@Entity
@Table(name = "employee_admin")
@HerokuOnly("employee_admin_mng")
class EmployeeAdmin(

    @Id
    @HCColumn("empcode__c")
    @Column(name = "employee_code", length = 40)
    val empCode: String
) : BaseEntity()
