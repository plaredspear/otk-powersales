package com.otoki.powersales.schedule.entity

import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HCTable
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.employee.entity.Employee
import jakarta.persistence.*

@Entity
@SFObject("AttendInfo__c")
@HCTable("attendinfo__c")
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
    @Column(name = "attend_info_id")
    val id: Long = 0,

    @HCColumn("sfid")
    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @SFField("EmployeeCode__c")
    @HCColumn("employeecode__c")
    @Column(name = "employee_code", nullable = false, length = 100)
    val employeeCode: String,

    @SFField("StartDate__c")
    @HCColumn("startdate__c")
    @Column(name = "start_date", nullable = false, length = 100)
    val startDate: String,

    @SFField("EndDate__c")
    @HCColumn("enddate__c")
    @Column(name = "end_date", length = 100)
    val endDate: String? = null,

    @SFField("AttendType__c")
    @HCColumn("attendtype__c")
    @Column(name = "attend_type", length = 100)
    val attendType: String? = null,

    @SFField("Status__c")
    @HCColumn("status__c")
    @Column(name = "status", length = 100)
    val status: String? = null,

    @SFField("OwnerId")
    @HCColumn("ownerid")
    @Column(name = "owner_sfid", length = 18)
    var ownerSfid: String? = null,

    @SFField("CreatedById")
    @HCColumn("createdbyid")
    @Column(name = "created_by_sfid", length = 18)
    var createdBySfid: String? = null,

    @SFField("LastModifiedById")
    @HCColumn("lastmodifiedbyid")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    var owner: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: Employee? = null
) : BaseEntity()
