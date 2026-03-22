package com.otoki.internal.sap.entity

import com.otoki.internal.common.entity.BaseEntity
import com.otoki.internal.common.salesforce.HCColumn
import com.otoki.internal.common.salesforce.HCTable
import com.otoki.internal.common.salesforce.SFField
import com.otoki.internal.common.salesforce.SFObject
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 사원 Entity
 *
 * 레거시 스키마 2개 테이블에 매핑:
 * - Primary: employee (사원 마스터)
 * - Secondary: employee_info (인증/기기 정보) — @OneToOne 관계
 *
 * employee_info 조인은 employee_number = employee_number (non-PK 컬럼).
 * JPA @SecondaryTable은 PK 기반 조인만 지원하므로, @OneToOne + delegate property로 구현.
 */
@Entity
@Table(name = "employee")
@SFObject("DKRetail__Employee__c")
@HCTable("dkretail__employee__c")
class Employee(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_id")
    val id: Long = 0,

    @HCColumn("sfid")
    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @SFField("DKRetail__EmpCode__c")
    @HCColumn("dkretail__empcode__c")
    @Column(name = "employee_number", unique = true, length = 100)
    val employeeNumber: String,

    @SFField("Name")
    @HCColumn("name")
    @Column(name = "name", length = 80)
    var name: String,

    @SFField("DKRetail__Birthdate__c")
    @HCColumn("dkretail__birthdate__c")
    @Column(name = "birth_date", length = 10)
    var birthDate: String? = null,

    @SFField("DKRetail__Status__c")
    @HCColumn("dkretail__status__c")
    @Column(name = "status", length = 40)
    var status: String? = null,

    @SFField("DKRetail__APPLoginActive__c")
    @HCColumn("dkretail__apploginactive__c")
    @Column(name = "app_login_active")
    var appLoginActive: Boolean? = null,

    @SFField("DKRetail__AppAuthority__c")
    @HCColumn("dkretail__appauthority__c")
    @Column(name = "app_authority", length = 255)
    var appAuthority: String? = null,

    @SFField("DKRetail__OrgName__c")
    @HCColumn("dkretail__orgname__c")
    @Column(name = "org_name", length = 100)
    var orgName: String? = null,

    @SFField("CostCenterCode__c")
    @HCColumn("costcentercode__c")
    @Column(name = "cost_center_code", length = 10)
    var costCenterCode: String? = null,

    @SFField("DKRetail__WorkPhone__c")
    @HCColumn("dkretail__workphone__c")
    @Column(name = "work_phone", length = 255)
    var workPhone: String? = null,

    @SFField("Phone__c")
    @HCColumn("phone__c")
    @Column(name = "phone", length = 40)
    val phone: String? = null,

    @SFField("DKRetail__HomePhone__c")
    @HCColumn("dkretail__homephone__c")
    @Column(name = "home_phone", length = 255)
    var homePhone: String? = null,

    @SFField("DKRetail__Sex__c")
    @HCColumn("dkretail__sex__c")
    @Column(name = "sex", length = 10)
    var sex: String? = null,

    @SFField("DKRetail__StartDate__c")
    @HCColumn("dkretail__startdate__c")
    @Column(name = "start_date")
    var startDate: LocalDate? = null,

    @SFField("DKRetail__EndDate__c")
    @HCColumn("dkretail__enddate__c")
    @Column(name = "end_date")
    var endDate: LocalDate? = null,

    @SFField("AgreementFlag__c")
    @HCColumn("agreementflag__c")
    @Column(name = "agreement_flag")
    var agreementFlag: Boolean? = null,

    @HCColumn("isdeleted")
    @Column(name = "is_deleted")
    val isDeleted: Boolean? = null,

    @Column(name = "professional_promotion_team", length = 50)
    var professionalPromotionTeam: String? = null,

    @SFField("DKRetail__Jikchak__c")
    @HCColumn("dkretail__jikchak__c")
    @Column(name = "jikchak", length = 100)
    var jikchak: String? = null,

    @SFField("DKRetail__Jikwee__c")
    @HCColumn("dkretail__jikwee__c")
    @Column(name = "jikwee", length = 40)
    var jikwee: String? = null,

    @SFField("DKRetail__Jikgub__c")
    @HCColumn("dkretail__jikgub__c")
    @Column(name = "jikgub", length = 40)
    var jikgub: String? = null,

    @SFField("DKRetail__WorkType__c")
    @HCColumn("dkretail__worktype__c")
    @Column(name = "work_type", length = 40)
    var workType: String? = null,

    @SFField("DKRetail__JobCode__c")
    @HCColumn("dkretail__jobcode__c")
    @Column(name = "job_code", length = 40)
    var jobCode: String? = null,

    @SFField("DKRetail__WorkArea__c")
    @HCColumn("dkretail__workarea__c")
    @Column(name = "work_area", length = 100)
    var workArea: String? = null,

    @SFField("DKRetail__Jikjong__c")
    @HCColumn("dkretail__jikjong__c")
    @Column(name = "jikjong", length = 40)
    var jikjong: String? = null,

    @SFField("DKRetail__AppointmentDate__c")
    @HCColumn("dkretail__appointmentdate__c")
    @Column(name = "appointment_date")
    var appointmentDate: LocalDate? = null,

    @SFField("OrdDetailNode__c")
    @HCColumn("orddetailnode__c")
    @Column(name = "ord_detail_node", length = 255)
    var ordDetailNode: String? = null,

    @SFField("DKRetail__CRM_WorkStartDate__c")
    @HCColumn("dkretail__crm_workstartdate__c")
    @Column(name = "crm_work_start_date")
    var crmWorkStartDate: LocalDate? = null,

    // --- Secondary Table (employee_info) 필드: constructor param only (JPA 미매핑) ---

    password: String = "",
    passwordChangeRequired: Boolean? = true,
    deviceUuid: String? = null,
    fcmToken: String? = null,
    lastAgreementNumber: String? = null
) : BaseEntity() {

    // --- employee_info @OneToOne 관계 ---

    @OneToOne(cascade = [CascadeType.ALL], fetch = FetchType.EAGER, optional = true)
    @JoinColumn(
        name = "employee_number",
        referencedColumnName = "employee_number",
        insertable = false,
        updatable = false,
        foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    var employeeInfo: EmployeeInfo? = EmployeeInfo(
        employeeNumber = employeeNumber,
        password = password,
        passwordChangeRequired = passwordChangeRequired,
        deviceUuid = deviceUuid,
        fcmToken = fcmToken,
        lastAgreementNumber = lastAgreementNumber
    )

    // --- Delegate properties (기존 인터페이스 유지) ---

    var password: String
        get() = employeeInfo?.password ?: ""
        set(value) { ensureEmployeeInfo().password = value }

    var passwordChangeRequired: Boolean?
        get() = employeeInfo?.passwordChangeRequired
        set(value) { ensureEmployeeInfo().passwordChangeRequired = value }

    var deviceUuid: String?
        get() = employeeInfo?.deviceUuid
        set(value) { ensureEmployeeInfo().deviceUuid = value }

    var fcmToken: String?
        get() = employeeInfo?.fcmToken
        set(value) { ensureEmployeeInfo().fcmToken = value }

    var lastAgreementNumber: String?
        get() = employeeInfo?.lastAgreementNumber
        set(value) { ensureEmployeeInfo().lastAgreementNumber = value }

    val gpsYn: Boolean?
        get() = employeeInfo?.gpsYn

    val gpsYnDate: LocalDateTime?
        get() = employeeInfo?.gpsYnDate

    private fun ensureEmployeeInfo(): EmployeeInfo {
        if (employeeInfo == null) {
            employeeInfo = EmployeeInfo(employeeNumber = employeeNumber)
        }
        return employeeInfo!!
    }

    // --- Computed properties ---

    /**
     * role은 DB에 저장하지 않고 appAuthority로부터 도출하는 computed property
     */
    val role: UserRole
        get() = when (appAuthority) {
            "조장" -> UserRole.LEADER
            "지점장" -> UserRole.ADMIN
            else -> UserRole.USER
        }

    // --- Domain methods ---

    fun changePassword(newEncodedPassword: String) {
        this.password = newEncodedPassword
        this.passwordChangeRequired = false
        this.updatedAt = LocalDateTime.now()
    }

    fun requiresGpsConsent(): Boolean {
        return agreementFlag != true
    }

    fun recordGpsConsent(agreementNumber: String? = null) {
        this.agreementFlag = true
        if (agreementNumber != null) {
            this.lastAgreementNumber = agreementNumber
        }
        this.updatedAt = LocalDateTime.now()
    }

    fun bindDevice(deviceId: String) {
        this.deviceUuid = deviceId
        this.updatedAt = LocalDateTime.now()
    }

    fun resetDevice() {
        this.deviceUuid = null
        this.updatedAt = LocalDateTime.now()
    }

    @PreUpdate
    fun onPreUpdate() {
        this.updatedAt = LocalDateTime.now()
    }
}
