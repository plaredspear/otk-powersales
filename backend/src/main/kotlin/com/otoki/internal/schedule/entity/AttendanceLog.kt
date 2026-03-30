package com.otoki.internal.schedule.entity

import com.otoki.internal.common.entity.BaseEntity
import com.otoki.internal.common.salesforce.SFField
import com.otoki.internal.common.salesforce.SFObject
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 출근현황 Entity
 * Salesforce: DKRetail__CommuteLog__c (Heroku DB 미동기화 — SF 전용 오브젝트)
 */
@Entity
@Table(name = "attendance_log")
@SFObject("DKRetail__CommuteLog__c")
class AttendanceLog(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_log_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @Column(name = "name", length = 80)
    val name: String? = null,

    @SFField("DKRetail__EmployeeId__c")
    @Column(name = "employee_sfid", length = 18)
    val employeeSfid: String? = null,

    @Column(name = "employee_id")
    val employeeId: Long? = null,

    @SFField("DKRetail__CommuteDate__c")
    @Column(name = "attendance_date")
    val attendanceDate: LocalDateTime? = null,

    @SFField("DKRetail__AccId__c")
    @Column(name = "account_sfid", length = 18)
    val accountSfid: String? = null,

    @Column(name = "account_id")
    val accountId: Int? = null,

    @SFField("DKRetail__SecondWorkType__c")
    @Column(name = "second_work_type", length = 255)
    val secondWorkType: String? = null,

    @SFField("DKRetail__Reason__c")
    @Column(name = "reason", length = 255)
    val reason: String? = null,

) : BaseEntity()
