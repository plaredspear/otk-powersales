package com.otoki.internal.sap.entity

import com.otoki.internal.common.entity.BaseEntity
import com.otoki.internal.common.salesforce.HCColumn
import com.otoki.internal.common.salesforce.HCTable
import com.otoki.internal.common.sap.SAPSource
import com.otoki.internal.common.sap.SyncMode
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime

@Entity
@Table(name = "tmp_claim")
@SAPSource(api = "/sap/ClaimReceive", syncMode = SyncMode.UPDATE_ONLY)
@HCTable("tmp_claim")
class TmpClaim(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @HCColumn("tmp_sapaccountname")
    @Column(name = "tmp_sapaccountname", length = 80)
    var tmpSapAccountName: String? = null,

    @HCColumn("tmp_sapaccountcode")
    @Column(name = "tmp_sapaccountcode", length = 80)
    var tmpSapAccountCode: String? = null,

    @HCColumn("tmp_productname")
    @Column(name = "tmp_productname", length = 100)
    var tmpProductName: String? = null,

    @HCColumn("tmp_productcode")
    @Column(name = "tmp_productcode", length = 80)
    var tmpProductCode: String? = null,

    @HCColumn("tmp_expirationdate")
    @Column(name = "tmp_expirationdate", length = 80)
    var tmpExpirationDate: String? = null,

    @HCColumn("tmp_claimtype1")
    @Column(name = "tmp_claimtype1", length = 80)
    var tmpClaimType1: String? = null,

    @HCColumn("tmp_claimtype2")
    @Column(name = "tmp_claimtype2", length = 80)
    var tmpClaimType2: String? = null,

    @HCColumn("tmp_description")
    @Column(name = "tmp_description", columnDefinition = "TEXT")
    var tmpDescription: String? = null,

    @HCColumn("tmp_quantity")
    @Column(name = "tmp_quantity", length = 80)
    var tmpQuantity: String? = null,

    @HCColumn("tmp_claimimagefilename")
    @Column(name = "tmp_claimimagefilename", length = 200)
    var tmpClaimImageFileName: String? = null,

    @HCColumn("tmp_partimagefilename")
    @Column(name = "tmp_partimagefilename", length = 200)
    var tmpPartImageFileName: String? = null,

    @HCColumn("tmp_amount")
    @Column(name = "tmp_amount", length = 80)
    var tmpAmount: String? = null,

    @HCColumn("tmp_purchasemethod")
    @Column(name = "tmp_purchasemethod", length = 80)
    var tmpPurchaseMethod: String? = null,

    @HCColumn("tmp_receiptimagefilename")
    @Column(name = "tmp_receiptimagefilename", length = 200)
    var tmpReceiptImageFileName: String? = null,

    @HCColumn("tmp_requesttype")
    @Column(name = "tmp_requesttype", columnDefinition = "TEXT")
    var tmpRequestType: String? = null,

    @HCColumn("tmp_employeecode")
    @Column(name = "tmp_employeecode", length = 80)
    var tmpEmployeeCode: String? = null,

    @HCColumn("tmp_claimtype1_name")
    @Column(name = "tmp_claimtype1_name", length = 80)
    var tmpClaimType1Name: String? = null,

    @HCColumn("tmp_claimtype2_name")
    @Column(name = "tmp_claimtype2_name", length = 80)
    var tmpClaimType2Name: String? = null,

    @HCColumn("tmp_purchasecode")
    @Column(name = "tmp_purchasecode", length = 80)
    var tmpPurchaseCode: String? = null,

    @HCColumn("tmp_claimimageextension")
    @Column(name = "tmp_claimimageextension", length = 80)
    var tmpClaimImageExtension: String? = null,

    @HCColumn("tmp_partimageextension")
    @Column(name = "tmp_partimageextension", length = 80)
    var tmpPartImageExtension: String? = null,

    @HCColumn("tmp_receiptimageextension")
    @Column(name = "tmp_receiptimageextension", length = 80)
    var tmpReceiptImageExtension: String? = null,

    @HCColumn("tmp_receiptimagebuffer")
    @Column(name = "tmp_receiptimagebuffer", columnDefinition = "TEXT")
    var tmpReceiptImageBuffer: String? = null,

    @HCColumn("tmp_partimagebuffer")
    @Column(name = "tmp_partimagebuffer", columnDefinition = "TEXT")
    var tmpPartImageBuffer: String? = null,

    @HCColumn("tmp_claimimagebuffer")
    @Column(name = "tmp_claimimagebuffer", columnDefinition = "TEXT")
    var tmpClaimImageBuffer: String? = null,

    @HCColumn("tmp_manufacturingdate")
    @Column(name = "tmp_manufacturingdate", length = 80)
    var tmpManufacturingDate: String? = null,

    // SAP 연동 컬럼
    @Column(name = "claim_name", length = 80)
    var claimName: String? = null,

    @Column(name = "claim_sequence", length = 80)
    var claimSequence: String? = null,

    @Column(name = "action_code", length = 20)
    var actionCode: String? = null,

    @Column(name = "claim_status", length = 40)
    var claimStatus: String? = null,

    @Column(name = "claim_content", columnDefinition = "TEXT")
    var claimContent: String? = null,

    @Column(name = "reason_type", length = 80)
    var reasonType: String? = null,

    @Column(name = "cosmos_key", length = 80)
    var cosmosKey: String? = null,

    @HCColumn("inst_date")
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    override var createdAt: LocalDateTime = LocalDateTime.now(),

    @HCColumn("upd_date")
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    override var updatedAt: LocalDateTime = LocalDateTime.now()
) : BaseEntity()
