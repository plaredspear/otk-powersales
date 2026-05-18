package com.otoki.powersales.admin.repository

import com.otoki.powersales.admin.entity.RolePermission
import com.otoki.powersales.auth.entity.UserRole
import org.springframework.data.jpa.repository.JpaRepository

interface RolePermissionRepository :
    JpaRepository<RolePermission, Long>,
    RolePermissionRepositoryCustom

fun RolePermissionRepository.findByRole(role: UserRole): List<RolePermission> = findByRoleName(role.name)

fun RolePermissionRepository.deleteByRole(role: UserRole) = deleteByRoleName(role.name)
