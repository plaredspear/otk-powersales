package com.otoki.powersales.suggestion.entity

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.suggestion.entity.converter.SuggestionActionStatusConverter
import com.otoki.powersales.suggestion.entity.converter.SuggestionCategoryConverter
import jakarta.persistence.*
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import java.time.LocalDate

/**
 * 제안 Entity — Salesforce `DKRetail__Proposal__c` (제안사항) 매핑.
 *
 * ## 레거시 매핑
 * - SF Apex: `IF_REST_MOBILE_ProposalRegist.cls#doPost`, `ProposalTriggerHandler.cls#beforeInsertProposal`
 * - SF Picklist `Category__c` 3값: 신제품 제안 / 기존제품 상품가치 향상 / 물류 클레임
 * - origin spec: #664 P1-B
 *
 * ## 어노테이션 정책
 * `@SFObject` + `@SFField` 만 부착 — SF 매핑 entity 에 `@HerokuOnly` / `@HCColumn` 부착은 정책상 금지
 * (`backend-conventions.md` §"Heroku Connect 어노테이션 정책").
 *
 * ## Q3 옵션 B (레거시 동등 — R17 WERK bug 재현)
 * `reception_logistics_center` / `responsible_logistics_center` 분기는 service 단에서 양쪽 동일 값을 set.
 * 정정 X — 별 스펙으로 분리 결정.
 */
@Entity
@Table(name = "suggestion")
@SFObject("DKRetail__Proposal__c")
class Suggestion(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "suggestion_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @Column(name = "proposal_number", nullable = false, length = 80, unique = true)
    val proposalNumber: String,

    @SFField("DKRetail__Title__c")
    @Column(name = "title", nullable = false, length = 250)
    var title: String,

    @SFField("DKRetail__Description__c")
    @Column(name = "content", nullable = false, columnDefinition = "text")
    var content: String,

    @Convert(converter = SuggestionCategoryConverter::class)
    @SFField("Category__c")
    @Column(name = "category", nullable = false, length = 50)
    var category: SuggestionCategory,

    @SFField("Category1__c")
    @Column(name = "category1", length = 255)
    var category1: String? = null,

    @SFField("Category2__c")
    @Column(name = "category2", length = 255)
    var category2: String? = null,

    @SFField("Category3__c")
    @Column(name = "category3", length = 255)
    var category3: String? = null,

    @SFField("DKRetail__SAPAccountCode__c")
    @Column(name = "sap_account_code", length = 100)
    var sapAccountCode: String? = null,

    @SFField("DKRetail__EmployeeId__c")
    @Column(name = "employee_sfid", length = 18)
    val employeeSfid: String? = null,

    @SFField("AccountId__c")
    @Column(name = "account_sfid", length = 18)
    val accountSfid: String? = null,

    @SFField("DKRetail__ProductId__c")
    @Column(name = "product_sfid", length = 18)
    val productSfid: String? = null,

    @SFField("ProductCode__c")
    @Column(name = "product_code", length = 20)
    var productCode: String? = null,

    @SFField("OrgCostCenterCode__c")
    @Column(name = "org_cost_center_code", length = 100)
    var orgCostCenterCode: String? = null,

    @SFField("CarNumber__c")
    @Column(name = "car_number", length = 20)
    var carNumber: String? = null,

    @SFField("ClaimDate__c")
    @Column(name = "claim_date")
    var claimDate: LocalDate? = null,

    @SFField("ClaimType__c")
    @Column(name = "claim_type", length = 200)
    var claimType: String? = null,

    @SFField("ClaimTypeMeasures__c")
    @Column(name = "claim_type_measures", length = 200)
    var claimTypeMeasures: String? = null,

    @SFField("LogisticsResponsibility__c")
    @Column(name = "logistics_responsibility", length = 20)
    var logisticsResponsibility: String? = null,

    @SFField("WERK1_TEXT2__c")
    @Column(name = "reception_logistics_center", length = 255)
    var receptionLogisticsCenter: String? = null,

    @SFField("WERK3_TEXT2__c")
    @Column(name = "responsible_logistics_center", length = 255)
    var responsibleLogisticsCenter: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: SuggestionStatus = SuggestionStatus.SUBMITTED,

    @Convert(converter = SuggestionActionStatusConverter::class)
    @SFField("ActionStatus__c")
    @Column(name = "action_status", length = 30)
    var actionStatus: SuggestionActionStatus? = null,

    @SFField("DuplicateProposalNum__c")
    @Column(name = "duplicate_proposal_num", length = 255)
    var duplicateProposalNum: String? = null,

    @Column(name = "is_deleted", nullable = false)
    var isDeleted: Boolean = false,

    // -- Relations --

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    @NotFound(action = NotFoundAction.IGNORE)
    val account: Account? = null,

    // Stage1 적재 시점에는 NULL (Stage2-A 에서 채워짐). 신규 INSERT 는 service create() 가 필수 검증.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    @NotFound(action = NotFoundAction.IGNORE)
    val employee: Employee? = null
) : BaseEntity()
