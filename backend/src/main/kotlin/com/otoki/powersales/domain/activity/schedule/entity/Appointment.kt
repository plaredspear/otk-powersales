package com.otoki.powersales.domain.activity.schedule.entity

import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import com.otoki.powersales.domain.org.employee.entity.Group
import com.otoki.powersales.user.entity.User
import jakarta.persistence.*
import java.time.LocalDate
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import com.otoki.powersales.platform.common.entity.OwnerUserDefaultListener
import com.otoki.powersales.platform.common.entity.DomainName
import com.otoki.powersales.platform.common.entity.FieldName

/**
 * 발령정보 Entity
 * Salesforce Appointment__c (발령정보) — Spec #736 SF Object 정합 (Group A R-2 + Custom 16 + 길이 정합).
 *
 * 컬럼명 mismatch (after_org_code ↔ OrgCode__c 등) 는 어노테이션만 매핑하고 컬럼명은 유지 (Q2 결정).
 * appoint_date 는 SF `date` 타입과 자연 정합되도록 `LocalDate` + DB `DATE` 로 매핑 (Spec #736 Q3 결정 번복 — sf-meta-diff/Appointment__c.md §9 Q2 후속).
 *
 * Spec #755: OwnerId R-2 정합 — `owner_sfid` (varchar 18) sync buffer + polymorphic FK 분기
 * (`owner_user_id` → backend User, `owner_group_id` → Group). XOR CHECK 제약 `chk_appointment_owner_xor` 으로
 * 둘 다 채움 금지. SF `Appointment__c.OwnerId.referenceTo = [Group, User]` 의 SF 메타 권위 정합.
 * Spec #736 §3 의 "OwnerId 제외" 결정은 본 스펙에서 번복.
 *
 * Spec #758: audit FK (createdBy / lastModifiedBy) 타입 Employee → User 일괄 전환.
 */
@EntityListeners(OwnerUserDefaultListener::class)
@DomainName("발령정보")
@Entity
@Table(
    name = "appointment",
    indexes = [
        Index(name = "idx_appointment_employee", columnList = "employee_code"),
        Index(name = "idx_appointment_date", columnList = "appoint_date"),
        Index(name = "idx_appointment_owner_user_id", columnList = "owner_user_id"),
        Index(name = "idx_appointment_owner_group_id", columnList = "owner_group_id")
    ]
)
@SFObject("Appointment__c")
class Appointment(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @FieldName("발령정보ID")
    @Column(name = "appointment_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @FieldName("이름")
    @Column(name = "name", length = 80)
    var name: String? = null,

    @SFField("EmployeeCode__c")
    @FieldName("사원코드")
    // SF nillable=true 정합 — 마이그레이션 SF NULL row 보존.
    @Column(name = "employee_code", length = 100)
    val employeeCode: String? = null,

    @SFField("isEmpCodeExist__c")
    @FieldName("사번존재여부")
    @Column(name = "emp_code_exist", nullable = false)
    var empCodeExist: Boolean = false,

    @SFField("OrgCode__c")
    @FieldName("조직코드")
    @Column(name = "after_org_code", length = 100)
    val afterOrgCode: String? = null,

    @SFField("OrgName__c")
    @FieldName("조직명")
    @Column(name = "after_org_name", length = 100)
    val afterOrgName: String? = null,

    @SFField("Jikchak__c")
    @FieldName("직책명")
    @Column(name = "jikchak", length = 100)
    val jikchak: String? = null,

    @SFField("Jikwee__c")
    @FieldName("직위명")
    @Column(name = "jikwee", length = 100)
    val jikwee: String? = null,

    @SFField("Jikgub__c")
    @FieldName("직급명")
    @Column(name = "jikgub", length = 100)
    val jikgub: String? = null,

    @SFField("WorkType__c")
    @FieldName("직군명")
    @Column(name = "work_type", length = 100)
    val workType: String? = null,

    @SFField("ManageType__c")
    @FieldName("사원구분명")
    @Column(name = "manage_type", length = 100)
    val manageType: String? = null,

    @SFField("JobCode__c")
    @FieldName("직무코드")
    @Column(name = "job_code", length = 100)
    val jobCode: String? = null,

    @SFField("WorkArea__c")
    @FieldName("실근무지역코드명")
    @Column(name = "work_area", length = 100)
    val workArea: String? = null,

    @SFField("Jikjong__c")
    @FieldName("직종명")
    @Column(name = "jikjong", length = 100)
    val jikjong: String? = null,

    @SFField("AppointmentDate__c")
    @FieldName("발령일자")
    // SF nillable=true 정합 — 마이그레이션 SF NULL row 보존.
    @Column(name = "appoint_date")
    val appointDate: LocalDate? = null,

    @SFField("JobName__c")
    @FieldName("직무명")
    @Column(name = "job_name", length = 100)
    val jobName: String? = null,

    @SFField("OrdDetailCode__c")
    @FieldName("발령코드")
    @Column(name = "ord_detail_code", length = 100)
    val ordDetailCode: String? = null,

    @SFField("OrdDetailNode__c")
    @FieldName("발령코드명")
    @Column(name = "ord_detail_node", length = 250)
    val ordDetailNode: String? = null,

    // -- Spec #736: Group A — IsDeleted --

    @SFField("IsDeleted")
    @FieldName("삭제여부")
    @Column(name = "is_deleted")
    val isDeleted: Boolean? = null,

    // -- Spec #736: Group A — CreatedById / LastModifiedById (R-2 패턴) --
    // OwnerId 는 Spec #755 에서 polymorphic R-2 (Group + User) 로 정합.
    // FK 타입은 Spec #758 에서 Employee → User 로 일괄 전환.

    @SFField("CreatedById")
    @Column(name = "created_by_sfid", length = 18)
    var createdBySfid: String? = null,

    @SFField("LastModifiedById")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

    // -- Spec #755: OwnerId polymorphic R-2 (referenceTo = [Group, User]) --
    // owner_sfid 단일 컬럼이 SF 원본 식별자 보존. owner_user_id / owner_group_id 둘 중
    // 하나만 채워지며 XOR CHECK 제약으로 enforce. sfid prefix `005` = User / `00G` = Group.

    @SFField("OwnerId")
    @Column(name = "owner_sfid", length = 18)
    var ownerSfid: String? = null,

    // -- Relations --

    @CreatedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: User? = null,

    @LastModifiedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    var ownerUser: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_group_id")
    var ownerGroup: Group? = null,

) : BaseEntity()
