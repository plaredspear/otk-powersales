package com.otoki.powersales.domain.support.notice.entity

import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.entity.Group
import com.otoki.powersales.domain.support.notice.enums.NoticeCategory
import com.otoki.powersales.domain.support.notice.enums.NoticeScope
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import com.otoki.powersales.common.entity.OwnerUserDefaultListener

@EntityListeners(OwnerUserDefaultListener::class)
@Entity
@Table(name = "notice")
@SFObject("DKRetail__Notice__c")
class Notice(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notice_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @Column(name = "name", length = 80)
    var name: String? = null,

    @SFField("Title__c")
    @Column(name = "title", length = 255)
    var title: String? = null,

    @SFField("EmployeeId__c")
    @Column(name = "employee_sfid", length = 18)
    val employeeSfid: String? = null,

    @SFField("DKRetail__Scope__c")
    @Column(name = "scope", length = 255)
    @Convert(converter = NoticeScopeConverter::class)
    var scope: NoticeScope? = null,

    @SFField("DKRetail__Category__c")
    @Column(name = "category", length = 255)
    @Convert(converter = NoticeCategoryConverter::class)
    var category: NoticeCategory? = null,

    @SFField("DKRetail__Contents__c")
    @Column(name = "contents", columnDefinition = "TEXT")
    var contents: String? = null,

    // DKRetail__EduCategory__c (Label="교육 카테고리(사용안함)") — Spec #849 부활: SF 메타 존재 non-calculated 필드는 데이터 무관 마이그레이션 대상. plain String 원본 보존 (enum 변환 없음). (과거 Spec #745 Q2 V100 DROP 부활)
    @SFField("DKRetail__EduCategory__c")
    @Column(name = "edu_category", length = 255)
    var eduCategory: String? = null,

    @SFField("DKRetail__Jeejum__c")
    @Column(name = "branch", length = 255)
    var branch: String? = null,

    @SFField("DKRetail__JeejumCode__c")
    @Column(name = "branch_code", length = 255)
    var branchCode: String? = null,

    @SFField("IsDeleted")
    @Column(name = "is_deleted")
    var isDeleted: Boolean? = null,

    // -- Relations --

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    val employee: Employee? = null,


    @SFField("OwnerId")
    @Column(name = "owner_sfid", length = 18)
    var ownerSfid: String? = null,

    @SFField("CreatedById")
    @Column(name = "created_by_sfid", length = 18)
    var createdBySfid: String? = null,

    @SFField("LastModifiedById")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

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
    var lastModifiedBy: User? = null,
) : BaseEntity()
