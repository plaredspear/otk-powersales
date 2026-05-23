package com.otoki.powersales.common.entity

import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.entity.Group
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import java.time.LocalDate

/**
 * 동의이력 Entity
 * Salesforce AgreementHistory__c (동의이력) — Spec #706 정합 + sf-meta-diff 후속 (OwnerId polymorphic + audit FK User 전환).
 *
 * - OwnerId (`[Group, User]` polymorphic) 는 spec #755 패턴: `owner_sfid` sync buffer +
 *   `owner_user_id` (User FK) + `owner_group_id` (Group FK) + XOR CHECK 제약.
 * - audit (CreatedById / LastModifiedById) FK 는 SF `referenceTo = [User]` 정합 — `User` entity 참조 (spec #757).
 */
@Entity
@Table(name = "agreement_history")
@SFObject("AgreementHistory__c")
class AgreementHistory(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "agreement_history_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @Column(name = "employee_id")
    val employeeId: Long? = null,

    @SFField("EmployeeId__c")
    @Column(name = "employee_sfid", length = 18)
    val employeeSfid: String? = null,

    @SFField("AgreementFlag__c")
    @Column(name = "agreement_flag", nullable = false)
    val agreementFlag: Boolean,

    @SFField("AgreementDate__c")
    @Column(name = "agreement_date", nullable = false)
    val agreementDate: LocalDate,

    @Column(name = "agreement_word_id")
    val agreementWordId: Long? = null,

    @SFField("AgreementWordId__c")
    @Column(name = "agreement_word_sfid", length = 18)
    val agreementWordSfid: String? = null,

    // -- Spec #706: Group A — IsDeleted --
    @SFField("IsDeleted")
    @Column(name = "is_deleted", nullable = false)
    var isDeleted: Boolean = false,

    // -- Spec #706: 표준 비-A 필드 (Name = 동의번호) --
    @SFField("Name")
    @Column(name = "name", length = 80)
    var name: String? = null,

    // -- Group A audit sfid sync buffer (R-2 패턴) --
    // SalesforceMigrationTool 이 Phase 2 에서 *_sfid → user.sfid → user.user_id lookup 으로 FK 채움.

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
    @JoinColumn(name = "employee_id", insertable = false, updatable = false)
    val employee: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_word_id", insertable = false, updatable = false)
    val agreementWord: AgreementWord? = null,

    // -- OwnerId polymorphic R-2 (referenceTo = [Group, User]) --
    // sfid prefix `005` = User / `00G` = Group. XOR CHECK 제약 chk_agreement_history_owner_xor.

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
