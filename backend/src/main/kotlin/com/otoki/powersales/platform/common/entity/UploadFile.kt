package com.otoki.powersales.platform.common.entity

import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import com.otoki.powersales.domain.org.employee.entity.Group
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy

/**
 * 업로드파일 Entity
 * Salesforce UploadFile__c (업로드파일) — Spec #712 SF Object 정합 (Group A + Reference R-2).
 */
@EntityListeners(OwnerUserDefaultListener::class)
@DomainName("업로드파일")
@Entity
@Table(name = "upload_file")
@SFObject("UploadFile__c")
class UploadFile(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("업로드파일ID")
    @Column(name = "upload_file_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @FieldName("이름")
    @Column(name = "name", length = 255)
    val name: String? = null,

    @SFField("UniqueKey__c")
    @FieldName("유니크키")
    @Column(name = "unique_key", length = 500)
    val uniqueKey: String? = null,

    // SF RecordId__c = 부모 SObject 의 sfid (parent_type 별로 claim/notice/proposal/site_activity).
    // *_sfid 패턴으로 명명 — Stage2 polymorphic-parent substep 이 (parent_type, record_sfid) → parent_id 변환.
    @SFField("RecordId__c")
    @Column(name = "record_sfid", length = 40)
    val recordSfid: String? = null,

    @SFField("Size__c")
    @FieldName("Size")
    @Column(name = "size", length = 100)
    val fileSize: String? = null,

    // SF Object__c 원본 (부모 SObject API 명). Stage1 이 직접 적재.
    @SFField("Object__c")
    @FieldName("오브젝트")
    @Column(name = "object_type", length = 40)
    val objectType: String? = null,

    // 신규 시스템 parent_type — Stage2 가 object_type 기준으로 파생 (DB DEFAULT 'UNKNOWN').
    // SF 직접 매핑 아님 (@SFField 미부착). 신규 INSERT 경로는 UploadFileParentTypes 상수 명시 지정.
    @FieldName("부모유형")
    @Column(name = "parent_type", nullable = false, length = 40)
    val parentType: String = "UNKNOWN",

    @FieldName("부모ID")
    @Column(name = "parent_id")
    val parentId: Long? = null,

    // --- Spec #616: SF 누락 비수식 3개 도입 ---

    @SFField("Url__c")
    @FieldName("Url")
    @Column(name = "url", length = 500)
    val url: String? = null,

    @SFField("UploadKbn__c")
    @FieldName("업로드구분")
    @Column(name = "upload_kbn", length = 200)
    val uploadKbn: String? = null,

    @SFField("FileId__c")
    @FieldName("파일아이디")
    @Column(name = "file_id", length = 100)
    val fileId: String? = null,

    // -- Spec #712: Group A — IsDeleted --
    @SFField("IsDeleted")
    @FieldName("삭제여부")
    @Column(name = "is_deleted")
    var isDeleted: Boolean? = null,

    // BaseEntity 미상속 — 자체 timestamp 컬럼 (§6.3 CreatedDate 직접 부착)
    @SFField("CreatedDate")
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    // updated_at ← SF LastModifiedDate (BaseEntity / UserRole 동일 관행). 마이그레이션 row 는
    // Stage1 이 SF LastModifiedDate 를 명시 적재, 신규 INSERT/UPDATE 는 @LastModifiedDate auditing.
    @LastModifiedDate
    @SFField("LastModifiedDate")
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

    @CreatedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: User? = null,

    @LastModifiedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: User? = null

) : AuditedEntity()
