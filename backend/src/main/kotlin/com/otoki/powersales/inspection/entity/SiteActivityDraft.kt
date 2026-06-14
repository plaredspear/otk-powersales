package com.otoki.powersales.inspection.entity

import com.otoki.powersales.platform.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate

/**
 * 현장점검 등록 임시저장(draft) 엔티티.
 *
 * 사원이 현장점검을 작성하다가 임시저장한 입력값을 보관한다.
 * 레거시 `salesforce2.tmp_onsite` 대응. 정식 등록(현장점검 생성) 성공 시 이 draft 는 삭제된다.
 * 사원(employee) 1건당 draft 1건(unique). [com.otoki.powersales.domain.activity.claim.entity.ClaimDraft] 패턴 정합.
 *
 * SF 와 동기화되지 않는 로컬 전용 테이블이므로 [com.otoki.powersales.platform.common.salesforce.SFObject] 어노테이션을 두지 않는다.
 *
 * 임시저장은 검증을 건너뛰므로 테마/거래처/현장유형 등 모든 입력 필드가 nullable 이다.
 * 거래처명/제품명은 prefill 표시용으로 저장 시점 값을 함께 보관한다(form-data 에 없는 정보).
 * 사진은 S3 키 2개(최대 2장)만 보관한다.
 */
@Entity
@Table(name = "site_activity_draft")
class SiteActivityDraft(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "site_activity_draft_id")
    val id: Long = 0,

    @Column(name = "employee_id", nullable = false)
    val employeeId: Long,

    @Column(name = "theme_id")
    var themeId: Long? = null,

    @Column(name = "category", length = 20)
    var category: String? = null,

    @Column(name = "account_id")
    var accountId: Long? = null,

    @Column(name = "account_name", length = 255)
    var accountName: String? = null,

    @Column(name = "inspection_date")
    var inspectionDate: LocalDate? = null,

    @Column(name = "field_type_code", length = 30)
    var fieldTypeCode: String? = null,

    @Column(name = "description", length = 4000)
    var description: String? = null,

    @Column(name = "product_code", length = 50)
    var productCode: String? = null,

    @Column(name = "product_name", length = 255)
    var productName: String? = null,

    @Column(name = "competitor_name", length = 255)
    var competitorName: String? = null,

    @Column(name = "competitor_activity", length = 4000)
    var competitorActivity: String? = null,

    @Column(name = "competitor_tasting")
    var competitorTasting: Boolean? = null,

    @Column(name = "competitor_product_name", length = 255)
    var competitorProductName: String? = null,

    @Column(name = "competitor_product_price")
    var competitorProductPrice: Int? = null,

    @Column(name = "competitor_sales_quantity")
    var competitorSalesQuantity: Int? = null,

    @Column(name = "photo_key_1", length = 255)
    var photoKey1: String? = null,

    @Column(name = "photo_key_2", length = 255)
    var photoKey2: String? = null,

) : BaseEntity() {

    /** 입력값 일괄 반영 (사진 키는 별도 처리). */
    fun apply(
        themeId: Long?,
        category: String?,
        accountId: Long?,
        accountName: String?,
        inspectionDate: LocalDate?,
        fieldTypeCode: String?,
        description: String?,
        productCode: String?,
        productName: String?,
        competitorName: String?,
        competitorActivity: String?,
        competitorTasting: Boolean?,
        competitorProductName: String?,
        competitorProductPrice: Int?,
        competitorSalesQuantity: Int?
    ) {
        this.themeId = themeId
        this.category = category
        this.accountId = accountId
        this.accountName = accountName
        this.inspectionDate = inspectionDate
        this.fieldTypeCode = fieldTypeCode
        this.description = description
        this.productCode = productCode
        this.productName = productName
        this.competitorName = competitorName
        this.competitorActivity = competitorActivity
        this.competitorTasting = competitorTasting
        this.competitorProductName = competitorProductName
        this.competitorProductPrice = competitorProductPrice
        this.competitorSalesQuantity = competitorSalesQuantity
    }
}
