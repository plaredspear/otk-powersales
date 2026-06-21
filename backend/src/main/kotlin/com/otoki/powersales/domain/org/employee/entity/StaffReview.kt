package com.otoki.powersales.domain.org.employee.entity

import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.platform.common.entity.converter.WorkingCategory1Converter
import com.otoki.powersales.platform.common.entity.converter.WorkingCategory2Converter
import com.otoki.powersales.platform.common.entity.converter.WorkingCategory3Converter
import com.otoki.powersales.platform.common.enums.WorkingCategory1
import com.otoki.powersales.platform.common.enums.WorkingCategory2
import com.otoki.powersales.platform.common.enums.WorkingCategory3
import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import java.time.LocalDate
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

/**
 * 사원평가 Entity
 * V202606161400 스키마: staff_review
 *
 * SF StaffReview__c (사원평가) 마이그레이션 대상.
 *
 * 복원 배경: V164 (2026-05-19) 에서 미사용 도메인으로 DROP 되었으나 SF 데이터 적재 대상으로 재도입.
 * BranchReview / HqReview 는 미복원 (사용자 결정) — BranchReviews__c 는 branch_review_sfid buffer 로만 보존.
 *
 * SF 정합 (prod describe):
 *  - OwnerId 필드 없음 → owner_* 미보유.
 *  - CreatedById / LastModifiedById referenceTo = [User] → audit FK 는 User.
 *  - DKRetail_EmployeeId__c referenceTo = [DKRetail__Employee__c] → employee FK.
 *  - Formula 8개 중 EmployeeName / EmployeeNumber / Branch / CostCenterCode / EmployeeTotalScore /
 *    EmployeeType / EntryDate / Jikwee 는 레거시에서 비수식 캐시 컬럼으로 운영되어 DB 컬럼 유지.
 */
@DomainName("사원평가")
@Entity
@Table(name = "staff_review")
@SFObject("StaffReview__c")
class StaffReview(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("사원평가ID")
    @Column(name = "staff_review_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @FieldName("이름")
    @Column(name = "name", length = 80)
    val name: String? = null,

    // -- 사원 식별 / 캐시 --

    @FieldName("사원ID")
    @Column(name = "employee_id", insertable = false, updatable = false)
    val employeeId: Long? = null,

    @SFField("DKRetail_EmployeeId__c")
    @Column(name = "employee_sfid", length = 18)
    var employeeSfid: String? = null,

    @SFField("EmployeeName__c")
    @FieldName("성명")
    @Column(name = "employee_name", length = 1300)
    var employeeName: String? = null,

    @SFField("EmployeeNumber__c")
    @FieldName("사번")
    @Column(name = "employee_code", length = 1300)
    var employeeCode: String? = null,

    @SFField("Branch__c")
    @FieldName("지점")
    @Column(name = "branch", length = 1300)
    var branch: String? = null,

    @SFField("BranchReviews__c")
    @Column(name = "branch_review_sfid", length = 18)
    var branchReviewSfid: String? = null,

    @SFField("CostCenterCode__c")
    @FieldName("지점코드")
    @Column(name = "cost_center_code", length = 1300)
    var costCenterCode: String? = null,

    @SFField("EmployeeTotalScore__c")
    @FieldName("사원합계점수")
    @Column(name = "employee_total_score")
    var employeeTotalScore: Double? = null,

    // -- 점수 항목 --

    @SFField("Attendance__c")
    @FieldName("근태(3점)")
    @Column(name = "attendance_score")
    var attendanceScore: Double? = null,

    @SFField("InstructionsDefault__c")
    @FieldName("지시불이행(3점)")
    @Column(name = "instruction_disobedience_score")
    var instructionDisobedienceScore: Double? = null,

    @SFField("Priority_EventItemManage__c")
    @FieldName("중점품목관리&amp;행사품목연출(5점)")
    @Column(name = "priority_item_event_score")
    var priorityItemEventScore: Double? = null,

    @SFField("DisplayManageEventGoals__c")
    @FieldName("진열관리&amp;행사목표달성(5점)")
    @Column(name = "display_event_goal_score")
    var displayEventGoalScore: Double? = null,

    @SFField("BusinessPartnerTies__c")
    @FieldName("거래처유대관계(2점)")
    @Column(name = "account_partnership_score")
    var accountPartnershipScore: Double? = null,

    @SFField("ClothesSatellite__c")
    @FieldName("복장및위생(2점)")
    @Column(name = "clothes_hygiene_score")
    var clothesHygieneScore: Double? = null,

    @SFField("ProductManageCallment__c")
    @FieldName("제품관리&amp;콜멘트(5점)")
    @Column(name = "product_manage_callment_score")
    var productManageCallmentScore: Double? = null,

    @SFField("EducationalEvaluation__c")
    @FieldName("교육평가(5점)")
    @Column(name = "education_evaluation_score")
    var educationEvaluationScore: Double? = null,

    // -- 근무유형 / 직무 / 시점 --

    @SFField("DKRetail_WorkingCategory1__c")
    @Convert(converter = WorkingCategory1Converter::class)
    @FieldName("근무유형1")
    @Column(name = "working_category1", length = 255)
    var workingCategory1: WorkingCategory1? = null,

    @SFField("DKRetail_WorkingCategory2__c")
    @Convert(converter = WorkingCategory2Converter::class)
    @FieldName("근무유형2")
    @Column(name = "working_category2", length = 255)
    var workingCategory2: WorkingCategory2? = null,

    @SFField("DKRetail_WorkingCategory3__c")
    @Convert(converter = WorkingCategory3Converter::class)
    @FieldName("근무유형3")
    @Column(name = "working_category3", length = 255)
    var workingCategory3: WorkingCategory3? = null,

    @SFField("JobCode__c")
    @FieldName("직무코드")
    @Column(name = "job_code", length = 20)
    var jobCode: String? = null,

    @SFField("FirstDayofMonth__c")
    @FieldName("월초기준일")
    @Column(name = "first_day_of_month")
    var firstDayOfMonth: LocalDate? = null,

    // -- 구분 / 입사일 / 직위 --

    @SFField("EmployeeType__c")
    @FieldName("구분")
    @Column(name = "employee_type", length = 1300)
    var employeeType: String? = null,

    @SFField("EntryDate__c")
    @FieldName("입사일")
    @Column(name = "entry_date")
    var entryDate: LocalDate? = null,

    @SFField("Jikwee__c")
    @FieldName("직위")
    @Column(name = "jikwee", length = 1300)
    var jikwee: String? = null,

    // -- Group A --

    @SFField("IsDeleted")
    @FieldName("삭제여부")
    @Column(name = "is_deleted")
    val isDeleted: Boolean? = null,

    // -- audit (CreatedById / LastModifiedById referenceTo = [User]) --
    // *_sfid: sync buffer (SF User Id). *_id: SF User → User 매핑 FK.

    @SFField("CreatedById")
    @Column(name = "created_by_sfid", length = 18)
    var createdBySfid: String? = null,

    @SFField("LastModifiedById")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

    // -- Relations --

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    var employee: Employee? = null,

    @CreatedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: User? = null,

    @LastModifiedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: User? = null,

) : BaseEntity()
