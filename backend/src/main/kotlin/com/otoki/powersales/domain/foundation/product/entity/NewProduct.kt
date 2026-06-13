package com.otoki.powersales.domain.foundation.product.entity

import com.otoki.powersales.platform.auth.sharing.entity.RecordType
import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.employee.entity.Group
import com.otoki.powersales.domain.foundation.product.entity.converter.InitiatorConverter
import com.otoki.powersales.user.entity.User
import com.otoki.powersales.domain.foundation.product.entity.converter.ManagementTypeConverter
import com.otoki.powersales.domain.foundation.product.entity.converter.NewProductStatusConverter
import com.otoki.powersales.domain.foundation.product.enums.Initiator
import com.otoki.powersales.domain.foundation.product.enums.ManagementType
import com.otoki.powersales.domain.foundation.product.enums.NewProductStatus
import jakarta.persistence.*
import java.time.LocalDate
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import com.otoki.powersales.common.entity.OwnerUserDefaultListener

/**
 * 신제품 Entity
 * Salesforce NewProduct__c (신제품) — Spec #737 SF Object 정합.
 *
 * Group A R-2 (Owner/CreatedBy/LastModifiedBy → Employee FK)
 * + Reference R-2 (Product_Code__c → Product FK)
 * + Picklist 3개 enum (Initiator / NewProductStatus / ManagementType 84)
 * + RecordType 9개 sfid 보존 (도메인 모델링은 #739 후속).
 */
@EntityListeners(OwnerUserDefaultListener::class)
@Entity
@Table(name = "new_product")
@SFObject("NewProduct__c")
class NewProduct(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "new_product_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @Column(name = "name", length = 80)
    val name: String? = null,

    // -- Custom 필드 (15개) --

    @SFField("Customer_Survey__c")
    @Column(name = "customer_survey")
    val customerSurvey: LocalDate? = null,

    @SFField("Initiator__c")
    @Convert(converter = InitiatorConverter::class)
    @Column(name = "initiator", length = 255)
    val initiator: Initiator? = null,

    @SFField("ManagementType__c")
    @Convert(converter = ManagementTypeConverter::class)
    @Column(name = "management_type", length = 255)
    val managementType: ManagementType? = null,

    @SFField("Marketability_Review_Report__c")
    @Column(name = "marketability_review_report")
    val marketabilityReviewReport: LocalDate? = null,

    @SFField("Product_Code__c")
    @Column(name = "product_code_sfid", length = 18)
    val productCodeSfid: String? = null,

    @SFField("Product_Name__c")
    @Column(name = "product_name", nullable = false, length = 100)
    val productName: String,

    @SFField("Product_code1__c")
    @Column(name = "product_code1", columnDefinition = "TEXT")
    val productCode1: String? = null,

    @SFField("Purpose__c")
    @Column(name = "purpose", nullable = false, length = 255)
    val purpose: String,

    @SFField("Release_Review_Report__c")
    @Column(name = "release_review_report", nullable = false)
    val releaseReviewReport: LocalDate,

    @SFField("Release__c")
    @Column(name = "release", nullable = false)
    val release: LocalDate,

    @SFField("Status__c")
    @Convert(converter = NewProductStatusConverter::class)
    @Column(name = "status", nullable = false, length = 255)
    val status: NewProductStatus,

    @SFField("firstpropose__c")
    @Column(name = "firstpropose", nullable = false)
    val firstpropose: LocalDate,

    @SFField("friday_taste__c")
    @Column(name = "friday_taste", nullable = false)
    val fridayTaste: LocalDate,

    @SFField("Upload_Description__c")
    @Column(name = "upload_description", length = 255)
    val uploadDescription: String? = null,

    @SFField("MarketingTeam__c")
    @Column(name = "marketing_team", length = 255)
    val marketingTeam: String? = null,

    // -- Group A — IsDeleted --

    @SFField("IsDeleted")
    @Column(name = "is_deleted")
    val isDeleted: Boolean? = null,

    // -- Group A — RecordType sfid (FK 미신설 — Q1) --

    @SFField("RecordTypeId")
    @Column(name = "record_type_sfid", length = 18)
    var recordTypeSfid: String? = null,

    // -- Group A — Owner / CreatedBy / LastModifiedBy (R-2 패턴) --

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

    // V199 — SF NewProduct__c.OwnerId.referenceTo = [Group, User] polymorphic. owner_id (Employee FK) →
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_code_id")
    var productCode: Product? = null,

    // V199 — RecordTypeId.referenceTo = [RecordType] → record_type.record_type_id FK.
    // Stage2 fk substep 이 record_type_sfid → record_type.record_type_id 자동 채움.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_type_id")
    var recordType: RecordType? = null,

    ) : BaseEntity()
