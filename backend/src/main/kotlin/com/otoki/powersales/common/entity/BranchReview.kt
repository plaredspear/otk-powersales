package com.otoki.powersales.common.entity

import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HCTable
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.employee.entity.Employee
import jakarta.persistence.*
import java.time.LocalDate

/**
 * 지점평가 Entity
 * Salesforce BranchReview__c (지점평가) — Spec #735 SF Object 정합 (Group A + Reference R-2 + Custom 42).
 *
 * 판촉/레이디 두 평가 부문 대칭 구조 — 평가 인원 / 합계 / 평균 각 9개씩.
 */
@Entity
@Table(name = "branch_review")
@SFObject("BranchReview__c")
@HCTable("branchreview__c")
class BranchReview(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "branch_review_id")
    val id: Long = 0,

    @HCColumn("sfid")
    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @HCColumn("name")
    @Column(name = "name", length = 80)
    val name: String? = null,

    // -- 식별 / 시점 --

    @SFField("BranchName__c")
    @HCColumn("branchname__c")
    @Column(name = "branch_name", length = 100)
    val branchName: String? = null,

    @SFField("CostCenterCode__c")
    @HCColumn("costcentercode__c")
    @Column(name = "cost_center_code", length = 100)
    val costCenterCode: String? = null,

    @SFField("FirstDayofMonth__c")
    @HCColumn("firstdayofmonth__c")
    @Column(name = "first_day_of_month")
    val firstDayOfMonth: LocalDate? = null,

    @SFField("Confirmed__c")
    @HCColumn("confirmed__c")
    @Column(name = "confirmed")
    val confirmed: Boolean? = null,

    // -- 판촉 부문 --

    @SFField("EmployeeEvaluationNumber__c")
    @HCColumn("employeeevaluationnumber__c")
    @Column(name = "employee_evaluation_number")
    val employeeEvaluationNumber: Double? = null,

    @SFField("SumAttendance__c")
    @HCColumn("sumattendance__c")
    @Column(name = "sum_attendance")
    val sumAttendance: Double? = null,

    @SFField("SumBusinessPartnerTies__c")
    @HCColumn("sumbusinesspartnerties__c")
    @Column(name = "sum_business_partner_ties")
    val sumBusinessPartnerTies: Double? = null,

    @SFField("SumClothesSatellite__c")
    @HCColumn("sumclothessatellite__c")
    @Column(name = "sum_clothes_satellite")
    val sumClothesSatellite: Double? = null,

    @SFField("SumDisplayManageEventGoals__c")
    @HCColumn("sumdisplaymanageeventgoals__c")
    @Column(name = "sum_display_manage_event_goals")
    val sumDisplayManageEventGoals: Double? = null,

    @SFField("SumEducationalEvaluation__c")
    @HCColumn("sumeducationalevaluation__c")
    @Column(name = "sum_educational_evaluation")
    val sumEducationalEvaluation: Double? = null,

    @SFField("SumInstructionsDefault__c")
    @HCColumn("suminstructionsdefault__c")
    @Column(name = "sum_instructions_default")
    val sumInstructionsDefault: Double? = null,

    @SFField("SumPriority_EventItemManage__c")
    @HCColumn("sumpriority_eventitemmanage__c")
    @Column(name = "sum_priority_event_item_manage")
    val sumPriorityEventItemManage: Double? = null,

    @SFField("SumProductManageCallment__c")
    @HCColumn("sumproductmanagecallment__c")
    @Column(name = "sum_product_manage_callment")
    val sumProductManageCallment: Double? = null,

    @SFField("AttendanceAverage__c")
    @HCColumn("attendanceaverage__c")
    @Column(name = "attendance_average")
    val attendanceAverage: Double? = null,

    @SFField("BusinessPartnerTiesAverage__c")
    @HCColumn("businesspartnertiesaverage__c")
    @Column(name = "business_partner_ties_average")
    val businessPartnerTiesAverage: Double? = null,

    @SFField("ClothesSatelliteAverage__c")
    @HCColumn("clothessatelliteaverage__c")
    @Column(name = "clothes_satellite_average")
    val clothesSatelliteAverage: Double? = null,

    @SFField("DisplayManageEventGoalsAverage__c")
    @HCColumn("displaymanageeventgoalsaverage__c")
    @Column(name = "display_manage_event_goals_average")
    val displayManageEventGoalsAverage: Double? = null,

    @SFField("EducationalEvaluationAverage__c")
    @HCColumn("educationalevaluationaverage__c")
    @Column(name = "educational_evaluation_average")
    val educationalEvaluationAverage: Double? = null,

    @SFField("InstructionsDefaultAverage__c")
    @HCColumn("instructionsdefaultaverage__c")
    @Column(name = "instructions_default_average")
    val instructionsDefaultAverage: Double? = null,

    @SFField("Priority_EventItemManageAverage__c")
    @HCColumn("priority_eventitemmanageaverage__c")
    @Column(name = "priority_event_item_manage_average")
    val priorityEventItemManageAverage: Double? = null,

    @SFField("ProductManageCallmentAverage__c")
    @HCColumn("productmanagecallmentaverage__c")
    @Column(name = "product_manage_callment_average")
    val productManageCallmentAverage: Double? = null,

    @SFField("SumTotalScoreAverage__c")
    @HCColumn("sumtotalscoreaverage__c")
    @Column(name = "sum_total_score_average")
    val sumTotalScoreAverage: Double? = null,

    @SFField("SumTotalScore__c")
    @HCColumn("sumtotalscore__c")
    @Column(name = "sum_total_score")
    val sumTotalScore: Double? = null,

    // -- 레이디 부문 --

    @SFField("EmployeeEvaluationNumber_lady__c")
    @HCColumn("employeeevaluationnumber_lady__c")
    @Column(name = "employee_evaluation_number_lady")
    val employeeEvaluationNumberLady: Double? = null,

    @SFField("SumAttendance_lady__c")
    @HCColumn("sumattendance_lady__c")
    @Column(name = "sum_attendance_lady")
    val sumAttendanceLady: Double? = null,

    @SFField("SumBusinessPartnerTies_lady__c")
    @HCColumn("sumbusinesspartnerties_lady__c")
    @Column(name = "sum_business_partner_ties_lady")
    val sumBusinessPartnerTiesLady: Double? = null,

    @SFField("SumClothesSatellite_lady__c")
    @HCColumn("sumclothessatellite_lady__c")
    @Column(name = "sum_clothes_satellite_lady")
    val sumClothesSatelliteLady: Double? = null,

    @SFField("SumDisplayManageEventGoals_lady__c")
    @HCColumn("sumdisplaymanageeventgoals_lady__c")
    @Column(name = "sum_display_manage_event_goals_lady")
    val sumDisplayManageEventGoalsLady: Double? = null,

    @SFField("SumEducationalEvaluation_lady__c")
    @HCColumn("sumeducationalevaluation_lady__c")
    @Column(name = "sum_educational_evaluation_lady")
    val sumEducationalEvaluationLady: Double? = null,

    @SFField("SumInstructionsDefault_lady__c")
    @HCColumn("suminstructionsdefault_lady__c")
    @Column(name = "sum_instructions_default_lady")
    val sumInstructionsDefaultLady: Double? = null,

    @SFField("SumPriority_EventItemManage_lady__c")
    @HCColumn("sumpriority_eventitemmanage_lady__c")
    @Column(name = "sum_priority_event_item_manage_lady")
    val sumPriorityEventItemManageLady: Double? = null,

    @SFField("SumProductManageCallment_lady__c")
    @HCColumn("sumproductmanagecallment_lady__c")
    @Column(name = "sum_product_manage_callment_lady")
    val sumProductManageCallmentLady: Double? = null,

    @SFField("AttendanceAverage_lady__c")
    @HCColumn("attendanceaverage_lady__c")
    @Column(name = "attendance_average_lady")
    val attendanceAverageLady: Double? = null,

    @SFField("BusinessPartnerTiesAverage_lady__c")
    @HCColumn("businesspartnertiesaverage_lady__c")
    @Column(name = "business_partner_ties_average_lady")
    val businessPartnerTiesAverageLady: Double? = null,

    @SFField("ClothesSatelliteAverage_lady__c")
    @HCColumn("clothessatelliteaverage_lady__c")
    @Column(name = "clothes_satellite_average_lady")
    val clothesSatelliteAverageLady: Double? = null,

    @SFField("DisplayManageEventGoalsAverage_lady__c")
    @HCColumn("displaymanageeventgoalsaverage_lady__c")
    @Column(name = "display_manage_event_goals_average_lady")
    val displayManageEventGoalsAverageLady: Double? = null,

    @SFField("EducationalEvaluationAverage_lady__c")
    @HCColumn("educationalevaluationaverage_lady__c")
    @Column(name = "educational_evaluation_average_lady")
    val educationalEvaluationAverageLady: Double? = null,

    @SFField("InstructionsDefaultAverage_lady__c")
    @HCColumn("instructionsdefaultaverage_lady__c")
    @Column(name = "instructions_default_average_lady")
    val instructionsDefaultAverageLady: Double? = null,

    @SFField("Priority_EventItemManageAverage_lady__c")
    @HCColumn("priority_eventitemmanageaverage_lady__c")
    @Column(name = "priority_event_item_manage_average_lady")
    val priorityEventItemManageAverageLady: Double? = null,

    @SFField("ProductManageCallmentAverage_lady__c")
    @HCColumn("productmanagecallmentaverage_lady__c")
    @Column(name = "product_manage_callment_average_lady")
    val productManageCallmentAverageLady: Double? = null,

    @SFField("SumTotalScoreAverage_lady__c")
    @HCColumn("sumtotalscoreaverage_lady__c")
    @Column(name = "sum_total_score_average_lady")
    val sumTotalScoreAverageLady: Double? = null,

    @SFField("SumTotalScore_lady__c")
    @HCColumn("sumtotalscore_lady__c")
    @Column(name = "sum_total_score_lady")
    val sumTotalScoreLady: Double? = null,

    // -- Group A — IsDeleted --

    @SFField("IsDeleted")
    @HCColumn("isdeleted")
    @Column(name = "is_deleted")
    val isDeleted: Boolean? = null,

    // -- Group A — OwnerId / CreatedById / LastModifiedById (R-2 패턴) --
    // *_sfid: SF User Id buffer (SalesforceMigrationTool 이 채움).
    // *_id: SF User → Employee 매핑으로 채우는 FK.

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

    // -- Relations --

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    var owner: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: Employee? = null,

) : BaseEntity()
