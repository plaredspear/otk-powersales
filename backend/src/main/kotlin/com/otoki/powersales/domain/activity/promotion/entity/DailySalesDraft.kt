package com.otoki.powersales.domain.activity.promotion.entity

import com.otoki.powersales.platform.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

/**
 * 일매출 마감 임시저장(draft) 엔티티.
 *
 * 여사원이 행사 일매출 마감을 작성하다가 임시저장한 입력값을 보관한다.
 * 레거시 `salesforce2.tmp_promotion` 대응. 최종 마감 시 [PromotionEmployee]
 * 본 row 에 값이 반영되고 이 draft 는 삭제된다. PromotionEmployee 1건당 draft 1건(unique).
 *
 * SF 와 동기화되지 않는 로컬 전용 테이블이므로 [com.otoki.powersales.platform.common.salesforce.SFObject] 어노테이션을 두지 않는다.
 */
@DomainName("일매출마감 임시저장")
@Entity
@Table(name = "daily_sales_draft")
class DailySalesDraft(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("일매출마감임시저장ID")
    @Column(name = "daily_sales_draft_id")
    val id: Long = 0,

    @FieldName("행사사원ID")
    @Column(name = "promotion_employee_id", nullable = false)
    val promotionEmployeeId: Long,

    @FieldName("사원ID")
    @Column(name = "employee_id", nullable = false)
    val employeeId: Long,

    @FieldName("기준단가")
    @Column(name = "base_price")
    var basePrice: BigDecimal? = null,

    @FieldName("대표품목판매수량")
    @Column(name = "primary_sales_quantity")
    var primarySalesQuantity: BigDecimal? = null,

    @FieldName("대표품목판매단가")
    @Column(name = "primary_sales_price")
    var primarySalesPrice: BigDecimal? = null,

    @FieldName("대표품목 매출")
    @Column(name = "primary_product_amount")
    var primaryProductAmount: BigDecimal? = null,

    @FieldName("기타판매수량")
    @Column(name = "other_sales_quantity")
    var otherSalesQuantity: BigDecimal? = null,

    @FieldName("기타매출금금액")
    @Column(name = "other_sales_amount")
    var otherSalesAmount: BigDecimal? = null,

    @FieldName("행사대체제품")
    @Column(name = "description", length = 50)
    var description: String? = null,

    @FieldName("S3ImageUniqueKey")
    @Column(name = "s3_image_unique_key", length = 255)
    var s3ImageUniqueKey: String? = null,

) : BaseEntity() {

    /** 입력값 일괄 반영 (이미지 키는 별도 처리). */
    fun apply(
        basePrice: BigDecimal?,
        primarySalesQuantity: BigDecimal?,
        primarySalesPrice: BigDecimal?,
        primaryProductAmount: BigDecimal?,
        otherSalesQuantity: BigDecimal?,
        otherSalesAmount: BigDecimal?,
        description: String?
    ) {
        this.basePrice = basePrice
        this.primarySalesQuantity = primarySalesQuantity
        this.primarySalesPrice = primarySalesPrice
        this.primaryProductAmount = primaryProductAmount
        this.otherSalesQuantity = otherSalesQuantity
        this.otherSalesAmount = otherSalesAmount
        this.description = description
    }
}
