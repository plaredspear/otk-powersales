package com.otoki.powersales.draft.entity

import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HerokuOnly
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime
import com.otoki.powersales.common.entity.AuditedEntity
@Entity
@Table(name = "tmp_promotion")
@HerokuOnly("tmp_promotion")
class TmpPromotion(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tmp_promotion_id")
    val id: Long = 0,

    @HCColumn("tmp_employeecode")
    @Column(name = "employee_code", length = 80)
    var tmpEmployeeCode: String? = null,

    @HCColumn("tmp_promotiontype")
    @Column(name = "promotion_type", length = 80)
    var tmpPromotionType: String? = null,

    @HCColumn("tmp_promotionname")
    @Column(name = "promotion_name", length = 100)
    var tmpPromotionName: String? = null,

    @HCColumn("tmp_promotionproductname")
    @Column(name = "promotion_product_name", length = 80)
    var tmpPromotionProductName: String? = null,

    @HCColumn("tmp_promotionproductcode")
    @Column(name = "promotion_product_code", length = 80)
    var tmpPromotionProductCode: String? = null,

    @HCColumn("tmp_baseprice")
    @Column(name = "base_price", length = 80)
    var tmpBasePrice: String? = null,

    @HCColumn("tmp_primaryquantity")
    @Column(name = "primary_quantity", length = 80)
    var tmpPrimaryQuantity: String? = null,

    @HCColumn("tmp_otherproduct")
    @Column(name = "other_product", length = 80)
    var tmpOtherProduct: String? = null,

    @HCColumn("tmp_otherquantity")
    @Column(name = "other_quantity", length = 80)
    var tmpOtherQuantity: String? = null,

    @HCColumn("tmp_othertotalamount")
    @Column(name = "other_total_amount", length = 80)
    var tmpOtherTotalAmount: String? = null,

    @HCColumn("tmp_imageurl")
    @Column(name = "image_url", length = 200)
    var tmpImageUrl: String? = null,

    @HCColumn("tmp_promotion_id")
    @Column(name = "heroku_promotion_id", length = 80)
    var tmpHerokuPromotionId: String? = null,

    @HCColumn("tmp_promotion_seq")
    @Column(name = "promotion_seq", length = 80)
    var tmpPromotionSeq: String? = null,

    @HCColumn("tmp_imagefilename")
    @Column(name = "image_file_name", length = 80)
    var tmpImageFileName: String? = null,

    @HCColumn("tmp_otherchangeproduct")
    @Column(name = "other_change_product", length = 80)
    var tmpOtherChangeProduct: String? = null,

    @Column(name = "employee_id")
    var employeeId: Long? = null,

    @Column(name = "product_id")
    var productId: Long? = null,

    @HCColumn("inst_date")
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @HCColumn("upd_date")
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) : AuditedEntity()