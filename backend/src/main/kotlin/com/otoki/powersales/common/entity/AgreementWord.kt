package com.otoki.powersales.common.entity

import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.employee.entity.Group
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 동의문구 Entity (Salesforce `AgreementWord__c`) — Spec #707 + sf-meta-diff 후속 (OwnerId polymorphic + audit FK User 전환).
 *
 * - OwnerId (`[Group, User]` polymorphic) 는 spec #755 패턴: `owner_sfid` sync buffer +
 *   `owner_user_id` (User FK) + `owner_group_id` (Group FK) + XOR CHECK 제약.
 * - audit (CreatedById / LastModifiedById) FK 는 SF `referenceTo = [User]` 정합 — `User` entity 참조 (spec #757).
 */
@Entity
@Table(name = "agreement_word")
@SFObject("AgreementWord__c")
class AgreementWord(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "agreement_word_id")
    val id: Int = 0,

    @SFField("Name")
    @Column(name = "name", length = 80, nullable = false)
    val name: String,

    @SFField("Contents__c")
    @Column(name = "contents", length = 8000)
    val contents: String? = null,

    @SFField("Active__c")
    @Column(name = "active", nullable = false)
    var active: Boolean = false,

    @SFField("ActiveDate__c")
    @Column(name = "active_date")
    var activeDate: LocalDate? = null,

    @SFField("AfterActiveDate__c")
    @Column(name = "after_active_date")
    var afterActiveDate: LocalDate? = null,

    // -- Spec #707: Group A — IsDeleted --
    @SFField("IsDeleted")
    @Column(name = "is_deleted")
    val isDeleted: Boolean? = null,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    // -- Spec #707: Group A — CreatedDate (BaseEntity 미상속 자체 매핑) --
    @SFField("CreatedDate")
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @SFField("LastModifiedDate")
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    // -- Spec #707: Group A — OwnerId / CreatedById / LastModifiedById (R-2 패턴) --
    @SFField("OwnerId")
    @Column(name = "owner_sfid", length = 18)
    var ownerSfid: String? = null,

    @SFField("CreatedById")
    @Column(name = "created_by_sfid", length = 18)
    var createdBySfid: String? = null,

    @SFField("LastModifiedById")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

    // -- OwnerId polymorphic R-2 (referenceTo = [Group, User]) --
    // sfid prefix `005` = User / `00G` = Group. XOR CHECK 제약 chk_agreement_word_owner_xor.

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
    var lastModifiedBy: User? = null

) : AuditedEntity() {

    /**
     * 활성 약관 비활성화 (cycle batch — rotation 분기의 oldObj 처리, 스펙 #654 / Q1 legacy 동등).
     *
     * 레거시 매핑: `AgreementWordBatch.cls:34-37` (`oldObj.Active__c=False; oldObj.AfterActiveDate__c=null`).
     */
    fun deactivate() {
        this.active = false
        this.afterActiveDate = null
    }

    /**
     * 도래 약관 활성화 + 다음 cycle 6개월 후로 갱신 (스펙 #654 / Q1 + Q6 legacy 동등).
     *
     * 레거시 매핑: `AgreementWordBatch.cls:39-41` (rotation 의 newObj 활성화) + `cls:54-56` (new-only 활성화).
     * 동작: ActiveDate = afterActiveDate (도래일을 활성일로 확정), afterActiveDate = activeDay + 6개월.
     */
    fun activate(activeDay: LocalDate) {
        this.active = true
        this.activeDate = activeDay
        this.afterActiveDate = activeDay.plusMonths(CYCLE_MONTHS)
    }

    companion object {
        /** 재동의 cycle 주기 — 6개월 (스펙 #654 / Q6 hardcoded, legacy `AgreementWordBatch.cls:41,56`). */
        const val CYCLE_MONTHS = 6L
    }
}