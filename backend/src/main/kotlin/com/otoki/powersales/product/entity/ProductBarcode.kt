package com.otoki.powersales.product.entity

import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.employee.entity.Employee
import jakarta.persistence.*

/**
 * 제품 바코드 Entity
 * V1 스키마 product_barcode 테이블에 매핑.
 * Product와 N:1 관계 (product_id FK).
 */
@Entity
@Table(name = "product_barcode")
@SFObject("ProductBarcode__c")
class ProductBarcode(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_barcode_id")
    val id: Int = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @Column(name = "name", length = 80)
    var name: String? = null,

    @SFField("ProductName__c")
    @Column(name = "product_name", length = 255)
    var productName: String? = null,

    @SFField("ProductBarcode__c")
    @Column(name = "barcode", length = 255)
    var barcode: String? = null,

    @SFField("ProductUnit__c")
    @Column(name = "unit", length = 255)
    var unit: String? = null,

    @SFField("ProductSequence__c")
    @Column(name = "sort_order", length = 255)
    var sortOrder: String? = null,

    @Column(name = "product_id")
    var productId: Long? = null,

    @SFField("Product__c")
    @Column(name = "product_sfid", length = 18)
    var productSfid: String? = null,

    @SFField("ProductCode__c")
    @Column(name = "product_code", length = 255)
    var productCode: String? = null,

    @SFField("CustomKey__c")
    @Column(name = "custom_key", unique = true, length = 255)
    var customKey: String? = null,

    @SFField("IsDeleted")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    var owner: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: Employee? = null,
) : BaseEntity()
