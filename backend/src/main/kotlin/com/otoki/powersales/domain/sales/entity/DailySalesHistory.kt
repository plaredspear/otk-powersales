package com.otoki.powersales.domain.sales.entity

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.platform.common.entity.OwnerUserDefaultListener
import com.otoki.powersales.platform.common.salesforce.HCColumn
import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import com.otoki.powersales.domain.org.employee.entity.Group
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

/**
 * 일별매출이력 Entity
 * SF: DailySalesHistory__c (일별매출이력)
 *
 * SAP 인바운드 적재 + SF 데이터 마이그레이션 대상.
 * sapAccountCode + salesDate 조합으로 externalKey 를 구성하여 UPSERT.
 */
@EntityListeners(OwnerUserDefaultListener::class)
@DomainName("일별매출이력")
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
    @FieldName("거래처코드")
    @Column(name = "sap_account_code", nullable = false, length = 100)
    val sapAccountCode: String,

    @SFField("SalesDate__c")
    @FieldName("매출발생년월일")
    @Column(name = "sales_date", nullable = false, length = 8)
    val salesDate: String,

    @SFField("Externalkey__c")
    @FieldName("Externalkey")
    @Column(name = "external_key", nullable = false, unique = true, length = 40)
    val externalKey: String,

    @SFField("AccountId__c")
    @Column(name = "account_sfid", length = 18)
    var accountSfid: String? = null,

    @SFField("ERPSalesAmount1__c")
    @FieldName("전산매출실적_상온 (원)")
    @Column(name = "erp_sales_amount1")
    var erpSalesAmount1: Double? = null,

    @SFField("ERPSalesAmount2__c")
    @FieldName("전산매출실적_라면 (원)")
    @Column(name = "erp_sales_amount2")
    var erpSalesAmount2: Double? = null,

    @SFField("ERPSalesAmount3__c")
    @FieldName("전산매출실적_냉장냉동 (원)")
    @Column(name = "erp_sales_amount3")
    var erpSalesAmount3: Double? = null,

    @SFField("ERPDistributionAmount1__c")
    @FieldName("물류배부매출실적_상온 (원)")
    @Column(name = "erp_distribution_amount1")
    var erpDistributionAmount1: Double? = null,

    @SFField("ERPDistributionAmount2__c")
    @FieldName("물류배부매출실적_라면 (원)")
    @Column(name = "erp_distribution_amount2")
    var erpDistributionAmount2: Double? = null,

    @SFField("ERPDistributionAmount3__c")
    @FieldName("물류배부매출실적_냉장냉동 (원)")
    @Column(name = "erp_distribution_amount3")
    var erpDistributionAmount3: Double? = null,

    @SFField("ERPSalesAmount__c")
    @FieldName("전산매출실적")
    @Column(name = "erp_sales_amount")
    var erpSalesAmount: Double? = null,

    @SFField("ERPDistributionAmount__c")
    @FieldName("물류배부매출실적")
    @Column(name = "erp_distribution_amount")
    var erpDistributionAmount: Double? = null,

    @SFField("LedgerAmount__c")
    @FieldName("원장매출")
    @Column(name = "ledger_amount")
    var ledgerAmount: Double? = null,

    // -- OwnerId polymorphic R-2 (referenceTo = [Group, User]) — owner_sfid sync buffer + owner_user/owner_group XOR --
    // Stage1 적재가 owner_sfid 채움, Stage2 fk substep 이 owner_user_id (005) / owner_group_id (00G) 분기 채움.
    @SFField("OwnerId")
    @Column(name = "owner_sfid", length = 18)
    var ownerSfid: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    var account: Account? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    var ownerUser: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_group_id")
    var ownerGroup: Group? = null,
) : BaseEntity()
