package com.otoki.powersales.domain.activity.inspection.entity

import com.otoki.powersales.platform.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

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
@DomainName("현장점검 임시저장")
@Entity
@Table(name = "site_activity_draft")
class SiteActivityDraft(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("현장점검임시저장ID")
    @Column(name = "site_activity_draft_id")
    val id: Long = 0,

    @FieldName("사원ID")
    @Column(name = "employee_id", nullable = false)
    val employeeId: Long,

    @FieldName("테마ID")
    @Column(name = "theme_id")
    var themeId: Long? = null,

    @FieldName("제안구분")
    @Column(name = "category", length = 20)
    var category: String? = null,

    @FieldName("거래처ID")
    @Column(name = "account_id")
    var accountId: Long? = null,

    @FieldName("거래처명")
    @Column(name = "account_name", length = 255)
    var accountName: String? = null,

    @FieldName("점검일자")
    @Column(name = "inspection_date")
    var inspectionDate: LocalDate? = null,

    @FieldName("현장유형코드")
    @Column(name = "field_type_code", length = 30)
    var fieldTypeCode: String? = null,

    @FieldName("행사대체제품")
    @Column(name = "description", length = 4000)
    var description: String? = null,

    @FieldName("제품코드")
    @Column(name = "product_code", length = 50)
    var productCode: String? = null,

    @FieldName("제품명")
    @Column(name = "product_name", length = 255)
    var productName: String? = null,

    @FieldName("경쟁사명")
    @Column(name = "competitor_name", length = 255)
    var competitorName: String? = null,

    @FieldName("경쟁사활동내용")
    @Column(name = "competitor_activity", length = 4000)
    var competitorActivity: String? = null,

    @FieldName("경쟁사시식여부")
    @Column(name = "competitor_tasting")
    var competitorTasting: Boolean? = null,

    @FieldName("경쟁사 상품명")
    @Column(name = "competitor_product_name", length = 255)
    var competitorProductName: String? = null,

    @FieldName("경쟁사제품가격")
    @Column(name = "competitor_product_price")
    var competitorProductPrice: Int? = null,

    @FieldName("경쟁사판매수량")
    @Column(name = "competitor_sales_quantity")
    var competitorSalesQuantity: Int? = null,

    @FieldName("사진키1")
    @Column(name = "photo_key_1", length = 255)
    var photoKey1: String? = null,

    @FieldName("사진키2")
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
