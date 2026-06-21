package com.otoki.powersales.domain.foundation.product.entity

import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import com.otoki.powersales.domain.org.employee.entity.Group
import com.otoki.powersales.domain.foundation.product.entity.converter.ProductStatusConverter
import com.otoki.powersales.domain.foundation.product.entity.converter.ProductTypeConverter
import com.otoki.powersales.domain.foundation.product.entity.converter.StorageConditionConverter
import com.otoki.powersales.domain.foundation.product.enums.ProductStatus
import com.otoki.powersales.domain.foundation.product.enums.ProductType
import com.otoki.powersales.domain.foundation.product.enums.StorageCondition
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import com.otoki.powersales.platform.common.entity.OwnerUserDefaultListener
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

/**
 * 제품 Entity
 * V1 스키마 product 테이블에 매핑.
 * Heroku Connect로 Salesforce Product 오브젝트와 동기화된다.
 */
@EntityListeners(OwnerUserDefaultListener::class)
@DomainName("제품")
@Entity
@Table(name = "product")
@SFObject("DKRetail__Product__c")
class Product(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("제품ID")
    @Column(name = "product_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @FieldName("이름")
    @Column(name = "name", length = 80)
    var name: String? = null,

    @SFField("DKRetail__ProductCode__c")
    @FieldName("제품코드")
    @Column(name = "product_code", unique = true, length = 100)
    val productCode: String? = null,

    @SFField("DKRetail__ProductType__c")
    @Convert(converter = ProductTypeConverter::class)
    @FieldName("형태구분")
    @Column(name = "product_type", length = 255)
    var productType: ProductType? = null,

    @SFField("DKRetail__ProductStatus__c")
    @Convert(converter = ProductStatusConverter::class)
    @FieldName("제품상태")
    @Column(name = "product_status", length = 255)
    var productStatus: ProductStatus? = null,

    @SFField("DKRetail__StoreCondition__c")
    @Convert(converter = StorageConditionConverter::class)
    @FieldName("보관방법")
    @Column(name = "storage_condition", length = 255)
    var storageCondition: StorageCondition? = null,

    @SFField("DKRetail__ShelfLife__c")
    @FieldName("유통기한")
    @Column(name = "shelf_life", length = 30)
    var shelfLife: String? = null,

    @SFField("DKRetail__ShelfLifeUnit__c")
    @FieldName("유통기한단위")
    @Column(name = "shelf_life_unit", length = 40)
    var shelfLifeUnit: String? = null,

    @SFField("DKRetail__Category1__c")
    @FieldName("대분류")
    @Column(name = "category1", length = 255)
    var productCategory1: String? = null,

    @SFField("DKRetail__Category2__c")
    @FieldName("중분류")
    @Column(name = "category2", length = 255)
    var productCategory2: String? = null,

    @SFField("DKRetail__Category3__c")
    @FieldName("소분류")
    @Column(name = "category3", length = 255)
    var productCategory3: String? = null,

    @SFField("DKRetail__CategoryCode1__c")
    @FieldName("대분류코드")
    @Column(name = "category_code1", length = 100)
    var categoryCode1: String? = null,

    @SFField("DKRetail__CategoryCode2__c")
    @FieldName("중분류코드")
    @Column(name = "category_code2", length = 100)
    var categoryCode2: String? = null,

    @SFField("DKRetail__CategoryCode3__c")
    @FieldName("소분류코드")
    @Column(name = "category_code3", length = 100)
    var categoryCode3: String? = null,

    @SFField("DKRetail__Unit__c")
    @FieldName("단위")
    @Column(name = "unit", length = 40)
    var unit: String? = null,

    @SFField("DKRetail__OrderingUnit__c")
    @FieldName("발주단위")
    @Column(name = "ordering_unit", length = 40)
    val orderingUnit: String? = null,

    @SFField("DKRetail__ConversionQuantity__c")
    @FieldName("환산수량")
    @Column(name = "conversion_quantity")
    val conversionQuantity: Double? = null,

    @SFField("DKRetail__BoxReceivingQuantity__c")
    @FieldName("박스입수량")
    @Column(name = "box_receiving_quantity", precision = 18, scale = 4)
    var boxReceivingQuantity: BigDecimal? = null,

    @SFField("DKRetail__StandardUnitPrice__c")
    @FieldName("표준가")
    @Column(name = "standard_unit_price", precision = 18, scale = 2)
    var standardUnitPrice: BigDecimal? = null,

    @SFField("SuperTax__c")
    @FieldName("부가세")
    @Column(name = "super_tax", precision = 18, scale = 0)
    var superTax: BigDecimal? = null,

    @SFField("DKRetail__LaunchDate__c")
    @FieldName("출시일")
    @Column(name = "launch_date")
    var launchDate: LocalDate? = null,

    @SFField("DKRetail__LogisticsBarCode__c")
    @FieldName("물류바코드")
    @Column(name = "logistics_barcode", length = 100)
    var logisticsBarcode: String? = null,

    @SFField("TasteGift__c")
    @FieldName("증정/시식 구분")
    @Column(name = "taste_gift", length = 1)
    var tasteGift: String? = null,

    @SFField("ProductFeatures__c")
    @FieldName("제품특징")
    @Column(name = "product_features", length = 255)
    val productFeatures: String? = null,

    @SFField("SellingPoint__c")
    @FieldName("셀링포인트")
    @Column(name = "selling_point", length = 255)
    val sellingPoint: String? = null,

    @SFField("Purpose__c")
    @FieldName("용도")
    @Column(name = "purpose", length = 255)
    val purpose: String? = null,

    @SFField("TargetAccountType__c")
    @FieldName("타겟거래처유형")
    @Column(name = "target_account_type", length = 255)
    val targetAccountType: String? = null,

    @SFField("Allergen__c")
    @FieldName("알러지 유발물질")
    @Column(name = "allergen", length = 255)
    val allergen: String? = null,

    @SFField("CrossContamination__c")
    @FieldName("교차오염")
    @Column(name = "cross_contamination", length = 255)
    val crossContamination: String? = null,

    @SFField("ImgRefPath__c")
    @FieldName("제품이미지 참조경로")
    @Column(name = "img_ref_path", length = 255)
    val imgRefPath: String? = null,

    @SFField("ImgRefPath_front__c")
    @FieldName("제품이미지 참조경로_전면")
    @Column(name = "img_ref_path_front", length = 255)
    val imgRefPathFront: String? = null,

    @SFField("ImgRefPath_back__c")
    @FieldName("제품이미지 참조경로_후면")
    @Column(name = "img_ref_path_back", length = 255)
    val imgRefPathBack: String? = null,

    @SFField("ImgRefPathTXT__c")
    @FieldName("제품이미지 참조경로_텍스트")
    @Column(name = "img_ref_path_txt", length = 255)
    val imgRefPathTxt: String? = null,

    @SFField("IsDeleted")
    @FieldName("삭제여부")
    @Column(name = "is_deleted")
    val isDeleted: Boolean? = null,

    // --- Spec #575: SAP 인바운드 레거시 필드 보존 ---
    // product_barcode 컬럼(productBarcode)은 제거됨: SAP ProductBarcode 수신값은 barcode(DKRetail__Barcode__c)
    // 로 적재하도록 전환(레거시 IF_REST_SAP_ProductMasterSend 동등) + 읽기 사용처/SF 메타 필드 부재로 dead 였음.

    @SFField("Pallet__c")
    @FieldName("파레트 입수량 (BOX)")
    @Column(name = "pallet", precision = 18, scale = 4)
    var pallet: BigDecimal? = null,

    // --- Spec #613: SF 누락 비수식 6개 도입 ---

    @SFField("DKRetail__Barcode__c")
    @FieldName("바코드")
    @Column(name = "barcode", length = 100)
    var barcode: String? = null,

    @SFField("manufacture__c")
    @FieldName("생산처")
    @Column(name = "manufacture", length = 30)
    var manufacture: String? = null,

    @SFField("manufacture_detail__c")
    @FieldName("생산처(세부)")
    @Column(name = "manufacture_detail", length = 30)
    var manufactureDetail: String? = null,

    @SFField("Claim_Management__c")
    @FieldName("클레임담당부서")
    @Column(name = "claim_management", length = 50)
    var claimManagement: String? = null,

    @SFField("New_Product__c")
    @Column(name = "new_product_sfid", length = 18)
    var newProductSfid: String? = null,

    @SFField("StoreCondition__c")
    @FieldName("보관방법(텍스트)")
    @Column(name = "store_condition_text", length = 255)
    var storeConditionText: String? = null,

    // -- sf-meta-diff Q1/Q2/Q3: Reference R-2 정합 --
    // Q1: OwnerId (`referenceTo = [Group, User]` polymorphic) → owner_sfid + owner_user (User?) + owner_group (Group?) + CHECK XOR.
    // Q2/Q3: CreatedById/LastModifiedById (`referenceTo = [User]`) → audit FK 타입 Employee → User 전환.
    // *_sfid: sync buffer (SF Id). sf-migrate Phase 2 가 `<관계>_sfid` → `user.sfid` / `group.sfid` lookup 으로 FK 채움.

    @SFField("OwnerId")
    @Column(name = "owner_sfid", length = 18)
    var ownerSfid: String? = null,

    @SFField("CreatedById")
    @Column(name = "created_by_sfid", length = 18)
    var createdBySfid: String? = null,

    @SFField("LastModifiedById")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

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

    // -- Spec #737: New_Product__c reference (R-2 패턴 FK 후처리) --
    // new_product_sfid 는 #613/V40 시점에 sfid 컨벤션으로 추가됨. 본 FK 는 그 sfid → new_product_id 매핑.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_product_id")
    var newProduct: NewProduct? = null,

    /* --- 주석 처리: V1에 없는 기존 필드 ---
    productId: V1에 없음 (sfid로 대체)
    piecesPerBox: V1에 없음 (boxReceivingQuantity로 대체)
    minOrderUnit: V1에 없음
    supplyQuantity: V1에 없음
    dcQuantity: V1에 없음
    unitPrice: V1에 없음 (standardUnitPrice로 대체)

    --- sf-meta-diff Q15: Formula 컬럼 제거 (§6.7 — calculated == true) ---
    standardPrice (StandardPrice__c — Formula: DKRetail__StandardUnitPrice__c)
    legacyBoxReceivingQuantity (BoxReceivingQuantity__c — Formula: DKRetail__BoxReceivingQuantity__c)
    필요 시 application 측 computed property 로 재현 (val standardPrice get() = standardUnitPrice 등).
    */
) : BaseEntity()
