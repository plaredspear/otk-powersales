package com.otoki.powersales.employee.entity

import com.otoki.powersales.common.entity.AuditedEntity
import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HerokuOnly
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime

/**
 * 사원 인증/기기 정보 Entity (employee_info 테이블)
 *
 * Employee 와 PK 공유 1:1 관계 (employee_info.employee_id = employee.employee_id).
 * EmployeeInfo 가 owning side (@MapsId) — 영속 시 부모(employee) PK 가 자식 PK 로 전파된다.
 * employee_code 는 PK 가 아닌 HC sync 자연 키(empcode__c) 컬럼으로 잔류.
 */
@Entity
@Table(name = "employee_info")
@HerokuOnly("employee_mng")
class EmployeeInfo(

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id")
    val employee: Employee,

    @HCColumn("empcode__c")
    @Column(name = "employee_code", length = 40)
    val employeeCode: String? = null,

    @HCColumn("emp_pwd")
    @Column(name = "password", length = 200)
    var password: String? = null,

    @HCColumn("pwd_yn")
    @Column(name = "password_change_required")
    var passwordChangeRequired: Boolean? = true,

    @HCColumn("emp_uuid")
    @Column(name = "device_uuid", length = 200)
    var deviceUuid: String? = null,

    @HCColumn("emp_token")
    @Column(name = "fcm_token", length = 200)
    var fcmToken: String? = null,

    @HCColumn("gps_yn")
    @Column(name = "gps_consent")
    val gpsYn: Boolean? = null,

    @HCColumn("gps_yn_date")
    @Column(name = "gps_consent_date")
    val gpsYnDate: LocalDateTime? = null,

    @Column(name = "last_agreement_number", length = 80)
    var lastAgreementNumber: String? = null,

    // --- 사용자별 "현재 사용 중인 앱 버전" 스냅샷 (백엔드 전용, HC sync 대상 아님) ---
    // 로그인/토큰 리프레시 때 클라이언트가 보고한 값으로 덮어쓴다(현재값만 유지, 이력 X).
    // 웹 관리자(사원 상세 > 앱 설정)에서 사용 버전 분포 파악 용도.

    @Column(name = "app_version_name", length = 40)
    var appVersionName: String? = null,

    @Column(name = "app_version_code")
    var appVersionCode: Long? = null,

    @Column(name = "app_platform", length = 20)
    var appPlatform: String? = null,

    @Column(name = "app_version_seen_at")
    var appVersionSeenAt: LocalDateTime? = null,

    /**
     * Heroku 원본 생성/수정 시각.
     *
     * BaseEntity 의 createdAt/updatedAt 은 SF 표준 컬럼명(`createddate`/`lastmodifieddate`)으로
     * `@HCColumn` 이 고정돼 있어 Heroku `employee_mng` 의 `inst_date`/`upd_date` 와 매핑되지 않는다.
     * BaseEntity 수정은 다른 엔티티에 영향을 주고, BaseEntity 상속 후 생성자 override 는
     * QueryDSL APT 가 부모·자식 양쪽에 path 를 생성해 충돌한다. 따라서 EducationPost / Tmp* 와 동일하게
     * audit 필드를 직접 소유하는 AuditedEntity 패턴을 사용해 `@HCColumn` 을 자유롭게 재지정한다.
     */
    @HCColumn("inst_date")
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @HCColumn("upd_date")
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) : AuditedEntity() {

    @Id
    @Column(name = "employee_id")
    var employeeId: Long = 0
}
