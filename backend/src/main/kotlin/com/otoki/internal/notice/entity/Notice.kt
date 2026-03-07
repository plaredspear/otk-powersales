package com.otoki.internal.notice.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "dkretail__notice__c")
class Notice(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @Column(name = "name", length = 80)
    var name: String? = null,

    @Column(name = "employeeid__c", length = 18)
    val employeeId: String? = null,

    @Column(name = "dkretail__scope__c", length = 255)
    val scope: String? = null,

    @Column(name = "dkretail__category__c", length = 255)
    @Convert(converter = NoticeCategoryConverter::class)
    var category: NoticeCategory? = null,

    @Column(name = "dkretail__contents__c", columnDefinition = "TEXT")
    var contents: String? = null,

    @Column(name = "dkretail__educategory__c", length = 255)
    val eduCategory: String? = null,

    @Column(name = "dkretail__jeejum__c", length = 255)
    var branch: String? = null,

    @Column(name = "dkretail__jeejumcode__c", length = 255)
    var branchCode: String? = null,

    @Column(name = "isdeleted")
    var isDeleted: Boolean? = null,

    @Column(name = "createddate")
    var createdDate: LocalDateTime? = null,

    @Column(name = "systemmodstamp")
    val systemModStamp: LocalDateTime? = null,

    @Column(name = "_hc_lastop", length = 32)
    val hcLastOp: String? = null,

    @Column(name = "_hc_err", columnDefinition = "TEXT")
    val hcErr: String? = null
)
