package com.otoki.powersales.notice.entity

import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HCTable
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.entity.Group
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*

@Entity
@Table(name = "notice")
@SFObject("DKRetail__Notice__c")
@HCTable("dkretail__notice__c")
class Notice(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notice_id")
    val id: Long = 0,

    @HCColumn("sfid")
    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @SFField("Name")
    @HCColumn("name")
    @Column(name = "name", length = 80)
    var name: String? = null,

    @SFField("Title__c")
    @HCColumn("title__c")
    @Column(name = "title", length = 255)
    var title: String? = null,

    @SFField("EmployeeId__c")
    @HCColumn("employeeid__c")
    @Column(name = "employee_sfid", length = 18)
    val employeeSfid: String? = null,

    @SFField("DKRetail__Scope__c")
    @HCColumn("dkretail__scope__c")
    @Column(name = "scope", length = 255)
    @Convert(converter = NoticeScopeConverter::class)
    val scope: NoticeScope? = null,

    @SFField("DKRetail__Category__c")
    @HCColumn("dkretail__category__c")
    @Column(name = "category", length = 255)
    @Convert(converter = NoticeCategoryConverter::class)
    var category: NoticeCategory? = null,

    @SFField("DKRetail__Contents__c")
    @HCColumn("dkretail__contents__c")
    @Column(name = "contents", columnDefinition = "TEXT")
    var contents: String? = null,

    // DKRetail__EduCategory__c (Label="사용안함") — Spec #745 Q2: E분류(사용안함) 컬럼 제거. 분석: 36 sobject 정합 분석 2026-05-13

    @SFField("DKRetail__Jeejum__c")
    @HCColumn("dkretail__jeejum__c")
    @Column(name = "branch", length = 255)
    var branch: String? = null,

    @SFField("DKRetail__JeejumCode__c")
    @HCColumn("dkretail__jeejumcode__c")
    @Column(name = "branch_code", length = 255)
    var branchCode: String? = null,

    @SFField("IsDeleted")
    @HCColumn("isdeleted")
    @Column(name = "is_deleted")
    var isDeleted: Boolean? = null,

    // -- Relations --

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    val employee: Employee? = null,


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
