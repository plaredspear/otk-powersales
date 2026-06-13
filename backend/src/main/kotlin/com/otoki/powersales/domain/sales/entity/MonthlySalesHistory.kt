package com.otoki.powersales.domain.sales.entity

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.employee.entity.Group
import com.otoki.powersales.domain.sales.entity.converter.SalesMonthConverter
import com.otoki.powersales.domain.sales.entity.converter.SalesYearConverter
import com.otoki.powersales.domain.sales.enums.SalesMonth
import com.otoki.powersales.domain.sales.enums.SalesYear
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import com.otoki.powersales.common.entity.OwnerUserDefaultListener

/**
 * 월매출 이력 Entity
 * V1 스키마: monthlysaleshistory__c (Heroku Connect 동기화)
 */
@EntityListeners(OwnerUserDefaultListener::class)
@Entity
@Table(name = "monthly_sales_history")
@SFObject("MonthlySalesHistory__c")
class MonthlySalesHistory(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "monthly_sales_history_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @Column(name = "name", length = 80)
    val name: String? = null,

    @SFField("SalesYear__c")
    @Convert(converter = SalesYearConverter::class)
    @Column(name = "sales_year", length = 4)
    var salesYear: SalesYear? = null,

    @SFField("SalesMonth__c")
    @Convert(converter = SalesMonthConverter::class)
    @Column(name = "sales_month", length = 2)
    var salesMonth: SalesMonth? = null,

    @SFField("LastMonthResults__c")
    @Column(name = "last_month_results", precision = 18, scale = 0)
    val lastMonthResults: BigDecimal? = null,

    @SFField("ShipClosingAmount__c")
    @Column(name = "ship_closing_amount")
    var shipClosingAmount: Double? = null,

    @SFField("ABCClosingAmount1__c")
    @Column(name = "abc_closing_amount1")
    var abcClosingAmount1: Double? = null,

    @SFField("ABCClosingAmount2__c")
    @Column(name = "abc_closing_amount2")
    var abcClosingAmount2: Double? = null,

    @SFField("ABCClosingAmount3__c")
    @Column(name = "abc_closing_amount3")
    var abcClosingAmount3: Double? = null,

    @SFField("AmbientPurpose__c")
    @Column(name = "ambient_purpose")
    val ambientPurpose: Double? = null,

    @SFField("FridgePurpose__c")
    @Column(name = "fridge_purpose")
    val fridgePurpose: Double? = null,

    @SFField("IsDeleted")
    @Column(name = "is_deleted")
    val isDeleted: Boolean? = null,

    @SFField("Externalkey__c")
    @Column(name = "external_key", unique = true, length = 40)
    var externalkeyC: String? = null,

    // SAP 인바운드 적재 전용 (amounts[5]) — SF 메타 미매핑이라 @SFField 부재. 현재 소비처 0건.
    @Column(name = "rl_sales")
    var rlsalesC: Double? = null,

    // SF 메타 정합: type=double precision=18 scale=0
    @SFField("TotalLedgerAmount__c")
    @Column(name = "total_ledger_amount", precision = 18, scale = 0)
    var totalLedgerAmount: BigDecimal? = null,

    @SFField("AccountId__c")
    @Column(name = "account_sfid", length = 18)
    var accountSfid: String? = null,

    @SFField("SAPAccountCode__c")
    @Column(name = "sap_account_code", length = 100)
    var sapAccountCode: String? = null,

    @SFField("SalesDate__c")
    @Column(name = "sales_date")
    var salesDate: LocalDate? = null,

    @SFField("LastMonthlySalesHistory__c")
    @Column(name = "last_monthly_sales_history_sfid", length = 18)
    var lastMonthlySalesHistorySfid: String? = null,

    @SFField("Confirm__c")
    @Column(name = "is_confirmed")
    var isConfirmed: Boolean? = null,

    @SFField("Remark__c")
    @Column(name = "remark", columnDefinition = "TEXT")
    var remark: String? = null,

    @SFField("ShipClosingAmountNH__c")
    @Column(name = "ship_closing_amount_nh")
    var shipClosingAmountNh: Double? = null,

    @SFField("ShipClosingAmount1__c")
    @Column(name = "ship_closing_amount1")
    var shipClosingAmount1: Double? = null,

    @SFField("ShipClosingAmount2__c")
    @Column(name = "ship_closing_amount2")
    var shipClosingAmount2: Double? = null,

    @SFField("ShipClosingAmount3__c")
    @Column(name = "ship_closing_amount3")
    var shipClosingAmount3: Double? = null,

    @SFField("ShipClosingAmount4__c")
    @Column(name = "ship_closing_amount4")
    var shipClosingAmount4: Double? = null,

    // SF 메타 정합: type=double precision=18 scale=0 (물류마감실적_합계). non-calculated 실저장 합계 필드.
    @SFField("ShipClosingSumAmount__c")
    @Column(name = "ship_closing_sum_amount")
    var shipClosingSumAmount: Double? = null,

    @SFField("ABCClosingAmount4__c")
    @Column(name = "abc_closing_amount4")
    var abcClosingAmount4: Double? = null,

    // SF 메타 정합: type=double precision=18 scale=0 (전산마감실적_합계). non-calculated 실저장 합계 필드.
    @SFField("ABCClosingSumAmount__c")
    @Column(name = "abc_closing_sum_amount")
    var abcClosingSumAmount: Double? = null,

    @SFField("LastMonthTargetByHand__c")
    @Column(name = "last_month_target_by_hand", precision = 18, scale = 0)
    var lastMonthTargetByHand: BigDecimal? = null,

    @SFField("ThisMonthTarget__c")
    @Column(name = "this_month_target", precision = 18, scale = 0)
    var thisMonthTarget: BigDecimal? = null,

    // -- Group A audit/owner (OwnerId polymorphic R-2 + Audit FK Employee→User 전환) --

    @SFField("OwnerId")
    @Column(name = "owner_sfid", length = 18)
    var ownerSfid: String? = null,

    @SFField("CreatedById")
    @Column(name = "created_by_sfid", length = 18)
    var createdBySfid: String? = null,

    @SFField("LastModifiedById")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

    // -- Relations --

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    var account: Account? = null,

    // OwnerId polymorphic R-2 (referenceTo = [Group, User]) — owner_user / owner_group XOR
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    var ownerUser: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_group_id")
    var ownerGroup: Group? = null,

    @CreatedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: User? = null,

    @LastModifiedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_monthly_sales_history_id")
    var lastMonthlySalesHistory: MonthlySalesHistory? = null,
) : BaseEntity()
