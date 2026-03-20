package com.otoki.internal.schedule.entity

import com.otoki.internal.common.entity.BaseEntity
import com.otoki.internal.common.salesforce.HCColumn
import com.otoki.internal.common.salesforce.HCTable
import com.otoki.internal.common.salesforce.SFField
import com.otoki.internal.common.salesforce.SFObject
import jakarta.persistence.*
import java.time.LocalDate

/**
 * 거래처 일정 Entity (진열마스터 확정 스케줄)
 * V1 스키마: displayworkschedulemaster__c
 */
@Entity
@Table(name = "display_work_schedule")
@SFObject("DKRetail__DisplayWorkScheduleMaster__c")
@HCTable("displayworkschedulemaster__c")
class DisplayWorkSchedule(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "display_work_schedule_id")
    val id: Long = 0,

    @HCColumn("sfid")
    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @SFField("Name")
    @HCColumn("name")
    @Column(name = "name", length = 80)
    val name: String? = null,

    @SFField("Account__c")
    @HCColumn("account__c")
    @Column(name = "account_id")
    val accountId: Int? = null,

    @SFField("FullName__c")
    @HCColumn("fullname__c")
    @Column(name = "employee_id")
    val employeeId: Long? = null,

    @SFField("StartDate__c")
    @HCColumn("startdate__c")
    @Column(name = "start_date")
    val startDate: LocalDate? = null,

    @SFField("EndDate__c")
    @HCColumn("enddate__c")
    @Column(name = "end_date")
    val endDate: LocalDate? = null,

    @SFField("Confirmed__c")
    @HCColumn("confirmed__c")
    @Column(name = "confirmed")
    var confirmed: Boolean? = null,

    @SFField("TypeOfWork1__c")
    @HCColumn("typeofwork1__c")
    @Column(name = "type_of_work1", length = 255)
    val typeOfWork1: String? = null,

    @SFField("TypeOfWork3__c")
    @HCColumn("typeofwork3__c")
    @Column(name = "type_of_work3", length = 255)
    val typeOfWork3: String? = null,

    @SFField("TypeOfWork5__c")
    @HCColumn("typeofwork5__c")
    @Column(name = "type_of_work5", length = 255)
    val typeOfWork5: String? = null,

    @SFField("OwnerId")
    @HCColumn("ownerid")
    @Column(name = "owner_id")
    val ownerId: Long? = null,

    @SFField("CostCenterCode__c")
    @HCColumn("costcentercode__c")
    @Column(name = "cost_center_code", length = 20)
    val costCenterCode: String? = null,

    @SFField("LastMonthRevenue__c")
    @HCColumn("lastmonthrevenue__c")
    @Column(name = "last_month_revenue")
    val lastMonthRevenue: Long? = null,

    @Column(name = "is_deleted")
    val isDeleted: Boolean? = null,

) : BaseEntity()
