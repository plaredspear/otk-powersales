package com.otoki.powersales.employee.entity

import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HCTable
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 사원 인증/기기 정보 Entity (employee_info 테이블)
 *
 * User와 1:1 관계. employee_code = employee_code 로 조인.
 */
@Entity
@Table(name = "employee_info")
@HCTable("employee_mng")
class EmployeeInfo(

    @Id
    @HCColumn("empcode__c")
    @Column(name = "employee_code", length = 40)
    val employeeCode: String,

    @HCColumn("emp_pwd")
    @Column(name = "password", length = 200)
    var password: String? = null,

    @HCColumn("pwd_yn")
    @Column(name = "password_change_required")
    var passwordChangeRequired: Boolean? = true,

    @HCColumn("emp_uuid")
    @Column(name = "device_uuid", length = 200)
    var deviceUuid: String? = null,

    @HCColumn("emp_token")
    @Column(name = "fcm_token", length = 200)
    var fcmToken: String? = null,

    @HCColumn("gps_yn")
    @Column(name = "gps_consent")
    val gpsYn: Boolean? = null,

    @HCColumn("gps_yn_date")
    @Column(name = "gps_consent_date")
    val gpsYnDate: LocalDateTime? = null,

    @Column(name = "last_agreement_number", length = 80)
    var lastAgreementNumber: String? = null
) : BaseEntity()
