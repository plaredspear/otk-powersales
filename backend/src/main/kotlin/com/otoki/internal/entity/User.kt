package com.otoki.internal.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 사용자 Entity
 */
@Entity
@Table(name = "users")
class User(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "employee_id", nullable = false, unique = true, length = 8)
    val employeeId: String,

    @Column(name = "password", nullable = false)
    var password: String,

    @Column(name = "name", nullable = false, length = 50)
    val name: String,

    @Column(name = "department", nullable = false, length = 50)
    val department: String,

    @Column(name = "branch_name", nullable = false, length = 50)
    val branchName: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    val role: UserRole = UserRole.USER,

    @Column(name = "password_change_required", nullable = false)
    var passwordChangeRequired: Boolean = true,

    @Column(name = "last_gps_consent_at")
    var lastGpsConsentAt: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {

    /**
     * 비밀번호 변경
     * passwordChangeRequired를 false로 설정
     */
    fun changePassword(newEncodedPassword: String) {
        this.password = newEncodedPassword
        this.passwordChangeRequired = false
        this.updatedAt = LocalDateTime.now()
    }

    /**
     * GPS 동의 필요 여부 판단
     * lastGpsConsentAt이 null(최초)이거나, 현재 시점 기준 6개월 경과 시 true
     */
    fun requiresGpsConsent(): Boolean {
        val consentAt = lastGpsConsentAt ?: return true
        return consentAt.plusMonths(6).isBefore(LocalDateTime.now())
    }

    /**
     * GPS 동의 기록
     */
    fun recordGpsConsent() {
        this.lastGpsConsentAt = LocalDateTime.now()
        this.updatedAt = LocalDateTime.now()
    }

    @PreUpdate
    fun onPreUpdate() {
        this.updatedAt = LocalDateTime.now()
    }
}
