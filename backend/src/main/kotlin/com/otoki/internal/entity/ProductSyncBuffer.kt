package com.otoki.internal.entity

import com.otoki.internal.common.entity.BaseEntity
import com.otoki.internal.common.salesforce.HCColumn
import com.otoki.internal.common.salesforce.HCTable
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * PLM 연동용 제품 동기화 버퍼 Entity.
 * product 테이블의 트리거(if__product)가 INSERT 시 id를 복사하므로 외부 할당 PK.
 * 읽기 전용 — 앱에서 INSERT/UPDATE 하지 않음.
 */
@Entity
@Table(name = "product_sync_buffer")
@HCTable("if_product__c")
class ProductSyncBuffer(

    @Id
    @Column(name = "product_sync_buffer_id")
    val id: Int = 0,

    @HCColumn("sfid")
    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @HCColumn("name")
    @Column(name = "name", length = 80)
    val name: String? = null,

    @HCColumn("dkretail__productcode__c")
    @Column(name = "product_code", length = 100)
    val productCode: String? = null,

    @HCColumn("dkretail__producttype__c")
    @Column(name = "product_type", length = 255)
    val productType: String? = null,

    @HCColumn("dkretail__productstatus__c")
    @Column(name = "product_status", length = 255)
    val productStatus: String? = null,

    @HCColumn("dkretail__storecondition__c")
    @Column(name = "storage_condition", length = 255)
    val storageCondition: String? = null,

    @HCColumn("dkretail__shelflife__c")
    @Column(name = "shelf_life", length = 30)
    val shelfLife: String? = null,

    @HCColumn("dkretail__shelflifeunit__c")
    @Column(name = "shelf_life_unit", length = 40)
    val shelfLifeUnit: String? = null,

    @HCColumn("dkretail__category1__c")
    @Column(name = "category1", length = 255)
    val category1: String? = null,

    @HCColumn("dkretail__category2__c")
    @Column(name = "category2", length = 255)
    val category2: String? = null,

    @HCColumn("dkretail__category3__c")
    @Column(name = "category3", length = 255)
    val category3: String? = null,

    @HCColumn("dkretail__categorycode1__c")
    @Column(name = "category_code1", length = 100)
    val categoryCode1: String? = null,

    @HCColumn("dkretail__categorycode2__c")
    @Column(name = "category_code2", length = 100)
    val categoryCode2: String? = null,

    @HCColumn("dkretail__categorycode3__c")
    @Column(name = "category_code3", length = 100)
    val categoryCode3: String? = null,

    @HCColumn("dkretail__unit__c")
    @Column(name = "unit", length = 40)
    val unit: String? = null,

    @HCColumn("dkretail__orderingunit__c")
    @Column(name = "ordering_unit", length = 40)
    val orderingUnit: String? = null,

    @HCColumn("dkretail__conversionquantity__c")
    @Column(name = "conversion_quantity")
    val conversionQuantity: Double? = null,

    @HCColumn("dkretail__boxreceivingquantity__c")
    @Column(name = "box_receiving_quantity")
    val boxReceivingQuantity: Double? = null,

    @HCColumn("dkretail__standardunitprice__c")
    @Column(name = "standard_unit_price")
    val standardUnitPrice: Double? = null,

    @HCColumn("supertax__c")
    @Column(name = "super_tax")
    val superTax: Double? = null,

    @HCColumn("dkretail__launchdate__c")
    @Column(name = "launch_date")
    val launchDate: LocalDate? = null,

    @HCColumn("dkretail__logisticsbarcode__c")
    @Column(name = "logistics_barcode", length = 100)
    val logisticsBarcode: String? = null,

    @HCColumn("tastegift__c")
    @Column(name = "taste_gift", length = 1)
    val tasteGift: String? = null,

    @HCColumn("productfeatures__c")
    @Column(name = "product_features", length = 255)
    val productFeatures: String? = null,

    @HCColumn("sellingpoint__c")
    @Column(name = "selling_point", length = 255)
    val sellingPoint: String? = null,

    @HCColumn("purpose__c")
    @Column(name = "purpose", length = 255)
    val purpose: String? = null,

    @HCColumn("targetaccounttype__c")
    @Column(name = "target_account_type", length = 255)
    val targetAccountType: String? = null,

    @HCColumn("allergen__c")
    @Column(name = "allergen", length = 255)
    val allergen: String? = null,

    @HCColumn("crosscontamination__c")
    @Column(name = "cross_contamination", length = 255)
    val crossContamination: String? = null,

    @HCColumn("imgrefpath__c")
    @Column(name = "img_ref_path", length = 255)
    val imgRefPath: String? = null,

    @HCColumn("imgrefpath_front__c")
    @Column(name = "img_ref_path_front", length = 255)
    val imgRefPathFront: String? = null,

    @HCColumn("imgrefpath_back__c")
    @Column(name = "img_ref_path_back", length = 255)
    val imgRefPathBack: String? = null,

    @HCColumn("imgrefpathtxt__c")
    @Column(name = "img_ref_path_txt", length = 255)
    val imgRefPathTxt: String? = null,

    @HCColumn("updateflag__c")
    @Column(name = "update_flag")
    val updateFlag: Boolean? = null,

    @HCColumn("isdeleted")
    @Column(name = "is_deleted")
    val isDeleted: Boolean? = null,

    @HCColumn("createddate")
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    override var createdAt: LocalDateTime = LocalDateTime.now(),

    @HCColumn("systemmodstamp")
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    override var updatedAt: LocalDateTime = LocalDateTime.now()
) : BaseEntity()
