package com.otoki.powersales.admin.repository

import com.otoki.powersales.admin.entity.RolePermission
import com.otoki.powersales.auth.entity.UserRole
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface RolePermissionRepository : JpaRepository<RolePermission, Long> {

    @Query("SELECT rp FROM RolePermission rp WHERE rp.role = :roleName")
    fun findByRoleName(@Param("roleName") roleName: String): List<RolePermission>

    @Modifying
    @Query("DELETE FROM RolePermission rp WHERE rp.role = :roleName")
    fun deleteByRoleName(@Param("roleName") roleName: String)
}

fun RolePermissionRepository.findByRole(role: UserRole): List<RolePermission> = findByRoleName(role.name)

fun RolePermissionRepository.deleteByRole(role: UserRole) = deleteByRoleName(role.name)
