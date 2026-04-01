package com.otoki.internal.common.entity

import com.otoki.internal.common.salesforce.HCColumn
import com.otoki.internal.common.salesforce.HCTable
import com.otoki.internal.sap.entity.EmployeeInfo
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 로그인 이력 Entity (login_history 테이블)
 */
@Entity
@Table(name = "login_history")
@HCTable("employee_his")
class LoginHistory(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "login_history_id")
    val id: Long = 0,

    @HCColumn("empcode__c")
    @Column(name = "employee_code", nullable = false, length = 80)
    val empCode: String,

    @HCColumn("inst_date")
    @Column(name = "login_at", nullable = false)
    val instDate: LocalDateTime = LocalDateTime.now(),

    // -- Relations --
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_code", insertable = false, updatable = false)
    val employeeInfo: EmployeeInfo? = null,
)
