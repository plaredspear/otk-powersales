package com.otoki.internal.schedule.entity

import com.otoki.internal.common.entity.BaseEntity
import com.otoki.internal.common.salesforce.HCColumn
import com.otoki.internal.common.salesforce.HCTable
import com.otoki.internal.common.salesforce.SFField
import com.otoki.internal.common.salesforce.SFObject
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
    @HCColumn("id")
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
    @Column(name = "employee_id")
    var employeeId: Long? = null,

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
    @Column(name = "account_id")
    var accountId: Int? = null,

    @SFField("teamleadersfid__c")
    @HCColumn("teamleadersfid__c")
    @Column(name = "team_leader_id")
    val teamLeaderId: Long? = null,

    @SFField("DKRetail__AltHolidayId__c")
    @HCColumn("dkretail__altholidayid__c")
    @Column(name = "alt_holiday_id")
    val altHolidayId: Long? = null,

    @SFField("DKRetail__CommuteLogId__c")
    @HCColumn("dkretail__commutelogid__c")
    @Column(name = "commute_log_id", length = 18)
    var commuteLogId: String? = null,

    @SFField("DKRetail__PromotionEmpId__c")
    @HCColumn("dkretail__promotionempid__c")
    @Column(name = "promotion_employee_id")
    var promotionEmployeeId: Long? = null,

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
    @Column(name = "isdeleted")
    val isDeleted: Boolean? = null,

) : BaseEntity() {
    fun updateForPromotion(
        employeeId: Long,
        accountId: Int,
        workingDate: LocalDate,
        workingType: String,
        workingCategory1: String,
        workingCategory3: String,
        workingCategory4: String?,
        promotionEmployeeId: Long
    ) {
        this.employeeId = employeeId
        this.accountId = accountId
        this.workingDate = workingDate
        this.workingType = workingType
        this.workingCategory1 = workingCategory1
        this.workingCategory3 = workingCategory3
        this.workingCategory4 = workingCategory4
        this.promotionEmployeeId = promotionEmployeeId
        this.updatedAt = LocalDateTime.now()
    }
}
