package com.otoki.powersales.auth.sharing.repository

import com.otoki.powersales.auth.sharing.entity.ProfileFieldPermission
import org.springframework.data.jpa.repository.JpaRepository

interface ProfileFieldPermissionRepository : JpaRepository<ProfileFieldPermission, Long> {
    fun findAllByProfileIdAndSObjectName(profileId: Long, sObjectName: String): List<ProfileFieldPermission>
}
