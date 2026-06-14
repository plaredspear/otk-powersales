package com.otoki.powersales.domain.activity.claim.entity

import com.otoki.powersales.platform.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate

/**
 * 클레임 등록 임시저장(draft) 엔티티.
 *
 * 사원이 클레임을 작성하다가 임시저장한 입력값을 보관한다.
 * 레거시 `salesforce2.tmp_claim` 대응. 정식 등록(클레임 생성) 성공 시 이 draft 는 삭제된다.
 * 사원(employee) 1건당 draft 1건(unique). [com.otoki.powersales.domain.activity.promotion.entity.DailySalesDraft] 패턴 정합.
 *
 * SF 와 동기화되지 않는 로컬 전용 테이블이므로 [com.otoki.powersales.platform.common.salesforce.SFObject] 어노테이션을 두지 않는다.
 *
 * 임시저장은 검증을 건너뛰므로 거래처/제품/종류 등 모든 입력 필드가 nullable 이다.
 * 거래처명/제품명은 prefill 표시용으로 저장 시점 값을 함께 보관한다(form-data 에 없는 정보).
 */
@Entity
@Table(name = "claim_draft")
class ClaimDraft(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "claim_draft_id")
    val id: Long = 0,

    @Column(name = "employee_id", nullable = false)
    val employeeId: Long,

    @Column(name = "account_id")
    var accountId: Long? = null,

    @Column(name = "account_name", length = 255)
    var accountName: String? = null,

    @Column(name = "product_code", length = 50)
    var productCode: String? = null,

    @Column(name = "product_name", length = 255)
    var productName: String? = null,

    @Column(name = "date_type", length = 30)
    var dateType: String? = null,

    @Column(name = "claim_date")
    var claimDate: LocalDate? = null,

    @Column(name = "claim_type1", length = 10)
    var claimType1: String? = null,

    @Column(name = "claim_type2", length = 10)
    var claimType2: String? = null,

    @Column(name = "defect_description", length = 4000)
    var defectDescription: String? = null,

    @Column(name = "defect_quantity")
    var defectQuantity: BigDecimal? = null,

    @Column(name = "purchase_amount")
    var purchaseAmount: BigDecimal? = null,

    @Column(name = "purchase_method_code", length = 10)
    var purchaseMethodCode: String? = null,

    @Column(name = "request_type_code", length = 255)
    var requestTypeCode: String? = null,

    @Column(name = "defect_photo_key", length = 255)
    var defectPhotoKey: String? = null,

    @Column(name = "label_photo_key", length = 255)
    var labelPhotoKey: String? = null,

    @Column(name = "receipt_photo_key", length = 255)
    var receiptPhotoKey: String? = null,

) : BaseEntity() {

    /** 입력값 일괄 반영 (이미지 키는 별도 처리). */
    fun apply(
        accountId: Long?,
        accountName: String?,
        productCode: String?,
        productName: String?,
        dateType: String?,
        claimDate: LocalDate?,
        claimType1: String?,
        claimType2: String?,
        defectDescription: String?,
        defectQuantity: BigDecimal?,
        purchaseAmount: BigDecimal?,
        purchaseMethodCode: String?,
        requestTypeCode: String?
    ) {
        this.accountId = accountId
        this.accountName = accountName
        this.productCode = productCode
        this.productName = productName
        this.dateType = dateType
        this.claimDate = claimDate
        this.claimType1 = claimType1
        this.claimType2 = claimType2
        this.defectDescription = defectDescription
        this.defectQuantity = defectQuantity
        this.purchaseAmount = purchaseAmount
        this.purchaseMethodCode = purchaseMethodCode
        this.requestTypeCode = requestTypeCode
    }
}
