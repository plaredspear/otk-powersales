package com.otoki.powersales.domain.foundation.account.entity

import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import com.otoki.powersales.domain.org.employee.entity.Group
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import com.otoki.powersales.platform.common.entity.OwnerUserDefaultListener
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

/**
 * 거래처유형마스터 Entity
 *
 * Salesforce `AccountCategoryMaster__c` 매핑.
 *
 * - Spec #605: 초기 SF 어노테이션 부착 (3 SFField + sfid + use_search)
 * - Spec #704: Group A R-2 (CreatedById / LastModifiedById / OwnerId) sfid + Employee FK
 * - Spec #755: OwnerId polymorphic R-2 정합 — SF `referenceTo = [Group, User]`.
 *   `owner_sfid` (sync buffer) + `owner_user` (User?) + `owner_group` (Group?) + CHECK XOR.
 *   sfid prefix `005` = User / `00G` = Group 분기.
 * - Spec #758: audit FK (createdBy / lastModifiedBy) Employee → User 일괄 전환.
 */
@EntityListeners(OwnerUserDefaultListener::class)
@DomainName("거래처유형마스터")
@Entity
@Table(
    name = "account_category_master",
    indexes = [
        Index(name = "idx_account_category_master_owner_user_id", columnList = "owner_user_id"),
        Index(name = "idx_account_category_master_owner_group_id", columnList = "owner_group_id"),
        Index(name = "idx_account_category_master_created_by_id", columnList = "created_by_id"),
        Index(name = "idx_account_category_master_last_modified_by_id", columnList = "last_modified_by_id")
    ]
)
@SFObject("AccountCategoryMaster__c")
class AccountCategoryMaster(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("거래처유형마스터ID")
    @Column(name = "account_category_master_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("AccountCode__c")
    @FieldName("거래처유형코드")
    // SF nillable=true 정합 — 마이그레이션 SF NULL row 보존.
    @Column(name = "account_code", unique = true, length = 255)
    val accountCode: String? = null,

    @SFField("Name")
    @FieldName("이름")
    // SF nillable=true 정합 — SAP inbound 무검증 raw 적재 + 마이그레이션 SF NULL row 보존.
    @Column(name = "name", length = 80)
    var name: String? = null,

    @SFField("useSearch__c")
    @FieldName("조회화면이용")
    @Column(name = "use_search", nullable = false)
    var useSearch: Boolean = false,

    // -- Spec #755: OwnerId polymorphic R-2 (referenceTo = [Group, User]) --
    // owner_sfid 단일 컬럼이 SF 원본 식별자 보존. owner_user_id / owner_group_id 둘 중
    // 하나만 채워지며 XOR CHECK 제약으로 enforce. sfid prefix `005` = User / `00G` = Group.

    @SFField("OwnerId")
    @Column(name = "owner_sfid", length = 18)
    var ownerSfid: String? = null,

    // -- Spec #704 + #758: Group A (CreatedById / LastModifiedById) sfid + User FK --

    @SFField("CreatedById")
    @Column(name = "created_by_sfid", length = 18)
    var createdBySfid: String? = null,

    @SFField("LastModifiedById")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

    @SFField("IsDeleted")
    @FieldName("삭제여부")
    @Column(name = "is_deleted")
    var isDeleted: Boolean? = null,

    // -- Relations --

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
    var lastModifiedBy: User? = null
) : BaseEntity()
