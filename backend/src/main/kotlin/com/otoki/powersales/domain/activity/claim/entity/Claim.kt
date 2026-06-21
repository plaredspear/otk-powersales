package com.otoki.powersales.domain.activity.claim.entity

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.activity.claim.entity.converter.ClaimChannelConverter
import com.otoki.powersales.domain.activity.claim.entity.converter.ClaimStatusConverter
import com.otoki.powersales.domain.activity.claim.entity.converter.ClaimType1Converter
import com.otoki.powersales.domain.activity.claim.entity.converter.ClaimType2Converter
import com.otoki.powersales.domain.activity.claim.entity.converter.PurchaseMethodConverter
import com.otoki.powersales.domain.activity.claim.entity.converter.RequestTypeSetConverter
import com.otoki.powersales.domain.activity.claim.entity.sfpicklist.PurchaseMethod
import com.otoki.powersales.domain.activity.claim.entity.sfpicklist.RequestType
import com.otoki.powersales.domain.activity.claim.enums.ClaimChannel
import com.otoki.powersales.domain.activity.claim.enums.ClaimDateType
import com.otoki.powersales.domain.activity.claim.enums.ClaimStatus
import com.otoki.powersales.domain.activity.claim.enums.ClaimType1
import com.otoki.powersales.domain.activity.claim.enums.ClaimType2
import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.entity.Group
import com.otoki.powersales.domain.foundation.product.entity.Product
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import com.otoki.powersales.platform.common.entity.OwnerUserDefaultListener
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

/**
 * 클레임 Entity
 * 사용자가 등록한 클레임 정보를 관리한다.
 */
