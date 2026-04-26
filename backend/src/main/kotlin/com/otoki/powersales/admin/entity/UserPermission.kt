package com.otoki.powersales.admin.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "user_permission")
class UserPermission(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_permission_id")
    val id: Long = 0,

    @Column(name = "employee_id", nullable = false)
    val employeeId: Long,

    @Column(name = "permission", nullable = false, length = 50)
    val permission: String,

    @Column(name = "granted_by", nullable = false)
    val grantedBy: Long,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
