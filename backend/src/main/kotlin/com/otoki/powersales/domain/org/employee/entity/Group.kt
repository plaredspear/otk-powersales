package com.otoki.powersales.domain.org.employee.entity

import com.otoki.powersales.domain.org.employee.entity.converter.GroupTypeConverter
import com.otoki.powersales.domain.org.employee.enums.GroupType
import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import com.otoki.powersales.platform.auth.entity.UserRole
import com.otoki.powersales.user.entity.User
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import com.otoki.powersales.platform.common.entity.OwnerUserDefaultListener
import jakarta.persistence.EntityListeners
import com.otoki.powersales.platform.common.entity.DomainName

/**
 * Salesforce `Group` 표준 sobject 매핑 entity (Spec #755).
 *
 * SF Group 의 15 필드를 전수 보존 (Q3 옵션 1 — SF prod raw JSON `Group.json`).
 * Queue / Regular / Role / Manager / Territory 등 17 Type 의 다목적 그룹.
 *
 * 본 시스템에서의 책임:
 * - SF 원본 식별자 보존 (sfid 매칭 마이그레이션 대상)
 * - Appointment.OwnerId polymorphic FK 의 Group 분기 참조 대상 (Spec #755 Q1)
 *
 * 비범위:
 * - GroupMember 멤버십 N:M (production 0건 분석 — Spec #755 §1)
 * - Group 데이터의 application 신규/수정 UI (Q4 — SF → backend 단방향)
 *
 * polymorphic reference (RelatedId / OwnerId) 는 sfid only 보존 (Q5 옵션 1).
 * audit (createdBy / lastModifiedBy) 는 backend `User` entity self-reference R-2 — Spec #757 정합.
 */
@EntityListeners(OwnerUserDefaultListener::class)
@DomainName("그룹")
@Entity
@Table(name = "\"group\"")
@SFObject("Group")
class Group(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    var sfid: String? = null,

    @SFField("Name")
    @Column(name = "name", nullable = false, length = 40)
    var name: String,

    @SFField("DeveloperName")
    @Column(name = "developer_name", length = 80)
    var developerName: String? = null,

    @SFField("Type")
    @Convert(converter = GroupTypeConverter::class)
    @Column(name = "type", nullable = false, length = 40)
    var type: GroupType,

    @SFField("RelatedId")
    @Column(name = "related_sfid", length = 18)
    var relatedSfid: String? = null,

    @SFField("OwnerId")
    @Column(name = "owner_sfid", length = 18)
    var ownerSfid: String? = null,

    @SFField("Email")
    @Column(name = "email", length = 255)
    var email: String? = null,

    @SFField("DoesSendEmailToMembers")
    @Column(name = "does_send_email_to_members", nullable = false)
    var doesSendEmailToMembers: Boolean = false,

    @SFField("DoesIncludeBosses")
    @Column(name = "does_include_bosses", nullable = false)
    var doesIncludeBosses: Boolean = false,

    @SFField("Description")
    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,

    // -- Group A R-2 audit (User FK — Spec #757) --

    @SFField("CreatedById")
    @Column(name = "created_by_sfid", length = 18)
    var createdBySfid: String? = null,

    @SFField("LastModifiedById")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

    // -- Relations --

    // RelatedId polymorphic [User, UserRole] — typed FK 2개 (spec #782 P2-B v1.4).
    // Stage 2 fk substep 이 related_sfid prefix 005 → relatedUserId, 00E → relatedUserRoleId 분기 채움.
    // CHECK 제약 — 정확히 0 또는 1만 NOT NULL (Type=Regular/Queue 는 RelatedId 부재).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_user_id")
    var relatedUser: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_user_role_id")
    var relatedUserRole: UserRole? = null,

    // OwnerId polymorphic [Organization, User] — User 분기만 FK (Organization 미매핑)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    var ownerUser: User? = null,

    @CreatedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: User? = null,

    @LastModifiedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: User? = null,

    ) : BaseEntity()
