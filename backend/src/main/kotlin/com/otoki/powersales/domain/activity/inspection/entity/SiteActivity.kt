package com.otoki.powersales.domain.activity.inspection.entity

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.platform.common.entity.OwnerUserDefaultListener
import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.entity.Group
import com.otoki.powersales.domain.foundation.product.entity.Product
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy

/**
 * 현장 점검 결과 Entity
 * V202605310100 스키마: site_activity
 *
 * SF DKRetail__SiteAcitivity__c (현장점검 결과, 관리형 패키지 객체) 마이그레이션 대상.
 * 부모 InspectionTheme(Theme__c) 의 자식 — 개별 현장점검 활동 1건.
 *
 * Formula 필드 4개 (ThemeName__c / EmployeeOrgName__c / BranchName__c / OrgName__c) 는
 * SF calculated 필드 → DB 컬럼 미추가 (backend-conventions §6.7).
 */
@EntityListeners(OwnerUserDefaultListener::class)
@Entity
@Table(name = "site_activity")
@SFObject("DKRetail__SiteAcitivity__c")
class SiteActivity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "site_activity_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @Column(name = "name", length = 80)
    val name: String? = null,

    // -- 점검 본문 --

    @SFField("DKRetail__ActivityDate__c")
    @Column(name = "activity_date")
    val activityDate: LocalDate? = null,

    @SFField("DKRetail__Category__c")
    @Column(name = "category", length = 255)
    val category: String? = null,

    @SFField("DKRetail__ProductType__c")
    @Column(name = "product_type", length = 255)
    val productType: String? = null,

    @SFField("DKRetail__Description__c")
    @Column(name = "description", length = 4000)
    val description: String? = null,

    @SFField("DKRetail__Title__c")
    @Column(name = "title", length = 250)
    val title: String? = null,

    @SFField("DKRetail__SAPAccountCode__c")
    @Column(name = "sap_account_code", length = 100)
    val sapAccountCode: String? = null,

    @SFField("CostCenterCode__c")
    @Column(name = "cost_center_code", length = 255)
    val costCenterCode: String? = null,

    // -- 경쟁사 활동 --

    @SFField("DKRetail__CompetitorName__c")
    @Column(name = "competitor_name", length = 100)
    val competitorName: String? = null,

    @SFField("DKRetail__CompetitorProductName__c")
    @Column(name = "competitor_product_name", length = 250)
    val competitorProductName: String? = null,

    @SFField("DKRetail__CompetitorActivityDescription__c")
    @Column(name = "competitor_activity_description", length = 2000)
    val competitorActivityDescription: String? = null,

    // SF 원본 API 명 오타(Proudct) 유지.
    @SFField("DKRetail__CompetitorProudctPrice__c")
    @Column(name = "competitor_proudct_price", precision = 18, scale = 0)
    val competitorProudctPrice: BigDecimal? = null,

    @SFField("DKRetail__SampleTastFlag__c")
    @Column(name = "sample_tast_flag", length = 255)
    val sampleTastFlag: String? = null,

    @SFField("DKRetail__SalesQuantity__c")
    @Column(name = "sales_quantity", precision = 18, scale = 0)
    val salesQuantity: BigDecimal? = null,

    @SFField("IsDeleted")
    @Column(name = "is_deleted")
    val isDeleted: Boolean? = null,

    // -- lookup: *_sfid sync buffer + *_id FK --

    @SFField("DKRetail__AccountId__c")
    @Column(name = "account_sfid", length = 18)
    var accountSfid: String? = null,

    @SFField("DKRetail__EmployeeId__c")
    @Column(name = "employee_sfid", length = 18)
    var employeeSfid: String? = null,

    @SFField("DKRetail__ProductId__c")
    @Column(name = "product_sfid", length = 18)
    var productSfid: String? = null,

    @SFField("ThemeId__c")
    @Column(name = "theme_sfid", length = 18)
    var themeSfid: String? = null,

    // -- owner / audit (InspectionTheme R-2 패턴 동일) --
    // *_sfid: Heroku Connect sync buffer (SF User Id).
    // *_id: SalesforceMigrationTool 이 SF User → 엔티티 매핑으로 채우는 FK.

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
    @JoinColumn(name = "account_id")
    var account: Account? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    var employee: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    var product: Product? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspection_theme_id")
    var inspectionTheme: InspectionTheme? = null,

    // SF OwnerId.referenceTo = [Group, User] polymorphic → owner_user_id + owner_group_id + XOR.
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
