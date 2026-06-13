package com.otoki.powersales.platform.auth.sharing.repository

import com.otoki.powersales.platform.auth.sharing.entity.UserRoleHierarchySnapshot
import org.springframework.data.jpa.repository.JpaRepository

interface UserRoleHierarchySnapshotRepository : JpaRepository<UserRoleHierarchySnapshot, Long>
