package com.otoki.internal.common.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 사용자 Entity
 *
 * 레거시 스키마 2개 테이블에 매핑:
 * - Primary: dkretail__employee__c (사원 마스터)
 * - Secondary: employee_mng (인증/기기 정보) — @OneToOne 관계
 *
 * employee_mng 조인은 empcode__c = dkretail__empcode__c (non-PK 컬럼).
 * JPA @SecondaryTable은 PK 기반 조인만 지원하므로, @OneToOne + delegate property로 구현.
 */
@Entity
@Table(name = "dkretail__employee__c")
class User(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @Column(name = "dkretail__empcode__c", unique = true, length = 100)
    val employeeId: String,

    @Column(name = "name", length = 80)
    var name: String,

    @Column(name = "dkretail__birthdate__c", length = 10)
    var birthDate: String? = null,

    @Column(name = "dkretail__status__c", length = 40)
    var status: String? = null,

    @Column(name = "dkretail__apploginactive__c")
    var appLoginActive: Boolean? = null,

    @Column(name = "dkretail__appauthority__c", length = 255)
    val appAuthority: String? = null,

    @Column(name = "dkretail__orgname__c", length = 100)
    val orgName: String? = null,

    @Column(name = "costcentercode__c", length = 10)
    var costCenterCode: String? = null,

    @Column(name = "dkretail__workphone__c", length = 255)
    var workPhone: String? = null,

    @Column(name = "phone__c", length = 40)
    val phone: String? = null,

    @Column(name = "dkretail__homephone__c", length = 255)
    var homePhone: String? = null,

    @Column(name = "dkretail__startdate__c")
    var startDate: LocalDate? = null,

    @Column(name = "agreementflag__c")
    var agreementFlag: Boolean? = null,

    @Column(name = "isdeleted")
    val isDeleted: Boolean? = null,

    @Column(name = "systemmodstamp")
    val systemModStamp: LocalDateTime? = null,

    @Column(name = "createddate")
    val createdDate: LocalDateTime? = null,

    @Column(name = "_hc_lastop", length = 32)
    val hcLastOp: String? = null,

    @Column(name = "_hc_err", columnDefinition = "TEXT")
    val hcErr: String? = null,

    // --- Secondary Table (employee_mng) 필드: constructor param only (JPA 미매핑) ---

    password: String = "",
    passwordChangeRequired: Boolean? = true,
    deviceUuid: String? = null,
    fcmToken: String? = null,
    lastAgreementNumber: String? = null
) {

    // --- employee_mng @OneToOne 관계 ---

    @OneToOne(cascade = [CascadeType.ALL], fetch = FetchType.EAGER, optional = true)
    @JoinColumn(
        name = "dkretail__empcode__c",
        referencedColumnName = "empcode__c",
        insertable = false,
        updatable = false,
        foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT)
    )
    var employeeMng: EmployeeMng? = EmployeeMng(
        empcode = employeeId,
        password = password,
        passwordChangeRequired = passwordChangeRequired,
        deviceUuid = deviceUuid,
        fcmToken = fcmToken,
        lastAgreementNumber = lastAgreementNumber
    )

    // --- Delegate properties (기존 인터페이스 유지) ---

    var password: String
        get() = employeeMng?.password ?: ""
        set(value) { ensureEmployeeMng().password = value }

    var passwordChangeRequired: Boolean?
        get() = employeeMng?.passwordChangeRequired
        set(value) { ensureEmployeeMng().passwordChangeRequired = value }

    var deviceUuid: String?
        get() = employeeMng?.deviceUuid
        set(value) { ensureEmployeeMng().deviceUuid = value }

    var fcmToken: String?
        get() = employeeMng?.fcmToken
        set(value) { ensureEmployeeMng().fcmToken = value }

    var lastAgreementNumber: String?
        get() = employeeMng?.lastAgreementNumber
        set(value) { ensureEmployeeMng().lastAgreementNumber = value }

    val gpsYn: Boolean?
        get() = employeeMng?.gpsYn

    val gpsYnDate: LocalDateTime?
        get() = employeeMng?.gpsYnDate

    val instDate: LocalDateTime?
        get() = employeeMng?.instDate

    var updDate: LocalDateTime?
        get() = employeeMng?.updDate
        set(value) { ensureEmployeeMng().updDate = value }

    private fun ensureEmployeeMng(): EmployeeMng {
        if (employeeMng == null) {
            employeeMng = EmployeeMng(empcode = employeeId)
        }
        return employeeMng!!
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
        this.updDate = LocalDateTime.now()
    }

    fun requiresGpsConsent(): Boolean {
        return agreementFlag != true
    }

    fun recordGpsConsent(agreementNumber: String? = null) {
        this.agreementFlag = true
        if (agreementNumber != null) {
            this.lastAgreementNumber = agreementNumber
        }
        this.updDate = LocalDateTime.now()
    }

    fun bindDevice(deviceId: String) {
        this.deviceUuid = deviceId
        this.updDate = LocalDateTime.now()
    }

    fun resetDevice() {
        this.deviceUuid = null
        this.updDate = LocalDateTime.now()
    }

    @PreUpdate
    fun onPreUpdate() {
        this.updDate = LocalDateTime.now()
    }
}
