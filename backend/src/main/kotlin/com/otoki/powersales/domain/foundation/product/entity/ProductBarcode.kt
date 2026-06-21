package com.otoki.powersales.domain.foundation.product.entity

import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import com.otoki.powersales.domain.org.employee.entity.Group
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import com.otoki.powersales.platform.common.entity.OwnerUserDefaultListener
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

/**
 * 제품 바코드 Entity
 * V1 스키마 product_barcode 테이블에 매핑.
 * Product와 N:1 관계 (product_id FK).
 */
@EntityListeners(OwnerUserDefaultListener::class)
@DomainName("제품바코드")
@Entity
@Table(name = "product_barcode")
@SFObject("ProductBarcode__c")
class ProductBarcode(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("제품바코드ID")
    @Column(name = "product_barcode_id")
    val id: Int = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @FieldName("이름")
    @Column(name = "name", length = 80)
    var name: String? = null,

    @SFField("ProductName__c")
    @FieldName("ProductName")
    @Column(name = "product_name", length = 255)
    var productName: String? = null,

    @SFField("ProductBarcode__c")
    @FieldName("바코드")
    @Column(name = "barcode", length = 255)
    var barcode: String? = null,

    @SFField("ProductUnit__c")
    @FieldName("단위")
    @Column(name = "unit", length = 255)
    var unit: String? = null,

    @SFField("ProductSequence__c")
    @FieldName("시퀀스")
    @Column(name = "sort_order", length = 255)
    var sortOrder: String? = null,

    @FieldName("제품ID")
    @Column(name = "product_id")
    var productId: Long? = null,

    @SFField("Product__c")
    @Column(name = "product_sfid", length = 18)
    var productSfid: String? = null,

    @SFField("ProductCode__c")
    @FieldName("ProductCode")
    @Column(name = "product_code", length = 255)
    var productCode: String? = null,

    @SFField("CustomKey__c")
    @FieldName("CustomKey")
    @Column(name = "custom_key", unique = true, length = 255)
    var customKey: String? = null,

    @SFField("IsDeleted")
    @FieldName("삭제여부")
    @Column(name = "is_deleted")
    val isDeleted: Boolean? = null,

    @SFField("OwnerId")
    @Column(name = "owner_sfid", length = 18)
    var ownerSfid: String? = null,

    @SFField("CreatedById")
    @Column(name = "created_by_sfid", length = 18)
    var createdBySfid: String? = null,

    @SFField("LastModifiedById")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

    // -- Relations --
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    val product: Product? = null,

    // V199 — SF ProductBarcode__c.OwnerId.referenceTo = [Group, User] polymorphic. owner_id (Employee FK) →
    // owner_user_id (User FK) + owner_group_id (Group FK) + XOR CHECK.
    // V200 — SF CreatedById/LastModifiedById.referenceTo = [User]. audit FK Employee → User 전환.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    var ownerUser: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_group_id")
    var ownerGroup: Group? = null,

    @CreatedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: User? = null,

    @LastModifiedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: User? = null,
) : BaseEntity()
