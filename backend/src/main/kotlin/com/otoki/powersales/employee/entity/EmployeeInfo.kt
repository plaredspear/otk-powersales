package com.otoki.powersales.employee.entity

import com.otoki.powersales.common.entity.BaseEntity
import com.otoki.powersales.common.salesforce.HCColumn
import com.otoki.powersales.common.salesforce.HerokuOnly
import jakarta.persistence.*
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
    var lastAgreementNumber: String? = null
) : BaseEntity() {

    @Id
    @Column(name = "employee_id")
    var employeeId: Long = 0
}
