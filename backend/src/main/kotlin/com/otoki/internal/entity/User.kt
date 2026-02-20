package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 사용자 Entity
 *
 * 레거시 스키마 2개 테이블에 매핑:
 * - Primary: dkretail__employee__c (사원 마스터)
 * - Secondary: employee_mng (인증/기기 정보)
 */
@Entity
@Table(name = "dkretail__employee__c")
@SecondaryTable(
    name = "employee_mng",
    pkJoinColumns = [PrimaryKeyJoinColumn(
        name = "empcode__c",
        referencedColumnName = "dkretail__empcode__c"
    )]
)
class User(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "sfid", length = 18)
    val sfid: String? = null,

    @Column(name = "dkretail__empcode__c", unique = true, length = 100)
    val employeeId: String,

    @Column(name = "name", length = 80)
    val name: String,

    @Column(name = "dkretail__birthdate__c", length = 10)
    val birthDate: String? = null,

    @Column(name = "dkretail__status__c", length = 40)
    val status: String? = null,

    @Column(name = "dkretail__apploginactive__c")
    val appLoginActive: Boolean? = null,

    @Column(name = "dkretail__appauthority__c", length = 255)
    val appAuthority: String? = null,

    @Column(name = "dkretail__orgname__c", length = 100)
    val orgName: String? = null,

    @Column(name = "costcentercode__c", length = 10)
    val costCenterCode: String? = null,

    @Column(name = "dkretail__workphone__c", length = 255)
    val workPhone: String? = null,

    @Column(name = "phone__c", length = 40)
    val phone: String? = null,

    @Column(name = "dkretail__homephone__c", length = 255)
    val homePhone: String? = null,

    @Column(name = "dkretail__startdate__c")
    val startDate: LocalDate? = null,

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

    // --- Secondary Table: employee_mng ---

    @Column(name = "emp_pwd", table = "employee_mng", length = 200)
    var password: String,

    @Column(name = "pwd_yn", table = "employee_mng")
    var passwordChangeRequired: Boolean? = true,

    @Column(name = "emp_uuid", table = "employee_mng", length = 200)
    var deviceUuid: String? = null,

    @Column(name = "emp_token", table = "employee_mng", length = 200)
    var fcmToken: String? = null,

    @Column(name = "gps_yn", table = "employee_mng")
    val gpsYn: Boolean? = null,

    @Column(name = "gps_yn_date", table = "employee_mng")
    val gpsYnDate: LocalDateTime? = null,

    @Column(name = "inst_date", table = "employee_mng")
    val instDate: LocalDateTime? = null,

    @Column(name = "upd_date", table = "employee_mng")
    var updDate: LocalDateTime? = null
) {

    /**
     * role은 DB에 저장하지 않고 appAuthority로부터 도출하는 computed property
     */
    val role: UserRole
        get() = when (appAuthority) {
            "조장" -> UserRole.LEADER
            "지점장" -> UserRole.ADMIN
            else -> UserRole.USER
        }

    fun changePassword(newEncodedPassword: String) {
        this.password = newEncodedPassword
        this.passwordChangeRequired = false
        this.updDate = LocalDateTime.now()
    }

    fun requiresGpsConsent(): Boolean {
        return agreementFlag != true
    }

    fun recordGpsConsent() {
        this.agreementFlag = true
        this.updDate = LocalDateTime.now()
    }

    @PreUpdate
    fun onPreUpdate() {
        this.updDate = LocalDateTime.now()
    }
}
