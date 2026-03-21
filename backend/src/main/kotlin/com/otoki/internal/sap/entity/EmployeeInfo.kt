package com.otoki.internal.sap.entity

import com.otoki.internal.common.entity.BaseEntity
import com.otoki.internal.common.salesforce.HCColumn
import com.otoki.internal.common.salesforce.HCTable
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 사원 인증/기기 정보 Entity (employee_info 테이블)
 *
 * User와 1:1 관계. employee_number = employee_number 로 조인.
 */
@Entity
@Table(name = "employee_info")
@HCTable("employee_mng")
class EmployeeInfo(

    @Id
    @HCColumn("empcode__c")
    @Column(name = "employee_number", length = 40)
    val employeeNumber: String,

    @HCColumn("emp_pwd")
    @Column(name = "emp_pwd", length = 200)
    var password: String? = null,

    @HCColumn("pwd_yn")
    @Column(name = "pwd_yn")
    var passwordChangeRequired: Boolean? = true,

    @HCColumn("emp_uuid")
    @Column(name = "emp_uuid", length = 200)
    var deviceUuid: String? = null,

    @HCColumn("emp_token")
    @Column(name = "emp_token", length = 200)
    var fcmToken: String? = null,

    @HCColumn("gps_yn")
    @Column(name = "gps_yn")
    val gpsYn: Boolean? = null,

    @HCColumn("gps_yn_date")
    @Column(name = "gps_yn_date")
    val gpsYnDate: LocalDateTime? = null,

    @Column(name = "last_agreement_number", length = 80)
    var lastAgreementNumber: String? = null
) : BaseEntity()
