package com.otoki.powersales.schedule.entity

import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HCTable
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.employee.entity.Employee
import jakarta.persistence.*

/**
 * 발령정보 Entity
 * Salesforce Appointment__c (발령정보) — Spec #736 SF Object 정합 (Group A R-2 + Custom 16 + 길이 정합).
 *
 * 컬럼명 mismatch (after_org_code ↔ OrgCode__c 등) 는 어노테이션만 매핑하고 컬럼명은 유지 (Q2 결정).
 * appoint_date 의 string ↔ date 타입 mismatch 는 SalesforceMigrationTool 의 date 변환에 위임 (Q3).
 */
@Entity
@Table(
    name = "appointment",
    indexes = [
        Index(name = "idx_appointment_employee", columnList = "employee_code"),
        Index(name = "idx_appointment_date", columnList = "appoint_date")
    ]
)
@SFObject("Appointment__c")
@HCTable("appointment__c")
class Appointment(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "appointment_id")
    val id: Long = 0,

    @HCColumn("sfid")
    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("Name")
    @HCColumn("name")
    @Column(name = "name", length = 80)
    var name: String? = null,

    @SFField("EmployeeCode__c")
    @HCColumn("employeecode__c")
    @Column(name = "employee_code", nullable = false, length = 100)
    val employeeCode: String,

    @SFField("isEmpCodeExist__c")
    @HCColumn("isempcodeexist__c")
    @Column(name = "emp_code_exist", nullable = false)
    var empCodeExist: Boolean = false,

    @SFField("OrgCode__c")
    @HCColumn("orgcode__c")
    @Column(name = "after_org_code", length = 100)
    val afterOrgCode: String? = null,

    @SFField("OrgName__c")
    @HCColumn("orgname__c")
    @Column(name = "after_org_name", length = 100)
    val afterOrgName: String? = null,

    @SFField("Jikchak__c")
    @HCColumn("jikchak__c")
    @Column(name = "jikchak", length = 100)
    val jikchak: String? = null,

    @SFField("Jikwee__c")
    @HCColumn("jikwee__c")
    @Column(name = "jikwee", length = 100)
    val jikwee: String? = null,

    @SFField("Jikgub__c")
    @HCColumn("jikgub__c")
    @Column(name = "jikgub", length = 100)
    val jikgub: String? = null,

    @SFField("WorkType__c")
    @HCColumn("worktype__c")
    @Column(name = "work_type", length = 100)
    val workType: String? = null,

    @SFField("ManageType__c")
    @HCColumn("managetype__c")
    @Column(name = "manage_type", length = 100)
    val manageType: String? = null,

    @SFField("JobCode__c")
    @HCColumn("jobcode__c")
    @Column(name = "job_code", length = 100)
    val jobCode: String? = null,

    @SFField("WorkArea__c")
    @HCColumn("workarea__c")
    @Column(name = "work_area", length = 100)
    val workArea: String? = null,

    @SFField("Jikjong__c")
    @HCColumn("jikjong__c")
    @Column(name = "jikjong", length = 100)
    val jikjong: String? = null,

    @SFField("AppointmentDate__c")
    @HCColumn("appointmentdate__c")
    @Column(name = "appoint_date", nullable = false, length = 8)
    val appointDate: String,

    @SFField("JobName__c")
    @HCColumn("jobname__c")
    @Column(name = "job_name", length = 100)
    val jobName: String? = null,

    @SFField("OrdDetailCode__c")
    @HCColumn("orddetailcode__c")
    @Column(name = "ord_detail_code", length = 100)
    val ordDetailCode: String? = null,

    @SFField("OrdDetailNode__c")
    @HCColumn("orddetailnode__c")
    @Column(name = "ord_detail_node", length = 250)
    val ordDetailNode: String? = null,

    // -- Spec #736: Group A — IsDeleted --

    @SFField("IsDeleted")
    @HCColumn("isdeleted")
    @Column(name = "is_deleted")
    val isDeleted: Boolean? = null,

    // -- Spec #736: Group A — CreatedById / LastModifiedById (R-2 패턴) --
    // OwnerId 는 신규 시스템에 Group 개념 부재로 제외 (§3 결정).

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
    @JoinColumn(name = "created_by_id")
    var createdBy: Employee? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: Employee? = null,

) : BaseEntity()
