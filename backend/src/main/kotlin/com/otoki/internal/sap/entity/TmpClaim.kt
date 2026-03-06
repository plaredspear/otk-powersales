package com.otoki.internal.sap.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "tmp_claim")
class TmpClaim(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "tmp_sapaccountname", length = 80)
    var tmpSapAccountName: String? = null,

    @Column(name = "tmp_sapaccountcode", length = 80)
    var tmpSapAccountCode: String? = null,

    @Column(name = "tmp_productname", length = 100)
    var tmpProductName: String? = null,

    @Column(name = "tmp_productcode", length = 80)
    var tmpProductCode: String? = null,

    @Column(name = "tmp_expirationdate", length = 80)
    var tmpExpirationDate: String? = null,

    @Column(name = "tmp_claimtype1", length = 80)
    var tmpClaimType1: String? = null,

    @Column(name = "tmp_claimtype2", length = 80)
    var tmpClaimType2: String? = null,

    @Column(name = "tmp_description", columnDefinition = "TEXT")
    var tmpDescription: String? = null,

    @Column(name = "tmp_quantity", length = 80)
    var tmpQuantity: String? = null,

    @Column(name = "tmp_claimimagefilename", length = 200)
    var tmpClaimImageFileName: String? = null,

    @Column(name = "tmp_partimagefilename", length = 200)
    var tmpPartImageFileName: String? = null,

    @Column(name = "tmp_amount", length = 80)
    var tmpAmount: String? = null,

    @Column(name = "tmp_purchasemethod", length = 80)
    var tmpPurchaseMethod: String? = null,

    @Column(name = "tmp_receiptimagefilename", length = 200)
    var tmpReceiptImageFileName: String? = null,

    @Column(name = "tmp_requesttype", columnDefinition = "TEXT")
    var tmpRequestType: String? = null,

    @Column(name = "inst_date")
    var instDate: LocalDateTime? = null,

    @Column(name = "upd_date")
    var updDate: LocalDateTime? = null,

    @Column(name = "tmp_employeecode", length = 80)
    var tmpEmployeeCode: String? = null,

    @Column(name = "tmp_claimtype1_name", length = 80)
    var tmpClaimType1Name: String? = null,

    @Column(name = "tmp_claimtype2_name", length = 80)
    var tmpClaimType2Name: String? = null,

    @Column(name = "tmp_purchasecode", length = 80)
    var tmpPurchaseCode: String? = null,

    @Column(name = "tmp_claimimageextension", length = 80)
    var tmpClaimImageExtension: String? = null,

    @Column(name = "tmp_partimageextension", length = 80)
    var tmpPartImageExtension: String? = null,

    @Column(name = "tmp_receiptimageextension", length = 80)
    var tmpReceiptImageExtension: String? = null,

    @Column(name = "tmp_receiptimagebuffer", columnDefinition = "TEXT")
    var tmpReceiptImageBuffer: String? = null,

    @Column(name = "tmp_partimagebuffer", columnDefinition = "TEXT")
    var tmpPartImageBuffer: String? = null,

    @Column(name = "tmp_claimimagebuffer", columnDefinition = "TEXT")
    var tmpClaimImageBuffer: String? = null,

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
    var cosmosKey: String? = null
)
