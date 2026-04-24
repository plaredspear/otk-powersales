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

    @Column(name = "parent_type", nullable = false, length = 30)
    val parentType: String = "UNKNOWN",

    @Column(name = "parent_id")
    val parentId: Long? = null,

    @HCColumn("isdeleted")
    @Column(name = "is_deleted")
    val isDeleted: Boolean? = null,

    @HCColumn("createddate")
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    override var createdAt: LocalDateTime = LocalDateTime.now(),

    @HCColumn("systemmodstamp")
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    override var updatedAt: LocalDateTime = LocalDateTime.now()
) : BaseEntity()
