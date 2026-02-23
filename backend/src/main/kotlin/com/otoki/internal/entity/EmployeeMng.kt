package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 사원 인증/기기 정보 Entity (employee_mng 테이블)
 *
 * User와 1:1 관계. empcode__c = dkretail__empcode__c 로 조인.
 */
@Entity
@Table(name = "employee_mng")
class EmployeeMng(

    @Id
    @Column(name = "empcode__c", length = 40)
    val empcode: String,

    @Column(name = "emp_pwd", length = 200)
    var password: String? = null,

    @Column(name = "pwd_yn")
    var passwordChangeRequired: Boolean? = true,

    @Column(name = "emp_uuid", length = 200)
    var deviceUuid: String? = null,

    @Column(name = "emp_token", length = 200)
    var fcmToken: String? = null,

    @Column(name = "gps_yn")
    val gpsYn: Boolean? = null,

    @Column(name = "gps_yn_date")
    val gpsYnDate: LocalDateTime? = null,

    @Column(name = "inst_date")
    val instDate: LocalDateTime? = null,

    @Column(name = "upd_date")
    var updDate: LocalDateTime? = null,

    @Column(name = "last_agreement_number", length = 80)
    var lastAgreementNumber: String? = null
)
