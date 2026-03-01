package com.otoki.internal.notice.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "uploadfile__c")
class UploadFile(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @Column(name = "name", length = 255)
    val name: String? = null,

    @Column(name = "uniquekey__c", length = 500)
    val uniqueKey: String? = null,

    @Column(name = "recordid__c", length = 18)
    val recordId: String? = null,

    @Column(name = "size__c", length = 50)
    val fileSize: String? = null,

    @Column(name = "isdeleted")
    val isDeleted: Boolean? = null,

    @Column(name = "createddate")
    val createdDate: LocalDateTime? = null
)
