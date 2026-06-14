package com.otoki.powersales.domain.org.employee.entity

import com.otoki.powersales.domain.org.employee.entity.converter.CrmWorkTypeConverter
import com.otoki.powersales.domain.org.employee.entity.converter.GenderConverter
import com.otoki.powersales.domain.org.employee.enums.CrmWorkType
import com.otoki.powersales.domain.org.employee.enums.EmployeeOrigin
import com.otoki.powersales.domain.org.employee.enums.Gender
import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import com.otoki.powersales.domain.activity.promotion.enums.ProfessionalPromotionTeamType
import com.otoki.powersales.domain.activity.promotion.entity.converter.ProfessionalPromotionTeamTypeConverter
import com.otoki.powersales.user.entity.User
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import com.otoki.powersales.platform.common.entity.OwnerUserDefaultListener
import com.otoki.powersales.domain.activity.schedule.entity.Appointment
import java.math.BigDecimal

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
@EntityListeners(OwnerUserDefaultListener::class)
@Entity
@Table(name = "employee")
@SFObject("DKRetail__Employee__c")
class Employee(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    val sfid: String? = null,

    // 사번(DKRetail__EmpCode__c) — SF 에서 blank 인 사원(외부 위탁 진열사원 등)이 존재하여 nullable.
    // 마이그레이션 전량 적재 정합 (common.kts employee_code nullable). plain UNIQUE 라 NULL 다중 허용.
    // 사번 없는 사원은 앱 로그인 대상이 아니므로 employeeInfo 가 null 이어도 도메인상 정상.
    @SFField("DKRetail__EmpCode__c")
    @Column(name = "employee_code", unique = true, length = 100)
    val employeeCode: String?,

    @SFField("Name")
    @Column(name = "name", length = 80)
    var name: String,

    @SFField("DKRetail__Birthdate__c")
    @Column(name = "birth_date", length = 10)
    var birthDate: String? = null,

    @SFField("DKRetail__Status__c")
    @Column(name = "status", length = 40)
    var status: String? = null,

    @SFField("DKRetail__APPLoginActive__c")
    @Column(name = "app_login_active")
    var appLoginActive: Boolean? = null,

    /**
     * SF `DKRetail__AppAuthority__c` picklist 4종 (조장 / 여사원 / 지점장 / AccountViewAll) raw value.
     *
     * 운영 분기는 [AppAuthority] 상수 비교 (`employee.role == AppAuthority.WOMAN` 등).
     * SAP 발령 → role 변환은 [AppAuthorityMapper.fromSapCodes] 사용.
     */
    @SFField("DKRetail__AppAuthority__c")
    @Column(name = "role", length = 50)
    var role: String? = null,

    @SFField("DKRetail__OrgName__c")
    @Column(name = "org_name", length = 100)
    var orgName: String? = null,

    @SFField("CostCenterCode__c")
    @Column(name = "cost_center_code", length = 10)
    var costCenterCode: String? = null,

    @SFField("DKRetail__WorkPhone__c")
    @Column(name = "work_phone", length = 255)
    var workPhone: String? = null,

    @SFField("Phone__c")
    @Column(name = "phone", length = 40)
    var phone: String? = null,

    @SFField("DKRetail__HomePhone__c")
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
    @Convert(converter = GenderConverter::class)
    @Column(name = "gender", length = 10)
    var gender: Gender? = null,

    @SFField("DKRetail__StartDate__c")
    @Column(name = "start_date")
    var startDate: LocalDate? = null,

    @SFField("DKRetail__EndDate__c")
    @Column(name = "end_date")
    var endDate: LocalDate? = null,

    @SFField("AgreementFlag__c")
    @Column(name = "agreement_flag")
    var agreementFlag: Boolean? = null,

    @SFField("IsDeleted")
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
    var totalAnnualLeave: BigDecimal? = null,

    @SFField("DKRetail__UsedAnnualLeave__c")
    @Column(name = "used_annual_leave", precision = 18, scale = 0)
    var usedAnnualLeave: BigDecimal? = null,

    @SFField("DKRetail__ManagerId__c")
    @Column(name = "manager_sfid", length = 18)
    var managerSfid: String? = null,

    @SFField("PostponedAppointment__c")
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
    @Column(name = "office_phone", length = 40)
    var officePhone: String? = null,

    @SFField("DKRetail__CRM_WorkType__c")
    @Convert(converter = CrmWorkTypeConverter::class)
    @Column(name = "crm_work_type", length = 255)
    var crmWorkType: CrmWorkType? = null,

    // OwnerId: SF `referenceTo = [Group, User]` polymorphic.
    // owner_sfid 는 SF 원본 식별자 보존 (sync buffer). owner_user_id / owner_group_id 둘 중
    // 하나만 채워지며 DB CHECK XOR 제약으로 enforce. sfid prefix `005` = User / `00G` = Group.
    @SFField("OwnerId")
    @Column(name = "owner_sfid", length = 18)
    var ownerSfid: String? = null,

    // CreatedById / LastModifiedById: SF `referenceTo = [User]`. FK 타입은 backend User entity.
    @SFField("CreatedById")
    @Column(name = "created_by_sfid", length = 18)
    var createdBySfid: String? = null,

    @SFField("LastModifiedById")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

    // -- Relations --

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    var manager: Employee? = null,

    // -- Spec #738: PostponedAppointment__c reference (R-2 패턴 FK 후처리) --
    // postponed_appointment_sfid 는 V36 / #713 시점에 sfid 컨벤션으로 추가됨. 본 FK 는 그 sfid → appointment.appointment_id 매핑.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "postponed_appointment_id")
    var postponedAppointment: Appointment? = null,

    // --- Secondary Table (employee_info) 필드: constructor param only (JPA 미매핑) ---

    password: String = "",
    passwordChangeRequired: Boolean? = true,
    deviceUuid: String? = null,
    fcmToken: String? = null,
    lastAgreementNumber: String? = null
) : BaseEntity() {

    // --- employee_info @OneToOne 관계 (PK 공유) ---
    // employee_info.employee_id = employee.employee_id 공유 PK 1:1. EmployeeInfo 가 owning side(@MapsId).
    // 영속 시 부모(this) PK 가 자식 PK 로 전파된다.
    // 본 필드는 inverse(non-owning) side — JPA 명세상 fetch=LAZY 는 hint 라 표준 환경에선 EAGER 로
    // fallback 될 수 있으나(IDE 경고), 본 프로젝트는 hibernate bytecode enhancement
    // (enableLazyInitialization, build.gradle.kts)가 켜져 있어 실제로 LAZY 로 동작한다
    // (EmployeeInfoSharedPkTest 가 isLoaded==false 로 검증). 제거하면 EAGER fallback → N+1. 유지 필수.

    @OneToOne(mappedBy = "employee", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    // employeeCode 가 null(사번 미보유 사원)이면 인증/디바이스 정보 대상이 아니므로 EmployeeInfo 를 만들지 않는다.
    var employeeInfo: EmployeeInfo? = employeeCode?.let {
        EmployeeInfo(
            employee = this,
            employeeCode = it,
            password = password,
            passwordChangeRequired = passwordChangeRequired,
            deviceUuid = deviceUuid,
            fcmToken = fcmToken,
            lastAgreementNumber = lastAgreementNumber
        )
    }

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

    // 사용자별 "현재 사용 중인 앱 버전" 스냅샷 (read-only 노출 — 갱신은 recordAppVersion 으로만).
    val appVersionName: String?
        get() = employeeInfo?.appVersionName

    val appVersionCode: Long?
        get() = employeeInfo?.appVersionCode

    val appPlatform: String?
        get() = employeeInfo?.appPlatform

    val appVersionSeenAt: LocalDateTime?
        get() = employeeInfo?.appVersionSeenAt

    private fun ensureEmployeeInfo(): EmployeeInfo {
        if (employeeInfo == null) {
            // 사번 미보유 사원은 인증/디바이스 정보 대상이 아니므로 EmployeeInfo 를 가질 수 없다.
            // EmployeeInfo.employeeId 는 @MapsId 로 영속 시 employee.id 가 자식 PK 로 전파된다.
            val code = employeeCode
                ?: throw IllegalStateException("사번(employee_code) 미보유 사원은 EmployeeInfo(인증/디바이스 정보)를 가질 수 없습니다")
            employeeInfo = EmployeeInfo(employee = this, employeeCode = code)
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

    /**
     * 클라이언트가 보고한 "현재 사용 중인 앱 버전" 스냅샷 갱신 (로그인/토큰 리프레시 시점).
     *
     * 현재값만 유지하며 이력은 남기지 않는다. 3개 식별 필드가 모두 null 이면(구버전 클라이언트
     * 미보고) 기존 값을 보존하기 위해 아무 것도 갱신하지 않는다.
     */
    fun recordAppVersion(versionName: String?, versionCode: Long?, platform: String?, seenAt: LocalDateTime) {
        if (versionName == null && versionCode == null && platform == null) return
        val info = ensureEmployeeInfo()
        info.appVersionName = versionName
        info.appVersionCode = versionCode
        info.appPlatform = platform
        info.appVersionSeenAt = seenAt
    }

    /**
     * 만나이 (SF `Age__c` 계산식 정합).
     *
     * SF formula: `FLOOR((TODAY() - DATEVALUE(Birthdate)) / 365.2425) + '살'`.
     * `birthDate` 는 `yyyy-MM-dd` 문자열. 미지정/파싱 불가 시 null.
     * SF 의 `여사원` 게이트는 호출 endpoint (role=WOMAN) 가 이미 한정.
     */
    fun calculateAge(today: LocalDate): String? {
        val birth = birthDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
        val days = ChronoUnit.DAYS.between(birth, today)
        if (days < 0) return null
        return "${Math.floor(days / 365.2425).toLong()}살"
    }

    /**
     * 근속년수 (SF `yearsOfService__c` 계산식 정합).
     *
     * SF formula: `FLOOR((TODAY() - StartDate) / 365) + '년'` (만나이와 달리 윤년 미보정 — 365 고정).
     * `startDate` 미지정 시 null.
     */
    fun calculateYearsOfService(today: LocalDate): String? {
        val start = startDate ?: return null
        val days = ChronoUnit.DAYS.between(start, today)
        if (days < 0) return null
        return "${days / 365}년"
    }
}
