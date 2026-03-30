package com.otoki.internal.sap.entity

import com.otoki.internal.common.entity.BaseEntity
import com.otoki.internal.common.salesforce.HCColumn
import com.otoki.internal.common.salesforce.HCTable
import com.otoki.internal.common.sap.SAPSource
import com.otoki.internal.common.sap.SAPUpsertKey
import com.otoki.internal.common.sap.SyncMode
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime

/**
 * 월매출 이력 Entity
 * V1 스키마: monthlysaleshistory__c (Heroku Connect 동기화)
 */
@Entity
@Table(name = "monthly_sales_history")
@SAPSource(api = "/sap/MonthlySalesHistory", syncMode = SyncMode.UPSERT)
@HCTable("monthlysaleshistory__c")
class MonthlySalesHistory(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "monthly_sales_history_id")
    val id: Long = 0,

    @HCColumn("sfid")
    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @HCColumn("name")
    @Column(name = "name", length = 80)
    val name: String? = null,

    @HCColumn("account_externalkey__c")
    @Column(name = "account_external_key", length = 1300)
    var accountExternalKey: String? = null,

    @HCColumn("account_branchname__c")
    @Column(name = "account_branch_name", length = 1300)
    val accountBranchName: String? = null,

    @HCColumn("account_type__c")
    @Column(name = "account_type", length = 1300)
    val accountType: String? = null,

    @HCColumn("salesyear__c")
    @Column(name = "sales_year", length = 255)
    var salesYear: String? = null,

    @HCColumn("salesmonth__c")
    @Column(name = "sales_month", length = 255)
    var salesMonth: String? = null,

    @HCColumn("fm_year__c")
    @Column(name = "fm_year")
    val fmYear: Double? = null,

    @HCColumn("fm_month__c")
    @Column(name = "fm_month")
    val fmMonth: Double? = null,

    @HCColumn("targetmonthresults__c")
    @Column(name = "target_month_results")
    val targetMonthResults: Double? = null,

    @HCColumn("lastmonthresults__c")
    @Column(name = "last_month_results")
    val lastMonthResults: Double? = null,

    @HCColumn("lastmonthtargetfomula__c")
    @Column(name = "last_month_target_formula")
    val lastMonthTargetFormula: Double? = null,

    @HCColumn("lastmonthtargetachievedratio__c")
    @Column(name = "last_month_target_achieved_ratio")
    val lastMonthTargetAchievedRatio: Double? = null,

    @HCColumn("shipclosingamount__c")
    @Column(name = "ship_closing_amount")
    var shipClosingAmount: Double? = null,

    @HCColumn("abcclosingamount1__c")
    @Column(name = "abc_closing_amount1")
    var abcClosingAmount1: Double? = null,

    @HCColumn("abcclosingamount2__c")
    @Column(name = "abc_closing_amount2")
    var abcClosingAmount2: Double? = null,

    @HCColumn("abcclosingamount3__c")
    @Column(name = "abc_closing_amount3")
    var abcClosingAmount3: Double? = null,

    @HCColumn("ambientpurpose__c")
    @Column(name = "ambient_purpose")
    val ambientPurpose: Double? = null,

    @HCColumn("fridgepurpose__c")
    @Column(name = "fridge_purpose")
    val fridgePurpose: Double? = null,

    @HCColumn("isdeleted")
    @Column(name = "is_deleted")
    val isDeleted: Boolean? = null,

    @SAPUpsertKey(composite = true, components = ["sapAccountCode", "salesYearMonth"])
    @Column(name = "external_key", unique = true, length = 30)
    var externalkeyC: String? = null,

    @Column(name = "rl_sales")
    var rlsalesC: Double? = null,

    @HCColumn("createddate")
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    override var createdAt: LocalDateTime = LocalDateTime.now(),

    @HCColumn("systemmodstamp")
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    override var updatedAt: LocalDateTime = LocalDateTime.now(),

    // -- Relations --

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    var account: Account? = null,
) : BaseEntity()
