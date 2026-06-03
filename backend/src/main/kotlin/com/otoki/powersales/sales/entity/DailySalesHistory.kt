package com.otoki.powersales.sales.entity

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import jakarta.persistence.*

/**
 * 일별매출이력 Entity
 * SF: DailySalesHistory__c (일별매출이력)
 *
 * SAP 인바운드 적재 + SF 데이터 마이그레이션 대상.
 * sapAccountCode + salesDate 조합으로 externalKey 를 구성하여 UPSERT.
 */
@Entity
@Table(name = "daily_sales_history")
@SFObject("DailySalesHistory__c")
class DailySalesHistory(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @HCColumn("sfid")
    @Column(name = "sfid", length = 18)
    var sfid: String? = null,

    @SFField("SAPAccountCode__c")
    @Column(name = "sap_account_code", nullable = false, length = 100)
    val sapAccountCode: String,

    @SFField("SalesDate__c")
    @Column(name = "sales_date", nullable = false, length = 8)
    val salesDate: String,

    @SFField("Externalkey__c")
    @Column(name = "external_key", nullable = false, unique = true, length = 40)
    val externalKey: String,

    @SFField("AccountId__c")
    @Column(name = "account_sfid", length = 18)
    var accountSfid: String? = null,

    @SFField("ERPSalesAmount1__c")
    @Column(name = "erp_sales_amount1")
    var erpSalesAmount1: Double? = null,

    @SFField("ERPSalesAmount2__c")
    @Column(name = "erp_sales_amount2")
    var erpSalesAmount2: Double? = null,

    @SFField("ERPSalesAmount3__c")
    @Column(name = "erp_sales_amount3")
    var erpSalesAmount3: Double? = null,

    @SFField("ERPDistributionAmount1__c")
    @Column(name = "erp_distribution_amount1")
    var erpDistributionAmount1: Double? = null,

    @SFField("ERPDistributionAmount2__c")
    @Column(name = "erp_distribution_amount2")
    var erpDistributionAmount2: Double? = null,

    @SFField("ERPDistributionAmount3__c")
    @Column(name = "erp_distribution_amount3")
    var erpDistributionAmount3: Double? = null,

    @SFField("ERPSalesAmount__c")
    @Column(name = "erp_sales_amount")
    var erpSalesAmount: Double? = null,

    @SFField("ERPDistributionAmount__c")
    @Column(name = "erp_distribution_amount")
    var erpDistributionAmount: Double? = null,

    @SFField("LedgerAmount__c")
    @Column(name = "ledger_amount")
    var ledgerAmount: Double? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    var account: Account? = null,
) : BaseEntity()
