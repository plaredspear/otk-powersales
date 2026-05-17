package com.otoki.powersales.user.entity

import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.user.entity.converter.ProfileTypeConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * SF User Object 매핑 Entity (Spec #757).
 *
 * SF 레거시의 표준 `User` Object 를 신규 backend `User` entity 로 직접 매핑.
 * Employee (인사 마스터) 와 별개로 인증/권한 책임을 분리한다.
 *
 * - Web 로그인 인증 대상 (Mobile 은 Employee 기반 — 비밀번호 분리)
 * - Employee 와의 매칭 키: `User.employee_code == Employee.employee_code`
 * - audit (created_by / last_modified_by / manager) 는 User → User self-reference (R-2)
 *
 * 후속 spec:
 * - #758: 12개+ R-2 entity 의 audit FK 를 Employee → User 로 일괄 전환
 * - #759: EmployeeProfileResolver + ProfileType enum + is_sales_support 산출
 * - #760: Spring Security UserDetailsService (Web 전용)
 */
@Entity
@Table(name = "\"user\"")
@SFObject("User")
class User(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    val id: Long = 0,

    @Column(name = "sfid", length = 18, unique = true)
    var sfid: String? = null,

    @SFField("Username")
    @Column(name = "username", nullable = false, unique = true, length = 80)
    var username: String,

    @SFField("Email")
    @Column(name = "email", length = 128)
    var email: String? = null,

    @SFField("IsActive")
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @SFField("DKRetail__EmployeeNumber__c")
    @Column(name = "employee_code", nullable = false, unique = true, length = 20)
    var employeeCode: String,

    @SFField("Name")
    @Column(name = "name", length = 121)
    var name: String? = null,

    @SFField("LastName")
    @Column(name = "last_name", length = 80)
    var lastName: String? = null,

    @SFField("FirstName")
    @Column(name = "first_name", length = 40)
    var firstName: String? = null,

    @SFField("Alias")
    @Column(name = "alias", length = 8)
    var alias: String? = null,

    @SFField("Title")
    @Column(name = "title", length = 80)
    var title: String? = null,

    @SFField("Department")
    @Column(name = "department", length = 80)
    var department: String? = null,

    @SFField("Division")
    @Column(name = "division", length = 80)
    var division: String? = null,

    @SFField("MobilePhone")
    @Column(name = "mobile_phone", length = 40)
    var mobilePhone: String? = null,

    @SFField("Phone")
    @Column(name = "phone", length = 40)
    var phone: String? = null,

    @SFField("HR_Code__c")
    @Column(name = "hr_code", length = 255)
    var hrCode: String? = null,

    @SFField("Branch__c")
    @Column(name = "branch", length = 255)
    var branch: String? = null,

    @SFField("LastLoginDate")
    @Column(name = "last_login_at")
    var lastLoginAt: LocalDateTime? = null,

    @SFField("ManagerId")
    @Column(name = "manager_sfid", length = 18)
    var managerSfid: String? = null,

    @SFField("ProfileId")
    @Column(name = "profile_sfid", length = 18)
    var profileSfid: String? = null,

    @SFField("UserRoleId")
    @Column(name = "user_role_sfid", length = 18)
    var userRoleSfid: String? = null,

    // -- Spec #759: EmployeeProfileResolver 산출 캐시 컬럼 --

    @Convert(converter = ProfileTypeConverter::class)
    @Column(name = "profile_type", nullable = false, length = 40)
    var profileType: ProfileType = ProfileType.STAFF,

    @Column(name = "is_sales_support")
    var isSalesSupport: Boolean? = false,

    // -- Web 인증 (Mobile 은 Employee.password 별도 운영) --

    @Column(name = "password", nullable = false, length = 255)
    var password: String,

    @Column(name = "password_change_required")
    var passwordChangeRequired: Boolean? = true,

    // -- Group A audit (self-reference R-2) --

    @SFField("CreatedById")
    @Column(name = "created_by_sfid", length = 18)
    var createdBySfid: String? = null,

    @SFField("LastModifiedById")
    @Column(name = "last_modified_by_sfid", length = 18)
    var lastModifiedBySfid: String? = null,

    @SFField("IsDeleted")
    @Column(name = "is_deleted")
    var isDeleted: Boolean? = null,

    // -- Relations (self-reference) --

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    var manager: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: User? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_id")
    var lastModifiedBy: User? = null,

    ) : BaseEntity() {

    /**
     * 비밀번호 변경 (Spec #760).
     *
     * BCrypt 해시 갱신 + `password_change_required = false` 처리.
     * 임시 비밀번호 → 자체 설정 비밀번호 전환 시 호출.
     */
    fun changePassword(encodedPassword: String) {
        this.password = encodedPassword
        this.passwordChangeRequired = false
    }

    /**
     * 마지막 로그인 시각 기록 (Spec #760).
     *
     * Web 로그인 성공 시 `last_login_at` 을 현재 UTC wall clock 으로 갱신.
     */
    fun recordLogin(at: java.time.LocalDateTime) {
        this.lastLoginAt = at
    }
}
