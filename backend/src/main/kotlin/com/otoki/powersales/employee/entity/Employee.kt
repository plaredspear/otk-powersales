package com.otoki.powersales.employee.entity

import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HCTable
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.employee.entity.converter.CrmWorkTypeConverter
import com.otoki.powersales.employee.entity.converter.GenderConverter
import jakarta.persistence.*
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import java.time.LocalDate
import java.time.LocalDateTime
import com.otoki.powersales.auth.converter.UserRoleConverter
import com.otoki.powersales.auth.entity.UserRoleEnum
import com.otoki.powersales.employee.enums.CrmWorkType
import com.otoki.powersales.employee.enums.EmployeeOrigin
import com.otoki.powersales.employee.enums.Gender
import com.otoki.powersales.promotion.enums.ProfessionalPromotionTeamType
import com.otoki.powersales.promotion.entity.converter.ProfessionalPromotionTeamTypeConverter
import com.otoki.powersales.user.entity.User

/**
 * 사원 Entity
 *
 * 레거시 스키마 2개 테이블에 매핑:
 * - Primary: employee (사원 마스터)
 * - Secondary: employee_info (인증/기기 정보) — @OneToOne 관계
 *
 * employee_info 조인은 employee_code = employee_code (non-PK 컬럼).
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
    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    @SFField("DKRetail__EmpCode__c")
    @HCColumn("dkretail__empcode__c")
    @Column(name = "employee_code", unique = true, length = 100)
    val employeeCode: String,

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
    @Convert(converter = UserRoleConverter::class)
    @Column(name = "role", length = 50)
    var role: UserRoleEnum? = null,

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
    var phone: String? = null,

    @SFField("DKRetail__HomePhone__c")
    @HCColumn("dkretail__homephone__c")
    @Column(name = "home_phone", length = 255)
    var homePhone: String? = null,

    @SFField("DKRetail__WorkEmail__c")
    @Column(name = "work_email", length = 100)
    var workEmail: String? = null,

    @SFField("DKRetail__Email__c")
    @Column(name = "email", length = 100)
    var email: String? = null,

    // 외부 키는 `Sex` 이며 Salesforce/SAP 호환을 위해 유지한다 (Spec #565)
    // Spec #713: @Enumerated → GenderConverter 전환 (SF 원본값 `남`/`여` 저장 + MALE/FEMALE backward compat)
    @SFField("DKRetail__Sex__c")
    @HCColumn("dkretail__sex__c")
    @Convert(converter = GenderConverter::class)
    @Column(name = "gender", length = 10)
    var gender: Gender? = null,

    @SFField("DKRetail__StartDate__c")
    @HCColumn("dkretail__startdate__c")
    @Column(name = "start_date")
    var startDate: LocalDate? = null,

    @SFField("DKRetail__EndDate__c")
    @Column(name = "end_date")
    var endDate: LocalDate? = null,

    @SFField("AgreementFlag__c")
    @HCColumn("agreementflag__c")
    @Column(name = "agreement_flag")
    var agreementFlag: Boolean? = null,

    @SFField("IsDeleted")
    @HCColumn("isdeleted")
    @Column(name = "is_deleted")
    val isDeleted: Boolean? = null,

    @SFField("ProfessionalPromotionTeam__c")
    @Convert(converter = ProfessionalPromotionTeamTypeConverter::class)
    @Column(name = "professional_promotion_team", length = 50)
    var professionalPromotionTeam: ProfessionalPromotionTeamType? = null,

    @SFField("DKRetail__Jikchak__c")
    @Column(name = "jikchak", length = 100)
    var jikchak: String? = null,

    @SFField("DKRetail__Jikwee__c")
    @Column(name = "jikwee", length = 40)
    var jikwee: String? = null,

    @SFField("DKRetail__Jikgub__c")
    @Column(name = "jikgub", length = 40)
    var jikgub: String? = null,

    @SFField("DKRetail__WorkType__c")
    @Column(name = "work_type", length = 40)
    var workType: String? = null,

    @SFField("DKRetail__JobCode__c")
    @Column(name = "job_code", length = 40)
    var jobCode: String? = null,

    @SFField("DKRetail__WorkArea__c")
    @Column(name = "work_area", length = 100)
    var workArea: String? = null,

    @SFField("DKRetail__Jikjong__c")
    @Column(name = "jikjong", length = 40)
    var jikjong: String? = null,

    @SFField("DKRetail__AppointmentDate__c")
    @Column(name = "appointment_date")
    var appointmentDate: LocalDate? = null,

    @SFField("OrdDetailNode__c")
    @Column(name = "ord_detail_node", length = 255)
    var ordDetailNode: String? = null,

    @SFField("DKRetail__CRM_WorkStartDate__c")
    @Column(name = "crm_work_start_date")
    var crmWorkStartDate: LocalDate? = null,

    // -- Spec #607: SF 누락 컬럼 9개 신규 도입 --

    @SFField("DKRetail__CostCenterCode__c")
    @Column(name = "dk_cost_center_code", length = 3)
    var dkCostCenterCode: String? = null,

    @SFField("DKRetail__LocationCode__c")
    @Column(name = "location_code", length = 100)
    var locationCode: String? = null,

    @SFField("DKRetail__TotalAnnualLeave__c")
    @Column(name = "total_annual_leave", precision = 18, scale = 0)
    var totalAnnualLeave: java.math.BigDecimal? = null,

    @SFField("DKRetail__UsedAnnualLeave__c")
    @Column(name = "used_annual_leave", precision = 18, scale = 0)
    var usedAnnualLeave: java.math.BigDecimal? = null,

    @SFField("DKRetail__ManagerId__c")
    @HCColumn("dkretail__managerid__c")
    @Column(name = "manager_sfid", length = 18)
    var managerSfid: String? = null,

    @SFField("PostponedAppointment__c")
    @HCColumn("postponedappointment__c")
    @Column(name = "postponed_appointment_sfid", length = 18)
    var postponedAppointmentSfid: String? = null,

    @SFField("LockingFlag__c")
    @Column(name = "locking_flag")
    var lockingFlag: Boolean? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "origin", length = 20)
    var origin: EmployeeOrigin? = EmployeeOrigin.SAP,

    // -- Spec #713: SF Object 정합 (Group A + Reference R-2) --

    @SFField("OfficePhone__c")
    @HCColumn("officephone__c")
    @Column(name = "office_phone", length = 40)
    var officePhone: String? = null,

    @SFField("DKRetail__CRM_WorkType__c")
    @HCColumn("dkretail__crm_worktype__c")
    @Convert(converter = CrmWorkTypeConverter::class)
    @Column(name = "crm_work_type", length = 255)
    var crmWorkType: CrmWorkType? = null,

    // OwnerId: SF `referenceTo = [Group, User]` polymorphic.
    // owner_sfid 는 SF 원본 식별자 보존 (sync buffer). owner_user_id / owner_group_id 둘 중
    // 하나만 채워지며 DB CHECK XOR 제약으로 enforce. sfid prefix `005` = User / `00G` = Group.
    @SFField("OwnerId")
    @HCColumn("ownerid")
    @Column(name = "owner_sfid", length = 18)
    var ownerSfid: String? = null,

    // CreatedById / LastModifiedById: SF `referenceTo = [User]`. FK 타입은 backend User entity.
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    var manager: Employee? = null,

    // -- Spec #738: PostponedAppointment__c reference (R-2 패턴 FK 후처리) --
    // postponed_appointment_sfid 는 V36 / #713 시점에 sfid 컨벤션으로 추가됨. 본 FK 는 그 sfid → appointment.appointment_id 매핑.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "postponed_appointment_id")
    var postponedAppointment: com.otoki.powersales.schedule.entity.Appointment? = null,

    // --- Secondary Table (employee_info) 필드: constructor param only (JPA 미매핑) ---

    password: String = "",
    passwordChangeRequired: Boolean? = true,
    deviceUuid: String? = null,
    fcmToken: String? = null,
    lastAgreementNumber: String? = null
) : BaseEntity() {

    // --- employee_info @OneToOne 관계 ---

    @OneToOne(cascade = [CascadeType.ALL], fetch = FetchType.LAZY, optional = true)
    @JoinColumn(
        name = "employee_code",
        referencedColumnName = "employee_code",
        insertable = false,
        updatable = false,
        foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    @NotFound(action = NotFoundAction.IGNORE)
    var employeeInfo: EmployeeInfo? = EmployeeInfo(
        employeeCode = employeeCode,
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
            employeeInfo = EmployeeInfo(employeeCode = employeeCode)
        }
        return employeeInfo!!
    }

    // --- Domain methods ---

    fun changePassword(newEncodedPassword: String) {
        this.password = newEncodedPassword
        this.passwordChangeRequired = false
    }

    /**
     * 운영자가 임시 비밀번호로 리셋 (Spec #582 P1-B §3.2.1).
     *
     * `changePassword` 와의 차이: 강제 변경 유도를 위해 `passwordChangeRequired = true` 로 설정한다.
     */
    fun resetPasswordToTemporary(encodedTemporaryPassword: String) {
        this.password = encodedTemporaryPassword
        this.passwordChangeRequired = true
    }

    fun requiresGpsConsent(): Boolean {
        return agreementFlag != true
    }

    fun recordGpsConsent(agreementNumber: String? = null) {
        this.agreementFlag = true
        if (agreementNumber != null) {
            this.lastAgreementNumber = agreementNumber
        }
    }

    fun bindDevice(deviceId: String) {
        this.deviceUuid = deviceId
    }

    fun resetDevice() {
        this.deviceUuid = null
    }
}
