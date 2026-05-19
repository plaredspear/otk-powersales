package com.otoki.powersales.common.entity

import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HCTable
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.employee.entity.Employee
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
@HCTable("uploadfile__c")
class UploadFile(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "upload_file_id")
    val id: Long = 0,

    @HCColumn("sfid")
    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @HCColumn("name")
    @Column(name = "name", length = 255)
    val name: String? = null,

    @SFField("UniqueKey__c")
    @HCColumn("uniquekey__c")
    @Column(name = "unique_key", length = 500)
    val uniqueKey: String? = null,

    @SFField("RecordId__c")
    @HCColumn("recordid__c")
    @Column(name = "record_id", length = 40)
    val recordId: String? = null,

    @SFField("Size__c")
    @HCColumn("size__c")
    @Column(name = "size", length = 100)
    val fileSize: String? = null,

    @SFField("Object__c")
    @HCColumn("object__c")
    @Column(name = "parent_type", nullable = false, length = 40)
    val parentType: String = "UNKNOWN",

    @Column(name = "parent_id")
    val parentId: Long? = null,

    // --- Spec #616: SF 누락 비수식 3개 도입 ---

    @SFField("Url__c")
    @HCColumn("url__c")
    @Column(name = "url", length = 500)
    val url: String? = null,

    @SFField("UploadKbn__c")
    @HCColumn("uploadkbn__c")
    @Column(name = "upload_kbn", length = 200)
    val uploadKbn: String? = null,

    @SFField("FileId__c")
    @HCColumn("fileid__c")
    @Column(name = "file_id", length = 100)
    val fileId: String? = null,

    // -- Spec #747 카테고리 A — D 분류 누락 (생성일) --
    @SFField("Date__c")
    @HCColumn("date__c")
    @Column(name = "file_date")
    val fileDate: java.time.LocalDate? = null,

    // -- Spec #712: Group A — IsDeleted --
    @SFField("IsDeleted")
    @HCColumn("isdeleted")
    @Column(name = "is_deleted")
    var isDeleted: Boolean? = null,

    // BaseEntity 미상속 — 자체 timestamp 컬럼 (§6.3 CreatedDate 직접 부착)
    @SFField("CreatedDate")
    @HCColumn("createddate")
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    // SystemModstamp: @HCColumn 유지, @SFField 부여 안 함 (§6.3 note)
    @HCColumn("systemmodstamp")
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    // -- Spec #712: Group A — OwnerId / CreatedById / LastModifiedById (R-2 패턴) --
    // *_sfid: SF User Id buffer (Heroku Connect / SalesforceMigrationTool 이 채움).
    // *_id: SF User → Employee 매핑 결과 FK.

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    var owner: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: Employee? = null

) : AuditedEntity()