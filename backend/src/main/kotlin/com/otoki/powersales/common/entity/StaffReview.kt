package com.otoki.powersales.common.entity

import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HCTable
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.employee.entity.Employee
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
    @Column(name = "staff_review_id")
    val id: Int = 0,

    @HCColumn("sfid")
    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @SFField("Name")
    @HCColumn("name")
    @Column(name = "name", length = 80)
    val name: String? = null,

    @Column(name = "employee_id")
    val employeeId: Long? = null,

    @SFField("DKRetail_EmployeeId__c")
    @HCColumn("dkretail_employeeid__c")
    @Column(name = "employee_sfid", length = 18)
    val employeeSfid: String? = null,

    @SFField("EmployeeName__c")
    @HCColumn("employeename__c")
    @Column(name = "employee_name", length = 1300)
    val employeeName: String? = null,

    @SFField("EmployeeNumber__c")
    @HCColumn("employeenumber__c")
    @Column(name = "employee_number", length = 1300)
    val employeeNumber: String? = null,

    @SFField("Branch__c")
    @HCColumn("branch__c")
    @Column(name = "branch", length = 1300)
    val branch: String? = null,

    @SFField("BranchReviews__c")
    @HCColumn("branchreviews__c")
    @Column(name = "branch_review_sfid", length = 18)
    val branchReviewSfid: String? = null,

    @SFField("CostCenterCode__c")
    @HCColumn("costcentercode__c")
    @Column(name = "cost_center_code", length = 1300)
    val costCenterCode: String? = null,

    @SFField("EmployeeTotalScore__c")
    @HCColumn("employeetotalscore__c")
    @Column(name = "employee_total_score")
    val employeeTotalScore: Double? = null,

    @SFField("Attendance__c")
    @HCColumn("attendance__c")
    @Column(name = "attendance_score")
    val attendanceScore: Double? = null,

    @SFField("InstructionsDefault__c")
    @HCColumn("instructionsdefault__c")
    @Column(name = "instruction_disobedience_score")
    val instructionDisobedienceScore: Double? = null,

    @SFField("Priority_EventItemManage__c")
    @HCColumn("priority_eventitemmanage__c")
    @Column(name = "priority_item_event_score")
    val priorityItemEventScore: Double? = null,

    @SFField("DisplayManageEventGoals__c")
    @HCColumn("displaymanageeventgoals__c")
    @Column(name = "display_event_goal_score")
    val displayEventGoalScore: Double? = null,

    @SFField("BusinessPartnerTies__c")
    @HCColumn("businesspartnerties__c")
    @Column(name = "account_partnership_score")
    val accountPartnershipScore: Double? = null,

    @SFField("ClothesSatellite__c")
    @HCColumn("clothessatellite__c")
    @Column(name = "clothes_hygiene_score")
    val clothesHygieneScore: Double? = null,

    @SFField("ProductManageCallment__c")
    @HCColumn("productmanagecallment__c")
    @Column(name = "product_manage_callment_score")
    val productManageCallmentScore: Double? = null,

    @SFField("EducationalEvaluation__c")
    @HCColumn("educationalevaluation__c")
    @Column(name = "education_evaluation_score")
    val educationEvaluationScore: Double? = null,

    @HCColumn("isdeleted")
    @Column(name = "is_deleted")
    val isDeleted: Boolean? = null,

    @HCColumn("createddate")
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @HCColumn("systemmodstamp")
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    // -- Relations --
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", insertable = false, updatable = false)
    val employee: Employee? = null
) : AuditedEntity()