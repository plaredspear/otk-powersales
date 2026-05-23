package com.otoki.powersales.draft.entity

import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HerokuOnly
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime
import com.otoki.powersales.common.entity.AuditedEntity
@Entity
@Table(name = "tmp_claim")
@HerokuOnly("tmp_claim")
class TmpClaim(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tmp_claim_id")
    val id: Long = 0,

    @HCColumn("tmp_sapaccountname")
    @Column(name = "sap_account_name", length = 80)
    var tmpSapAccountName: String? = null,

    @HCColumn("tmp_sapaccountcode")
    @Column(name = "sap_account_code", length = 80)
    var tmpSapAccountCode: String? = null,

    @HCColumn("tmp_productname")
    @Column(name = "product_name", length = 100)
    var tmpProductName: String? = null,

    @HCColumn("tmp_productcode")
    @Column(name = "product_code", length = 80)
    var tmpProductCode: String? = null,

    @HCColumn("tmp_expirationdate")
    @Column(name = "expiration_date", length = 80)
    var tmpExpirationDate: String? = null,

    @HCColumn("tmp_claimtype1")
    @Column(name = "claim_type1", length = 80)
    var tmpClaimType1: String? = null,

    @HCColumn("tmp_claimtype2")
    @Column(name = "claim_type2", length = 80)
    var tmpClaimType2: String? = null,

    @HCColumn("tmp_description")
    @Column(name = "description", columnDefinition = "TEXT")
    var tmpDescription: String? = null,

    @HCColumn("tmp_quantity")
    @Column(name = "quantity", length = 80)
    var tmpQuantity: String? = null,

    @HCColumn("tmp_claimimagefilename")
    @Column(name = "claim_image_file_name", length = 200)
    var tmpClaimImageFileName: String? = null,

    @HCColumn("tmp_partimagefilename")
    @Column(name = "part_image_file_name", length = 200)
    var tmpPartImageFileName: String? = null,

    @HCColumn("tmp_amount")
    @Column(name = "amount", length = 80)
    var tmpAmount: String? = null,

    @HCColumn("tmp_purchasemethod")
    @Column(name = "purchase_method", length = 80)
    var tmpPurchaseMethod: String? = null,

    @HCColumn("tmp_receiptimagefilename")
    @Column(name = "receipt_image_file_name", length = 200)
    var tmpReceiptImageFileName: String? = null,

    @HCColumn("tmp_requesttype")
    @Column(name = "request_type", columnDefinition = "TEXT")
    var tmpRequestType: String? = null,

    @HCColumn("tmp_employeecode")
    @Column(name = "employee_code", length = 80)
    var tmpEmployeeCode: String? = null,

    @Column(name = "account_id")
    var accountId: Long? = null,

    @Column(name = "employee_id")
    var employeeId: Long? = null,

    @Column(name = "product_id")
    var productId: Long? = null,

    @HCColumn("tmp_claimtype1_name")
    @Column(name = "claim_type1_name", length = 80)
    var tmpClaimType1Name: String? = null,

    @HCColumn("tmp_claimtype2_name")
    @Column(name = "claim_type2_name", length = 80)
    var tmpClaimType2Name: String? = null,

    @HCColumn("tmp_purchasecode")
    @Column(name = "purchase_code", length = 80)
    var tmpPurchaseCode: String? = null,

    @HCColumn("tmp_claimimageextension")
    @Column(name = "claim_image_extension", length = 80)
    var tmpClaimImageExtension: String? = null,

    @HCColumn("tmp_partimageextension")
    @Column(name = "part_image_extension", length = 80)
    var tmpPartImageExtension: String? = null,

    @HCColumn("tmp_receiptimageextension")
    @Column(name = "receipt_image_extension", length = 80)
    var tmpReceiptImageExtension: String? = null,

    @HCColumn("tmp_receiptimagebuffer")
    @Column(name = "receipt_image_buffer", columnDefinition = "TEXT")
    var tmpReceiptImageBuffer: String? = null,

    @HCColumn("tmp_partimagebuffer")
    @Column(name = "part_image_buffer", columnDefinition = "TEXT")
    var tmpPartImageBuffer: String? = null,

    @HCColumn("tmp_claimimagebuffer")
    @Column(name = "claim_image_buffer", columnDefinition = "TEXT")
    var tmpClaimImageBuffer: String? = null,

    @HCColumn("tmp_manufacturingdate")
    @Column(name = "manufacturing_date", length = 80)
    var tmpManufacturingDate: String? = null,

    @HCColumn("inst_date")
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @HCColumn("upd_date")
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) : AuditedEntity()