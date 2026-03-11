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
 * V1 스키마 dkretail__product__c 테이블에 매핑.
 * Heroku Connect로 Salesforce Product 오브젝트와 동기화된다.
 */
@Entity
@Table(name = "dkretail__product__c")
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

    @Column(name = "dkretail__productcode__c", length = 100)
    @SFField("DKRetail__ProductCode__c")
    @HCColumn("dkretail__productcode__c")
    val productCode: String? = null,

    @Column(name = "dkretail__producttype__c", length = 255)
    @SFField("DKRetail__ProductType__c")
    @HCColumn("dkretail__producttype__c")
    var productType: String? = null,

    @Column(name = "dkretail__productstatus__c", length = 255)
    @SFField("DKRetail__ProductStatus__c")
    @HCColumn("dkretail__productstatus__c")
    var productStatus: String? = null,

    @Column(name = "dkretail__storecondition__c", length = 255)
    @SFField("DKRetail__StoreCondition__c")
    @HCColumn("dkretail__storecondition__c")
    var storageCondition: String? = null,

    @Column(name = "dkretail__shelflife__c", length = 30)
    @SFField("DKRetail__ShelfLife__c")
    @HCColumn("dkretail__shelflife__c")
    var shelfLife: String? = null,

    @Column(name = "dkretail__shelflifeunit__c", length = 40)
    @SFField("DKRetail__ShelfLifeUnit__c")
    @HCColumn("dkretail__shelflifeunit__c")
    var shelfLifeUnit: String? = null,

    @Column(name = "shelflifefull__c", length = 1300)
    @SFField("ShelfLifeFull__c")
    @HCColumn("shelflifefull__c")
    val shelfLifeFull: String? = null,

    @Column(name = "dkretail__category1__c", length = 255)
    @SFField("DKRetail__Category1__c")
    @HCColumn("dkretail__category1__c")
    var category1: String? = null,

    @Column(name = "dkretail__category2__c", length = 255)
    @SFField("DKRetail__Category2__c")
    @HCColumn("dkretail__category2__c")
    var category2: String? = null,

    @Column(name = "dkretail__category3__c", length = 255)
    @SFField("DKRetail__Category3__c")
    @HCColumn("dkretail__category3__c")
    var category3: String? = null,

    @Column(name = "dkretail__categorycode1__c", length = 100)
    @SFField("DKRetail__CategoryCode1__c")
    @HCColumn("dkretail__categorycode1__c")
    var categoryCode1: String? = null,

    @Column(name = "dkretail__categorycode2__c", length = 100)
    @SFField("DKRetail__CategoryCode2__c")
    @HCColumn("dkretail__categorycode2__c")
    var categoryCode2: String? = null,

    @Column(name = "dkretail__categorycode3__c", length = 100)
    @SFField("DKRetail__CategoryCode3__c")
    @HCColumn("dkretail__categorycode3__c")
    var categoryCode3: String? = null,

    @Column(name = "dkretail__unit__c", length = 40)
    @SFField("DKRetail__Unit__c")
    @HCColumn("dkretail__unit__c")
    var unit: String? = null,

    @Column(name = "dkretail__orderingunit__c", length = 40)
    @SFField("DKRetail__OrderingUnit__c")
    @HCColumn("dkretail__orderingunit__c")
    val orderingUnit: String? = null,

    @Column(name = "dkretail__conversionquantity__c")
    @SFField("DKRetail__ConversionQuantity__c")
    @HCColumn("dkretail__conversionquantity__c")
    val conversionQuantity: Double? = null,

    @Column(name = "dkretail__boxreceivingquantity__c")
    @SFField("DKRetail__BoxReceivingQuantity__c")
    @HCColumn("dkretail__boxreceivingquantity__c")
    var boxReceivingQuantity: Double? = null,

    @Column(name = "dkretail__standardunitprice__c")
    @SFField("DKRetail__StandardUnitPrice__c")
    @HCColumn("dkretail__standardunitprice__c")
    val standardUnitPrice: Double? = null,

    @Column(name = "standardprice__c")
    @SFField("StandardPrice__c")
    @HCColumn("standardprice__c")
    var standardPrice: Double? = null,

    @Column(name = "supertax__c")
    @SFField("SuperTax__c")
    @HCColumn("supertax__c")
    var superTax: Double? = null,

    @Column(name = "dkretail__launchdate__c")
    @SFField("DKRetail__LaunchDate__c")
    @HCColumn("dkretail__launchdate__c")
    var launchDate: LocalDate? = null,

    @Column(name = "dkretail__logisticsbarcode__c", length = 100)
    @SFField("DKRetail__LogisticsBarCode__c")
    @HCColumn("dkretail__logisticsbarcode__c")
    var logisticsBarcode: String? = null,

    @Column(name = "tastegift__c", length = 1)
    @SFField("TasteGift__c")
    @HCColumn("tastegift__c")
    var tasteGift: String? = null,

    @Column(name = "productfeatures__c", length = 255)
    @SFField("ProductFeatures__c")
    @HCColumn("productfeatures__c")
    val productFeatures: String? = null,

    @Column(name = "sellingpoint__c", length = 255)
    @SFField("SellingPoint__c")
    @HCColumn("sellingpoint__c")
    val sellingPoint: String? = null,

    @Column(name = "purpose__c", length = 255)
    @SFField("Purpose__c")
    @HCColumn("purpose__c")
    val purpose: String? = null,

    @Column(name = "targetaccounttype__c", length = 255)
    @SFField("TargetAccountType__c")
    @HCColumn("targetaccounttype__c")
    val targetAccountType: String? = null,

    @Column(name = "allergen__c", length = 255)
    @SFField("Allergen__c")
    @HCColumn("allergen__c")
    val allergen: String? = null,

    @Column(name = "crosscontamination__c", length = 255)
    @SFField("CrossContamination__c")
    @HCColumn("crosscontamination__c")
    val crossContamination: String? = null,

    @Column(name = "imgrefpath__c", length = 255)
    @SFField("ImgRefPath__c")
    @HCColumn("imgrefpath__c")
    val imgRefPath: String? = null,

    @Column(name = "imgrefpath_front__c", length = 255)
    @SFField("ImgRefPath_front__c")
    @HCColumn("imgrefpath_front__c")
    val imgRefPathFront: String? = null,

    @Column(name = "imgrefpath_back__c", length = 255)
    @SFField("ImgRefPath_back__c")
    @HCColumn("imgrefpath_back__c")
    val imgRefPathBack: String? = null,

    @Column(name = "imgrefpathtxt__c", length = 255)
    @SFField("ImgRefPathTXT__c")
    @HCColumn("imgrefpathtxt__c")
    val imgRefPathTxt: String? = null,

    @Column(name = "isdeleted")
    @HCColumn("isdeleted")
    val isDeleted: Boolean? = null,

    @Column(name = "createddate")
    @HCColumn("createddate")
    val createdDate: LocalDateTime? = null,

    @Column(name = "systemmodstamp")
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
