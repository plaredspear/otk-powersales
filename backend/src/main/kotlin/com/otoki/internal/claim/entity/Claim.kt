package com.otoki.internal.claim.entity

import com.otoki.internal.common.salesforce.SFField
import com.otoki.internal.common.salesforce.SFObject
import com.otoki.internal.sap.entity.Account
import com.otoki.internal.sap.entity.Employee
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

    @SFField("DKRetail__ClaimType1__c")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    val category: ClaimCategory,

    @SFField("DKRetail__ClaimType2__c")
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
    val createdAt: LocalDateTime = LocalDateTime.now()
)
