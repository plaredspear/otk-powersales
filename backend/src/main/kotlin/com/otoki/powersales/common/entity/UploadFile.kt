package com.otoki.powersales.common.entity

import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.employee.entity.Group
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime

/**
 * 업로드파일 Entity
 * Salesforce UploadFile__c (업로드파일) — Spec #712 SF Object 정합 (Group A + Reference R-2).
 */
@Entity
@Table(name = "upload_file")
@SFObject("UploadFile__c")
class UploadFile(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "upload_file_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @Column(name = "name", length = 255)
    val name: String? = null,

    @SFField("UniqueKey__c")
    @Column(name = "unique_key", length = 500)
    val uniqueKey: String? = null,

    @SFField("RecordId__c")
    @Column(name = "record_id", length = 40)
    val recordId: String? = null,

    @SFField("Size__c")
    @Column(name = "size", length = 100)
    val fileSize: String? = null,

    @SFField("Object__c")
    @Column(name = "parent_type", nullable = false, length = 40)
    val parentType: String = "UNKNOWN",

    @Column(name = "parent_id")
    val parentId: Long? = null,

    // --- Spec #616: SF 누락 비수식 3개 도입 ---

    @SFField("Url__c")
    @Column(name = "url", length = 500)
    val url: String? = null,

    @SFField("UploadKbn__c")
    @Column(name = "upload_kbn", length = 200)
    val uploadKbn: String? = null,

    @SFField("FileId__c")
    @Column(name = "file_id", length = 100)
    val fileId: String? = null,

    // -- Spec #747 카테고리 A — D 분류 누락 (생성일) --
    @SFField("Date__c")
    @Column(name = "file_date")
    val fileDate: java.time.LocalDate? = null,

    // -- Spec #712: Group A — IsDeleted --
    @SFField("IsDeleted")
    @Column(name = "is_deleted")
    var isDeleted: Boolean? = null,

    // BaseEntity 미상속 — 자체 timestamp 컬럼 (§6.3 CreatedDate 직접 부착)
    @SFField("CreatedDate")
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    // SystemModstamp: @SFField 부여 안 함 (§6.3 note)
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    // -- Spec #712: Group A — OwnerId / CreatedById / LastModifiedById (R-2 패턴) --
    // *_sfid: SF User Id buffer (Heroku Connect / SalesforceMigrationTool 이 채움).
    // *_id: SF User → Employee 매핑 결과 FK.

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

    // V199 — SF UploadFile__c.OwnerId.referenceTo = [Group, User] polymorphic. owner_id (Employee FK) →
    // owner_user_id (User FK) + owner_group_id (Group FK) + XOR CHECK.
    // V200 — SF CreatedById/LastModifiedById.referenceTo = [User]. audit FK Employee → User 전환.
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

) : AuditedEntity()