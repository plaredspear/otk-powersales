package com.otoki.powersales.domain.activity.suggestion.entity

import com.otoki.powersales.domain.activity.suggestion.entity.converter.SuggestionActionStatusConverter
import com.otoki.powersales.domain.activity.suggestion.entity.converter.SuggestionCategoryConverter
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.entity.Group
import com.otoki.powersales.domain.foundation.product.entity.Product
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import java.time.LocalDate
import java.time.LocalDateTime
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import com.otoki.powersales.platform.common.entity.OwnerUserDefaultListener
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

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
@EntityListeners(OwnerUserDefaultListener::class)
@DomainName("제안")
@Entity
@Table(name = "suggestion")
@SFObject("DKRetail__Proposal__c")
class Suggestion(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("제안ID")
    @Column(name = "suggestion_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @FieldName("제안번호")
    @Column(name = "proposal_number", nullable = false, length = 80, unique = true)
    val proposalNumber: String,

    @SFField("DKRetail__Title__c")
    @FieldName("제목")
    // SF nillable=true 정합 — 마이그레이션 SF NULL row 보존.
    @Column(name = "title", length = 250)
    var title: String? = null,

    @SFField("DKRetail__Description__c")
    @FieldName("제안내용")
    // SF nillable=true 정합 — 마이그레이션 SF NULL row 보존.
    @Column(name = "content", columnDefinition = "text")
    var content: String? = null,

    @Convert(converter = SuggestionCategoryConverter::class)
    @SFField("Category__c")
    @FieldName("제안구분")
    // SF nillable=true 정합 — 마이그레이션 SF NULL row 보존.
    @Column(name = "category", length = 255)
    var category: SuggestionCategory? = null,

    // -- Spec #849: deprecated SF Text(40) `DKRetail__Category__c` (제안구분 구버전 텍스트) 부활. plain String 원본 보존. --
    @SFField("DKRetail__Category__c")
    @FieldName("제안구분(DK)")
    @Column(name = "dk_category", length = 40)
    var dkCategory: String? = null,

    // SF Text(40) `DKRetail__Type__c` (제안유형). plain String 원본 보존.
    @SFField("DKRetail__Type__c")
    @FieldName("제안유형")
    @Column(name = "type", length = 40)
    var type: String? = null,

    @SFField("Category1__c")
    @FieldName("대분류")
    @Column(name = "category1", length = 255)
    var category1: String? = null,

    @SFField("Category2__c")
    @FieldName("중분류")
    @Column(name = "category2", length = 255)
    var category2: String? = null,

    @SFField("Category3__c")
    @FieldName("소분류")
    @Column(name = "category3", length = 255)
    var category3: String? = null,

    @SFField("DKRetail__SAPAccountCode__c")
    @FieldName("SAP거래처코드")
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

    @SFField("OrgCostCenterCode__c")
    @FieldName("조직코스트센터코드")
    @Column(name = "org_cost_center_code", length = 100)
    var orgCostCenterCode: String? = null,

    /** SF CostCenterCode__c (라벨 "조직유형") — 등록 사원 소속 코스트센터코드 원본. 데이터 보존용 (신규 조회 권한 필터엔 미사용). */
    @SFField("CostCenterCode__c")
    @FieldName("조직유형")
    @Column(name = "cost_center_code", length = 255)
    var costCenterCode: String? = null,

    @SFField("CarNumber__c")
    @FieldName("물류 차량번호")
    @Column(name = "car_number", length = 255)
    var carNumber: String? = null,

    @SFField("ClaimDate__c")
    @FieldName("물류 클레임 발생일자")
    @Column(name = "claim_date")
    var claimDate: LocalDate? = null,

    @SFField("ClaimType__c")
    @FieldName("클레임 항목")
    @Column(name = "claim_type", length = 255)
    var claimType: String? = null,

    @SFField("ClaimTypeMeasures__c")
    @FieldName("클레임 항목(조치사항)")
    @Column(name = "claim_type_measures", length = 255)
    var claimTypeMeasures: String? = null,

    @SFField("LogisticsResponsibility__c")
    @FieldName("물류책임")
    @Column(name = "logistics_responsibility", length = 255)
    var logisticsResponsibility: String? = null,

    @SFField("WERK1_TEXT2__c")
    @FieldName("접수 물류센터")
    @Column(name = "reception_logistics_center", length = 255)
    var receptionLogisticsCenter: String? = null,

    @SFField("WERK3_TEXT2__c")
    @FieldName("책임 물류센터")
    @Column(name = "responsible_logistics_center", length = 255)
    var responsibleLogisticsCenter: String? = null,

    @Enumerated(EnumType.STRING)
    @FieldName("상태")
    @Column(name = "status", nullable = false, length = 20)
    var status: SuggestionStatus = SuggestionStatus.SUBMITTED,

    @Convert(converter = SuggestionActionStatusConverter::class)
    @SFField("ActionStatus__c")
    @FieldName("조치상태")
    @Column(name = "action_status", length = 30)
    var actionStatus: SuggestionActionStatus? = null,

    // Spec #833 sf-align-suggestion-action-fields — SF describe 정합 도입
    @SFField("ActionContent__c")
    @FieldName("조치내용")
    @Column(name = "action_content", columnDefinition = "text")
    var actionContent: String? = null,

    @SFField("ActionManager__c")
    @FieldName("조치담당자(직급/이름)")
    @Column(name = "action_manager", length = 200)
    var actionManager: String? = null,

    @SFField("ActionNum__c")
    @FieldName("조치번호")
    @Column(name = "action_num", length = 30)
    var actionNum: String? = null,

    @SFField("DuplicateProposalNum__c")
    @FieldName("중복 제안번호")
    @Column(name = "duplicate_proposal_num", length = 255)
    var duplicateProposalNum: String? = null,

    @SFField("IsDeleted")
    @FieldName("삭제여부")
    @Column(name = "is_deleted", nullable = false)
    var isDeleted: Boolean = false,

    // -- Audit / Owner sfid sync buffer (V192) — Stage1 적재 채움, Stage2-A FK resolve 가 id 컬럼으로 변환 --

    @SFField("OwnerId")
    @Column(name = "owner_sfid", length = 18)
    var ownerSfid: String? = null,

    @SFField("CreatedById")
    @Column(name = "created_by_sfid", length = 18)
    var createdBySfid: String? = null,

    @SFField("LastModifiedById")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

    // -- SF outbound 전송상태 추적 (mobile 제안/물류클레임 등록 dual-write — IF_REST_MOBILE_ProposalRegist).
    //    SF 매핑 없음(@SFField 미부착) — backend 내부 lifecycle 전용 (클레임 dual-write 정합). --

    @Enumerated(EnumType.STRING)
    @FieldName("SF전송상태")
    @Column(name = "sf_send_status", length = 20)
    var sfSendStatus: SuggestionSfSendStatus? = null,

    @FieldName("SF전송시각")
    @Column(name = "sf_sent_at")
    var sfSentAt: LocalDateTime? = null,

    @FieldName("SF전송실패사유")
    @Column(name = "sf_send_fail_message", length = 1000)
    var sfSendFailMessage: String? = null,

    @FieldName("SF전송시도횟수")
    @Column(name = "sf_send_attempt_count", nullable = false)
    var sfSendAttemptCount: Int = 0,

    // -- Relations --

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    @NotFound(action = NotFoundAction.IGNORE)
    val account: Account? = null,

    // Stage1 적재 시점에는 NULL (Stage2-A 에서 채워짐). 신규 INSERT 는 service create() 가 필수 검증.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    @NotFound(action = NotFoundAction.IGNORE)
    val employee: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    @NotFound(action = NotFoundAction.IGNORE)
    var product: Product? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    @NotFound(action = NotFoundAction.IGNORE)
    var ownerUser: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_group_id")
    @NotFound(action = NotFoundAction.IGNORE)
    var ownerGroup: Group? = null,

    @CreatedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    @NotFound(action = NotFoundAction.IGNORE)
    var createdBy: User? = null,

    @LastModifiedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    @NotFound(action = NotFoundAction.IGNORE)
    var lastModifiedBy: User? = null,
) : BaseEntity()
