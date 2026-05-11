package com.otoki.powersales.claim.entity

import com.otoki.powersales.claim.entity.converter.ClaimChannelConverter
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.employee.entity.Employee
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
        Index(name = "idx_claim_store", columnList = "store_id")
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
    @JoinColumn(name = "store_id", nullable = false)
    val account: Account,

    @Column(name = "store_name", nullable = false, length = 100)
    val accountName: String,

    @SFField("DKRetail__ProductCode__c")
    @Column(name = "product_code", nullable = false, length = 20)
    val productCode: String,

    @Column(name = "product_name", nullable = false, length = 200)
    val productName: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "date_type", nullable = false, length = 20)
    val dateType: ClaimDateType,

    @SFField("DKRetail__ClaimDate__c")
    @Column(name = "date", nullable = false)
    val date: LocalDate,

    // Spec #606: FK 컬럼은 @SFField 미부착 (§2.4/§6.6 정책).
    // SF picklist `DKRetail__ClaimType1__c`/`ClaimType2__c` 의 entity 매핑은
    // 후속 별도 스펙 (마스터 정합) 에서 처리.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    val category: ClaimCategory,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subcategory_id", nullable = false)
    val subcategory: ClaimSubcategory,

    @SFField("DKRetail__Description__c")
    @Column(name = "defect_description", nullable = false, length = 1000)
    val defectDescription: String,

    @SFField("DKRetail__Quantity__c")
    @Column(name = "defect_quantity", nullable = false)
    val defectQuantity: Int,

    @SFField("DKRetail__Amount__c")
    @Column(name = "purchase_amount")
    val purchaseAmount: Int? = null,

    @SFField("DKRetail__PurchaseMethod__c")
    @Column(name = "purchase_method_code", length = 10)
    val purchaseMethodCode: String? = null,

    @Column(name = "purchase_method_name", length = 50)
    val purchaseMethodName: String? = null,

    @SFField("DKRetail__RequestType__c")
    @Column(name = "request_type_code", length = 10)
    val requestTypeCode: String? = null,

    @Column(name = "request_type_name", length = 50)
    val requestTypeName: String? = null,

    @SFField("DKRetail__Status__c")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    val status: ClaimStatus = ClaimStatus.SUBMITTED,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

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
    var actionDate: LocalDateTime? = null
)
