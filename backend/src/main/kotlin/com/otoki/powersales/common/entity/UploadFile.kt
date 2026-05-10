package com.otoki.powersales.common.entity

import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HCTable
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime
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
    @Column(name = "sfid", length = 18)
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
    @Column(name = "record_id", length = 18)
    val recordId: String? = null,

    @SFField("Size__c")
    @HCColumn("size__c")
    @Column(name = "size", length = 50)
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

    @HCColumn("isdeleted")
    @Column(name = "is_deleted")
    var isDeleted: Boolean? = null,

    @HCColumn("createddate")
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @HCColumn("systemmodstamp")
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) : AuditedEntity()