@EntityListeners(OwnerUserDefaultListener::class)
@DomainName("클레임")
@Entity
@Table(
    name = "claim",
    indexes = [
        Index(name = "idx_claim_employee_created", columnList = "employee_id,created_at"),
        Index(name = "idx_claim_account", columnList = "account_id")
    ]
)
@SFObject("DKRetail__Claim__c")
class Claim(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("클레임ID")
    @Column(name = "claim_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("DKRetail__EmployeeId__c")
    @Column(name = "employee_sfid", length = 18)
    val employeeSfid: String? = null,

    @SFField("DKRetail__AccountId__c")
    @Column(name = "account_sfid", length = 18)
    val accountSfid: String? = null,

    // -- Relations --

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    val employee: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    var account: Account? = null,

    @Enumerated(EnumType.STRING)
    @FieldName("일자유형")
    @Column(name = "date_type", length = 20)
    var dateType: ClaimDateType? = null,

    @SFField("DKRetail__ClaimDate__c")
    @FieldName("발생일자")
    @Column(name = "date", nullable = false)
    var date: LocalDate,

    // Spec #743: SF picklist `DKRetail__ClaimType1__c`/`ClaimType2__c` 직접 enum 매핑.
    // 기존 ClaimCategory/ClaimSubcategory FK 제거 (#741 옵션 C 적용).
    // 계층 제약 (claimType2.parent == claimType1) 은 service-layer 에서 검증.
    @SFField("DKRetail__ClaimType1__c")
    @Convert(converter = ClaimType1Converter::class)
    @FieldName("클레임종류1")
    @Column(name = "claim_type1", nullable = false, length = 10)
    var claimType1: ClaimType1,

    @SFField("DKRetail__ClaimType2__c")
    @Convert(converter = ClaimType2Converter::class)
    @FieldName("클레임종류2")
    @Column(name = "claim_type2", nullable = false, length = 10)
    var claimType2: ClaimType2,

    @SFField("DKRetail__Description__c")
    @FieldName("불만내역")
    @Column(name = "defect_description", nullable = false, length = 4000)
    var defectDescription: String,

    @SFField("DKRetail__Quantity__c")
    @FieldName("불량수량")
    @Column(name = "defect_quantity", nullable = false)
    var defectQuantity: BigDecimal,

    @SFField("DKRetail__Amount__c")
    @FieldName("금액(원)")
    @Column(name = "purchase_amount")
    var purchaseAmount: BigDecimal? = null,

    @SFField("DKRetail__PurchaseMethod__c")
    @FieldName("구매방법")
    @Column(name = "purchase_method_code", length = 255)
    @Convert(converter = PurchaseMethodConverter::class)
    var purchaseMethodCode: PurchaseMethod? = null,

    @SFField("DKRetail__RequestType__c")
    @FieldName("요청사항")
    @Column(name = "request_type_code", length = 4099)
    @Convert(converter = RequestTypeSetConverter::class)
    var requestTypeCode: Set<RequestType> = emptySet(),

    // Spec #705: SF Status picklist 정합 — DB 저장값 = SF 한국어 원본 (임시저장/전송완료/전송실패).
    // 신규 application 신규 클레임 디폴트 = DRAFT (임시저장).
    @SFField("DKRetail__Status__c")
    @Convert(converter = ClaimStatusConverter::class)
    @FieldName("상태")
    @Column(name = "status", nullable = false, length = 20)
    var status: ClaimStatus = ClaimStatus.DRAFT,

    // -- SAP 인바운드 갱신 컬럼 (Spec #561) --

    // Q5: SF Name (string 80) 정합 — 절단 위험 회피 (was VARCHAR(50)).
    @SFField("Name")
    @FieldName("이름")
    @Column(name = "name", length = 80)
    var name: String? = null,

    // Q7: SF DKRetail__counselNumber__c (string 40) 정합 — 절단 위험 회피 (was VARCHAR(30)).
    @SFField("DKRetail__counselNumber__c")
    @FieldName("상담번호")
    @Column(name = "counsel_number", length = 40)
    var counselNumber: String? = null,

    // Q6: SF DKRetail__ActionCode__c (string 40) 정합 — 절단 위험 회피 (was VARCHAR(20)).
    @SFField("DKRetail__ActionCode__c")
    @FieldName("조치코드")
    @Column(name = "action_code", length = 40)
    var actionCode: String? = null,

    // SF DKRetail__ActionStatus__c (string 10) 정합 — VARCHAR(50) → VARCHAR(10).
    @SFField("DKRetail__ActionStatus__c")
    @FieldName("조치상태")
    @Column(name = "action_status", length = 10)
    var actionStatus: String? = null,

    // SF ActContent__c (textarea 2000) 정합 — TEXT → VARCHAR(2000).
    @SFField("ActContent__c")
    @FieldName("조치내용")
    @Column(name = "act_content", length = 2000)
    var actContent: String? = null,

    // SF DKRetail__ReasonType__c (string 100) 정합 — 절단 위험 회피 (was VARCHAR(20)).
    @SFField("DKRetail__ReasonType__c")
    @FieldName("원인별 분류")
    @Column(name = "reason_type", length = 100)
    var reasonType: String? = null,

    // SF DKRetail__CosmosKey__c (string 40) 정합 — VARCHAR(50) → VARCHAR(40).
    @SFField("DKRetail__CosmosKey__c")
    @FieldName("클레임접수번호(COSMOS)")
    @Column(name = "cosmos_key", length = 40)
    var cosmosKey: String? = null,

    // -- Spec #606: SF 누락 컬럼 16개 신규 도입 --

    @SFField("DKRetail__ProductId__c")
    @Column(name = "product_sfid", length = 18)
    var productSfid: String? = null,

    @SFField("ReturnOrderNumber__c")
    @FieldName("거래처납품일자")
    @Column(name = "customer_delivery_date")
    var customerDeliveryDate: LocalDate? = null,

    @SFField("DKRetail__ReturnOrderNumber__c")
    @FieldName("반품오더번호")
    @Column(name = "return_order_number", length = 100)
    var returnOrderNumber: String? = null,

    @SFField("DKRetail__ExpirationDate__c")
    @FieldName("유통기한")
    @Column(name = "expiration_date")
    var expirationDate: LocalDate? = null,

    @SFField("DKRetail__InterfaceDate__c")
    @FieldName("전송일자")
    @Column(name = "interface_date")
    var interfaceDate: LocalDateTime? = null,

    @SFField("DKRetail__ManufacturingDate__c")
    @FieldName("제조일자")
    @Column(name = "manufacturing_date")
    var manufacturingDate: LocalDate? = null,

    @SFField("DKRetail__InitialClaim__c")
    @FieldName("최초불만")
    @Column(name = "initial_claim", length = 250)
    var initialClaim: String? = null,

    @SFField("DKRetail__LogisticsCenter__c")
    @FieldName("출고처")
    @Column(name = "logistics_center", length = 50)
    var logisticsCenter: String? = null,

    @SFField("ClaimSequence__c")
    @FieldName("COSMOS 상담번호")
    @Column(name = "claim_sequence", length = 255)
    var claimSequence: String? = null,

    @SFField("DKRetail__DetailSNSName__c")
    @FieldName("세부점포명")
    @Column(name = "detail_sns_name", length = 250)
    var detailSnsName: String? = null,

    @SFField("CostCenterCode__c")
    @FieldName("CC Code")
    @Column(name = "cost_center_code", length = 100)
    var costCenterCode: String? = null,

    @SFField("division__c")
    @FieldName("부서")
    @Column(name = "division", length = 100)
    var division: String? = null,

    @SFField("DKRetail__Channel__c")
    @Convert(converter = ClaimChannelConverter::class)
    @FieldName("접수채널")
    @Column(name = "channel", length = 20)
    var channel: ClaimChannel? = null,

    @SFField("DKRetail__SampleCollectionFlag__c")
    @FieldName("샘플회수여부")
    @Column(name = "sample_collection_flag")
    var sampleCollectionFlag: Boolean? = null,

    @SFField("ImageCount__c")
    @FieldName("첨부이미지개수")
    @Column(name = "image_count", length = 10)
    var imageCount: String? = null,

    @SFField("DKRetail__ActionDate__c")
    @FieldName("조치일시")
    @Column(name = "action_date")
    var actionDate: LocalDateTime? = null,

    // -- Spec #705: Group A — IsDeleted --

    @SFField("IsDeleted")
    @FieldName("삭제여부")
    @Column(name = "is_deleted", nullable = false)
    var isDeleted: Boolean = false,

    // -- Spec sf-meta-diff Q2/Q3/Q4: Reference R-2 정합 --
    // Q2: OwnerId (`referenceTo = [Group, User]` polymorphic) → owner_sfid + owner_user (User?) + owner_group (Group?) + CHECK XOR.
    // Q3/Q4: CreatedById/LastModifiedById (`referenceTo = [User]`) → audit FK 타입 Employee → User 전환.
    // *_sfid: sync buffer (SF Id). sf-migrate Phase 2 가 `<관계>_sfid` → `user.sfid` / `group.sfid` lookup 으로 FK 채움.

    @SFField("OwnerId")
    @Column(name = "owner_sfid", length = 18)
    var ownerSfid: String? = null,

    @SFField("CreatedById")
    @Column(name = "created_by_sfid", length = 18)
    var createdBySfid: String? = null,

    @SFField("LastModifiedById")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

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

    // -- Spec #705: Reference R-2 (ProductId FK 신규 추가) --
    // 기존 product_sfid 는 유지 (SF 식별자 buffer). product_code 는 SAP 제품 코드 (별도 식별자).

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    var product: Product? = null,

    // -- Spec #829: SF outbound dual-write 추적 (web admin 등록 경로 한정) --
    // SF 매핑 없음 — backend 내부 lifecycle 만 관리.

    @FieldName("전송시각")
    @Column(name = "sent_at")
    var sentAt: LocalDateTime? = null,

    @FieldName("전송실패메시지")
    @Column(name = "send_fail_message", length = 1000)
    var sendFailMessage: String? = null,

    @FieldName("전송시도횟수")
    @Column(name = "send_attempt_count", nullable = false)
    var sendAttemptCount: Int = 0
) : BaseEntity()
