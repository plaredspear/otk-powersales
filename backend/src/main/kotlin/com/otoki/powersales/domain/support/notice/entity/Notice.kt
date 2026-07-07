package com.otoki.powersales.domain.support.notice.entity

import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.entity.Group
import com.otoki.powersales.domain.support.notice.enums.NoticeCategory
import com.otoki.powersales.domain.support.notice.enums.NoticeScope
import com.otoki.powersales.domain.support.notice.enums.NoticeStatus
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import com.otoki.powersales.platform.common.entity.OwnerUserDefaultListener
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

@EntityListeners(OwnerUserDefaultListener::class)
@DomainName("공지사항")
@Entity
@Table(name = "notice")
@SFObject("DKRetail__Notice__c")
class Notice(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("공지사항ID")
    @Column(name = "notice_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @FieldName("이름")
    @Column(name = "name", length = 80)
    var name: String? = null,

    @SFField("Title__c")
    @FieldName("제목")
    @Column(name = "title", length = 255)
    var title: String? = null,

    @SFField("EmployeeId__c")
    @Column(name = "employee_sfid", length = 18)
    val employeeSfid: String? = null,

    @SFField("DKRetail__Scope__c")
    @FieldName("공개범위")
    @Column(name = "scope", length = 255)
    @Convert(converter = NoticeScopeConverter::class)
    var scope: NoticeScope? = null,

    @SFField("DKRetail__Category__c")
    @FieldName("카테고리")
    @Column(name = "category", length = 255)
    @Convert(converter = NoticeCategoryConverter::class)
    var category: NoticeCategory? = null,

    @SFField("DKRetail__Contents__c")
    @FieldName("상세내용")
    @Column(name = "contents", columnDefinition = "TEXT")
    var contents: String? = null,

    // DKRetail__EduCategory__c (Label="교육 카테고리(사용안함)") — Spec #849 부활: SF 메타 존재 non-calculated 필드는 데이터 무관 마이그레이션 대상. plain String 원본 보존 (enum 변환 없음). (과거 Spec #745 Q2 V100 DROP 부활)
    @SFField("DKRetail__EduCategory__c")
    @FieldName("교육 카테고리")
    @Column(name = "edu_category", length = 255)
    var eduCategory: String? = null,

    @SFField("DKRetail__Jeejum__c")
    @FieldName("지점")
    @Column(name = "branch", length = 255)
    var branch: String? = null,

    @SFField("DKRetail__JeejumCode__c")
    @FieldName("지점코드")
    @Column(name = "branch_code", length = 255)
    var branchCode: String? = null,

    @SFField("IsDeleted")
    @FieldName("삭제여부")
    @Column(name = "is_deleted")
    var isDeleted: Boolean? = null,

    // 발행 상태 — SF 메타에 없는 신규 로컬 컬럼(저장/발행 분리). DB 저장은 enum name(DRAFT/PUBLISHED).
    @FieldName("발행상태")
    @Column(name = "status", length = 20)
    @Convert(converter = NoticeStatusConverter::class)
    var status: NoticeStatus? = null,

    // 낙관적 락 버전 — 동시 수정 방어(여러 관리자가 같은 공지를 동시 편집 시 lost update + 인라인 이미지
    // 교차 오삭제 차단). SF 메타에 없는 신규 로컬 컬럼. 상세조회 응답에 실어 보내고 수정 요청에 되받아,
    // 저장 시 JPA 가 버전 불일치를 감지하면 ObjectOptimisticLockingFailureException → 409 로 거부한다.
    @FieldName("버전")
    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0,

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
