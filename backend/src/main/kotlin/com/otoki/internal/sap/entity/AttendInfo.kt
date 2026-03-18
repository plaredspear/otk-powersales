package com.otoki.internal.sap.entity

import com.otoki.internal.common.salesforce.SFField
import com.otoki.internal.common.salesforce.SFObject
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@SFObject("AttendInfo__c")
@Table(
    name = "attend_info",
    indexes = [
        Index(name = "idx_attend_info_employee", columnList = "employee_code"),
        Index(name = "idx_attend_info_start_date", columnList = "start_date")
    ]
)
class AttendInfo(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @SFField("EmployeeCode__c")
    @Column(name = "employee_code", nullable = false, length = 20)
    val employeeCode: String,

    @SFField("StartDate__c")
    @Column(name = "start_date", nullable = false, length = 8)
    val startDate: String,

    @SFField("EndDate__c")
    @Column(name = "end_date", length = 8)
    val endDate: String? = null,

    @SFField("AttendType__c")
    @Column(name = "attend_type", length = 50)
    val attendType: String? = null,

    @SFField("Status__c")
    @Column(name = "status", length = 20)
    val status: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
