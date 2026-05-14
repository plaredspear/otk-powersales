package com.otoki.powersales.common.entity

import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HCTable
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.employee.entity.Group
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import java.time.LocalDate

/**
 * 지점평가 Entity
 * Salesforce BranchReview__c (지점평가) — Group A + Reference R-2 + 식별/시점 4.
 *
 * Roll-Up Summary 20 + Formula 18 (판촉/레이디 평가 인원 / 합계 / 평균) 은 §6.7 정책에 따라 DB 컬럼 부재.
 * 자식 sobject (BranchReviewItem 추정) 매핑 entity 가 생기면 Kotlin 측 aggregate 재현.
 *
 * sf-meta-diff 후속 (2026-05-14):
 * - OwnerId (`[Group, User]` polymorphic) 는 spec #755 패턴: `owner_sfid` sync buffer +
 *   `owner_user_id` (User FK) + `owner_group_id` (Group FK) + XOR CHECK 제약.
 * - audit (CreatedById / LastModifiedById) FK 는 SF `referenceTo = [User]` 정합 — `User` entity 참조 (spec #757).
 */
@Entity
@Table(name = "branch_review")
@SFObject("BranchReview__c")
@HCTable("branchreview__c")
class BranchReview(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "branch_review_id")
    val id: Long = 0,

    @HCColumn("sfid")
    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @HCColumn("name")
    @Column(name = "name", length = 80)
    val name: String? = null,

    // -- 식별 / 시점 --

    @SFField("BranchName__c")
    @HCColumn("branchname__c")
    @Column(name = "branch_name", length = 100)
    val branchName: String? = null,

    @SFField("CostCenterCode__c")
    @HCColumn("costcentercode__c")
    @Column(name = "cost_center_code", length = 100)
    val costCenterCode: String? = null,

    @SFField("FirstDayofMonth__c")
    @HCColumn("firstdayofmonth__c")
    @Column(name = "first_day_of_month")
    val firstDayOfMonth: LocalDate? = null,

    @SFField("Confirmed__c")
    @HCColumn("confirmed__c")
    @Column(name = "confirmed")
    val confirmed: Boolean? = null,

    // -- Group A — IsDeleted --

    @SFField("IsDeleted")
    @HCColumn("isdeleted")
    @Column(name = "is_deleted")
    val isDeleted: Boolean? = null,

    // -- Group A audit sfid sync buffer (R-2 패턴) --
    // SalesforceMigrationTool 이 Phase 2 에서 *_sfid → user.sfid → user.user_id (또는 group.group_id) lookup 으로 FK 채움.

    @SFField("OwnerId")
    @HCColumn("ownerid")
    @Column(name = "owner_sfid", length = 18)
    var ownerSfid: String? = null,

    @SFField("CreatedById")
    @HCColumn("createdbyid")
    @Column(name = "created_by_sfid", length = 18)
    var createdBySfid: String? = null,

    @SFField("LastModifiedById")
    @HCColumn("lastmodifiedbyid")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

    // -- Relations --
    // OwnerId polymorphic R-2 (referenceTo = [Group, User]) — sfid prefix `005` = User / `00G` = Group.
    // XOR CHECK 제약 chk_branch_review_owner_xor.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    var ownerUser: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_group_id")
    var ownerGroup: Group? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: User? = null,

) : BaseEntity()
