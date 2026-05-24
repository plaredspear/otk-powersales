package com.otoki.powersales.promotion.entity

import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.product.entity.Product
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*

/**
 * 행사상품 (DKRetail__PromotionProduct__c — DKRetail 관리형 패키지 SObject "상세 POS품목").
 *
 * 레거시 PromotionTriggerHandler 가 행사마스터 신규 등록 / 대표품목 변경 시 자동 생성·upsert.
 * 행사 1건당 정확히 1건만 유지 (PromotionIdExt__c 외부 키 기반 upsert — 신규에서는 promotion_id UNIQUE 제약).
 */
@Entity
@Table(name = "promotion_product")
@SFObject("DKRetail__PromotionProduct__c")
class PromotionProduct(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "promotion_product_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @Column(name = "name", length = 80)
    var name: String? = null,

    // SF: DKRetail__PromotionId__c (Master-Detail → DKRetail__Promotion__c)
    // 신규: promotion_id (FK + UNIQUE — 행사 1건당 PromotionProduct 1건)
    @Column(name = "promotion_id", nullable = false, unique = true)
    val promotionId: Long,

    @SFField("DKRetail__PromotionId__c")
    @Column(name = "promotion_sfid", length = 18)
    var promotionSfid: String? = null,

    // SF: DKRetail__ProductId__c (Lookup → DKRetail__Product__c, SetNull)
    @Column(name = "product_id")
    var productId: Long? = null,

    @SFField("DKRetail__ProductId__c")
    @Column(name = "product_sfid", length = 18)
    var productSfid: String? = null,

    // SF: DKRetail__Price__c (Number(18,0))
    @SFField("DKRetail__Price__c")
    @Column(name = "price")
    var price: Long? = null,

    // SF: PromotionIdExt__c (Text(100), externalId) — 레거시 upsert 외부 키
    @SFField("PromotionIdExt__c")
    @Column(name = "promotion_id_ext", length = 100)
    var promotionIdExt: String? = null,

    // V193 — SF describe 상 OwnerId 필드 부재 (Master-Detail child, V154 PromotionEmployee 동일 패턴). owner_sfid 제거.

    @SFField("CreatedById")
    @Column(name = "created_by_sfid", length = 18)
    var createdBySfid: String? = null,

    @SFField("LastModifiedById")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

    @SFField("IsDeleted")
    @Column(name = "is_deleted", nullable = false)
    var isDeleted: Boolean = false,

) : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id", insertable = false, updatable = false)
    var promotion: Promotion? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    var product: Product? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: User? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: User? = null

    fun updateProduct(productId: Long?) {
        this.productId = productId
    }
}
