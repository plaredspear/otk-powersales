package com.otoki.powersales.sales.entity

import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HCTable
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import com.otoki.powersales.common.entity.AuditedEntity
import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.employee.entity.Employee
/**
 * 월매출 이력 Entity
 * V1 스키마: monthlysaleshistory__c (Heroku Connect 동기화)
 */
@Entity
@Table(name = "monthly_sales_history")
@SFObject("MonthlySalesHistory__c")
@HCTable("monthlysaleshistory__c")
class MonthlySalesHistory(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "monthly_sales_history_id")
    val id: Long = 0,

    @HCColumn("sfid")
    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @SFField("Name")
    @HCColumn("name")
    @Column(name = "name", length = 80)
    val name: String? = null,

    @SFField("SalesYear__c")
    @HCColumn("salesyear__c")
    @Column(name = "sales_year", length = 255)
    var salesYear: String? = null,

    @SFField("SalesMonth__c")
    @HCColumn("salesmonth__c")
    @Column(name = "sales_month", length = 255)
    var salesMonth: String? = null,

    @SFField("LastMonthResults__c")
    @HCColumn("lastmonthresults__c")
    @Column(name = "last_month_results")
    val lastMonthResults: Double? = null,

    @SFField("ShipClosingAmount__c")
    @HCColumn("shipclosingamount__c")
    @Column(name = "ship_closing_amount")
    var shipClosingAmount: Double? = null,

    @SFField("ABCClosingAmount1__c")
    @HCColumn("abcclosingamount1__c")
    @Column(name = "abc_closing_amount1")
    var abcClosingAmount1: Double? = null,

    @SFField("ABCClosingAmount2__c")
    @HCColumn("abcclosingamount2__c")
    @Column(name = "abc_closing_amount2")
    var abcClosingAmount2: Double? = null,

    @SFField("ABCClosingAmount3__c")
    @HCColumn("abcclosingamount3__c")
    @Column(name = "abc_closing_amount3")
    var abcClosingAmount3: Double? = null,

    @SFField("AmbientPurpose__c")
    @HCColumn("ambientpurpose__c")
    @Column(name = "ambient_purpose")
    val ambientPurpose: Double? = null,

    @SFField("FridgePurpose__c")
    @HCColumn("fridgepurpose__c")
    @Column(name = "fridge_purpose")
    val fridgePurpose: Double? = null,

    @SFField("IsDeleted")
    @HCColumn("isdeleted")
    @Column(name = "is_deleted")
    val isDeleted: Boolean? = null,

    @SFField("Externalkey__c")
    @HCColumn("externalkey__c")
    @Column(name = "external_key", unique = true, length = 30)
    var externalkeyC: String? = null,

    // SAP 인바운드 적재 전용 (amounts[5]) — SF 메타 미매핑이라 @SFField 부재. 현재 소비처 0건.
    @Column(name = "rl_sales")
    var rlsalesC: Double? = null,

    // Spec #575: SAP TotalLedgerAmount 보존 (누적 합산 로직은 D1 결정으로 별도 스펙 분리)
    @SFField("TotalLedgerAmount__c")
    @HCColumn("totalledgeramount__c")
    @Column(name = "total_ledger_amount", precision = 18, scale = 4)
    var totalLedgerAmount: BigDecimal? = null,

    // -- Spec #601: SF 누락 컬럼 신규 도입 --

    @SFField("AccountId__c")
    @HCColumn("accountid__c")
    @Column(name = "account_sfid", length = 18)
    var accountSfid: String? = null,

    @SFField("SAPAccountCode__c")
    @HCColumn("sapaccountcode__c")
    @Column(name = "sap_account_code", length = 100)
    var sapAccountCode: String? = null,

    @SFField("SalesDate__c")
    @HCColumn("salesdate__c")
    @Column(name = "sales_date")
    var salesDate: LocalDate? = null,

    @SFField("LastMonthlySalesHistory__c")
    @HCColumn("lastmonthlysaleshistory__c")
    @Column(name = "last_monthly_sales_history_sfid", length = 18)
    var lastMonthlySalesHistorySfid: String? = null,

    @SFField("Confirm__c")
    @HCColumn("confirm__c")
    @Column(name = "is_confirmed")
    var isConfirmed: Boolean? = null,

    @SFField("HQReviews__c")
    @HCColumn("hqreviews__c")
    @Column(name = "hq_review_sfid", length = 18)
    var hqReviewSfid: String? = null,

    @SFField("Remark__c")
    @HCColumn("remark__c")
    @Column(name = "remark", columnDefinition = "TEXT")
    var remark: String? = null,

    @SFField("ShipClosingAmountNH__c")
    @HCColumn("shipclosingamountnh__c")
    @Column(name = "ship_closing_amount_nh")
    var shipClosingAmountNh: Double? = null,

    @SFField("ShipClosingAmount1__c")
    @HCColumn("shipclosingamount1__c")
    @Column(name = "ship_closing_amount1")
    var shipClosingAmount1: Double? = null,

    @SFField("ShipClosingAmount2__c")
    @HCColumn("shipclosingamount2__c")
    @Column(name = "ship_closing_amount2")
    var shipClosingAmount2: Double? = null,

    @SFField("ShipClosingAmount3__c")
    @HCColumn("shipclosingamount3__c")
    @Column(name = "ship_closing_amount3")
    var shipClosingAmount3: Double? = null,

    @SFField("ShipClosingAmount4__c")
    @HCColumn("shipclosingamount4__c")
    @Column(name = "ship_closing_amount4")
    var shipClosingAmount4: Double? = null,

    @SFField("ShipClosingSumAmount__c")
    @HCColumn("shipclosingsumamount__c")
    @Column(name = "ship_closing_sum_amount")
    var shipClosingSumAmount: Double? = null,

    @SFField("ABCClosingAmount4__c")
    @HCColumn("abcclosingamount4__c")
    @Column(name = "abc_closing_amount4")
    var abcClosingAmount4: Double? = null,

    @SFField("ABCClosingSumAmount__c")
    @HCColumn("abcclosingsumamount__c")
    @Column(name = "abc_closing_sum_amount")
    var abcClosingSumAmount: Double? = null,

    @SFField("LastMonthTargetByHand__c")
    @HCColumn("lastmonthtargetbyhand__c")
    @Column(name = "last_month_target_by_hand")
    var lastMonthTargetByHand: Double? = null,

    @SFField("ThisMonthTarget__c")
    @HCColumn("thismonthtarget__c")
    @Column(name = "this_month_target")
    var thisMonthTarget: Double? = null,

    // -- Group A: System timestamps (Spec #729 R-2 정합) --

    @SFField("CreatedDate")
    @HCColumn("createddate")
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @SFField("LastModifiedDate")
    @HCColumn("lastmodifieddate")
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

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
    @JoinColumn(name = "account_id")
    var account: Account? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    var owner: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: Employee? = null,
) : AuditedEntity()
