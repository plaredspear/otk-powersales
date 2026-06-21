package com.otoki.powersales.platform.common.entity

import com.otoki.powersales.platform.common.salesforce.HCColumn
import com.otoki.powersales.platform.common.salesforce.HerokuOnly
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 로그인 이력 Entity (login_history 테이블)
 */
@DomainName("로그인이력")
@Entity
@Table(name = "login_history")
@HerokuOnly("employee_his")
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
)
