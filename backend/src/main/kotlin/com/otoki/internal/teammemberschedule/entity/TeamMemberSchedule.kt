package com.otoki.internal.teammemberschedule.entity

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
@Table(name = "dkretail__teammemberschedule__c")
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
    @Column(name = "dkretail__employeeid__c", length = 18)
    var employeeId: String? = null,

    @SFField("DKRetail__WorkingDate__c")
    @HCColumn("dkretail__workingdate__c")
    @Column(name = "dkretail__workingdate__c")
    var workingDate: LocalDate? = null,

    @SFField("DKRetail__WorkingType__c")
    @HCColumn("dkretail__workingtype__c")
    @Column(name = "dkretail__workingtype__c", length = 255)
    var workingType: String? = null,

    @SFField("DKRetail__WorkingCategory1__c")
    @HCColumn("dkretail__workingcategory1__c")
    @Column(name = "dkretail__workingcategory1__c", length = 255)
    var workingCategory1: String? = null,

    @SFField("DKRetail__WorkingCategory2__c")
    @HCColumn("dkretail__workingcategory2__c")
    @Column(name = "dkretail__workingcategory2__c", length = 255)
    val workingCategory2: String? = null,

    @SFField("DKRetail__WorkingCategory3__c")
    @HCColumn("dkretail__workingcategory3__c")
    @Column(name = "dkretail__workingcategory3__c", length = 255)
    var workingCategory3: String? = null,

    @SFField("WorkingCategory4__c")
    @HCColumn("workingcategory4__c")
    @Column(name = "workingcategory4__c", length = 255)
    var workingCategory4: String? = null,

    @SFField("AccountId__c")
    @HCColumn("accountid__c")
    @Column(name = "accountid__c", length = 18)
    var accountId: String? = null,

    @SFField("teamleadersfid__c")
    @HCColumn("teamleadersfid__c")
    @Column(name = "teamleadersfid__c", length = 100)
    val teamLeaderSfid: String? = null,

    @SFField("DKRetail__AltHolidayId__c")
    @HCColumn("dkretail__altholidayid__c")
    @Column(name = "dkretail__altholidayid__c", length = 18)
    val altHolidayId: String? = null,

    @SFField("DKRetail__CommuteLogId__c")
    @HCColumn("dkretail__commutelogid__c")
    @Column(name = "dkretail__commutelogid__c", length = 18)
    var commuteLogId: String? = null,

    @SFField("DKRetail__PromotionEmpId__c")
    @HCColumn("dkretail__promotionempid__c")
    @Column(name = "dkretail__promotionempid__c", length = 18)
    var promotionEmpId: String? = null,

    @SFField("DKRetail__PromotionEmpIdExt__c")
    @Column(name = "dkretail__promotionempidext__c", length = 50)
    var promotionEmpIdExt: String? = null,

    @SFField("CommuteReportDateTime__c")
    @HCColumn("commutereportdatetime__c")
    @Column(name = "commutereportdatetime__c")
    val commuteReportDatetime: LocalDateTime? = null,

    @SFField("ID__c")
    @HCColumn("id__c")
    @Column(name = "id__c", length = 30)
    val idField: String? = null,

    @SFField("TraversalFlag__c")
    @HCColumn("traversalflag__c")
    @Column(name = "traversalflag__c", length = 255)
    val traversalFlag: String? = null,

    @HCColumn("isworkreport__c")
    @Column(name = "isworkreport__c", length = 1300)
    val isWorkReport: String? = null,

    @SFField("Equipment1__c")
    @HCColumn("equipment1__c")
    @Column(name = "equipment1__c", length = 10)
    val equipment1: String? = null,

    @SFField("Equipment2__c")
    @HCColumn("equipment2__c")
    @Column(name = "equipment2__c", length = 10)
    val equipment2: String? = null,

    @SFField("Equipment3__c")
    @HCColumn("equipment3__c")
    @Column(name = "equipment3__c", length = 10)
    val equipment3: String? = null,

    @SFField("Equipment4__c")
    @HCColumn("equipment4__c")
    @Column(name = "equipment4__c", length = 10)
    val equipment4: String? = null,

    @SFField("Equipment5__c")
    @HCColumn("equipment5__c")
    @Column(name = "equipment5__c", length = 10)
    val equipment5: String? = null,

    @SFField("Equipment6__c")
    @HCColumn("equipment6__c")
    @Column(name = "equipment6__c", length = 10)
    val equipment6: String? = null,

    @SFField("Equipment7__c")
    @HCColumn("equipment7__c")
    @Column(name = "equipment7__c", length = 10)
    val equipment7: String? = null,

    @SFField("Equipment8__c")
    @HCColumn("equipment8__c")
    @Column(name = "equipment8__c", length = 10)
    val equipment8: String? = null,

    @SFField("Equipment9__c")
    @HCColumn("equipment9__c")
    @Column(name = "equipment9__c", length = 10)
    val equipment9: String? = null,

    @SFField("Equipment10__c")
    @HCColumn("equipment10__c")
    @Column(name = "equipment10__c", length = 10)
    val equipment10: String? = null,

    @SFField("Yes_ChkCnt__c")
    @HCColumn("yes_chkcnt__c")
    @Column(name = "yes_chkcnt__c")
    val yesChkCnt: Double? = null,

    @SFField("No_ChkCnt__c")
    @HCColumn("no_chkcnt__c")
    @Column(name = "no_chkcnt__c")
    val noChkCnt: Double? = null,

    @SFField("precaution_chk__c")
    @HCColumn("precaution_chk__c")
    @Column(name = "precaution_chk__c")
    val precautionChk: Double? = null,

    @SFField("precaution__c")
    @HCColumn("precaution__c")
    @Column(name = "precaution__c", length = 3000)
    val precaution: String? = null,

    @SFField("StartTime__c")
    @HCColumn("starttime__c")
    @Column(name = "starttime__c")
    val startTime: LocalDateTime? = null,

    @SFField("CompleteTime__c")
    @HCColumn("completetime__c")
    @Column(name = "completetime__c")
    val completeTime: LocalDateTime? = null,

    @HCColumn("isdeleted")
    @Column(name = "isdeleted")
    val isDeleted: Boolean? = null,

    @HCColumn("createddate")
    @Column(name = "createddate")
    val createdDate: LocalDateTime? = null,

    @HCColumn("systemmodstamp")
    @Column(name = "systemmodstamp")
    var systemModStamp: LocalDateTime? = null,

    @HCColumn("_hc_lastop")
    @Column(name = "_hc_lastop", length = 32)
    val hcLastOp: String? = null,

    @HCColumn("_hc_err")
    @Column(name = "_hc_err", columnDefinition = "TEXT")
    val hcErr: String? = null

    // --- 주석 처리: V2 기존 필드 ---
    // userId: Long — V1에서 employeeId(String sfid)로 대체
    // storeName: String — V1에 없음
    // scheduleDate: LocalDate — V1에서 workingDate로 대체
    // startTime: LocalTime — V1에서 startTime(LocalDateTime)으로 대체
    // endTime: LocalTime — V1에서 completeTime(LocalDateTime)으로 대체
    // type: String — V1에서 workingType으로 대체
) {
    fun updateForPromotion(
        employeeId: String,
        accountId: String,
        workingDate: LocalDate,
        workingType: String,
        workingCategory1: String,
        workingCategory3: String,
        workingCategory4: String?,
        promotionEmpId: String
    ) {
        this.employeeId = employeeId
        this.accountId = accountId
        this.workingDate = workingDate
        this.workingType = workingType
        this.workingCategory1 = workingCategory1
        this.workingCategory3 = workingCategory3
        this.workingCategory4 = workingCategory4
        this.promotionEmpId = promotionEmpId
        this.promotionEmpIdExt = promotionEmpId
        this.systemModStamp = LocalDateTime.now()
    }
}
