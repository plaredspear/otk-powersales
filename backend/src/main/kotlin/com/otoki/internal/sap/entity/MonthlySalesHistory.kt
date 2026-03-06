package com.otoki.internal.sap.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 월매출 이력 Entity
 * V1 스키마: monthlysaleshistory__c (Heroku Connect 동기화)
 */
@Entity
@Table(name = "monthlysaleshistory__c")
class MonthlySalesHistory(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @Column(name = "name", length = 80)
    val name: String? = null,

    @Column(name = "account_externalkey__c", length = 1300)
    var accountExternalKey: String? = null,

    @Column(name = "account_branchname__c", length = 1300)
    val accountBranchName: String? = null,

    @Column(name = "account_type__c", length = 1300)
    val accountType: String? = null,

    @Column(name = "salesyear__c", length = 255)
    var salesYear: String? = null,

    @Column(name = "salesmonth__c", length = 255)
    var salesMonth: String? = null,

    @Column(name = "fm_year__c")
    val fmYear: Double? = null,

    @Column(name = "fm_month__c")
    val fmMonth: Double? = null,

    @Column(name = "targetmonthresults__c")
    val targetMonthResults: Double? = null,

    @Column(name = "lastmonthresults__c")
    val lastMonthResults: Double? = null,

    @Column(name = "lastmonthtargetfomula__c")
    val lastMonthTargetFormula: Double? = null,

    @Column(name = "lastmonthtargetachievedratio__c")
    val lastMonthTargetAchievedRatio: Double? = null,

    @Column(name = "shipclosingamount__c")
    var shipClosingAmount: Double? = null,

    @Column(name = "abcclosingamount1__c")
    var abcClosingAmount1: Double? = null,

    @Column(name = "abcclosingamount2__c")
    var abcClosingAmount2: Double? = null,

    @Column(name = "abcclosingamount3__c")
    var abcClosingAmount3: Double? = null,

    @Column(name = "ambientpurpose__c")
    val ambientPurpose: Double? = null,

    @Column(name = "fridgepurpose__c")
    val fridgePurpose: Double? = null,

    @Column(name = "isdeleted")
    val isDeleted: Boolean? = null,

    @Column(name = "createddate")
    val createdDate: LocalDateTime? = null,

    @Column(name = "systemmodstamp")
    val systemModStamp: LocalDateTime? = null,

    @Column(name = "_hc_lastop", length = 32)
    val hcLastOp: String? = null,

    @Column(name = "_hc_err", columnDefinition = "TEXT")
    val hcErr: String? = null,

    @Column(name = "externalkey__c", length = 30)
    var externalkeyC: String? = null,

    @Column(name = "rlsales__c")
    var rlsalesC: Double? = null
)
