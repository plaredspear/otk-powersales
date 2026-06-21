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
@DomainName("임시저장 행사")
@Entity
@Table(name = "tmp_promotion")
@HerokuOnly("tmp_promotion")
class TmpPromotion(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("임시저장행사ID")
    @Column(name = "tmp_promotion_id")
    val id: Long = 0,

    @HCColumn("tmp_employeecode")
    @FieldName("사번")
    @Column(name = "employee_code", length = 80)
    var tmpEmployeeCode: String? = null,

    @HCColumn("tmp_promotiontype")
    @FieldName("행사유형")
    @Column(name = "promotion_type", length = 80)
    var tmpPromotionType: String? = null,

    @HCColumn("tmp_promotionname")
    @FieldName("행사명")
    @Column(name = "promotion_name", length = 100)
    var tmpPromotionName: String? = null,

    @HCColumn("tmp_promotionproductname")
    @FieldName("행사품목명")
    @Column(name = "promotion_product_name", length = 80)
    var tmpPromotionProductName: String? = null,

    @HCColumn("tmp_promotionproductcode")
    @FieldName("행사품목코드")
    @Column(name = "promotion_product_code", length = 80)
    var tmpPromotionProductCode: String? = null,

    @HCColumn("tmp_baseprice")
    @FieldName("기준단가")
    @Column(name = "base_price", length = 80)
    var tmpBasePrice: String? = null,

    @HCColumn("tmp_primaryquantity")
    @FieldName("기본수량")
    @Column(name = "primary_quantity", length = 80)
    var tmpPrimaryQuantity: String? = null,

    @HCColumn("tmp_otherproduct")
    @FieldName("기타제품")
    @Column(name = "other_product", length = 80)
    var tmpOtherProduct: String? = null,

    @HCColumn("tmp_otherquantity")
    @FieldName("증정수량")
    @Column(name = "other_quantity", length = 80)
    var tmpOtherQuantity: String? = null,

    @HCColumn("tmp_othertotalamount")
    @FieldName("증정총액")
    @Column(name = "other_total_amount", length = 80)
    var tmpOtherTotalAmount: String? = null,

    @HCColumn("tmp_imageurl")
    @FieldName("이미지URL")
    @Column(name = "image_url", length = 200)
    var tmpImageUrl: String? = null,

    @HCColumn("tmp_promotion_id")
    @FieldName("Heroku행사ID")
    @Column(name = "heroku_promotion_id", length = 80)
    var tmpHerokuPromotionId: String? = null,

    @HCColumn("tmp_promotion_seq")
    @FieldName("행사순번")
    @Column(name = "promotion_seq", length = 80)
    var tmpPromotionSeq: String? = null,

    @HCColumn("tmp_imagefilename")
    @FieldName("이미지파일명")
    @Column(name = "image_file_name", length = 80)
    var tmpImageFileName: String? = null,

    @HCColumn("tmp_otherchangeproduct")
    @FieldName("증정대체품목")
    @Column(name = "other_change_product", length = 80)
    var tmpOtherChangeProduct: String? = null,

    @FieldName("사원ID")
    @Column(name = "employee_id")
    var employeeId: Long? = null,

    @FieldName("제품ID")
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