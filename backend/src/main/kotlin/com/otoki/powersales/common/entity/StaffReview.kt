package com.otoki.powersales.common.entity

import com.otoki.powersales.common.entity.converter.WorkingCategory1Converter
import com.otoki.powersales.common.entity.converter.WorkingCategory2Converter
import com.otoki.powersales.common.entity.converter.WorkingCategory3Converter
import com.otoki.powersales.common.enums.WorkingCategory1
import com.otoki.powersales.common.enums.WorkingCategory2
import com.otoki.powersales.common.enums.WorkingCategory3
import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HCTable
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.employee.entity.Employee
import jakarta.persistence.*
import java.time.LocalDate

/**
 * 직원 평가 Entity
 * Salesforce StaffReview__c (사원평가) — Spec #711 SF Object 정합 (Group A + Reference R-2).
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
    @Column(name = "employee_code", length = 1300)
    val employeeCode: String? = null,

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

    @SFField("DKRetail_WorkingCategory1__c")
    @HCColumn("dkretail_workingcategory1__c")
    @Convert(converter = WorkingCategory1Converter::class)
    @Column(name = "working_category1", length = 255)
    val workingCategory1: WorkingCategory1? = null,

    @SFField("DKRetail_WorkingCategory2__c")
    @HCColumn("dkretail_workingcategory2__c")
    @Convert(converter = WorkingCategory2Converter::class)
    @Column(name = "working_category2", length = 255)
    val workingCategory2: WorkingCategory2? = null,

    @SFField("DKRetail_WorkingCategory3__c")
    @HCColumn("dkretail_workingcategory3__c")
    @Convert(converter = WorkingCategory3Converter::class)
    @Column(name = "working_category3", length = 255)
    val workingCategory3: WorkingCategory3? = null,

    @SFField("JobCode__c")
    @HCColumn("jobcode__c")
    @Column(name = "job_code", length = 20)
    val jobCode: String? = null,

    @SFField("FirstDayofMonth__c")
    @HCColumn("firstdayofmonth__c")
    @Column(name = "first_day_of_month")
    val firstDayOfMonth: LocalDate? = null,

    // -- Spec #711: Group A — IsDeleted --
    @SFField("IsDeleted")
    @HCColumn("isdeleted")
    @Column(name = "is_deleted")
    val isDeleted: Boolean? = null,

    // -- Spec #711: Group A — CreatedById / LastModifiedById (R-2 패턴) --
    // *_sfid: SF User Id buffer (Heroku Connect / SalesforceMigrationTool 이 채움).
    // *_by: SalesforceMigrationTool 이 SF User → Employee 매핑으로 채우는 FK.

    @SFField("CreatedById")
    @HCColumn("createdbyid")
    @Column(name = "created_by_sfid", length = 18)
    var createdBySfid: String? = null,

    @SFField("LastModifiedById")
    @HCColumn("lastmodifiedbyid")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

    // -- Relations --

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", insertable = false, updatable = false)
    val employee: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: Employee? = null,

    // -- Spec #735: BranchReview FK (R-2 후처리) --
    // branch_review_sfid 는 #711 에서 SF buffer 로 추가됨. 본 FK 는 그 sfid → branch_review_id 매핑.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_review_id")
    var branchReview: BranchReview? = null,

    // -- Spec #747 카테고리 A — D 분류 누락 (도메인 핵심) --
    @SFField("EmployeeType__c")
    @HCColumn("employeetype__c")
    @Column(name = "employee_type", length = 1300)
    var employeeType: String? = null,

    @SFField("EntryDate__c")
    @HCColumn("entrydate__c")
    @Column(name = "entry_date")
    var entryDate: LocalDate? = null,

    @SFField("Jikwee__c")
    @HCColumn("jikwee__c")
    @Column(name = "jikwee", length = 1300)
    var jikwee: String? = null,

    ) : BaseEntity()
