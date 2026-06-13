package com.otoki.powersales.domain.sales.entity

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.entity.OwnerUserDefaultListener
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.employee.entity.Group
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy

/**
 * 거래처목표등록마스터 Entity (SF `SalesProgressRateMaster__c`).
 *
 * 거래처별 월 목표 금액(상온/냉동냉장/라면/유지) + 당월·전월 매출 실적을 보관하는 마스터.
 * 데이터 권위: SF (HC sync 대상 아님 — Heroku PG 미존재. SF → RDS 단방향 마이그레이션).
 *
 * Formula 필드(6개)는 SOQL 적재 불가 + DB 컬럼 미추가 정책:
 * - AccountCode__c / AccountName__c / AccountType__c / AccoutBranchName__c (Account__r lookup)
 * - ProgressRate__c (CurrentMonthSalesAmount / TargetSum)
 * - TargetSum__c (RT+FR+RM+FO 합계) — 필요 시 응용 코드에서 산출
 */
@EntityListeners(OwnerUserDefaultListener::class)
@Entity
@Table(name = "sales_progress_rate_master")
@SFObject("SalesProgressRateMaster__c")
class SalesProgressRateMaster(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sales_progress_rate_master_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @Column(name = "name", length = 80)
    val name: String? = null,

    @SFField("AccountCDUpl__c")
    @Column(name = "account_cd_upl", length = 255)
    var accountCdUpl: String? = null,

    @SFField("BusinessRate__c")
    @Column(name = "business_rate")
    var businessRate: Double? = null,

    @SFField("CurrentMonthSalesAmount__c")
    @Column(name = "current_month_sales_amount")
    var currentMonthSalesAmount: Double? = null,

    @SFField("ExternalKey__c")
    @Column(name = "external_key", unique = true, length = 255)
    var externalKey: String? = null,

    @SFField("FOTartgetAmount__c")
    @Column(name = "fo_target_amount")
    var foTargetAmount: Double? = null,

    @SFField("FRTargetAmount__c")
    @Column(name = "fr_target_amount")
    var frTargetAmount: Double? = null,

    @SFField("PreviousMonthSalesAmount__c")
    @Column(name = "previous_month_sales_amount")
    var previousMonthSalesAmount: Double? = null,

    @SFField("RMTartgetAmount__c")
    @Column(name = "rm_target_amount")
    var rmTargetAmount: Double? = null,

    @SFField("RTTargetAmount__c")
    @Column(name = "rt_target_amount")
    var rtTargetAmount: Double? = null,

    @SFField("TargetMonth__c")
    @Column(name = "target_month", length = 100)
    var targetMonth: String? = null,

    // SF Label "합계 목표(미사용)" — 운영 미사용 컬럼이나 적재 정합 위해 보존.
    @SFField("TargetSumAmount__c")
    @Column(name = "target_sum_amount")
    var targetSumAmount: Double? = null,

    @SFField("TargetYear__c")
    @Column(name = "target_year", length = 100)
    var targetYear: String? = null,

    @SFField("accountbranchView__c")
    @Column(name = "account_branch_view", length = 100)
    var accountBranchView: String? = null,

    @SFField("AccountBranchCode__c")
    @Column(name = "account_branch_code", length = 255)
    var accountBranchCode: String? = null,

    @SFField("IsDeleted")
    @Column(name = "is_deleted")
    val isDeleted: Boolean? = null,

    @SFField("Account__c")
    @Column(name = "account_sfid", length = 18)
    var accountSfid: String? = null,

    // -- Group A audit/owner (OwnerId polymorphic R-2 + Audit FK User) --

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
) : BaseEntity()
