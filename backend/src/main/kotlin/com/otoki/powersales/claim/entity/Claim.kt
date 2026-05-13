package com.otoki.powersales.claim.entity

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.claim.entity.converter.ClaimChannelConverter
import com.otoki.powersales.claim.entity.converter.ClaimStatusConverter
import com.otoki.powersales.claim.entity.converter.ClaimType1Converter
import com.otoki.powersales.claim.entity.converter.ClaimType2Converter
import com.otoki.powersales.claim.entity.converter.PurchaseMethodConverter
import com.otoki.powersales.claim.entity.converter.RequestTypeSetConverter
import com.otoki.powersales.claim.entity.sfpicklist.PurchaseMethod
import com.otoki.powersales.claim.entity.sfpicklist.RequestType
import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.product.entity.Product
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 클레임 Entity
 * 사용자가 등록한 클레임 정보를 관리한다.
 */
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
    @Column(name = "claim_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @SFField("DKRetail__EmployeeId__c")
    @Column(name = "employee_sfid", length = 18)
    val employeeSfid: String? = null,

    @SFField("DKRetail__AccountId__c")
    @Column(name = "account_sfid", length = 18)
    val accountSfid: String? = null,

    // -- Relations --

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    val employee: Employee,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    val account: Account,

    @Column(name = "store_name", nullable = false, length = 100)
    val accountName: String,

    @SFField("DKRetail__ProductCode__c")
    @Column(name = "product_code", nullable = false, length = 1300)
    val productCode: String,

    @Column(name = "product_name", nullable = false, length = 200)
    val productName: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "date_type", nullable = false, length = 20)
    val dateType: ClaimDateType,

    @SFField("DKRetail__ClaimDate__c")
    @Column(name = "date", nullable = false)
    val date: LocalDate,

    // Spec #743: SF picklist `DKRetail__ClaimType1__c`/`ClaimType2__c` 직접 enum 매핑.
    // 기존 ClaimCategory/ClaimSubcategory FK 제거 (#741 옵션 C 적용).
    // 계층 제약 (claimType2.parent == claimType1) 은 service-layer 에서 검증.
    @SFField("DKRetail__ClaimType1__c")
    @Convert(converter = ClaimType1Converter::class)
    @Column(name = "claim_type1", nullable = false, length = 10)
    val claimType1: ClaimType1,

    @SFField("DKRetail__ClaimType2__c")
    @Convert(converter = ClaimType2Converter::class)
    @Column(name = "claim_type2", nullable = false, length = 10)
    val claimType2: ClaimType2,

    @SFField("DKRetail__Description__c")
    @Column(name = "defect_description", nullable = false, length = 4000)
    val defectDescription: String,

    @SFField("DKRetail__Quantity__c")
    @Column(name = "defect_quantity", nullable = false)
    val defectQuantity: Long,

    @SFField("DKRetail__Amount__c")
    @Column(name = "purchase_amount")
    val purchaseAmount: Long? = null,

    @SFField("DKRetail__PurchaseMethod__c")
    @Column(name = "purchase_method_code", length = 255)
    @Convert(converter = PurchaseMethodConverter::class)
    val purchaseMethodCode: PurchaseMethod? = null,

    @Column(name = "purchase_method_name", length = 50)
    val purchaseMethodName: String? = null,

    @SFField("DKRetail__RequestType__c")
    @Column(name = "request_type_code", length = 4099)
    @Convert(converter = RequestTypeSetConverter::class)
    val requestTypeCode: Set<RequestType> = emptySet(),

    @Column(name = "request_type_name", length = 50)
    val requestTypeName: String? = null,

    // Spec #705: SF Status picklist 정합 — DB 저장값 = SF 한국어 원본 (임시저장/전송완료/전송실패).
    // 신규 application 신규 클레임 디폴트 = DRAFT (임시저장).
    @SFField("DKRetail__Status__c")
    @Convert(converter = ClaimStatusConverter::class)
    @Column(name = "status", nullable = false, length = 20)
    val status: ClaimStatus = ClaimStatus.DRAFT,

    // -- SAP 인바운드 갱신 컬럼 (Spec #561) --

    @SFField("Name")
    @Column(name = "name", length = 50)
    var name: String? = null,

    @SFField("DKRetail__counselNumber__c")
    @Column(name = "counsel_number", length = 30)
    var counselNumber: String? = null,

    @SFField("DKRetail__ActionCode__c")
    @Column(name = "action_code", length = 20)
    var actionCode: String? = null,

    @SFField("DKRetail__ActionStatus__c")
    @Column(name = "action_status", length = 50)
    var actionStatus: String? = null,

    @SFField("ActContent__c")
    @Column(name = "act_content", columnDefinition = "text")
    var actContent: String? = null,

    @SFField("DKRetail__ReasonType__c")
    @Column(name = "reason_type", length = 20)
    var reasonType: String? = null,

    @SFField("DKRetail__CosmosKey__c")
    @Column(name = "cosmos_key", length = 50)
    var cosmosKey: String? = null,

    // -- Spec #606: SF 누락 컬럼 16개 신규 도입 --

    @SFField("DKRetail__ProductId__c")
    @Column(name = "product_sfid", length = 18)
    var productSfid: String? = null,

    // -- Spec #747 카테고리 A — 도메인 핵심 누락 D 분류 (SF len=1300 캐시) --

    @SFField("DKRetail__Barcode__c")
    @Column(name = "barcode", length = 1300)
    var barcode: String? = null,

    @SFField("DKRetail__Phone__c")
    @Column(name = "phone", length = 1300)
    var phone: String? = null,

    @SFField("DKRetail__ProductStatus__c")
    @Column(name = "product_status", length = 1300)
    var productStatus: String? = null,

    @SFField("ReturnOrderNumber__c")
    @Column(name = "customer_delivery_date")
    var customerDeliveryDate: LocalDate? = null,

    @SFField("DKRetail__ReturnOrderNumber__c")
    @Column(name = "return_order_number", length = 100)
    var returnOrderNumber: String? = null,

    @SFField("DKRetail__ExpirationDate__c")
    @Column(name = "expiration_date")
    var expirationDate: LocalDate? = null,

    @SFField("DKRetail__InterfaceDate__c")
    @Column(name = "interface_date")
    var interfaceDate: LocalDateTime? = null,

    @SFField("DKRetail__ManufacturingDate__c")
    @Column(name = "manufacturing_date")
    var manufacturingDate: LocalDate? = null,

    @SFField("DKRetail__InitialClaim__c")
    @Column(name = "initial_claim", length = 250)
    var initialClaim: String? = null,

    @SFField("DKRetail__LogisticsCenter__c")
    @Column(name = "logistics_center", length = 50)
    var logisticsCenter: String? = null,

    @SFField("ClaimSequence__c")
    @Column(name = "claim_sequence", length = 255)
    var claimSequence: String? = null,

    @SFField("DKRetail__DetailSNSName__c")
    @Column(name = "detail_sns_name", length = 250)
    var detailSnsName: String? = null,

    @SFField("CostCenterCode__c")
    @Column(name = "cost_center_code", length = 100)
    var costCenterCode: String? = null,

    @SFField("division__c")
    @Column(name = "division", length = 100)
    var division: String? = null,

    @SFField("DKRetail__Channel__c")
    @Convert(converter = ClaimChannelConverter::class)
    @Column(name = "channel", length = 20)
    var channel: ClaimChannel? = null,

    @SFField("DKRetail__SampleCollectionFlag__c")
    @Column(name = "sample_collection_flag")
    var sampleCollectionFlag: Boolean? = null,

    @SFField("ImageCount__c")
    @Column(name = "image_count", length = 10)
    var imageCount: String? = null,

    @SFField("DKRetail__ActionDate__c")
    @Column(name = "action_date")
    var actionDate: LocalDateTime? = null,

    // -- Spec #705: Group A — IsDeleted --

    @SFField("IsDeleted")
    @HCColumn("isdeleted")
    @Column(name = "is_deleted", nullable = false)
    var isDeleted: Boolean = false,

    // -- Spec #705: Reference R-2 (OwnerId / CreatedById / LastModifiedById sfid + Employee FK) --
    // *_sfid: Heroku Connect sync / SalesforceMigrationTool 이 채우는 buffer (SF User Id).
    // *_by / owner: SF User → Employee 매핑 결과 FK.

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    var owner: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: Employee? = null,

    // -- Spec #705: Reference R-2 (ProductId FK 신규 추가) --
    // 기존 product_sfid 는 유지 (SF 식별자 buffer). product_code 는 SAP 제품 코드 (별도 식별자).

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    var product: Product? = null
) : BaseEntity()
