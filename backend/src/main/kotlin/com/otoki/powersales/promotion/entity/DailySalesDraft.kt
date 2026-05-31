package com.otoki.powersales.promotion.entity

import com.otoki.powersales.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

/**
 * 일매출 마감 임시저장(draft) 엔티티.
 *
 * 여사원이 행사 일매출 마감을 작성하다가 임시저장한 입력값을 보관한다.
 * 레거시 `salesforce2.tmp_promotion` 대응. 최종 마감 시 [com.otoki.powersales.promotion.entity.PromotionEmployee]
 * 본 row 에 값이 반영되고 이 draft 는 삭제된다. PromotionEmployee 1건당 draft 1건(unique).
 *
 * SF 와 동기화되지 않는 로컬 전용 테이블이므로 [com.otoki.powersales.common.salesforce.SFObject] 어노테이션을 두지 않는다.
 */
@Entity
@Table(name = "daily_sales_draft")
class DailySalesDraft(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "daily_sales_draft_id")
    val id: Long = 0,

    @Column(name = "promotion_employee_id", nullable = false)
    val promotionEmployeeId: Long,

    @Column(name = "employee_id", nullable = false)
    val employeeId: Long,

    @Column(name = "base_price")
    var basePrice: BigDecimal? = null,

    @Column(name = "primary_sales_quantity")
    var primarySalesQuantity: BigDecimal? = null,

    @Column(name = "primary_sales_price")
    var primarySalesPrice: BigDecimal? = null,

    @Column(name = "primary_product_amount")
    var primaryProductAmount: BigDecimal? = null,

    @Column(name = "other_sales_quantity")
    var otherSalesQuantity: BigDecimal? = null,

    @Column(name = "other_sales_amount")
    var otherSalesAmount: BigDecimal? = null,

    @Column(name = "description", length = 50)
    var description: String? = null,

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
