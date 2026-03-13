package com.otoki.internal.teammemberschedule.entity

import com.otoki.internal.common.salesforce.HCColumn
import com.otoki.internal.common.salesforce.HCTable
import com.otoki.internal.common.salesforce.SFField
import com.otoki.internal.common.salesforce.SFObject
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 거래처 일정 Entity (진열마스터 확정 스케줄)
 * V1 스키마: displayworkschedulemaster__c
 */
@Entity
@Table(name = "displayworkschedulemaster__c")
@SFObject("DKRetail__DisplayWorkScheduleMaster__c")
@HCTable("displayworkschedulemaster__c")
class DisplayWorkSchedule(

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

    @SFField("Account__c")
    @HCColumn("account__c")
    @Column(name = "account__c", length = 18)
    val account: String? = null,

    @SFField("FullName__c")
    @HCColumn("fullname__c")
    @Column(name = "fullname__c", length = 18)
    val fullName: String? = null,

    @SFField("StartDate__c")
    @HCColumn("startdate__c")
    @Column(name = "startdate__c")
    val startDate: LocalDate? = null,

    @SFField("EndDate__c")
    @HCColumn("enddate__c")
    @Column(name = "enddate__c")
    val endDate: LocalDate? = null,

    @SFField("Confirmed__c")
    @HCColumn("confirmed__c")
    @Column(name = "confirmed__c")
    val confirmed: Boolean? = null,

    @SFField("TypeOfWork1__c")
    @HCColumn("typeofwork1__c")
    @Column(name = "typeofwork1__c", length = 255)
    val typeOfWork1: String? = null,

    @SFField("TypeOfWork3__c")
    @HCColumn("typeofwork3__c")
    @Column(name = "typeofwork3__c", length = 255)
    val typeOfWork3: String? = null,

    @SFField("TypeOfWork5__c")
    @HCColumn("typeofwork5__c")
    @Column(name = "typeofwork5__c", length = 255)
    val typeOfWork5: String? = null,

    @HCColumn("createdbyid")
    @Column(name = "createdbyid", length = 18)
    val createdById: String? = null,

    @HCColumn("ownerid")
    @Column(name = "ownerid", length = 18)
    val ownerId: String? = null,

    @HCColumn("isdeleted")
    @Column(name = "isdeleted")
    val isDeleted: Boolean? = null,

    @HCColumn("createddate")
    @Column(name = "createddate")
    val createdDate: LocalDateTime? = null,

    @HCColumn("systemmodstamp")
    @Column(name = "systemmodstamp")
    val systemModStamp: LocalDateTime? = null,

    @HCColumn("_hc_lastop")
    @Column(name = "_hc_lastop", length = 32)
    val hcLastOp: String? = null,

    @HCColumn("_hc_err")
    @Column(name = "_hc_err", columnDefinition = "TEXT")
    val hcErr: String? = null

    // --- 주석 처리: V2 기존 필드 ---
    // userId: Long — V1에서 fullName(String sfid)로 대체
    // storeId: Long — V1에서 account(String sfid)로 대체
    // storeName: String — V1에 없음
    // storeCode: String — V1에 없음
    // address: String? — V1에 없음
    // workCategory: String — V1에서 typeOfWork1로 대체
    // scheduleDate: LocalDate — V1에서 startDate로 대체
)
