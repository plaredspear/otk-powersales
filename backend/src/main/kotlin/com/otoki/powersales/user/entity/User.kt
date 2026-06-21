package com.otoki.powersales.user.entity

import com.otoki.powersales.platform.common.entity.BaseEntity
import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import com.otoki.powersales.platform.common.entity.DomainName

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
 * - #759: EmployeeProfileResolver + is_sales_support 산출
 * - #760: Spring Security UserDetailsService (Web 전용)
 */
@DomainName("사용자")
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
    @Column(name = "employee_code", unique = true, length = 20)
    var employeeCode: String?,

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

    // SF Text(100) `prnflag__c` (홍보영양사여부). 데이터 보존용 — 신규 조회/로직 미사용.
    @SFField("prnflag__c")
    @Column(name = "prn_flag", length = 100)
    var prnFlag: String? = null,

    @SFField("LastLoginDate")
    @Column(name = "last_login_at")
    var lastLoginAt: LocalDateTime? = null,

    @SFField("ManagerId")
    @Column(name = "manager_sfid", length = 18)
    var managerSfid: String? = null,

    @SFField("ProfileId")
    @Column(name = "profile_sfid", length = 18)
    var profileSfid: String? = null,

    /** SF Profile lookup FK (Spec #780). Stage 2 fk substep 으로 [profileSfid] → profile_id 자동 채움 — read-only audit. */
    @Column(name = "profile_id")
    var profileId: Long? = null,

    @SFField("UserRoleId")
    @Column(name = "user_role_sfid", length = 18)
    var userRoleSfid: String? = null,

    /** SF UserRole lookup FK (Spec #780). Stage 2 fk substep 으로 [userRoleSfid] → user_role_id 자동 채움 — read-only audit. */
    @Column(name = "user_role_id")
    var userRoleId: Long? = null,

    // -- Spec #759: EmployeeProfileResolver 산출 캐시 컬럼 --

    @Column(name = "is_sales_support")
    var isSalesSupport: Boolean? = false,

    /**
     * Employee.cost_center_code derived 캐시 (Spec #759 패턴 — is_sales_support 와 동일).
     *
     * 인증/권한 hot path 에서 Employee lookup 없이 데이터 스코프 (조장 → 같은 부서 여사원 등) 판정용.
     * SoT 는 Employee.cost_center_code. 쓰기 site (SAP 발령 / 사원 마스터 upsert / 운영자 사원 수정 / 유예 발령 배치)
     * 에서 동기 갱신. 변경 빈도 매우 낮음 (사원당 1~2년/회, 대부분 SAP 사원 마스터 매일 upsert 의 no-op).
     */
    @Column(name = "cost_center_code", length = 20)
    var costCenterCode: String? = null,

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

    @CreatedBy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    var createdBy: User? = null,

    @LastModifiedBy
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
