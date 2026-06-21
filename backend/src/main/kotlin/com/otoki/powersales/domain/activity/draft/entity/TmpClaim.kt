package com.otoki.powersales.domain.activity.draft.entity

import com.otoki.powersales.platform.common.salesforce.HCColumn
import com.otoki.powersales.platform.common.salesforce.HerokuOnly
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime
import com.otoki.powersales.platform.common.entity.AuditedEntity
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName
@DomainName("임시저장 클레임")
@Entity
@Table(name = "tmp_claim")
@HerokuOnly("tmp_claim")
class TmpClaim(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("임시저장클레임ID")
    @Column(name = "tmp_claim_id")
    val id: Long = 0,

    @HCColumn("tmp_sapaccountname")
    @FieldName("SAP거래처명")
    @Column(name = "sap_account_name", length = 80)
    var tmpSapAccountName: String? = null,

    @HCColumn("tmp_sapaccountcode")
    @FieldName("거래처코드")
    @Column(name = "sap_account_code", length = 80)
    var tmpSapAccountCode: String? = null,

    @HCColumn("tmp_productname")
    @FieldName("제품명")
    @Column(name = "product_name", length = 100)
    var tmpProductName: String? = null,

    @HCColumn("tmp_productcode")
    @FieldName("제품코드")
    @Column(name = "product_code", length = 80)
    var tmpProductCode: String? = null,

    @HCColumn("tmp_expirationdate")
    @FieldName("유통기한")
    @Column(name = "expiration_date", length = 80)
    var tmpExpirationDate: String? = null,

    @HCColumn("tmp_claimtype1")
    @FieldName("클레임종류1")
    @Column(name = "claim_type1", length = 80)
    var tmpClaimType1: String? = null,

    @HCColumn("tmp_claimtype2")
    @FieldName("클레임종류2")
    @Column(name = "claim_type2", length = 80)
    var tmpClaimType2: String? = null,

    @HCColumn("tmp_description")
    @FieldName("행사대체제품")
    @Column(name = "description", columnDefinition = "TEXT")
    var tmpDescription: String? = null,

    @HCColumn("tmp_quantity")
    @FieldName("수량")
    @Column(name = "quantity", length = 80)
    var tmpQuantity: String? = null,

    @HCColumn("tmp_claimimagefilename")
    @FieldName("클레임이미지파일명")
    @Column(name = "claim_image_file_name", length = 200)
    var tmpClaimImageFileName: String? = null,

    @HCColumn("tmp_partimagefilename")
    @FieldName("부품이미지파일명")
    @Column(name = "part_image_file_name", length = 200)
    var tmpPartImageFileName: String? = null,

    @HCColumn("tmp_amount")
    @FieldName("총금액")
    @Column(name = "amount", length = 80)
    var tmpAmount: String? = null,

    @HCColumn("tmp_purchasemethod")
    @FieldName("구매방법")
    @Column(name = "purchase_method", length = 80)
    var tmpPurchaseMethod: String? = null,

    @HCColumn("tmp_receiptimagefilename")
    @FieldName("영수증이미지파일명")
    @Column(name = "receipt_image_file_name", length = 200)
    var tmpReceiptImageFileName: String? = null,

    @HCColumn("tmp_requesttype")
    @FieldName("요청유형")
    @Column(name = "request_type", columnDefinition = "TEXT")
    var tmpRequestType: String? = null,

    @HCColumn("tmp_employeecode")
    @FieldName("사번")
    @Column(name = "employee_code", length = 80)
    var tmpEmployeeCode: String? = null,

    @FieldName("거래처ID")
    @Column(name = "account_id")
    var accountId: Long? = null,

    @FieldName("사원ID")
    @Column(name = "employee_id")
    var employeeId: Long? = null,

    @FieldName("제품ID")
    @Column(name = "product_id")
    var productId: Long? = null,

    @HCColumn("tmp_claimtype1_name")
    @FieldName("클레임유형1명")
    @Column(name = "claim_type1_name", length = 80)
    var tmpClaimType1Name: String? = null,

    @HCColumn("tmp_claimtype2_name")
    @FieldName("클레임유형2명")
    @Column(name = "claim_type2_name", length = 80)
    var tmpClaimType2Name: String? = null,

    @HCColumn("tmp_purchasecode")
    @FieldName("구매처코드")
    @Column(name = "purchase_code", length = 80)
    var tmpPurchaseCode: String? = null,

    @HCColumn("tmp_claimimageextension")
    @FieldName("클레임이미지확장자")
    @Column(name = "claim_image_extension", length = 80)
    var tmpClaimImageExtension: String? = null,

    @HCColumn("tmp_partimageextension")
    @FieldName("부품이미지확장자")
    @Column(name = "part_image_extension", length = 80)
    var tmpPartImageExtension: String? = null,

    @HCColumn("tmp_receiptimageextension")
    @FieldName("영수증이미지확장자")
    @Column(name = "receipt_image_extension", length = 80)
    var tmpReceiptImageExtension: String? = null,

    @HCColumn("tmp_receiptimagebuffer")
    @FieldName("영수증이미지버퍼")
    @Column(name = "receipt_image_buffer", columnDefinition = "TEXT")
    var tmpReceiptImageBuffer: String? = null,

    @HCColumn("tmp_partimagebuffer")
    @FieldName("부품이미지버퍼")
    @Column(name = "part_image_buffer", columnDefinition = "TEXT")
    var tmpPartImageBuffer: String? = null,

    @HCColumn("tmp_claimimagebuffer")
    @FieldName("클레임이미지버퍼")
    @Column(name = "claim_image_buffer", columnDefinition = "TEXT")
    var tmpClaimImageBuffer: String? = null,

    @HCColumn("tmp_manufacturingdate")
    @FieldName("제조일자")
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