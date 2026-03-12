package com.otoki.internal.sap.entity

import com.otoki.internal.common.salesforce.HCColumn
import com.otoki.internal.common.salesforce.HCTable
import com.otoki.internal.common.salesforce.SFField
import com.otoki.internal.common.salesforce.SFObject
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 제품 Entity
 * V1 스키마 product 테이블에 매핑.
 * Heroku Connect로 Salesforce Product 오브젝트와 동기화된다.
 */
@Entity
@Table(name = "product")
@SFObject("DKRetail__Product__c")
@HCTable("dkretail__product__c")
class Product(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @HCColumn("id")
    val id: Long = 0,

    @HCColumn("sfid")
    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @SFField("Name")
    @HCColumn("name")
    @Column(name = "name", length = 80)
    var name: String? = null,

    @SFField("DKRetail__ProductCode__c")
    @HCColumn("dkretail__productcode__c")
    @Column(name = "product_code", length = 100)
    val productCode: String? = null,

    @SFField("DKRetail__ProductType__c")
    @HCColumn("dkretail__producttype__c")
    @Column(name = "product_type", length = 255)
    var productType: String? = null,

    @SFField("DKRetail__ProductStatus__c")
    @HCColumn("dkretail__productstatus__c")
    @Column(name = "product_status", length = 255)
    var productStatus: String? = null,

    @SFField("DKRetail__StoreCondition__c")
    @HCColumn("dkretail__storecondition__c")
    @Column(name = "storage_condition", length = 255)
    var storageCondition: String? = null,

    @SFField("DKRetail__ShelfLife__c")
    @HCColumn("dkretail__shelflife__c")
    @Column(name = "shelf_life", length = 30)
    var shelfLife: String? = null,

    @SFField("DKRetail__ShelfLifeUnit__c")
    @HCColumn("dkretail__shelflifeunit__c")
    @Column(name = "shelf_life_unit", length = 40)
    var shelfLifeUnit: String? = null,

    @SFField("ShelfLifeFull__c")
    @HCColumn("shelflifefull__c")
    @Column(name = "shelf_life_full", length = 1300)
    val shelfLifeFull: String? = null,

    @SFField("DKRetail__Category1__c")
    @HCColumn("dkretail__category1__c")
    @Column(name = "category1", length = 255)
    var category1: String? = null,

    @SFField("DKRetail__Category2__c")
    @HCColumn("dkretail__category2__c")
    @Column(name = "category2", length = 255)
    var category2: String? = null,

    @SFField("DKRetail__Category3__c")
    @HCColumn("dkretail__category3__c")
    @Column(name = "category3", length = 255)
    var category3: String? = null,

    @SFField("DKRetail__CategoryCode1__c")
    @HCColumn("dkretail__categorycode1__c")
    @Column(name = "category_code1", length = 100)
    var categoryCode1: String? = null,

    @SFField("DKRetail__CategoryCode2__c")
    @HCColumn("dkretail__categorycode2__c")
    @Column(name = "category_code2", length = 100)
    var categoryCode2: String? = null,

    @SFField("DKRetail__CategoryCode3__c")
    @HCColumn("dkretail__categorycode3__c")
    @Column(name = "category_code3", length = 100)
    var categoryCode3: String? = null,

    @SFField("DKRetail__Unit__c")
    @HCColumn("dkretail__unit__c")
    @Column(name = "unit", length = 40)
    var unit: String? = null,

    @SFField("DKRetail__OrderingUnit__c")
    @HCColumn("dkretail__orderingunit__c")
    @Column(name = "ordering_unit", length = 40)
    val orderingUnit: String? = null,

    @SFField("DKRetail__ConversionQuantity__c")
    @HCColumn("dkretail__conversionquantity__c")
    @Column(name = "conversion_quantity")
    val conversionQuantity: Double? = null,

    @SFField("DKRetail__BoxReceivingQuantity__c")
    @HCColumn("dkretail__boxreceivingquantity__c")
    @Column(name = "box_receiving_quantity")
    var boxReceivingQuantity: Double? = null,

    @SFField("DKRetail__StandardUnitPrice__c")
    @HCColumn("dkretail__standardunitprice__c")
    @Column(name = "standard_unit_price")
    val standardUnitPrice: Double? = null,

    @SFField("StandardPrice__c")
    @HCColumn("standardprice__c")
    @Column(name = "standard_price")
    var standardPrice: Double? = null,

    @SFField("SuperTax__c")
    @HCColumn("supertax__c")
    @Column(name = "super_tax")
    var superTax: Double? = null,

    @SFField("DKRetail__LaunchDate__c")
    @HCColumn("dkretail__launchdate__c")
    @Column(name = "launch_date")
    var launchDate: LocalDate? = null,

    @SFField("DKRetail__LogisticsBarCode__c")
    @HCColumn("dkretail__logisticsbarcode__c")
    @Column(name = "logistics_barcode", length = 100)
    var logisticsBarcode: String? = null,

    @SFField("TasteGift__c")
    @HCColumn("tastegift__c")
    @Column(name = "taste_gift", length = 1)
    var tasteGift: String? = null,

    @SFField("ProductFeatures__c")
    @HCColumn("productfeatures__c")
    @Column(name = "product_features", length = 255)
    val productFeatures: String? = null,

    @SFField("SellingPoint__c")
    @HCColumn("sellingpoint__c")
    @Column(name = "selling_point", length = 255)
    val sellingPoint: String? = null,

    @SFField("Purpose__c")
    @HCColumn("purpose__c")
    @Column(name = "purpose", length = 255)
    val purpose: String? = null,

    @SFField("TargetAccountType__c")
    @HCColumn("targetaccounttype__c")
    @Column(name = "target_account_type", length = 255)
    val targetAccountType: String? = null,

    @SFField("Allergen__c")
    @HCColumn("allergen__c")
    @Column(name = "allergen", length = 255)
    val allergen: String? = null,

    @SFField("CrossContamination__c")
    @HCColumn("crosscontamination__c")
    @Column(name = "cross_contamination", length = 255)
    val crossContamination: String? = null,

    @SFField("ImgRefPath__c")
    @HCColumn("imgrefpath__c")
    @Column(name = "img_ref_path", length = 255)
    val imgRefPath: String? = null,

    @SFField("ImgRefPath_front__c")
    @HCColumn("imgrefpath_front__c")
    @Column(name = "img_ref_path_front", length = 255)
    val imgRefPathFront: String? = null,

    @SFField("ImgRefPath_back__c")
    @HCColumn("imgrefpath_back__c")
    @Column(name = "img_ref_path_back", length = 255)
    val imgRefPathBack: String? = null,

    @SFField("ImgRefPathTXT__c")
    @HCColumn("imgrefpathtxt__c")
    @Column(name = "img_ref_path_txt", length = 255)
    val imgRefPathTxt: String? = null,

    @HCColumn("isdeleted")
    @Column(name = "is_deleted")
    val isDeleted: Boolean? = null,

    @HCColumn("createddate")
    @Column(name = "created_date")
    val createdDate: LocalDateTime? = null,

    @HCColumn("systemmodstamp")
    @Column(name = "system_mod_stamp")
    val systemModStamp: LocalDateTime? = null,

    @HCColumn("_hc_lastop")
    @Column(name = "_hc_lastop", length = 32)
    val hcLastOp: String? = null,

    @HCColumn("_hc_err")
    @Column(name = "_hc_err", columnDefinition = "TEXT")
    val hcErr: String? = null

    /* --- 주석 처리: V1에 없는 기존 필드 ---
    productId: V1에 없음 (sfid로 대체)
    piecesPerBox: V1에 없음 (boxReceivingQuantity로 대체)
    minOrderUnit: V1에 없음
    supplyQuantity: V1에 없음
    dcQuantity: V1에 없음
    unitPrice: V1에 없음 (standardUnitPrice로 대체)
    */
)
