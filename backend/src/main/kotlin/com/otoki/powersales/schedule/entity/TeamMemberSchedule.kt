package com.otoki.powersales.schedule.entity

import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HCTable
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.leave.entity.AlternativeHoliday
import com.otoki.powersales.promotion.entity.PromotionEmployee
import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.employee.entity.Employee
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 일정 Entity
 * V1 스키마: dkretail__teammemberschedule__c (팀원 스케줄 + 안전점검 장비 + 업무보고)
 */
@Entity
@Table(name = "team_member_schedule")
@SFObject("DKRetail__TeamMemberSchedule__c")
@HCTable("dkretail__teammemberschedule__c")
class TeamMemberSchedule(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "team_member_schedule_id")
    val id: Long = 0,

    @HCColumn("sfid")
    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @SFField("Name")
    @HCColumn("name")
    @Column(name = "name", length = 80)
    val name: String? = null,

    @SFField("DKRetail__EmployeeId__c")
    @HCColumn("dkretail__employeeid__c")
    @Column(name = "employee_sfid", length = 18)
    val employeeSfid: String? = null,

    @SFField("DKRetail__WorkingDate__c")
    @HCColumn("dkretail__workingdate__c")
    @Column(name = "working_date")
    var workingDate: LocalDate? = null,

    @SFField("DKRetail__WorkingType__c")
    @HCColumn("dkretail__workingtype__c")
    @Column(name = "working_type", length = 255)
    var workingType: String? = null,

    @SFField("DKRetail__WorkingCategory1__c")
    @HCColumn("dkretail__workingcategory1__c")
    @Column(name = "working_category1", length = 255)
    var workingCategory1: String? = null,

    @SFField("DKRetail__WorkingCategory2__c")
    @HCColumn("dkretail__workingcategory2__c")
    @Column(name = "working_category2", length = 255)
    val workingCategory2: String? = null,

    @SFField("DKRetail__WorkingCategory3__c")
    @HCColumn("dkretail__workingcategory3__c")
    @Column(name = "working_category3", length = 255)
    var workingCategory3: String? = null,

    @SFField("WorkingCategory4__c")
    @HCColumn("workingcategory4__c")
    @Column(name = "working_category4", length = 255)
    var workingCategory4: String? = null,

    @SFField("AccountId__c")
    @HCColumn("accountid__c")
    @Column(name = "account_sfid", length = 18)
    val accountSfid: String? = null,

    @SFField("teamleadersfid__c")
    @HCColumn("teamleadersfid__c")
    @Column(name = "team_leader_sfid", length = 18)
    val teamLeaderSfid: String? = null,

    @SFField("DKRetail__AltHolidayId__c")
    @HCColumn("dkretail__altholidayid__c")
    @Column(name = "alt_holiday_sfid", length = 18)
    val altHolidaySfid: String? = null,

    @SFField("DKRetail__CommuteLogId__c")
    @HCColumn("dkretail__commutelogid__c")
    @Column(name = "commute_log_id", length = 18)
    var commuteLogId: String? = null,

    @SFField("DKRetail__PromotionEmpId__c")
    @HCColumn("dkretail__promotionempid__c")
    @Column(name = "promotion_employee_sfid", length = 18)
    val promotionEmployeeSfid: String? = null,

    /**
     * 진열 마스터(`DKRetail__DisplayWorkScheduleMaster__c`) 연결 sfid (Spec #587 P1-B).
     * 진열 출근 시 마스터의 sfid 를 그대로 카피. 마이그레이션 후 display_work_schedule_id 가 채워진다.
     */
    @SFField("DisplayWorkScheduleMaster__c")
    @HCColumn("displayworkschedulemaster__c")
    @Column(name = "display_work_schedule_sfid", length = 18)
    val displayWorkScheduleSfid: String? = null,

    @SFField("CommuteReportDateTime__c")
    @HCColumn("commutereportdatetime__c")
    @Column(name = "commute_report_datetime")
    val commuteReportDatetime: LocalDateTime? = null,

    @SFField("ID__c")
    @HCColumn("id__c")
    @Column(name = "id_field", length = 30)
    val idField: String? = null,

    @SFField("TraversalFlag__c")
    @HCColumn("traversalflag__c")
    @Column(name = "traversal_flag", length = 255)
    val traversalFlag: String? = null,

    @SFField("isworkreport__c")
    @HCColumn("isworkreport__c")
    @Column(name = "is_work_report", length = 1300)
    val isWorkReport: String? = null,

    @SFField("Equipment1__c")
    @HCColumn("equipment1__c")
    @Column(name = "equipment1", length = 10)
    val equipment1: String? = null,

    @SFField("Equipment2__c")
    @HCColumn("equipment2__c")
    @Column(name = "equipment2", length = 10)
    val equipment2: String? = null,

    @SFField("Equipment3__c")
    @HCColumn("equipment3__c")
    @Column(name = "equipment3", length = 10)
    val equipment3: String? = null,

    @SFField("Equipment4__c")
    @HCColumn("equipment4__c")
    @Column(name = "equipment4", length = 10)
    val equipment4: String? = null,

    @SFField("Equipment5__c")
    @HCColumn("equipment5__c")
    @Column(name = "equipment5", length = 10)
    val equipment5: String? = null,

    @SFField("Equipment6__c")
    @HCColumn("equipment6__c")
    @Column(name = "equipment6", length = 10)
    val equipment6: String? = null,

    @SFField("Equipment7__c")
    @HCColumn("equipment7__c")
    @Column(name = "equipment7", length = 10)
    val equipment7: String? = null,

    @SFField("Equipment8__c")
    @HCColumn("equipment8__c")
    @Column(name = "equipment8", length = 10)
    val equipment8: String? = null,

    @SFField("Equipment9__c")
    @HCColumn("equipment9__c")
    @Column(name = "equipment9", length = 10)
    val equipment9: String? = null,

    @SFField("Equipment10__c")
    @HCColumn("equipment10__c")
    @Column(name = "equipment10", length = 10)
    val equipment10: String? = null,

    @SFField("Yes_ChkCnt__c")
    @HCColumn("yes_chkcnt__c")
    @Column(name = "yes_chk_cnt")
    val yesChkCnt: Double? = null,

    @SFField("No_ChkCnt__c")
    @HCColumn("no_chkcnt__c")
    @Column(name = "no_chk_cnt")
    val noChkCnt: Double? = null,

    @SFField("precaution_chk__c")
    @HCColumn("precaution_chk__c")
    @Column(name = "precaution_chk")
    val precautionChk: Double? = null,

    @SFField("precaution__c")
    @HCColumn("precaution__c")
    @Column(name = "precaution", length = 3000)
    val precaution: String? = null,

    @SFField("StartTime__c")
    @HCColumn("starttime__c")
    @Column(name = "start_time")
    val startTime: LocalDateTime? = null,

    @SFField("CompleteTime__c")
    @HCColumn("completetime__c")
    @Column(name = "complete_time")
    val completeTime: LocalDateTime? = null,

    @HCColumn("isdeleted")
    @Column(name = "is_deleted")
    val isDeleted: Boolean? = null,

    // -- Spec #609: SF 누락 컬럼 7개 신규 도입 (Q1 옵션 1) --

    @SFField("HRCode__c")
    @Column(name = "hr_code", length = 40)
    var hrCode: String? = null,

    @SFField("DKRetail__PromotionEmpIdExt__c")
    @Column(name = "promotion_emp_id_ext", length = 40)
    var promotionEmpIdExt: String? = null,

    @SFField("SecondWorkType__c")
    @Column(name = "second_work_type", length = 40)
    var secondWorkType: String? = null,

    @SFField("WorkingCategory5__c")
    @Column(name = "working_category5", length = 40)
    var workingCategory5: String? = null,

    @SFField("ref_accountName__c")
    @Column(name = "ref_account_name", length = 255)
    var refAccountName: String? = null,

    @SFField("MonthlyFemaleEmployeeIntegrationSchedule__c")
    @Column(name = "monthly_female_employee_integration_schedule_sfid", length = 18)
    var monthlyFemaleEmployeeIntegrationScheduleSfid: String? = null,

    @SFField("ProfessionalPromotionTeam__c")
    @Column(name = "professional_promotion_team", length = 100)
    var professionalPromotionTeam: String? = null,

    /**
     * 대리 등록자(조장) employee_id. 조장이 본인 팀원의 일정을 대리 등록할 때 audit trail 용도로 저장.
     * 본 스펙 외부 INSERT(스케줄러 자동 생성 등)는 NULL.
     */
    @Column(name = "proxy_registered_by")
    val proxyRegisteredBy: Long? = null,

    // -- Relations --

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    var employee: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    var account: Account? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_leader_id")
    val teamLeader: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alt_holiday_id")
    val altHoliday: AlternativeHoliday? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_employee_id")
    var promotionEmployee: PromotionEmployee? = null,

    /**
     * 진열 마스터 (Spec #587 P1-B). 진열 출근 케이스에서 채워진다.
     * FK 제약은 본 스펙 비범위 (spec.md §6.2 — 후속 스펙에서 추가 예정).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "display_work_schedule_id")
    var displayWorkSchedule: DisplayWorkSchedule? = null,

) : BaseEntity() {
    fun updateForPromotion(
        employee: Employee,
        account: Account,
        workingDate: LocalDate,
        workingType: String,
        workingCategory1: String,
        workingCategory3: String,
        workingCategory4: String?,
        promotionEmployee: PromotionEmployee
    ) {
        this.employee = employee
        this.account = account
        this.workingDate = workingDate
        this.workingType = workingType
        this.workingCategory1 = workingCategory1
        this.workingCategory3 = workingCategory3
        this.workingCategory4 = workingCategory4
        this.promotionEmployee = promotionEmployee
        this.updatedAt = LocalDateTime.now()
    }
}
