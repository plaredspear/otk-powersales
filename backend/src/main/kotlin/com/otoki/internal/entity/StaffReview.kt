package com.otoki.internal.entity

import com.otoki.internal.common.entity.BaseEntity
import com.otoki.internal.common.salesforce.HCColumn
import com.otoki.internal.common.salesforce.HCTable
import com.otoki.internal.common.salesforce.SFField
import com.otoki.internal.common.salesforce.SFObject
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime

/**
 * 직원 평가 Entity
 * V1 스키마: staffreview__c (Heroku Connect 동기화)
 */
@Entity
@Table(name = "staff_review")
@SFObject("StaffReview__c")
@HCTable("staffreview__c")
class StaffReview(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @HCColumn("id")
    val id: Int = 0,

    @HCColumn("sfid")
    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @SFField("Name")
    @HCColumn("name")
    @Column(name = "name", length = 80)
    val name: String? = null,

    @SFField("DKRetail_EmployeeId__c")
    @HCColumn("dkretail_employeeid__c")
    @Column(name = "employee_id")
    val employeeId: Long? = null,

    @SFField("EmployeeName__c")
    @HCColumn("employeename__c")
    @Column(name = "employeename__c", length = 1300)
    val employeeName: String? = null,

    @SFField("EmployeeNumber__c")
    @HCColumn("employeenumber__c")
    @Column(name = "employeenumber__c", length = 1300)
    val employeeNumber: String? = null,

    @SFField("Branch__c")
    @HCColumn("branch__c")
    @Column(name = "branch__c", length = 1300)
    val branch: String? = null,

    @SFField("BranchReviews__c")
    @HCColumn("branchreviews__c")
    @Column(name = "branchreviews__c", length = 18)
    val branchReviews: String? = null,

    @SFField("CostCenterCode__c")
    @HCColumn("costcentercode__c")
    @Column(name = "costcentercode__c", length = 1300)
    val costCenterCode: String? = null,

    @SFField("EmployeeTotalScore__c")
    @HCColumn("employeetotalscore__c")
    @Column(name = "employeetotalscore__c")
    val employeeTotalScore: Double? = null,

    @SFField("Attendance__c")
    @HCColumn("attendance__c")
    @Column(name = "attendance__c")
    val attendance: Double? = null,

    @SFField("InstructionsDefault__c")
    @HCColumn("instructionsdefault__c")
    @Column(name = "instructionsdefault__c")
    val instructionsDefault: Double? = null,

    @SFField("Priority_EventItemManage__c")
    @HCColumn("priority_eventitemmanage__c")
    @Column(name = "priority_eventitemmanage__c")
    val priorityEventItemManage: Double? = null,

    @SFField("DisplayManageEventGoals__c")
    @HCColumn("displaymanageeventgoals__c")
    @Column(name = "displaymanageeventgoals__c")
    val displayManageEventGoals: Double? = null,

    @SFField("BusinessPartnerTies__c")
    @HCColumn("businesspartnerties__c")
    @Column(name = "businesspartnerties__c")
    val businessPartnerTies: Double? = null,

    @SFField("ClothesSatellite__c")
    @HCColumn("clothessatellite__c")
    @Column(name = "clothessatellite__c")
    val clothesSatellite: Double? = null,

    @SFField("ProductManageCallment__c")
    @HCColumn("productmanagecallment__c")
    @Column(name = "productmanagecallment__c")
    val productManageCallment: Double? = null,

    @SFField("EducationalEvaluation__c")
    @HCColumn("educationalevaluation__c")
    @Column(name = "educationalevaluation__c")
    val educationalEvaluation: Double? = null,

    @HCColumn("isdeleted")
    @Column(name = "isdeleted")
    val isDeleted: Boolean? = null,

    @HCColumn("createddate")
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    override var createdAt: LocalDateTime = LocalDateTime.now(),

    @HCColumn("systemmodstamp")
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    override var updatedAt: LocalDateTime = LocalDateTime.now()
) : BaseEntity()
