package com.otoki.powersales.domain.activity.inspection.entity

import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import com.otoki.powersales.domain.org.employee.entity.Group
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import java.time.LocalDate
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import com.otoki.powersales.platform.common.entity.OwnerUserDefaultListener
import com.otoki.powersales.platform.common.entity.DomainName

/**
 * 현장 점검 테마 Entity
 * V1 스키마: theme__c
 */
@EntityListeners(OwnerUserDefaultListener::class)
@DomainName("현장점검테마")
@Entity
@Table(name = "inspection_theme")
@SFObject("Theme__c")
class InspectionTheme(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inspection_theme_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @Column(name = "name", length = 80)
    val name: String? = null,

    @SFField("Title__c")
    @Column(name = "title", length = 250)
    val title: String? = null,

    @SFField("StartDate__c")
    @Column(name = "start_date")
    val startDate: LocalDate? = null,

    @SFField("EndDate__c")
    @Column(name = "end_date")
    val endDate: LocalDate? = null,

    @SFField("Department__c")
    @Column(name = "department", length = 100)
    val department: String? = null,

    @SFField("BranchCode__c")
    @Column(name = "branch_code", length = 30)
    val branchCode: String? = null,

    @SFField("PublicFlag__c")
    @Column(name = "public_flag")
    val publicFlag: Boolean? = null,

    @SFField("IsDeleted")
    @Column(name = "is_deleted")
    val isDeleted: Boolean? = null,

    // -- Spec #714: OwnerId / CreatedById / LastModifiedById R-2 패턴 --
    // *_sfid: Heroku Connect sync 가 채우는 buffer (SF User Id).
    // *_id: SalesforceMigrationTool 이 SF User → Employee 매핑으로 채우는 FK.

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

    // V199 — SF Theme__c.OwnerId.referenceTo = [Group, User] polymorphic. owner_id (Employee FK) →
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

) : BaseEntity()
