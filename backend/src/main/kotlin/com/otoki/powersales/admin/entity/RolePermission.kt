package com.otoki.powersales.admin.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "role_permission")
class RolePermission(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_permission_id")
    val id: Long = 0,

    @Column(name = "role", nullable = false, length = 50)
    val role: String,

    @Column(name = "permission", nullable = false, length = 50)
    val permission: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
