package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 인터페이스 제품 Entity
 * V1 스키마 if_product 테이블에 매핑.
 * product 테이블의 트리거(if__product)가 INSERT 시 id를 복사하므로 외부 할당 PK.
 * 읽기 전용 — 앱에서 INSERT/UPDATE 하지 않음.
 */
@Entity
@Table(name = "if_product")
class InterfaceProduct(

    @Id
    val id: Int = 0,

    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @Column(name = "name", length = 80)
    val name: String? = null,

    @Column(name = "product_code", length = 100)
    val productCode: String? = null,

    @Column(name = "product_type", length = 255)
    val productType: String? = null,

    @Column(name = "product_status", length = 255)
    val productStatus: String? = null,

    @Column(name = "storage_condition", length = 255)
    val storageCondition: String? = null,

    @Column(name = "shelf_life", length = 30)
    val shelfLife: String? = null,

    @Column(name = "shelf_life_unit", length = 40)
    val shelfLifeUnit: String? = null,

    @Column(name = "category1", length = 255)
    val category1: String? = null,

    @Column(name = "category2", length = 255)
    val category2: String? = null,

    @Column(name = "category3", length = 255)
    val category3: String? = null,

    @Column(name = "category_code1", length = 100)
    val categoryCode1: String? = null,

    @Column(name = "category_code2", length = 100)
    val categoryCode2: String? = null,

    @Column(name = "category_code3", length = 100)
    val categoryCode3: String? = null,

    @Column(name = "unit", length = 40)
    val unit: String? = null,

    @Column(name = "ordering_unit", length = 40)
    val orderingUnit: String? = null,

    @Column(name = "conversion_quantity")
    val conversionQuantity: Double? = null,

    @Column(name = "box_receiving_quantity")
    val boxReceivingQuantity: Double? = null,

    @Column(name = "standard_unit_price")
    val standardUnitPrice: Double? = null,

    @Column(name = "super_tax")
    val superTax: Double? = null,

    @Column(name = "launch_date")
    val launchDate: LocalDate? = null,

    @Column(name = "logistics_barcode", length = 100)
    val logisticsBarcode: String? = null,

    @Column(name = "taste_gift", length = 1)
    val tasteGift: String? = null,

    @Column(name = "product_features", length = 255)
    val productFeatures: String? = null,

    @Column(name = "selling_point", length = 255)
    val sellingPoint: String? = null,

    @Column(name = "purpose", length = 255)
    val purpose: String? = null,

    @Column(name = "target_account_type", length = 255)
    val targetAccountType: String? = null,

    @Column(name = "allergen", length = 255)
    val allergen: String? = null,

    @Column(name = "cross_contamination", length = 255)
    val crossContamination: String? = null,

    @Column(name = "img_ref_path", length = 255)
    val imgRefPath: String? = null,

    @Column(name = "img_ref_path_front", length = 255)
    val imgRefPathFront: String? = null,

    @Column(name = "img_ref_path_back", length = 255)
    val imgRefPathBack: String? = null,

    @Column(name = "img_ref_path_txt", length = 255)
    val imgRefPathTxt: String? = null,

    @Column(name = "update_flag")
    val updateFlag: Boolean? = null,

    @Column(name = "is_deleted")
    val isDeleted: Boolean? = null,

    @Column(name = "created_date")
    val createdDate: LocalDateTime? = null,

    @Column(name = "system_mod_stamp")
    val systemModStamp: LocalDateTime? = null,

)
