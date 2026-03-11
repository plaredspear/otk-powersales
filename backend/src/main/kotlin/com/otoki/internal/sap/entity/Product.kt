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

    @Column(name = "sfid", length = 18)
    @HCColumn("sfid")
    val sfid: String? = null,

    @Column(name = "name", length = 80)
    @SFField("Name")
    @HCColumn("name")
    var name: String? = null,

    @Column(name = "product_code", length = 100)
    @SFField("DKRetail__ProductCode__c")
    @HCColumn("dkretail__productcode__c")
    val productCode: String? = null,

    @Column(name = "product_type", length = 255)
    @SFField("DKRetail__ProductType__c")
    @HCColumn("dkretail__producttype__c")
    var productType: String? = null,

    @Column(name = "product_status", length = 255)
    @SFField("DKRetail__ProductStatus__c")
    @HCColumn("dkretail__productstatus__c")
    var productStatus: String? = null,

    @Column(name = "storage_condition", length = 255)
    @SFField("DKRetail__StoreCondition__c")
    @HCColumn("dkretail__storecondition__c")
    var storageCondition: String? = null,

    @Column(name = "shelf_life", length = 30)
    @SFField("DKRetail__ShelfLife__c")
    @HCColumn("dkretail__shelflife__c")
    var shelfLife: String? = null,

    @Column(name = "shelf_life_unit", length = 40)
    @SFField("DKRetail__ShelfLifeUnit__c")
    @HCColumn("dkretail__shelflifeunit__c")
    var shelfLifeUnit: String? = null,

    @Column(name = "shelf_life_full", length = 1300)
    @SFField("ShelfLifeFull__c")
    @HCColumn("shelflifefull__c")
    val shelfLifeFull: String? = null,

    @Column(name = "category1", length = 255)
    @SFField("DKRetail__Category1__c")
    @HCColumn("dkretail__category1__c")
    var category1: String? = null,

    @Column(name = "category2", length = 255)
    @SFField("DKRetail__Category2__c")
    @HCColumn("dkretail__category2__c")
    var category2: String? = null,

    @Column(name = "category3", length = 255)
    @SFField("DKRetail__Category3__c")
    @HCColumn("dkretail__category3__c")
    var category3: String? = null,

    @Column(name = "category_code1", length = 100)
    @SFField("DKRetail__CategoryCode1__c")
    @HCColumn("dkretail__categorycode1__c")
    var categoryCode1: String? = null,

    @Column(name = "category_code2", length = 100)
    @SFField("DKRetail__CategoryCode2__c")
    @HCColumn("dkretail__categorycode2__c")
    var categoryCode2: String? = null,

    @Column(name = "category_code3", length = 100)
    @SFField("DKRetail__CategoryCode3__c")
    @HCColumn("dkretail__categorycode3__c")
    var categoryCode3: String? = null,

    @Column(name = "unit", length = 40)
    @SFField("DKRetail__Unit__c")
    @HCColumn("dkretail__unit__c")
    var unit: String? = null,

    @Column(name = "ordering_unit", length = 40)
    @SFField("DKRetail__OrderingUnit__c")
    @HCColumn("dkretail__orderingunit__c")
    val orderingUnit: String? = null,

    @Column(name = "conversion_quantity")
    @SFField("DKRetail__ConversionQuantity__c")
    @HCColumn("dkretail__conversionquantity__c")
    val conversionQuantity: Double? = null,

    @Column(name = "box_receiving_quantity")
    @SFField("DKRetail__BoxReceivingQuantity__c")
    @HCColumn("dkretail__boxreceivingquantity__c")
    var boxReceivingQuantity: Double? = null,

    @Column(name = "standard_unit_price")
    @SFField("DKRetail__StandardUnitPrice__c")
    @HCColumn("dkretail__standardunitprice__c")
    val standardUnitPrice: Double? = null,

    @Column(name = "standard_price")
    @SFField("StandardPrice__c")
    @HCColumn("standardprice__c")
    var standardPrice: Double? = null,

    @Column(name = "super_tax")
    @SFField("SuperTax__c")
    @HCColumn("supertax__c")
    var superTax: Double? = null,

    @Column(name = "launch_date")
    @SFField("DKRetail__LaunchDate__c")
    @HCColumn("dkretail__launchdate__c")
    var launchDate: LocalDate? = null,

    @Column(name = "logistics_barcode", length = 100)
    @SFField("DKRetail__LogisticsBarCode__c")
    @HCColumn("dkretail__logisticsbarcode__c")
    var logisticsBarcode: String? = null,

    @Column(name = "taste_gift", length = 1)
    @SFField("TasteGift__c")
    @HCColumn("tastegift__c")
    var tasteGift: String? = null,

    @Column(name = "product_features", length = 255)
    @SFField("ProductFeatures__c")
    @HCColumn("productfeatures__c")
    val productFeatures: String? = null,

    @Column(name = "selling_point", length = 255)
    @SFField("SellingPoint__c")
    @HCColumn("sellingpoint__c")
    val sellingPoint: String? = null,

    @Column(name = "purpose", length = 255)
    @SFField("Purpose__c")
    @HCColumn("purpose__c")
    val purpose: String? = null,

    @Column(name = "target_account_type", length = 255)
    @SFField("TargetAccountType__c")
    @HCColumn("targetaccounttype__c")
    val targetAccountType: String? = null,

    @Column(name = "allergen", length = 255)
    @SFField("Allergen__c")
    @HCColumn("allergen__c")
    val allergen: String? = null,

    @Column(name = "cross_contamination", length = 255)
    @SFField("CrossContamination__c")
    @HCColumn("crosscontamination__c")
    val crossContamination: String? = null,

    @Column(name = "img_ref_path", length = 255)
    @SFField("ImgRefPath__c")
    @HCColumn("imgrefpath__c")
    val imgRefPath: String? = null,

    @Column(name = "img_ref_path_front", length = 255)
    @SFField("ImgRefPath_front__c")
    @HCColumn("imgrefpath_front__c")
    val imgRefPathFront: String? = null,

    @Column(name = "img_ref_path_back", length = 255)
    @SFField("ImgRefPath_back__c")
    @HCColumn("imgrefpath_back__c")
    val imgRefPathBack: String? = null,

    @Column(name = "img_ref_path_txt", length = 255)
    @SFField("ImgRefPathTXT__c")
    @HCColumn("imgrefpathtxt__c")
    val imgRefPathTxt: String? = null,

    @Column(name = "is_deleted")
    @HCColumn("isdeleted")
    val isDeleted: Boolean? = null,

    @Column(name = "created_date")
    @HCColumn("createddate")
    val createdDate: LocalDateTime? = null,

    @Column(name = "system_mod_stamp")
    @HCColumn("systemmodstamp")
    val systemModStamp: LocalDateTime? = null,

    @Column(name = "_hc_lastop", length = 32)
    @HCColumn("_hc_lastop")
    val hcLastOp: String? = null,

    @Column(name = "_hc_err", columnDefinition = "TEXT")
    @HCColumn("_hc_err")
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
