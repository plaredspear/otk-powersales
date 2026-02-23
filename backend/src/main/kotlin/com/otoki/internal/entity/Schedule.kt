package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 일정 Entity
 * V1 스키마: dkretail__teammemberschedule__c (팀원 스케줄 + 안전점검 장비 + 업무보고)
 */
@Entity
@Table(name = "dkretail__teammemberschedule__c")
class Schedule(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @Column(name = "name", length = 80)
    val name: String? = null,

    @Column(name = "dkretail__employeeid__c", length = 18)
    val employeeId: String? = null,

    @Column(name = "dkretail__workingdate__c")
    val workingDate: LocalDate? = null,

    @Column(name = "dkretail__workingtype__c", length = 255)
    val workingType: String? = null,

    @Column(name = "dkretail__workingcategory1__c", length = 255)
    val workingCategory1: String? = null,

    @Column(name = "dkretail__workingcategory2__c", length = 255)
    val workingCategory2: String? = null,

    @Column(name = "dkretail__workingcategory3__c", length = 255)
    val workingCategory3: String? = null,

    @Column(name = "workingcategory4__c", length = 255)
    val workingCategory4: String? = null,

    @Column(name = "accountid__c", length = 18)
    val accountId: String? = null,

    @Column(name = "teamleadersfid__c", length = 100)
    val teamLeaderSfid: String? = null,

    @Column(name = "dkretail__altholidayid__c", length = 18)
    val altHolidayId: String? = null,

    @Column(name = "dkretail__commutelogid__c", length = 18)
    val commuteLogId: String? = null,

    @Column(name = "dkretail__promotionempid__c", length = 18)
    val promotionEmpId: String? = null,

    @Column(name = "commutereportdatetime__c")
    val commuteReportDatetime: LocalDateTime? = null,

    @Column(name = "id__c", length = 30)
    val idField: String? = null,

    @Column(name = "traversalflag__c", length = 255)
    val traversalFlag: String? = null,

    @Column(name = "isworkreport__c", length = 1300)
    val isWorkReport: String? = null,

    @Column(name = "equipment1__c", length = 10)
    val equipment1: String? = null,

    @Column(name = "equipment2__c", length = 10)
    val equipment2: String? = null,

    @Column(name = "equipment3__c", length = 10)
    val equipment3: String? = null,

    @Column(name = "equipment4__c", length = 10)
    val equipment4: String? = null,

    @Column(name = "equipment5__c", length = 10)
    val equipment5: String? = null,

    @Column(name = "equipment6__c", length = 10)
    val equipment6: String? = null,

    @Column(name = "equipment7__c", length = 10)
    val equipment7: String? = null,

    @Column(name = "equipment8__c", length = 10)
    val equipment8: String? = null,

    @Column(name = "equipment9__c", length = 10)
    val equipment9: String? = null,

    @Column(name = "equipment10__c", length = 10)
    val equipment10: String? = null,

    @Column(name = "yes_chkcnt__c")
    val yesChkCnt: Double? = null,

    @Column(name = "no_chkcnt__c")
    val noChkCnt: Double? = null,

    @Column(name = "precaution_chk__c")
    val precautionChk: Double? = null,

    @Column(name = "precaution__c", length = 3000)
    val precaution: String? = null,

    @Column(name = "starttime__c")
    val startTime: LocalDateTime? = null,

    @Column(name = "completetime__c")
    val completeTime: LocalDateTime? = null,

    @Column(name = "isdeleted")
    val isDeleted: Boolean? = null,

    @Column(name = "createddate")
    val createdDate: LocalDateTime? = null,

    @Column(name = "systemmodstamp")
    val systemModStamp: LocalDateTime? = null,

    @Column(name = "_hc_lastop", length = 32)
    val hcLastOp: String? = null,

    @Column(name = "_hc_err", columnDefinition = "TEXT")
    val hcErr: String? = null

    // --- 주석 처리: V2 기존 필드 ---
    // userId: Long — V1에서 employeeId(String sfid)로 대체
    // storeName: String — V1에 없음
    // scheduleDate: LocalDate — V1에서 workingDate로 대체
    // startTime: LocalTime — V1에서 startTime(LocalDateTime)으로 대체
    // endTime: LocalTime — V1에서 completeTime(LocalDateTime)으로 대체
    // type: String — V1에서 workingType으로 대체
)
