package com.otoki.internal.product.entity

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
class Product(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @Column(name = "name", length = 80)
    val name: String? = null,

    @Column(name = "dkretail__productcode__c", length = 100)
    val productCode: String? = null,

    @Column(name = "dkretail__producttype__c", length = 255)
    val productType: String? = null,

    @Column(name = "dkretail__productstatus__c", length = 255)
    val productStatus: String? = null,

    @Column(name = "dkretail__storecondition__c", length = 255)
    val storageCondition: String? = null,

    @Column(name = "dkretail__shelflife__c", length = 30)
    val shelfLife: String? = null,

    @Column(name = "dkretail__shelflifeunit__c", length = 40)
    val shelfLifeUnit: String? = null,

    @Column(name = "shelflifefull__c", length = 1300)
    val shelfLifeFull: String? = null,

    @Column(name = "dkretail__category1__c", length = 255)
    val category1: String? = null,

    @Column(name = "dkretail__category2__c", length = 255)
    val category2: String? = null,

    @Column(name = "dkretail__category3__c", length = 255)
    val category3: String? = null,

    @Column(name = "dkretail__categorycode1__c", length = 100)
    val categoryCode1: String? = null,

    @Column(name = "dkretail__categorycode2__c", length = 100)
    val categoryCode2: String? = null,

    @Column(name = "dkretail__categorycode3__c", length = 100)
    val categoryCode3: String? = null,

    @Column(name = "dkretail__unit__c", length = 40)
    val unit: String? = null,

    @Column(name = "dkretail__orderingunit__c", length = 40)
    val orderingUnit: String? = null,

    @Column(name = "dkretail__conversionquantity__c")
    val conversionQuantity: Double? = null,

    @Column(name = "dkretail__boxreceivingquantity__c")
    val boxReceivingQuantity: Double? = null,

    @Column(name = "dkretail__standardunitprice__c")
    val standardUnitPrice: Double? = null,

    @Column(name = "standardprice__c")
    val standardPrice: Double? = null,

    @Column(name = "supertax__c")
    val superTax: Double? = null,

    @Column(name = "dkretail__launchdate__c")
    val launchDate: LocalDate? = null,

    @Column(name = "dkretail__logisticsbarcode__c", length = 100)
    val logisticsBarcode: String? = null,

    @Column(name = "tastegift__c", length = 1)
    val tasteGift: String? = null,

    @Column(name = "productfeatures__c", length = 255)
    val productFeatures: String? = null,

    @Column(name = "sellingpoint__c", length = 255)
    val sellingPoint: String? = null,

    @Column(name = "purpose__c", length = 255)
    val purpose: String? = null,

    @Column(name = "targetaccounttype__c", length = 255)
    val targetAccountType: String? = null,

    @Column(name = "allergen__c", length = 255)
    val allergen: String? = null,

    @Column(name = "crosscontamination__c", length = 255)
    val crossContamination: String? = null,

    @Column(name = "imgrefpath__c", length = 255)
    val imgRefPath: String? = null,

    @Column(name = "imgrefpath_front__c", length = 255)
    val imgRefPathFront: String? = null,

    @Column(name = "imgrefpath_back__c", length = 255)
    val imgRefPathBack: String? = null,

    @Column(name = "imgrefpathtxt__c", length = 255)
    val imgRefPathTxt: String? = null,

    @Column(name = "isdeleted")
    val isDeleted: Boolean? = null,

    @Column(name = "createddate")
    val createdDate: LocalDateTime? = null,

    @Column(name = "systemmodstamp")
    val systemModStamp: LocalDateTime? = null,

    @Column(name = "_hc_lastop", length = 32)
    val hcLastOp: String? = null,

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
