package com.otoki.powersales.auth.sharing.repository

import com.otoki.powersales.auth.sharing.entity.UserRoleHierarchySnapshot
import org.springframework.data.jpa.repository.JpaRepository

interface UserRoleHierarchySnapshotRepository : JpaRepository<UserRoleHierarchySnapshot, Long>
